/* ============================================================
   n-ticket 대기열 + 좌석 선택 — 인트로에서 넘어온 반응속도로 시작한다.

   순번을 정하는 상수는 i-ticket-new 와 같은 값을 쓴다. 대기 시간이 랭킹의 일부라
   연습 종류마다 다르면 기록을 나란히 비교할 수 없기 때문이다.

   진행률과 단계 판정은 실물 tickets.interpark.com/waiting 의 로직 그대로다.
       barPercent = (firstRank - rank) / firstRank * 100
       isAlmost   = barPercent > 90     → 포인트색이 빨강으로 바뀐다

   좌석 측정은 대기열이 끝나는 순간부터 시작한다(보안문자 입력 시간 포함).
   서버가 (지금 - startAt - 카운트다운 5초) 와 total_duration_ms 를 2초 오차로 대조하므로,
   반응·대기·좌석 사이에 재지 않는 구간이 생기면 기록이 거부된다.

   좌석 화면의 동작은 실물 onestop/seat 에 직접 들어가 하나씩 눌러보고 맞췄다.
   자세한 실측값은 docs/nol-ticketing-ui.md 참고.
   ============================================================ */
import { authFetch, showAlert } from '/js/common.js';

const INTRO_URL = '/practice/n-ticket/intro';

const QUEUE = { MIN: 5000, MAX: 200000, DEQ: 20000, MAX_REACTION: 3000 };
const ALMOST_PERCENT = 90;
const AWAITERS_BEHIND = 15320;
const LOADING_MS = 800;
const TICK_MS = 200;

const SEAT_LIMIT_MS = 10 * 60 * 1000;
const MAX_PICK = 4;

const reaction = parseInt(sessionStorage.getItem('pkt.reactionTimeMs') || '0', 10);
const sessionId = sessionStorage.getItem('pkt.sessionId') || '';
if (!reaction) {
    window.location.replace(INTRO_URL);
}

const $ = id => document.getElementById(id);
const now = () => performance.now();
const fmt = ms => (ms / 1000).toFixed(2);
const won = n => n.toLocaleString('ko-KR') + '원';

/* decayStart 는 좌석이 팔리기 시작하는 시점이다. 대기열이 시작된 순간으로 잡아야
   대기열에서 보낸 시간만큼 이미 팔려 있어 "늦게 들어오면 자리가 없다"가 따로 계산 없이 성립한다. */
const T = { queue: 0, seatStart: 0, seat: 0, firstRank: 0, decayStart: 0 };

function showScreen(id) {
    document.querySelectorAll('.nt-screen').forEach(s => s.classList.toggle('is-active', s.id === id));
}

/* ═══════════ 대기열 ═══════════ */

function phase(state) {
    if (state.isDone) return ['회원님의 순서입니다.', '예매를 진행해주세요'];
    if (state.isAlmost) return ['곧 회원님의 순서가 다가옵니다.', '예매를 준비해주세요.'];
    return ['접속 인원이 많아 대기 중입니다.', '조금만 기다려주세요.'];
}

function startQueue() {
    const step = Math.min(Math.floor(Math.min(reaction, QUEUE.MAX_REACTION) / 100), 30);
    const firstRank = Math.round(QUEUE.MIN + (step / 30) * (QUEUE.MAX - QUEUE.MIN));
    T.firstRank = firstRank;

    const rootEl = $('waitRoot');
    const rankEl = $('queueRank');
    const barEl = $('queueBar');
    const fillEl = $('queueFill');
    const totalEl = $('queueTotal');
    const rateEl = $('bookingRate');
    const line1El = $('waitLine1');
    const line2El = $('waitLine2');

    const startedAt = now();
    T.decayStart = startedAt;

    const render = () => {
        const elapsedSec = (now() - startedAt) / 1000;
        const rank = Math.max(0, Math.floor(firstRank - elapsedSec * QUEUE.DEQ));

        const barPercent = rank <= 0 ? 100 : (firstRank - rank) / firstRank * 100;
        const isDone = barPercent === 100;
        const isAlmost = barPercent > ALMOST_PERCENT;

        rankEl.textContent = rank.toLocaleString('ko-KR');
        fillEl.style.width = barPercent + '%';
        rootEl.classList.toggle('is-almost', isAlmost);
        barEl.classList.toggle('is-done', isDone);

        totalEl.textContent = (rank + AWAITERS_BEHIND).toLocaleString('ko-KR') + '명';

        const rate = Math.min(100, Math.round(barPercent * 0.96));
        rateEl.textContent = rate + '%';
        rateEl.dataset.rate = rate;

        const [l1, l2] = phase({ isDone, isAlmost });
        line1El.textContent = l1;
        line2El.textContent = l2;

        if (isDone) {
            clearInterval(timer);
            T.queue = now() - startedAt;
            enterSeat();
        }
    };

    render();
    const timer = setInterval(render, TICK_MS);
}

/* ═══════════ 좌석 도면 ═══════════ */

const VB = { w: 741, h: 612 };

const GRADE = {
    R: { name: 'R석', price: 165000, color: '#7a68a8' },
    S: { name: 'S석', price: 154000, color: '#4f8a70' },
};

const SOLD_FILL = '#edeff3';
const PLAN = { yT: 92, t: 52, Ro: 70, pitch: 100, botTarget: 120, gap: 4 };

/* 링은 한 겹이다. 실물은 3층까지 있지만 좌석 간격을 실물과 같은 3 으로 두면
   겹이 늘어날수록 좌석 수가 감당이 안 된다 */
const RINGS = [
    { xL: 116, xR: 625, yB: 528, base: 101, grade: 'S' },
];

/* 블록 폭은 3열 기준으로 고정한다. 열을 줄이면 블록이 넓어지는 게 아니라 플로어가 좁아져야 한다 */
const FLOOR = { x: [200, 541], y: [92, 420], cols: 2, widthCols: 3, rows: 4, gap: 8, grade: 'R' };

const rad = deg => deg * Math.PI / 180;
const px = (cx, cy, r, deg) => [cx + r * Math.cos(rad(deg)), cy + r * Math.sin(rad(deg))];

/* 위에서부터 같은 간격으로 자르고 남는 길이는 마지막 조각이 갖는다.
   링이 달라도 앞쪽 줄들의 y 가 그대로 맞는 이유가 이것이다. */
function slicePitch(a, b, pitch, gap) {
    const n = Math.max(1, Math.round((b - a) / pitch));
    return Array.from({ length: n }, (_, i) => {
        const s = a + i * pitch;
        return [s, i === n - 1 ? b : s + pitch - gap];
    });
}

function sliceEven(a, b, target, gap) {
    const n = Math.max(1, Math.round((b - a) / target));
    const w = (b - a - gap * (n - 1)) / n;
    return Array.from({ length: n }, (_, i) => [a + i * (w + gap), a + i * (w + gap) + w]);
}

/* 무대를 둘러싸는 말굽 + 플로어. 좌표를 손으로 찍지 않고 생성하므로
   중심선 기준 좌우 대칭이 자동으로 맞는다. */
function buildBlocks() {
    const out = [];
    const { yT, t, Ro, pitch, botTarget, gap } = PLAN;
    const F = FLOOR;

    const fw = (F.x[1] - F.x[0] - F.gap * (F.widthCols - 1)) / F.widthCols;
    const fh = (F.y[1] - F.y[0] - F.gap * (F.rows - 1)) / F.rows;
    const fx = F.x[0] + (F.x[1] - F.x[0] - (F.cols * fw + F.gap * (F.cols - 1))) / 2;
    for (let r = 0; r < F.rows; r++) for (let c = 0; c < F.cols; c++) {
        out.push({
            id: 'F' + (r * F.cols + c + 1), grade: F.grade, kind: 'rect',
            x: fx + c * (fw + F.gap), y: F.y[0] + r * (fh + F.gap), w: fw, h: fh, vertical: false,
        });
    }

    for (const { xL, xR, yB, base, grade } of RINGS) {
        const colEnd = yB - Ro;
        const cols = slicePitch(yT, colEnd - gap, pitch, gap);
        const rows = sliceEven(xL + Ro + gap, xR - Ro - gap, botTarget, gap);
        let n = base;

        for (const [y0, y1] of cols)
            out.push({ id: n++, grade, kind: 'rect', x: xR - t, y: y0, w: t, h: y1 - y0, vertical: true });

        out.push({ id: n++, grade, kind: 'corner', cx: xR - Ro, cy: yB - Ro, r0: Ro - t, r1: Ro, a0: 0, a1: 90 });

        for (const [x0, x1] of rows.slice().reverse())
            out.push({ id: n++, grade, kind: 'rect', x: x0, y: yB - t, w: x1 - x0, h: t, vertical: false });

        out.push({ id: n++, grade, kind: 'corner', cx: xL + Ro, cy: yB - Ro, r0: Ro - t, r1: Ro, a0: 90, a1: 180 });

        for (const [y0, y1] of cols.slice().reverse())
            out.push({ id: n++, grade, kind: 'rect', x: xL, y: y0, w: t, h: y1 - y0, vertical: true });
    }
    return out;
}

function label(x, y, text, rot, size = 13) {
    const r = rot ? ` transform="rotate(${rot} ${x.toFixed(1)} ${y.toFixed(1)})"` : '';
    return `<text x="${x.toFixed(1)}" y="${y.toFixed(1)}" text-anchor="middle" dominant-baseline="central"`
        + ` fill="#fff" font-size="${size}" font-weight="700" pointer-events="none"${r}>${text}</text>`;
}

/* 모서리는 바깥·안쪽이 같은 중심의 호라서 두께가 일정하게 유지되고,
   위쪽 변은 세로 열 폭과 옆쪽 변은 가로 행 높이와 그대로 맞물린다. */
function cornerPath(b) {
    const [ox, oy] = px(b.cx, b.cy, b.r1, b.a0);
    const [ox2, oy2] = px(b.cx, b.cy, b.r1, b.a1);
    const [ix2, iy2] = px(b.cx, b.cy, b.r0, b.a1);
    const [ix, iy] = px(b.cx, b.cy, b.r0, b.a0);
    const f = v => v.toFixed(1);
    return `M ${f(ox)} ${f(oy)} A ${b.r1} ${b.r1} 0 0 1 ${f(ox2)} ${f(oy2)}`
        + ` L ${f(ix2)} ${f(iy2)} A ${b.r0} ${b.r0} 0 0 0 ${f(ix)} ${f(iy)} Z`;
}

/* 실물의 ① 공연장 도면은 확대해도 사라지지 않는다. 회색 바탕에 구역 자리를 흰색으로 칠하고
   구역 이름과 열 문자를 그려 둔 그림이라, 구역 색이 걷히면 그 아래에서 이것이 드러난다.
   우리가 이걸 안 그려서 확대하면 점만 남았다. */
function renderBase(blocks) {
    let out = `<rect width="${VB.w}" height="${VB.h}" fill="${SOLD_FILL}" id="planBase"/>`;

    for (const b of blocks) {
        out += b.kind === 'rect'
            ? `<rect x="${b.x.toFixed(1)}" y="${b.y.toFixed(1)}" width="${b.w.toFixed(1)}"`
              + ` height="${b.h.toFixed(1)}" fill="#fff"/>`
            : `<path d="${cornerPath(b)}" fill="#fff"/>`;
    }

    for (const b of blocks) {
        if (b.kind !== 'rect') continue;
        const a = seatArea(b);
        const gray = t => t.replace('fill="#fff"', 'fill="#8b8d92"');

        /* 열 문자는 좌석 한 줄에 하나씩 붙으므로 크기가 간격을 넘으면 글자끼리 겹친다 */
        const letter = SEAT.spacing * 0.6;

        if (!isRoomy(b)) {
            out += gray(label(b.x + b.w * RING.numMargin / 2, b.y + b.h / 2, b.id, -90, b.w * RING.numSize));
            let col = 0;
            for (let x = a.x0; x <= a.x1; x += SEAT.spacing) {
                col++;
                out += label(x, b.y + 3.2, rowLabel(col), -90, letter)
                    .replace('fill="#fff"', 'fill="#a8aab0"');
            }
            continue;
        }

        out += gray(label(b.x + b.w / 2, b.y + b.h * 0.17, b.id, 0, b.h * 0.24));
        let row = 0;
        for (let y = a.y0; y <= a.y1; y += SEAT.spacing) {
            row++;
            out += label(b.x + 4.5, y, rowLabel(row), 0, letter)
                .replace('fill="#fff"', 'fill="#a8aab0"');
        }
    }

    return out
        + `<rect x="265" y="22" width="211" height="52" rx="4" fill="#cfd3db"/>`
        + `<text x="370.5" y="48" text-anchor="middle" dominant-baseline="central"`
        + ` font-size="15" font-weight="800" fill="#5b5f6b" letter-spacing="3">STAGE</text>`
        + `<rect x="305" y="432" width="131" height="36" rx="4" fill="#cfd3db"/>`
        + `<text x="370.5" y="450" text-anchor="middle" dominant-baseline="central"`
        + ` font-size="12" font-weight="700" fill="#5b5f6b">CONSOLE</text>`;
}

function renderBlocks(blocks, withLabel = true) {
    return blocks.map(b => {
        const fill = GRADE[b.grade].color;
        if (b.kind === 'rect') {
            const shape = `<rect x="${b.x.toFixed(1)}" y="${b.y.toFixed(1)}" width="${b.w.toFixed(1)}"`
                + ` height="${b.h.toFixed(1)}" rx="2" fill="${fill}"/>`;
            return shape + (withLabel ? label(b.x + b.w / 2, b.y + b.h / 2, b.id, b.vertical ? -90 : 0) : '');
        }
        const mid = (b.a0 + b.a1) / 2;
        const [lx, ly] = px(b.cx, b.cy, (b.r0 + b.r1) / 2, mid);
        let rot = mid + 90;
        while (rot > 90) rot -= 180;
        return `<path d="${cornerPath(b)}" fill="${fill}"/>` + (withLabel ? label(lx, ly, b.id, rot) : '');
    }).join('');
}

/* ═══════════ 좌석 ═══════════ */

/* 간격과 반지름은 실물 값 그대로다. 대신 도면에 들어가는 구역을 줄여 좌석 수를 맞췄다(플로어 2열·링 1겹). */
const SEAT = { spacing: 3, radius: 1 };

/* 좌석은 전부 열린 채로 그리고 시간이 지나면서 닫힌다 — 잔여석을 미리 정하지 않는다.
   남은 좌석이 (1 - t/총시간)^k 로 줄어들어 처음엔 몰아치고 뒤로 갈수록 느려진다.
   실측이 아니라 목업(docs/mockups/n-ticket/seat-decay.html)으로 체감을 맞춘 값이다. */
/* random 은 자리를 안 가리고 사는 비율이다. 이게 없으면 앞 구역이 통째로 비워진 뒤에야
   뒤 구역이 팔려서, 뒤쪽에 빈자리가 흩어져 있는 실제 예매창과 달라진다. */
const SELL = { totalMs: 40000, k: 2, jitter: 400, random: .20 };

/* 무대에서 가까운 자리부터 팔린다. 흔들림을 안 섞으면 동심원으로 퍼져 부자연스럽다 */
const STAGE_AT = { x: 370.5, y: 74 };

const seats = [];

/* 실물은 넓은 플로어 블록과 좁은 링 블록을 다르게 적는다.
   플로어는 구역 이름을 격자 위에, 열 문자를 격자 왼쪽 바깥에 둔다.
   링은 폭이 좁아 왼쪽에 자리를 못 내므로 열 문자를 위쪽 가장자리에 눕혀 놓고,
   구역 번호는 세로로 돌려 격자 위에 얹는다. 좌석과 글자가 같은 기준을 써야 하므로
   영역을 여기서 한 번만 정한다. */
const isRoomy = b => b.kind === 'rect' && b.w >= 60 && b.h >= 40;

/* 링 블록은 왼쪽을 비워 구역 번호를 넣는다. 비워두지 않으면 좌석이 번호 위에 얹혀 글자가 갉인다.
   실측(2026-08-14, 104구역): 카드 폭 62.5 에 왼쪽 여백 17.8 · 오른쪽 3.4 */
const RING = { numMargin: 0.285, rightMargin: 0.054, numSize: 0.138 };

function seatArea(b) {
    const roomy = isRoomy(b);
    return {
        x0: b.x + (roomy ? 9 : b.w * RING.numMargin),
        x1: b.x + b.w - (roomy ? 4 : b.w * RING.rightMargin),
        y0: b.y + (roomy ? b.h * 0.32 : 7),
        y1: b.y + b.h - 4,
    };
}

/* 실물은 열을 알파벳으로 매긴다 (308구역 K열 15번) */
const rowLabel = n => (n <= 26 ? String.fromCharCode(64 + n)
    : String.fromCharCode(64 + Math.floor((n - 1) / 26)) + String.fromCharCode(65 + (n - 1) % 26));

function pushSeat(block, row, col, x, y, out) {
    const color = GRADE[block.grade].color;
    const i = seats.push({ block: block.id, grade: block.grade, row: rowLabel(row), col, x, y }) - 1;
    out.push(`<circle class="nt-seat-dot is-open" r="${SEAT.radius}" cx="${x.toFixed(1)}" cy="${y.toFixed(1)}"`
        + ` fill="${color}" stroke="${color}" data-i="${i}"/>`);
}

function renderSeats(blocks) {
    const out = [];
    const sp = SEAT.spacing;

    for (const b of blocks) {
        if (b.kind === 'rect') {
            const { x0, x1, y0, y1 } = seatArea(b);
            let row = 0;
            for (let y = y0; y <= y1; y += sp) {
                row++;
                let col = 0;
                for (let x = x0; x <= x1; x += sp) pushSeat(b, row, ++col, x, y, out);
            }
            continue;
        }
        /* 모서리는 격자로 깔면 블록 밖으로 튀어나온다. 호를 따라 같은 간격으로 놓는다 */
        let row = 0;
        for (let r = b.r0 + 3; r <= b.r1 - 3; r += sp) {
            row++;
            const n = Math.floor(r * rad(b.a1 - b.a0) / sp);
            for (let i = 0; i < n; i++) {
                const deg = b.a0 + (b.a1 - b.a0) * (i + 0.5) / n;
                const [x, y] = px(b.cx, b.cy, r, deg);
                pushSeat(b, row, i + 1, x, y, out);
            }
        }
    }
    return out.join('');
}

/* ═══════════ 좌석이 팔려 나간다 ═══════════ */

/* 좌석마다 순위를 매겨 두고 경과 시간으로 "지금 몇 개 닫혔나"만 구한다.
   그래야 매 틱에 손대는 원이 새로 닫힌 몇 개뿐이다 — 8,481 개를 다 훑으면 확대가 멈칫한다. */
let sellOrder = [];
let sellCursor = 0;
let soldCount = 0;

/* 첫 틱에 수천 개가 한꺼번에 닫힌다. 그때마다 찾으면 그 멈칫이 기록에 들어간다 */
let seatEls = [];

function buildSellOrder() {
    const far = Math.hypot(VB.w, VB.h);
    sellOrder = seats
        .map((s, i) => ({ i, k: Math.random() < SELL.random
            ? Math.random() * far
            : Math.hypot(s.x - STAGE_AT.x, s.y - STAGE_AT.y)
              + (s.grade === 'R' ? 0 : 40) + Math.random() * SELL.jitter }))
        .sort((a, b) => a.k - b.k)
        .map(s => s.i);
}

function soldAt(ms) {
    if (ms >= SELL.totalMs) return seats.length;
    return Math.floor(seats.length * (1 - Math.pow(1 - ms / SELL.totalMs, SELL.k)));
}

/* 이미 잡은 자리는 건너뛴다. 남이 채가는 것이지 내 것을 뺏는 게 아니다 */
function sellUpTo(target) {
    while (soldCount < target && sellCursor < sellOrder.length) {
        const i = sellOrder[sellCursor++];
        if (picked.includes(i)) continue;
        const dot = seatEls[i];
        if (dot) {
            dot.classList.replace('is-open', 'is-sold');
            dot.setAttribute('fill', SOLD_FILL);
            dot.setAttribute('stroke', SOLD_FILL);
        }
        soldCount++;
    }
}

const isSoldOut = () => sellCursor >= sellOrder.length && !picked.length;

/* 좌석은 대기열이 도는 동안 미리 다 그려둔다. 확대할 때마다 다시 그리면
   그 순간 화면이 멈칫하고, 그 멈칫이 그대로 기록에 들어간다. */
function buildPlan() {
    const blocks = buildBlocks();
    $('baseLayer').innerHTML = renderBase(blocks);
    $('seatLayer').innerHTML = renderSeats(blocks);
    seatEls = [...$('seatLayer').children];
    buildSellOrder();
    $('blockLayer').innerHTML = renderBlocks(blocks);
    $('miniLayer').innerHTML = `<rect width="${VB.w}" height="${VB.h}" fill="${SOLD_FILL}"/>`
        + renderBlocks(blocks, false);

    const used = new Set(blocks.map(b => b.grade));
    $('gradeList').innerHTML = Object.entries(GRADE)
        .filter(([k]) => used.has(k))
        .map(([, g]) => `<li><i style="background:${g.color}"></i>${g.name}<b>${won(g.price)}</b></li>`)
        .join('');
}

/* ═══════════ 확대·이동 ═══════════ */

/* 실물은 전체보기 배율에서 6 → 12 → 24 로 두 배씩 뛴다. 확대하면 좌석이 붙는다.
   전환은 CSS 가 아니라 프레임 애니메이션이다 — 실물을 재보니 약 300ms 에
   가운데가 빠른 곡선이었다(절반 시점에 정확히 절반을 지난다). */
const ZOOM = { steps: [6, 12, 24], seatFrom: 6, ms: 300 };
const view = { z: 1, x: 0, y: 0, fit: 1 };
let anim = 0;
let miniShown = false;
let miniTimer = 0;

/* 미니맵은 평소에 숨어 있다가 도면을 움직이는 동안에만 떠오른다 */
function flashMiniMap() {
    miniShown = true;
    clearTimeout(miniTimer);
    miniTimer = setTimeout(() => { miniShown = false; paintView() }, 900);
}

function fitScale() {
    const vp = $('planViewport');
    return Math.min(vp.clientWidth / VB.w, vp.clientHeight / VB.h);
}

function clamp(v, a, b) { return Math.min(b, Math.max(a, v)) }

function clampXY(z, x, y) {
    const vp = $('planViewport');
    const W = vp.clientWidth, H = vp.clientHeight;
    const mw = VB.w * z, mh = VB.h * z;
    return [
        mw <= W ? (W - mw) / 2 : clamp(x, W - mw, 0),
        mh <= H ? (H - mh) / 2 : clamp(y, H - mh, 0),
    ];
}

function paintView() {
    const vp = $('planViewport');
    const map = $('planMap');
    map.style.transform = `translate(${view.x.toFixed(1)}px, ${view.y.toFixed(1)}px) scale(${view.z})`;
    map.classList.toggle('is-seatlevel', view.z >= ZOOM.seatFrom);

    const zoomed = view.z > view.fit + 0.001;
    $('zoomOut').disabled = !zoomed;
    $('zoomIn').disabled = view.z >= ZOOM.steps[ZOOM.steps.length - 1] - 0.001;
    $('zoomFit').disabled = !zoomed;
    $('miniMap').classList.toggle('is-on', zoomed && miniShown);
    if (!zoomed) return;

    const mini = $('miniMap');
    const mv = $('miniView');
    const sx = mini.clientWidth / VB.w, sy = mini.clientHeight / VB.h;
    mv.style.left = (-view.x / view.z * sx) + 'px';
    mv.style.top = (-view.y / view.z * sy) + 'px';
    mv.style.width = Math.min(VB.w, vp.clientWidth / view.z) * sx + 'px';
    mv.style.height = Math.min(VB.h, vp.clientHeight / view.z) * sy + 'px';
}

function setView(z, x, y) {
    cancelAnimationFrame(anim);
    view.z = z;
    [view.x, view.y] = clampXY(z, x, y);
    paintView();
}

const easeInOut = p => (p < .5 ? 4 * p * p * p : 1 - Math.pow(-2 * p + 2, 3) / 2);

function animateTo(z, x, y) {
    flashMiniMap();
    const [tx, ty] = clampXY(z, x, y);
    const from = { z: view.z, x: view.x, y: view.y };
    const t0 = performance.now();
    cancelAnimationFrame(anim);

    const step = () => {
        const k = easeInOut(Math.min(1, (performance.now() - t0) / ZOOM.ms));
        view.z = from.z + (z - from.z) * k;
        view.x = from.x + (tx - from.x) * k;
        view.y = from.y + (ty - from.y) * k;
        paintView();
        if (k < 1) anim = requestAnimationFrame(step);
    };
    step();
}

/* 화면의 한 점을 도면 좌표로 되돌린다. 구역을 눌렀을 때 그 자리를 중심에 놓으려면 필요하다 */
function toMap(clientX, clientY) {
    const r = $('planViewport').getBoundingClientRect();
    return [(clientX - r.left - view.x) / view.z, (clientY - r.top - view.y) / view.z];
}

function centerOn(mx, my, z) {
    const vp = $('planViewport');
    animateTo(z, vp.clientWidth / 2 - mx * z, vp.clientHeight / 2 - my * z);
}

const zoomLevels = () => [view.fit, ...ZOOM.steps];

function levelIndex() {
    const all = zoomLevels();
    let best = 0;
    all.forEach((v, i) => { if (Math.abs(v - view.z) < Math.abs(all[best] - view.z)) best = i });
    return best;
}

/* 누른 지점을 제자리에 두고 배율만 바꾼다. 휠 확대가 커서 밑을 붙잡고 있어야 자연스럽다 */
function zoomAt(clientX, clientY, dir) {
    const all = zoomLevels();
    const z = all[clamp(levelIndex() + dir, 0, all.length - 1)];
    if (Math.abs(z - view.z) < 0.001) return;
    const [mx, my] = toMap(clientX, clientY);
    const r = $('planViewport').getBoundingClientRect();
    animateTo(z, clientX - r.left - mx * z, clientY - r.top - my * z);
}

function zoomByButton(dir) {
    const r = $('planViewport').getBoundingClientRect();
    zoomAt(r.left + r.width / 2, r.top + r.height / 2, dir);
}

/* 트랙패드는 한 번 굴려도 이벤트를 수십 개 쏟는다. 그대로 받으면 찔끔찔끔 확대된다.
   실물은 한 번 굴리면 확대 버튼 한 번과 같으므로, 전환이 끝날 때까지 다음 것을 무시한다. */
let wheelLock = 0;

function onWheel(e) {
    e.preventDefault();
    const t = performance.now();
    if (t < wheelLock) return;
    wheelLock = t + ZOOM.ms + 60;
    zoomAt(e.clientX, e.clientY, e.deltaY < 0 ? 1 : -1);
}

function resetView(animated = true) {
    view.fit = fitScale();
    const [x, y] = clampXY(view.fit, 0, 0);
    animated ? animateTo(view.fit, x, y) : setView(view.fit, x, y);
}

/* 드래그로 도면을 움직인다. 좌석을 누른 것과 구분하려고 움직인 거리를 본다 */
let drag = null;

/* 포인터를 캡처하면 그 뒤의 이벤트는 target 이 전부 뷰포트로 바뀐다.
   무엇을 눌렀는지는 누른 순간에만 알 수 있으므로 여기서 기억해 둔다.
   확대 버튼·미니맵도 뷰포트 안에 있어서, 그 위에서는 캡처하지 않아야 클릭이 살아남는다. */
function onPointerDown(e) {
    if (e.button !== 0) return;
    if (e.target.closest('button, .nt-minimap, .nt-grade-layer')) return;
    drag = { x: e.clientX, y: e.clientY, vx: view.x, vy: view.y, moved: false, target: e.target };
    $('planViewport').setPointerCapture(e.pointerId);
}

function onPointerMove(e) {
    if (!drag) return;
    const dx = e.clientX - drag.x, dy = e.clientY - drag.y;
    if (!drag.moved && Math.hypot(dx, dy) > 4) {
        drag.moved = true;
    }
    if (!drag.moved) return;
    flashMiniMap();
    setView(view.z, drag.vx + dx, drag.vy + dy);
}

function onPointerUp(e) {
    if (!drag) return;
    const { moved, target } = drag;
    drag = null;
    if (moved || !target?.closest) return;

    const seat = target.closest('.nt-seat-dot.is-open');
    if (seat) { toggleSeat(seat); return; }

    /* 구역을 누르면 그 자리를 중심에 두고 좌석이 보이는 배율까지 확대한다 */
    if (view.z < ZOOM.seatFrom && target.closest('#blockLayer')) {
        const [mx, my] = toMap(e.clientX, e.clientY);
        centerOn(mx, my, ZOOM.steps[0]);
    }
}

function onMiniJump(e) {
    const r = $('miniMap').getBoundingClientRect();
    centerOn((e.clientX - r.left) / r.width * VB.w, (e.clientY - r.top) / r.height * VB.h, view.z);
}

/* ═══════════ 좌석 선택 ═══════════ */

const picked = [];

const seatName = s => `${s.block}구역 ${s.row}열 ${s.col}번`;

function renderPicked() {
    $('pickedList').innerHTML = picked.map(i => {
        const s = seats[i], g = GRADE[s.grade];
        return `<li><i>${g.name}</i><b>${seatName(s)}</b><span>${won(g.price)}</span></li>`;
    }).join('');

    $('pickedEmpty').hidden = picked.length > 0;
    $('pickedNum').textContent = picked.length || '';
    $('clearBtn').hidden = picked.length === 0;
    $('doneBtn').disabled = picked.length === 0;
}

function toggleSeat(dot) {
    const i = Number(dot.dataset.i);
    const at = picked.indexOf(i);
    if (at >= 0) {
        picked.splice(at, 1);
        dot.classList.remove('is-picked');
    } else {
        if (picked.length >= MAX_PICK) {
            showHint(`한 번에 ${MAX_PICK}석까지 선택할 수 있습니다`);
            return;
        }
        picked.push(i);
        dot.classList.add('is-picked');
    }
    renderPicked();
}

function clearPicked() {
    picked.slice().forEach(i => {
        document.querySelector(`.nt-seat-dot[data-i="${i}"]`)?.classList.remove('is-picked');
    });
    picked.length = 0;
    renderPicked();
}

let hintTimer = null;

function showHint(msg, ms = 2000) {
    const el = $('planHint');
    el.textContent = msg;
    el.hidden = false;
    clearTimeout(hintTimer);
    hintTimer = setTimeout(() => { el.hidden = true }, ms);
}

/* ═══════════ 좌석 선택 시간 ═══════════ */

let seatTimer = null;

function startSeatTimer() {
    const el = $('seatTimer');
    const box = el.parentElement;
    const until = now() + SEAT_LIMIT_MS;

    const tick = () => {
        sellUpTo(soldAt(now() - T.decayStart));
        if (isSoldOut()) {
            clearInterval(seatTimer);
            showSoldOut();
            return;
        }

        const left = Math.max(0, until - now());
        const sec = Math.ceil(left / 1000);
        el.textContent = `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}`;
        box.classList.toggle('is-urgent', sec <= 60);

        if (left > 0) return;
        clearInterval(seatTimer);
        showAlert({ title: '시간 만료', msg: '좌석 선택 시간이 끝났습니다.\n다시 연습해주세요.' })
            .then(() => { window.location.href = INTRO_URL });
    };

    tick();
    seatTimer = setInterval(tick, 500);
}

/* ═══════════ 보안문자 ═══════════ */

/* 보안문자는 실물 이미지를 쓴다. 이미지는 static/image/practice/captcha/ 에 있고
   정답은 index.json 에 짝지어 두었다. 원본이 210×70 인데 176px 로 줄여 보여주므로
   실물과 같은 정도로 부드러워진다. */
const CAP = { count: 6, dir: '/image/practice/captcha' };

let capAnswer = '';
let capPool = [];
let capOrder = [];

async function loadCaptchaPool() {
    try {
        const res = await fetch(`${CAP.dir}/index.json`);
        capPool = await res.json();
    } catch (e) {
        console.error('[Practicket] 보안문자 목록을 불러오지 못했습니다:', e);
        capPool = [];
    }
}

/* 같은 것이 연달아 나오지 않게 한 바퀴 다 돌고 나서 다시 섞는다 */
function nextCaptcha() {
    if (!capPool.length) return null;
    if (!capOrder.length) {
        capOrder = capPool.map((_, i) => i);
        for (let i = capOrder.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [capOrder[i], capOrder[j]] = [capOrder[j], capOrder[i]];
        }
    }
    return capPool[capOrder.pop()];
}

function newCaptcha() {
    const pick = nextCaptcha();
    if (pick) {
        capAnswer = pick.a;
        $('capImage').src = `${CAP.dir}/${pick.f}`;
    }
    $('capInput').value = '';
    $('capErr').textContent = '';
    $('capSubmit').disabled = true;
}

/* 실물은 영문이 아닌 글자를 아예 못 넣게 막고 소문자는 대문자로 바꿔 넣는다.
   그리고 정확히 6자일 때만 입력완료가 켜진다. */
function onCaptchaInput() {
    const el = $('capInput');
    const raw = el.value;
    const letters = raw.replace(/[^a-zA-Z]/g, '');
    el.value = letters.toUpperCase();
    $('capErr').textContent = letters.length !== raw.length ? '영문 글자만 입력 가능합니다.' : '';
    $('capSubmit').disabled = el.value.length !== CAP.count;
}

/* 틀려도 이미지를 새로 뽑지 않고 입력값도 지우지 않는다. 실물이 그렇다. */
function submitCaptcha() {
    if ($('capSubmit').disabled) return;
    if ($('capInput').value !== capAnswer) {
        $('capErr').textContent = '입력한 문자를 다시 확인해주세요';
        return;
    }
    $('capModal').hidden = true;
    $('capDim').hidden = true;
    showHint('원하는 좌석을 직접 선택해주세요.', 2600);
}

/* ═══════════ 화면 전환 ═══════════ */

function enterSeat() {
    T.seatStart = now();
    showScreen('screen-seat');
    resetView(false);
    startSeatTimer();
    newCaptcha();
    $('capDim').hidden = false;
    $('capModal').hidden = false;
    $('capInput').focus();
}

/* 매진은 실패라 기록을 보내지 않는다. 막대에 좌석 구간까지 넣어야
   시간을 어디서 다 썼는지가 드러난다 — i-ticket 과 같은 모달이다. */
function showSoldOut() {
    const reactionMs = Math.round(reaction);
    const queueMs = Math.round(T.queue);
    const seatMs = Math.round(now() - T.seatStart);
    const total = reactionMs + queueMs + seatMs;

    $('pkt-fail-meta').textContent =
        `N-Ticket · 대기 순번 ${T.firstRank.toLocaleString('ko-KR')}번에서 출발`;
    $('pkt-fail-msg').textContent =
        `좌석이 다 팔리기까지 ${SELL.totalMs / 1000}초, 여기까지 ${fmt(total)}초 걸렸어요`;

    $('pkt-fail-reaction').textContent = fmt(reactionMs) + '초';
    $('pkt-fail-queue').textContent = fmt(queueMs) + '초';
    $('pkt-fail-seat').textContent = fmt(seatMs) + '초';
    $('pkt-fail-total').textContent = fmt(total) + '초';

    $('pkt-fail-stack').innerHTML = [reactionMs, queueMs, seatMs].map((ms, i) => {
        const pct = ms / total * 100;
        return `<i class="pkt-seg${i + 1}" style="width:${pct.toFixed(1)}%">`
            + `${pct >= 12 ? (ms / 1000).toFixed(1) : ''}</i>`;
    }).join('');

    const worst = [[reactionMs, '반응 속도'], [queueMs, '대기열'], [seatMs, '좌석 화면']]
        .sort((a, b) => b[0] - a[0])[0];
    $('pkt-fail-hint').innerHTML =
        `가장 오래 걸린 구간은 <b>${worst[1]} ${(worst[0] / 1000).toFixed(1)}초</b>예요.`;

    $('pkt-soldout-overlay').classList.add('visible');
    sessionStorage.removeItem('pkt.sessionId');
}

async function finish() {
    if (!picked.length) return;
    clearInterval(seatTimer);
    T.seat = now() - T.seatStart;

    const reactionMs = Math.round(reaction);
    const queueMs = Math.round(T.queue);
    const seatMs = Math.round(T.seat);
    const total = reactionMs + queueMs + seatMs;

    const first = seats[picked[0]];
    const extra = picked.length > 1 ? ` 외 ${picked.length - 1}석` : '';

    $('scoreTotal').innerHTML = `${fmt(total)}<span>초</span>`;
    $('scoreReaction').textContent = fmt(reactionMs) + '초';
    $('scoreQueue').textContent = fmt(queueMs) + '초';
    $('scoreSeat').textContent = fmt(seatMs) + '초';
    $('scoreRank').textContent = T.firstRank.toLocaleString('ko-KR') + '번';
    $('scoreSeatName').textContent = `${GRADE[first.grade].name} ${seatName(first)}${extra}`;

    showScreen('screen-result');

    if (!sessionId) return;

    try {
        const res = await authFetch('/api/practice/complete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                session_id: sessionId,
                total_duration_ms: total,
                reaction_time_ms: reactionMs,
                queue_wait_ms: queueMs,
                seat_selection_ms: seatMs,
                queue_initial_rank: T.firstRank,
            }),
        });

        if (res.ok) {
            const data = await res.json();
            if (data.percentile != null && data.total_users >= 2) {
                $('scoreRankVal').innerHTML = `상위 ${data.percentile}%`
                    + `<small>${data.total_users.toLocaleString('ko-KR')}명 중 ${data.my_rank}위</small>`;
                $('scoreRankBar').hidden = false;
            }
        } else {
            const err = await res.json().catch(() => ({}));
            $('scoreNotice').textContent = err.message || '기록이 저장되지 않았습니다.';
            $('scoreNotice').hidden = false;
        }
    } catch (e) {
        console.error('[Practicket] complete 실패:', e);
        $('scoreNotice').textContent = '네트워크 오류로 기록이 저장되지 않았습니다.';
        $('scoreNotice').hidden = false;
    } finally {
        sessionStorage.removeItem('pkt.sessionId');
        sessionStorage.removeItem('pkt.reactionStartMs');
        sessionStorage.removeItem('pkt.reactionTimeMs');
    }
}

/* ═══════════ 시작 ═══════════ */

const viewport = $('planViewport');
viewport.addEventListener('pointerdown', onPointerDown);
viewport.addEventListener('pointermove', onPointerMove);
viewport.addEventListener('pointerup', onPointerUp);
viewport.addEventListener('pointercancel', () => { drag = null });
viewport.addEventListener('wheel', onWheel, { passive: false });

$('zoomIn').addEventListener('click', () => zoomByButton(1));
$('zoomOut').addEventListener('click', () => zoomByButton(-1));
$('zoomFit').addEventListener('click', () => resetView());
$('miniMap').addEventListener('click', onMiniJump);
$('doneBtn').addEventListener('click', finish);
$('clearBtn').addEventListener('click', clearPicked);

$('gradeChip').addEventListener('click', () => {
    const layer = $('gradeLayer');
    layer.hidden = !layer.hidden;
});

$('capReload').addEventListener('click', newCaptcha);
$('capVoice').addEventListener('click', () => {
    $('capErr').textContent = '음성 안내는 연습 화면에서 제공하지 않습니다';
});
$('capSubmit').addEventListener('click', submitCaptcha);
$('capInput').addEventListener('input', onCaptchaInput);
$('capInput').addEventListener('keydown', e => {
    if (e.key === 'Enter') submitCaptcha();
});

window.addEventListener('resize', () => {
    const wasFit = Math.abs(view.z - view.fit) < 0.001;
    view.fit = fitScale();
    setView(wasFit ? view.fit : view.z, view.x, view.y);
});

if (reaction) {
    setTimeout(() => {
        showScreen('screen-queue');
        startQueue();
        // 도면을 같은 프레임에서 만들면 대기열 첫 화면이 늦게 뜬다
        setTimeout(buildPlan, 0);
        loadCaptchaPool();
    }, LOADING_MS);
}
