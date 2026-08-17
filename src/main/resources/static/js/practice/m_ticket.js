/*
  M-Ticket 좌석 선택.
  인트로(/practice/m-ticket/intro)에서 sessionStorage 로 세션·반응속도·대기열 결과를 인계받고,
  여기서 좌석 선택 시간을 재서 /api/practice/complete 로 보낸다.

  실물 멜론은 보안문자를 두 번 요구한다 — 처음엔 건너뛸 수 있고, 좌석을 확정할 때는 못 건너뛴다.
  좌석 선택 시간은 첫 보안문자가 닫힌 순간부터 잰다.

  구역을 갈아타는 길은 실물과 같이 셋이다 — 미니맵 클릭 / 좌석도 전체보기 / 등급표의 구역 목록.
*/
import { authFetch } from '/js/common.js';
import {
    bestRecordKey, renderSplitBar, readBestRecord, saveBestRecord,
    renderCompleteHint, renderBestChip, renderFailHint, bindShareButton
} from '/js/practice/result-split.js';

const INTRO_URL = '/practice/m-ticket/intro';
const BEST_RECORD_KEY = bestRecordKey('m-ticket');

const sessionId = sessionStorage.getItem('pkt.sessionId');
if (!sessionId) window.location.replace(INTRO_URL);

const $ = id => document.getElementById(id);
const now = () => performance.now();
const fmt = ms => (ms / 1000).toFixed(2);
const won = n => n.toLocaleString('ko-KR') + '원';

const T = {
    reaction: parseInt(sessionStorage.getItem('pkt.reactionTimeMs') || '0', 10),
    queue: parseInt(sessionStorage.getItem('pkt.queueWaitMs') || '0', 10),
    initialRank: parseInt(sessionStorage.getItem('pkt.queueInitialRank') || '0', 10),
    seatStart: 0,
    seat: 0,
};

const GRADES = {
    R: { key: 'R', name: 'R석', price: 154000, color: '#7a68a8' },
    S: { key: 'S', name: 'S석', price: 143000, color: '#2e9e57' },
};
const MAX_PICK = 2;

/* ══════════ 좌석도 ══════════
   프랙티켓홀 도면이다. 배치 골격은 실물 좌석도를 참고했고 구역 번호는 우리 것이다. */

const NS = 'http://www.w3.org/2000/svg';
const svgEl = (tag, attrs) => {
    const n = document.createElementNS(NS, tag);
    for (const k in attrs) n.setAttribute(k, attrs[k]);
    return n;
};

function sector(cx, cy, r1, r2, a1, a2) {
    const p = (r, a) => [cx + r * Math.cos(a), cy + r * Math.sin(a)];
    const [x1, y1] = p(r1, a1), [x2, y2] = p(r1, a2), [x3, y3] = p(r2, a2), [x4, y4] = p(r2, a1);
    return `M${x1} ${y1}A${r1} ${r1} 0 0 1 ${x2} ${y2}L${x3} ${y3}A${r2} ${r2} 0 0 0 ${x4} ${y4}Z`;
}

const MAP = { w: 700, h: 510, cx: 350, cy: 128, sy: 1.18 };
const RINGS = [
    { r1: 178, r2: 236, grade: 'R', from: 11, count: 7, floor: '2F' },
    { r1: 240, r2: 320, grade: 'S', from: 28, count: 8, floor: '3F' },
];
const FLOOR_ZONES = ['가', '나', '다', '라'];

const ZONES_OF = {
    R: [...FLOOR_ZONES, ...Array.from({ length: 7 }, (_, i) => String(11 + i))],
    S: Array.from({ length: 8 }, (_, i) => String(28 + i)),
};

const setBase = (shape, v) => shape.setAttribute('fill-opacity', v);

function buildZoneMap(target, opts) {
    const { mini = false, clickable = false } = opts || {};
    const { w, h, cx, cy, sy } = MAP;
    const svg = svgEl('svg', { viewBox: `0 0 ${w} ${h}`, preserveAspectRatio: 'xMidYMid meet' });
    const fs = n => mini ? Math.max(6, Math.round(n * 0.45)) : n;
    const shapes = new Map();

    const label = (x, y, s, size, fill) => {
        const t = svgEl('text', {
            x, y, 'text-anchor': 'middle', 'dominant-baseline': 'central',
            'font-size': fs(size), fill: fill || '#fff', 'pointer-events': 'none',
        });
        t.textContent = s;
        svg.appendChild(t);
    };

    const zone = (shape, name, grade) => {
        shape.setAttribute('fill', GRADES[grade].color);
        shape.setAttribute('stroke', '#fff');
        shape.setAttribute('stroke-width', mini ? 1 : 2.5);
        setBase(shape, .9);
        if (clickable) {
            shape.style.cursor = 'pointer';
            shape.addEventListener('click', () => enterZone(name, grade));
        }
        shapes.set(name, { shape, grade });
        svg.appendChild(shape);
    };

    if (!mini) {
        ['- 무대는 예매 편의를 위해 표기된 것으로 실제 무대와 다를 수 있습니다.',
         '구역 내 상단이 무대와 가까운 쪽입니다. 가로로 나란히 예매하시기 바랍니다.']
            .forEach((s, i) => label(cx, 22 + i * 19, s, 13, '#9a9a9a'));
    }

    svg.appendChild(svgEl('rect', { x: cx - 118, y: cy - 66, width: 236, height: 58, fill: '#e9e9e9' }));
    svg.appendChild(svgEl('rect', { x: cx - 26, y: cy - 12, width: 52, height: 30, fill: '#e9e9e9' }));
    label(cx, cy - 36, 'STAGE', 22, '#b0b0b0');

    const fw = 92, fh = 62;
    [['가', cx - 128], ['나', cx + 36]].forEach(([nm, x]) => {
        zone(svgEl('rect', { x, y: cy + 12, width: fw, height: fh }), nm, 'R');
        label(x + fw / 2, cy + 12 + fh / 2, nm, 24);
        if (!mini) {
            svg.appendChild(svgEl('rect', { x: x + 24, y: cy - 6, width: 44, height: 17, fill: '#7d7d7d' }));
            label(x + 46, cy + 2, 'Floor', 11);
        }
    });
    [['다', cx - 128], ['라', cx + 36]].forEach(([nm, x]) => {
        zone(svgEl('rect', { x, y: cy + 86, width: fw, height: 40 }), nm, 'R');
        label(x + fw / 2, cy + 106, nm, 24);
    });

    if (!mini) {
        svg.appendChild(svgEl('rect', { x: cx - 62, y: cy + 134, width: 124, height: 22, fill: 'none', stroke: '#dcdcdc' }));
        label(cx, cy + 145, 'FOH', 12, '#c8c8c8');
    }

    const badges = [];
    RINGS.forEach(ring => {
        const a0 = Math.PI * 0.035, a1 = Math.PI * 0.965;
        const step = (a1 - a0) / ring.count;
        for (let i = 0; i < ring.count; i++) {
            const s = a0 + i * step, e = s + step * 0.975;
            const nm = String(ring.from + i);
            const path = svgEl('path', {
                d: sector(cx, cy, ring.r1, ring.r2, s, e),
                transform: `translate(0 ${cy}) scale(1 ${sy}) translate(0 ${-cy})`,
            });
            zone(path, nm, ring.grade);

            const mid = (s + e) / 2, rm = (ring.r1 + ring.r2) / 2;
            label(cx + rm * Math.cos(mid), cy + rm * Math.sin(mid) * sy, nm, 19);

            // 층 배지는 양 끝 구역 바깥 모서리 위로 뺀다 — 구역에 걸치면 안 된다
            if (i === 0 || i === ring.count - 1) {
                const edge = i === 0 ? s : e;
                badges.push([cx + rm * Math.cos(mid), cy + ring.r1 * Math.sin(edge) * sy - 22, ring.floor]);
            }
        }
    });

    if (!mini) {
        badges.forEach(([x, y, s]) => {
            svg.appendChild(svgEl('circle', { cx: x, cy: y, r: 15, fill: '#7d7d7d' }));
            label(x, y, s, 13);
        });
    }

    target.innerHTML = '';
    target.appendChild(svg);
    return shapes;
}

let mainShapes = new Map(), miniShapes = new Map();

/* 도면 두 장을 상태에 맞춰 칠한다.
   큰 도면은 고른 등급만, 미니맵은 좌석 단계에선 보고 있는 구역만 남긴다 — 실물과 같다. */
function paintMaps() {
    const onSeat = document.body.classList.contains('stage-seat');
    const active = hoverGrade || openGrade;
    const byGrade = g => !active || g === active;

    mainShapes.forEach(({ shape, grade }) => setBase(shape, byGrade(grade) ? .9 : .12));
    miniShapes.forEach(({ shape, grade }, nm) => {
        const on = onSeat ? nm === curZone : byGrade(grade);
        setBase(shape, on ? .9 : (onSeat ? .25 : .12));
    });
}

/* ══════════ 등급 표 ══════════ */

let openGrade = null, hoverGrade = null;

function renderGradeTable() {
    $('grade-table').innerHTML = Object.values(GRADES).map(g => `
        <tbody>
        <tr class="g${openGrade === g.key ? ' on' : ''}" data-grade="${g.key}">
            <th><span class="chip" style="background:${g.color}"></span></th>
            <td class="nm">${g.name}</td>
            <td class="pr">${won(g.price)}</td>
            <td class="rs"></td>
            <td class="ar"><i></i></td>
        </tr>
        <tr class="zn${openGrade === g.key ? ' on' : ''}">
            <td colspan="5">
                ${ZONES_OF[g.key].map(z => `<button data-zone="${z}" data-grade="${g.key}">${z} 구역</button>`).join('')}
            </td>
        </tr>
        </tbody>`).join('');

    $('grade-table').querySelectorAll('.g').forEach(tr => {
        tr.addEventListener('click', () => {
            openGrade = openGrade === tr.dataset.grade ? null : tr.dataset.grade;
            paintMaps();
            renderGradeTable();
        });
        tr.addEventListener('mouseenter', () => { hoverGrade = tr.dataset.grade; paintMaps(); });
        tr.addEventListener('mouseleave', () => { hoverGrade = null; paintMaps(); });
    });
    $('grade-table').querySelectorAll('.zn button').forEach(b =>
        b.addEventListener('click', () => enterZone(b.dataset.zone, b.dataset.grade)));
}

$('mo-grades').innerHTML = Object.values(GRADES)
    .map(g => `<li data-grade="${g.key}"><i style="background:${g.color}"></i>${g.name}</li>`).join('');

$('mo-sheet-grades').innerHTML = Object.values(GRADES).map(g => `
    <li><i style="background:${g.color}"></i><span class="nm">${g.name}</span><span class="pr">${won(g.price)}</span></li>`).join('');

let moGrade = null, moArea = null;

$('mo-grades').querySelectorAll('li').forEach(li => li.addEventListener('click', () => {
    $('mo-grades').querySelectorAll('li').forEach(x => x.classList.remove('on'));
    li.classList.add('on');
    moGrade = li.dataset.grade;
    moArea = null;
    $('mo-go').classList.remove('on');
    $('mo-areas').innerHTML = ZONES_OF[moGrade].map(z => `<li data-zone="${z}">${z} 구역</li>`).join('');
    $('mo-areas').querySelectorAll('li').forEach(a => a.addEventListener('click', () => {
        $('mo-areas').querySelectorAll('li').forEach(x => x.classList.remove('on'));
        a.classList.add('on');
        moArea = a.dataset.zone;
        $('mo-go').classList.add('on');
    }));
}));

/* ══════════ 좌석 ══════════ */

const picked = new Set();
let curZone = '', curGrade = 'R';

const ROWS = 16, COLS = 22, ZONE_SEATS = ROWS * COLS;

/* 좌석은 대기열이 시작된 순간부터 팔린다 — 대기열에서 끈 만큼 이미 자리가 없다.
   값과 곡선은 n-ticket 과 같다. 오픈 직후가 가장 빠르고 갈수록 느려진다.
   실물 멜론은 폴링하지 않으므로 그 결과는 새로고침을 눌러야 보인다. */
const SELL = { totalMs: 40000, k: 2 };

const sellStartAt = Number(sessionStorage.getItem('pkt.queueStartAt')) || Date.now();

/* 좋은 자리부터 나가되 앞줄이 칼같이 채워지지는 않는다.
   JITTER 가 열 간격(100)보다 커야 열을 넘나들고, RANDOM 비율은 자리를 안 가리는 사람이라
   뒤쪽에도 구멍이 생긴다. 흔들림을 구역 이름에서 뽑으므로 같은 구역이면 순서가 늘 같다. */
const SPREAD = { JITTER: 600, RANDOM: .30 };

let sellOrder = new Map();

function buildSellOrder(zone) {
    let seed = [...zone].reduce((a, ch) => (a * 31 + ch.charCodeAt(0)) % 2147483647, 7);
    const rnd = () => (seed = (seed * 1103515245 + 12345) % 2147483648) / 2147483648;

    const seats = [];
    for (let r = 1; r <= ROWS; r++)
        for (let c = 1; c <= COLS; c++)
            seats.push({
                id: `${r}-${c}`,
                score: rnd() < SPREAD.RANDOM
                    ? rnd() * ROWS * 100
                    : r * 100 + Math.abs(c - (COLS + 1) / 2) * 4 + rnd() * SPREAD.JITTER,
            });

    seats.sort((a, b) => a.score - b.score);
    sellOrder = new Map(seats.map((s, i) => [s.id, i]));
}

let soldCount = 0;

function refreshSold() {
    const ms = Date.now() - sellStartAt;
    soldCount = ms >= SELL.totalMs ? ZONE_SEATS
        : Math.floor(ZONE_SEATS * (1 - Math.pow(1 - ms / SELL.totalMs, SELL.k)));
}

const isSold = id => sellOrder.get(id) < soldCount;
const isSoldOut = () => soldCount >= ZONE_SEATS;

function drawGrid() {
    const grid = $('grid');
    grid.innerHTML = '';
    for (let r = 1; r <= ROWS; r++) {
        const row = document.createElement('div');
        row.className = 'grow';
        row.innerHTML = `<span class="rn">${r}</span>`;

        const cells = document.createElement('div');
        cells.className = 'cs';
        for (let c = 1; c <= COLS; c++) {
            const id = `${r}-${c}`;
            const cell = document.createElement('div');
            const free = !isSold(id);
            cell.className = 'cell' + (free ? ' free' : '') + (picked.has(id) ? ' pick' : '');
            if (free) {
                cell.style.background = GRADES[curGrade].color;
                cell.addEventListener('click', () => toggleSeat(cell, r, c));
            }
            cells.appendChild(cell);
        }
        row.appendChild(cells);
        grid.appendChild(row);
    }
}

function enterZone(name, grade) {
    curZone = name;
    curGrade = grade;
    picked.clear();
    buildSellOrder(name);
    refreshSold();

    document.body.classList.add('stage-seat');
    $('map').classList.add('seat');
    $('zonehint').textContent = `현재 보고 계신 구역은 ${name} 구역입니다.`;
    $('mo-area').textContent = `${name} 구역`;
    $('mo-go').textContent = '다음 (0석)';
    $('mo-go').classList.remove('on');

    paintMaps();
    drawGrid();
    syncSeat();
}

/* 좌석도 전체보기 · 구역변경 — 구역 선택 화면으로 되돌아간다 */
function backToZones() {
    curZone = '';
    picked.clear();
    document.body.classList.remove('stage-seat');
    $('map').classList.remove('seat');
    $('mo-area').textContent = '구역을 선택해 주세요';
    $('mo-go').textContent = '좌석선택';
    $('mo-go').classList.remove('on');
    moArea = null;
    $('mo-areas').querySelectorAll('li').forEach(x => x.classList.remove('on'));
    paintMaps();
    syncSeat();
}

function toggleSeat(cell, r, c) {
    const id = `${r}-${c}`;
    if (picked.has(id)) {
        picked.delete(id);
        cell.classList.remove('pick');
    } else if (picked.size >= MAX_PICK) {
        return showToast(`최대 ${MAX_PICK}매까지 선택할 수 있습니다.`);
    } else {
        picked.add(id);
        cell.classList.add('pick');
    }
    syncSeat();
}

const seatName = id => {
    const [r, c] = id.split('-');
    return `${curZone}구역 ${r}열 ${c}번`;
};

function syncSeat() {
    const n = picked.size;
    const onSeat = document.body.classList.contains('stage-seat');

    $('pc-done').classList.toggle('on', n > 0);
    $('pc-bar-msg').innerHTML = n
        ? `선택한 좌석 총 <b>&nbsp;${n}석</b>이 선택되었습니다.`
        : (onSeat ? '좌석을 선택해 주세요' : '구역을 먼저 선택해주세요');
    $('pc-bar-sub').textContent = onSeat ? '' : '(화면을 직접 선택하거나 우측 좌석등급을 선택해주세요)';
    $('pc-chips').innerHTML = n
        ? [...picked].map(id => `<span>${seatName(id)}</span>`).join('')
        : '<span class="none">선택한 좌석이 없습니다.</span>';

    $('mo-sel').textContent = `선택좌석(${n})`;
    $('mo-go').textContent = onSeat ? `다음 (${n}석)` : '좌석선택';
    $('mo-go').classList.toggle('on', onSeat ? n > 0 : !!moArea);
}

/* ══════════ 우측 패널 조작 ══════════ */

$('map-all').addEventListener('click', () => askThenLeaveSeats(backToZones));
$('mo-area-change').addEventListener('click', () => askThenLeaveSeats(backToZones));

$('pc-bar-toggle').addEventListener('click', () => $('pc-bar').classList.toggle('open'));

/* 실물은 좌석 데이터를 다시 받아오는 동안 도면 위에 로딩만 띄운다.
   그 사이 팔린 좌석이 이때 비로소 회색으로 바뀐다 — 실물도 새로고침 전에는 알 수 없다. */
function refresh() {
    $('loading').classList.add('on');
    setTimeout(() => {
        $('loading').classList.remove('on');
        paintMaps();
        if (document.body.classList.contains('stage-seat')) {
            picked.clear();
            refreshSold();
            drawGrid();
            if (isSoldOut()) soldOut();
        }
        syncSeat();
    }, 500);
}

/* 고른 좌석이 사라지는 동작이라 실물은 확인을 받는다 */
function askThenLeaveSeats(run) {
    if (!picked.size) return run();
    ask({
        title: '구역 변경',
        msg: '해당 구역에서 선택한 좌석 정보는 사라집니다. 계속 하시겠습니까?',
        confirm: true,
        onOk: run,
    });
}

$('pc-refresh').addEventListener('click', () => askThenLeaveSeats(refresh));
$('mo-refresh').addEventListener('click', () => askThenLeaveSeats(refresh));

$('pc-done').addEventListener('click', confirmSeats);
$('mo-go').addEventListener('click', () => {
    if (!document.body.classList.contains('stage-seat')) {
        if (!moArea) return showToast('구역을 선택해 주세요.');
        return enterZone(moArea, moGrade);
    }
    confirmSeats();
});

/* 실물은 브라우저 alert · confirm 을 쓴다. 우리 화면은 950×652 안에 갇혀 있어
   시스템 창이 바깥에 뜨면 시선이 끊기므로 같은 자리에 모달로 띄운다. */
let askOk = null;

function ask({ title, msg, confirm = false, onOk }) {
    $('ask-title').textContent = title;
    $('ask-msg').textContent = msg;
    $('ask').classList.toggle('confirm', confirm);
    $('ask').classList.add('open');
    $('ask-dim').classList.add('open');
    askOk = onOk;
}

function closeAsk() {
    $('ask').classList.remove('open');
    $('ask-dim').classList.remove('open');
}

$('ask-ok').addEventListener('click', () => { closeAsk(); const f = askOk; askOk = null; if (f) f(); });
$('ask-cancel').addEventListener('click', () => { askOk = null; closeAsk(); });

function confirmSeats() {
    if (!picked.size) return showToast('좌석을 선택해 주세요.');
    if (!capSolved) return openCaptcha('final');
    submitSeats();
}

/* 실물은 완료를 누른 순간에만 서버가 좌석을 확인한다. 고르는 동안에는 아무도 잡아두지 않는다. */
function submitSeats() {
    refreshSold();
    if ([...picked].some(isSold)) return seatTaken();
    finish();
}

function seatTaken() {
    ask({
        title: '좌석 선택 실패',
        msg: '다른 고객님이 결제 중인 좌석입니다.',
        onOk: () => {
            picked.clear();
            drawGrid();
            syncSeat();
            if (isSoldOut()) soldOut();
        },
    });
}

/* 전석 매진. 기록은 보내지 않는다 — 좌석을 못 잡았으니 순위에 낄 기록이 아니다.
   막대에 좌석 구간까지 넣어야 시간을 어디서 다 썼는지가 드러난다. */
function soldOut() {
    if ($('pkt-soldout-overlay').classList.contains('visible')) return;

    const seatMs = T.seatStart ? Math.round(now() - T.seatStart) : 0;
    const segments = [T.reaction, T.queue, seatMs];
    const total = segments.reduce((a, b) => a + b, 0);

    const meta = ['M-Ticket'];
    if (T.initialRank) meta.push(`대기 순번 ${T.initialRank.toLocaleString('ko-KR')}번에서 출발`);
    $('pkt-fail-meta').textContent = meta.join(' · ');

    $('pkt-fail-msg').textContent =
        `좌석이 다 팔리기까지 ${SELL.totalMs / 1000}초, 여기까지 ${fmt(total)}초 걸렸어요`;

    $('pkt-fail-total').textContent = fmt(total) + '초';
    $('pkt-fail-reaction').textContent = fmt(T.reaction) + '초';
    $('pkt-fail-queue').textContent = fmt(T.queue) + '초';
    $('pkt-fail-seat').textContent = fmt(seatMs) + '초';

    renderSplitBar($('pkt-fail-stack'), segments, total);
    renderFailHint($('pkt-fail-hint'), segments);

    $('pkt-soldout-overlay').classList.add('visible');
    sessionStorage.removeItem('pkt.sessionId');
}

/* ══════════ 보안문자 ══════════
   실물은 서버가 만든 PNG 라 그대로 못 쓴다. 검정 단색, 좌우로 눌린 굵은 글자,
   겹치는 자간, 가로지르는 사선 하나 — 관찰한 특징만 옮겼다. */

const CAP_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
let capText = '', capMode = 'first', capSolved = false;

$('cap-reload').innerHTML =
    '<svg viewBox="0 0 20 20" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round">'
    + '<path d="M16.5 10a6.5 6.5 0 1 1-1.9-4.6"/><path d="M16.6 3.2v3.1h-3.1"/></svg>';
$('cap-sound').innerHTML =
    '<svg viewBox="0 0 20 20" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round">'
    + '<path d="M4 7.6h2.6L10.4 4.5v11L6.6 12.4H4z" fill="currentColor" stroke="none"/>'
    + '<path d="M13.2 7.4a3.6 3.6 0 0 1 0 5.2"/><path d="M15.3 5.3a6.5 6.5 0 0 1 0 9.4"/></svg>';

function drawCaptcha() {
    const canvas = $('cap-canvas');
    const ctx = canvas.getContext('2d');
    const w = canvas.width, h = canvas.height;
    capText = Array.from({ length: 6 }, () => CAP_CHARS[Math.floor(Math.random() * CAP_CHARS.length)]).join('');

    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, w, h);

    const size = Math.round(h * 0.62);
    const step = (w - size * 0.7) / capText.length;

    ctx.fillStyle = '#111';
    capText.split('').forEach((ch, i) => {
        ctx.save();
        ctx.translate(size * 0.35 + i * step, h * 0.66 + (Math.random() * 6 - 3));
        ctx.rotate(Math.random() * 0.16 - 0.08);
        ctx.scale(0.86, 1.18);
        ctx.font = `bold ${size}px "Times New Roman", serif`;
        ctx.fillText(ch, 0, 0);
        ctx.restore();
    });

    ctx.strokeStyle = '#111';
    ctx.lineWidth = Math.max(2, h * 0.035);
    ctx.beginPath();
    ctx.moveTo(w * 0.04, h * 0.74);
    ctx.lineTo(w * 0.96, h * 0.26 + Math.random() * h * 0.12);
    ctx.stroke();
}

function openCaptcha(mode) {
    capMode = mode;
    drawCaptcha();
    $('cap-input').value = '';
    $('cap-later').textContent = mode === 'first'
        ? '좌석 먼저 확인하고 나중에 입력하기'
        : '좌석 선택 다시 하기';
    $('cap').classList.add('open');
    $('cap-dim').classList.add('open');
}

function closeCaptcha() {
    $('cap').classList.remove('open');
    $('cap-dim').classList.remove('open');
    if (!T.seatStart) T.seatStart = now();   // 좌석 시간은 첫 보안문자를 넘긴 순간부터
}

$('cap-reload').addEventListener('click', drawCaptcha);
$('cap-sound').addEventListener('click', () => showToast('음성 안내는 연습에서 제공하지 않습니다.'));
$('cap-later').addEventListener('click', closeCaptcha);
$('cap-submit').addEventListener('click', () => {
    if ($('cap-input').value.trim().toUpperCase() !== capText) {
        showToast('문자가 일치하지 않습니다.');
        drawCaptcha();
        $('cap-input').value = '';
        return;
    }
    const wasFinal = capMode === 'final';
    capSolved = true;
    closeCaptcha();
    if (wasFinal) submitSeats();
});
$('cap-input').addEventListener('keydown', e => { if (e.key === 'Enter') $('cap-submit').click(); });

/* ══════════ 완료 ══════════ */

async function finish() {
    T.seat = Math.round(now() - T.seatStart);
    const total = T.reaction + T.queue + T.seat;

    let result = {
        total_duration_ms: total,
        reaction_time_ms: T.reaction,
        queue_wait_ms: T.queue,
        seat_selection_ms: T.seat,
        queue_initial_rank: T.initialRank,
    };

    try {
        const res = await authFetch('/api/practice/complete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ session_id: sessionId, ...result }),
        });
        if (res.ok) {
            result = { ...result, ...await res.json() };
        } else {
            const err = await res.json().catch(() => ({}));
            showToast(err.message || '기록 저장에 실패했습니다.');
        }
    } catch (e) {
        console.error('[Practicket] complete 실패:', e);
        showToast('네트워크 오류로 기록이 저장되지 않았습니다.');
    } finally {
        sessionStorage.removeItem('pkt.sessionId');
        sessionStorage.removeItem('pkt.reactionTimeMs');
        sessionStorage.removeItem('pkt.queueWaitMs');
        sessionStorage.removeItem('pkt.queueInitialRank');
    }

    showCompleteModal(result);
}

/* i-ticket 과 같은 모달이다. 총 시간이 곧 세 구간의 합이라 막대 척도도 그 합을 쓴다. */
function showCompleteModal(r) {
    const sec = ms => (ms / 1000).toFixed(3) + '초';

    const meta = ['M-Ticket', stamp()];
    if (r.queue_initial_rank) meta.push(`대기 순번 ${r.queue_initial_rank.toLocaleString('ko-KR')}번에서 출발`);
    $('pkt-meta').textContent = meta.join(' · ');

    $('pkt-total-num').textContent = (r.total_duration_ms / 1000).toFixed(3);
    $('pkt-reaction').textContent = sec(r.reaction_time_ms);
    $('pkt-queue').textContent = sec(r.queue_wait_ms);
    $('pkt-seat').textContent = sec(r.seat_selection_ms);

    const segments = [r.reaction_time_ms, r.queue_wait_ms, r.seat_selection_ms];
    const segmentSum = segments.reduce((a, b) => a + b, 0);
    const best = readBestRecord(BEST_RECORD_KEY);
    const bestSum = best ? best.segments.reduce((a, b) => a + b, 0) : 0;

    const scale = Math.max(segmentSum, bestSum);
    renderSplitBar($('pkt-stack'), segments, scale);

    const refBar = $('pkt-refbar');
    if (best) {
        renderSplitBar(refBar, best.segments, scale, false);
        refBar.style.display = 'flex';
        $('pkt-ref-label').textContent = '흐린 막대 = 내 최고 기록';
    } else {
        refBar.style.display = 'none';
        $('pkt-ref-label').textContent = '';
    }

    renderCompleteHint(segments, segmentSum, best);
    renderBestChip(r.total_duration_ms, best);
    saveBestRecord(BEST_RECORD_KEY, r.total_duration_ms, segments, best);
    bindShareButton(r, 'M_TICKET');

    const bar = $('pkt-percentile-bar');
    if (r.percentile != null && r.total_users >= 2) {
        $('pkt-percentile-value').innerHTML =
            `상위 ${r.percentile}%<span class="pb-sub">/ ${r.total_users.toLocaleString('ko-KR')}명 중 ${r.my_rank}위</span>`;
        bar.style.display = 'flex';
    } else {
        bar.style.display = 'none';
    }

    $('pkt-complete-overlay').classList.add('visible');
}

function stamp() {
    const d = new Date();
    const p = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())}  ${p(d.getHours())}:${p(d.getMinutes())}`;
}

/* ══════════ 토스트 ══════════ */

let toastTimer;
function showToast(msg) {
    const t = $('toast');
    t.textContent = msg;
    t.classList.add('on');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.remove('on'), 1700);
}

/* ══════════ 시작 ══════════ */

if (sessionId) {
    mainShapes = buildZoneMap($('zonemap'), { clickable: true });
    miniShapes = buildZoneMap($('minimap'), { mini: true, clickable: true });
    buildZoneMap($('mo-thumb'), { mini: true });
    renderGradeTable();
    syncSeat();
    openCaptcha('first');
}
