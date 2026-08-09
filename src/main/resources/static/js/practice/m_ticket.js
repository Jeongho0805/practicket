/*
  M-Ticket 좌석 선택.
  인트로(/practice/m-ticket/intro)에서 sessionStorage 로 세션·반응속도·대기열 결과를 인계받고,
  여기서 좌석 선택 시간을 재서 /api/practice/complete 로 보낸다.

  실물 멜론은 보안문자를 두 번 요구한다 — 처음엔 건너뛸 수 있고, 좌석을 확정할 때는 못 건너뛴다.
  좌석 선택 시간은 첫 보안문자가 닫힌 순간부터 잰다.

  구역을 갈아타는 길은 실물과 같이 셋이다 — 미니맵 클릭 / 좌석도 전체보기 / 등급표의 구역 목록.
*/
import { authFetch } from '/js/common.js';

const INTRO_URL = '/practice/m-ticket/intro';

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
    R: { key: 'R', name: 'R석', price: 154000, color: '#7a68a8', rest: 412 },
    S: { key: 'S', name: 'S석', price: 143000, color: '#2e9e57', rest: 1183 },
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

/* 흐림 처리와 hover 가 서로를 덮어쓰지 않게 기준 투명도를 도형에 적어둔다 */
const setBase = (shape, v) => {
    shape.setAttribute('data-base', v);
    shape.setAttribute('fill-opacity', v);
};

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
            shape.addEventListener('mouseenter', () => shape.setAttribute('fill-opacity', 1));
            shape.addEventListener('mouseleave', () =>
                shape.setAttribute('fill-opacity', shape.getAttribute('data-base')));
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
    const byGrade = g => !openGrade || g === openGrade;

    mainShapes.forEach(({ shape, grade }) => setBase(shape, byGrade(grade) ? .9 : .12));
    miniShapes.forEach(({ shape, grade }, nm) => {
        const on = onSeat ? nm === curZone : byGrade(grade);
        setBase(shape, on ? .9 : (onSeat ? .25 : .12));
    });
}

/* ══════════ 등급 표 ══════════ */

let openGrade = null;

function renderGradeTable() {
    $('grade-table').innerHTML = Object.values(GRADES).map(g => `
        <tbody>
        <tr class="g${openGrade === g.key ? ' on' : ''}" data-grade="${g.key}">
            <th><span class="chip" style="background:${g.color}"></span></th>
            <td class="nm">${g.name}</td>
            <td class="pr">${won(g.price)}</td>
            <td class="rs">${g.rest}</td>
            <td class="ar"><i></i></td>
        </tr>
        <tr class="zn${openGrade === g.key ? ' on' : ''}">
            <td colspan="5">
                ${ZONES_OF[g.key].map(z => `<button data-zone="${z}" data-grade="${g.key}">${z} 구역</button>`).join('')}
            </td>
        </tr>
        </tbody>`).join('');

    $('grade-table').querySelectorAll('.g').forEach(tr => tr.addEventListener('click', () => {
        openGrade = openGrade === tr.dataset.grade ? null : tr.dataset.grade;
        paintMaps();
        renderGradeTable();
    }));
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

function drawGrid() {
    const grid = $('grid');
    grid.innerHTML = '';
    for (let r = 1; r <= 16; r++) {
        const row = document.createElement('div');
        row.className = 'grow';
        row.innerHTML = `<span class="rn">${r}</span>`;

        const cells = document.createElement('div');
        cells.className = 'cs';
        for (let c = 1; c <= 22; c++) {
            const cell = document.createElement('div');
            const free = Math.random() < 0.42;
            cell.className = 'cell' + (free ? ' free' : '');
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

$('map-all').addEventListener('click', backToZones);
$('mo-area-change').addEventListener('click', backToZones);

$('pc-bar-toggle').addEventListener('click', () => $('pc-bar').classList.toggle('open'));

function refresh() {
    Object.values(GRADES).forEach(g => {
        g.rest = Math.max(0, g.rest + Math.round((Math.random() - .58) * 60));
    });
    renderGradeTable();
    paintMaps();
    if (document.body.classList.contains('stage-seat')) {
        picked.clear();
        drawGrid();
    }
    syncSeat();
    showToast('잔여석을 새로 불러왔습니다.');
}

$('pc-refresh').addEventListener('click', refresh);
$('mo-refresh').addEventListener('click', refresh);

$('pc-done').addEventListener('click', confirmSeats);
$('mo-go').addEventListener('click', () => {
    if (!document.body.classList.contains('stage-seat')) {
        if (!moArea) return showToast('구역을 선택해 주세요.');
        return enterZone(moArea, moGrade);
    }
    confirmSeats();
});

function confirmSeats() {
    if (!picked.size) return showToast('좌석을 선택해 주세요.');
    openCaptcha('final');
}

/* ══════════ 보안문자 ══════════
   실물은 서버가 만든 PNG 라 그대로 못 쓴다. 검정 단색, 좌우로 눌린 굵은 글자,
   겹치는 자간, 가로지르는 사선 하나 — 관찰한 특징만 옮겼다. */

const CAP_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
let capText = '', capMode = 'first';

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
    closeCaptcha();
    if (wasFinal) finish();
});
$('cap-input').addEventListener('keydown', e => { if (e.key === 'Enter') $('cap-submit').click(); });

/* ══════════ 완료 ══════════ */

async function finish() {
    T.seat = Math.round(now() - T.seatStart);
    const total = T.reaction + T.queue + T.seat;

    const seatNames = [...picked].map(id => `${GRADES[curGrade].name} ${seatName(id)}`);

    $('r-seat').textContent = seatNames.join(', ');
    $('r-reaction').textContent = fmt(T.reaction) + '초';
    $('r-queue').textContent = fmt(T.queue) + '초';
    $('r-pick').textContent = fmt(T.seat) + '초';
    $('r-initial').textContent = T.initialRank.toLocaleString() + '번';
    $('r-total').textContent = fmt(total);

    try {
        const res = await authFetch('/api/practice/complete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                session_id: sessionId,
                total_duration_ms: total,
                reaction_time_ms: T.reaction,
                queue_wait_ms: T.queue,
                seat_selection_ms: T.seat,
                queue_initial_rank: T.initialRank,
            }),
        });
        if (res.ok) {
            const data = await res.json();
            if (data.percentile != null && data.total_users >= 2) {
                $('r-rank').textContent =
                    `상위 ${data.percentile}% / ${data.total_users.toLocaleString()}명 중 ${data.my_rank}위`;
                $('r-rankbar').classList.add('on');
            }
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

    $('result-dim').classList.add('open');
    $('result').classList.add('open');
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
