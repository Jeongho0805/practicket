import { authFetch, showAlert } from '/js/common.js';
import * as run from '/js/practice/run-state.js';
import { startCountdown } from '/js/practice/countdown.js';
import { QUEUE, initialRank as rankFor } from '/js/practice/queue-model.js';

/* 인트로가 뜨면 이전 판을 접는다. 뒤로가기로 돌아와도 진행 상태가 남아 있으면
   앞으로가기 한 번에 끝난 판이 되살아난다. */
run.clearRun();

const COUNTDOWN_SECONDS = 6;

/* 오픈 안내 문구. 데스크톱과 모바일이 형식이 다른 것도 실물 그대로다. */
const OPEN_NOTICE = {
    pc: '2026.02.08(일) 오후 18:00 티켓오픈!',
    mo: '2월 8일 18:00 티켓오픈!',
};

const SCHEDULE = [
    { date: '2026년 02월 08일 일요일', times: ['18시 00분'] },
];

const SEATS = [
    ['R석', '154,000원'],
    ['S석', '143,000원'],
];

const picked = { date: null, time: null };
let unlocked = false;
let reactionMs = 0;

const $ = (id) => document.getElementById(id);
const now = () => performance.now();

function renderDates() {
    // 실물은 고를 날이 하루뿐이면 그 날도 자동으로 선택한다 (날짜·회차가 하나면 바로 예매하기)
    if (SCHEDULE.length === 1) picked.date = SCHEDULE[0].date;

    $('date-body').innerHTML = SCHEDULE
        .map((s) => `<div class="opt${picked.date === s.date ? ' selected' : ''}" data-date="${s.date}">${s.date}</div>`)
        .join('');
}

function renderTimes() {
    const entry = SCHEDULE.find((s) => s.date === picked.date);
    if (!entry) {
        $('time-body').innerHTML = '<div class="placeholder">날짜를 선택해주세요!</div>';
        return;
    }
    // 실물은 회차가 하나뿐이면 누르지 않아도 자동으로 선택된다 — 클릭 한 번이 곧 기록 차이다
    if (entry.times.length === 1) picked.time = entry.times[0];

    $('time-body').innerHTML = entry.times
        .map((t) => `<div class="opt${picked.time === t ? ' selected' : ''}" data-time="${t}">${t}</div>`)
        .join('');
}

function renderSeats() {
    if (!picked.time) {
        $('seat-body').innerHTML = '<div class="placeholder">회차를 선택해주세요!</div>';
        return;
    }
    const rows = SEATS.map(([grade, price]) => `<li><span>${grade}</span><span>${price}</span></li>`).join('');
    $('seat-body').innerHTML = `<div class="seat-note">선택한 회차별 좌석 현황</div><ul class="seat-list">${rows}</ul>`;
}

function renderBookButton() {
    const ready = unlocked && picked.date && picked.time;
    $('pc-book').disabled = !ready;
    $('pick-msg').textContent = ready ? `${picked.date} · ${picked.time}` : '';
}

function render() {
    renderTimes();
    renderSeats();
    renderBookButton();
}

function formatCounter(total) {
    const m = Math.floor(total / 60).toString().padStart(2, '0');
    const s = (total % 60).toString().padStart(2, '0');
    return `${m} 분 ${s} 초`;
}

function formatLeft(seconds) {
    if (seconds < 60) return `${seconds}초`;
    return `${Math.floor(seconds / 60)}분 ${seconds % 60}초`;
}

function onPick(event) {
    if (!unlocked) return;

    const opt = event.target.closest('.opt');
    if (!opt) return;

    if (opt.dataset.date) {
        picked.date = opt.dataset.date;
        picked.time = null;
        $('date-body').querySelectorAll('.opt').forEach((el) => el.classList.remove('selected'));
        opt.classList.add('selected');
    } else if (opt.dataset.time) {
        picked.time = opt.dataset.time;
    }
    render();
}

function unlock() {
    unlocked = true;
    run.markOpened();

    $('open-line').classList.add('is-hidden');
    $('process-cols').classList.remove('is-hidden');
    $('btn-bar').classList.remove('is-hidden');

    $('pc-book').textContent = '예매하기';
    $('mo-book').textContent = '예매하기';
    $('mo-book').classList.remove('pre-open');
    $('mo-book').disabled = false;

    render();
}

function runCountdown() {
    $('mo-book').classList.add('pre-open');

    startCountdown(COUNTDOWN_SECONDS, (sec) => {
        const left = `(남은시간 ${formatCounter(sec)})`;
        $('open-txt').textContent = `${OPEN_NOTICE.pc} ${left}`;
        $('mo-book').textContent = `${OPEN_NOTICE.mo} ${left}`;
    }, unlock);
}

async function startPractice() {
    try {
        const res = await authFetch('/api/practice/start?type=M_TICKET', { method: 'POST' });
        if (!res.ok) {
            await showAlert({ title: '세션 오류', msg: '연습 세션을 시작할 수 없습니다.\n다시 로그인하거나 나중에 시도해주세요.' });
            window.location.replace('/practice');
            return;
        }
        const data = await res.json();
        run.setSessionId(data.session_id);
    } catch (e) {
        console.error('[Practicket] Failed to start session:', e);
        await showAlert({ title: '네트워크 오류', msg: '네트워크 오류가 발생했습니다.\n다시 시도해주세요.' });
        window.location.replace('/practice');
        return;
    }

    $('start-modal').style.display = 'none';
    runCountdown();
}

/* 순번이 많을수록 나쁜 상태를 보여준다 — 실물도 원활/다소 지연/지연 3단계다 */
function badgeFor(count) {
    if (count > 100000) return ['delay', '지연'];
    if (count > 30000) return ['slow', '다소 지연'];
    return ['normal', '원활'];
}

function startQueue() {
    const initialRank = rankFor(reactionMs);
    run.setInitialRank(initialRank);

    // 실물의 "뒤에 N명" 자리. 우리는 서버 대기열이 없으므로 초기 순번에서 파생시킨다.
    const behind = Math.round(initialRank * 0.37);
    $('q-behind').textContent = behind.toLocaleString();

    /* 실물은 넷퍼넬에 진입을 물어보는 동안 순번 자리가 비어 있다.
       팝업을 먼저 띄우고 로딩이 끝난 뒤에 숫자가 돌기 시작한다. */
    $('queue-dim').style.display = 'block';
    $('queue-pop').style.display = 'block';
    $('q-count').textContent = '-';
    $('q-left').textContent = '계산 중';

    setTimeout(() => runQueueTicker(initialRank), QUEUE.LOADING_MS);
}

function runQueueTicker(initialRank) {
    const startedAt = now();

    const paint = () => {
        const elapsedSec = (now() - startedAt) / 1000;
        const cur = Math.max(0, Math.floor(initialRank - elapsedSec * QUEUE.DEQ));

        $('q-count').textContent = cur.toLocaleString();
        $('q-left').textContent = formatLeft(Math.ceil(cur / QUEUE.DEQ));

        const pct = initialRank > 0 ? Math.min(100, ((initialRank - cur) / initialRank) * 100) : 100;
        $('q-fill').style.width = pct + '%';
        if (pct >= 100) $('q-dot2').classList.add('on');

        const [cls, label] = badgeFor(cur);
        const badge = $('q-badge');
        badge.className = 'q-badge ' + cls;
        badge.textContent = label;

        if (cur <= 0) {
            clearInterval(timer);
            run.markQueuePassed();
            run.sendCheckpoint(authFetch);
            enterSeat();
        }
    };

    paint();
    const timer = setInterval(paint, QUEUE.TICK_MS);
}

function enterSeat() {
    window.location.href = '/practice/m-ticket';
}

function book() {
    run.markBooked();
    reactionMs = run.reactionMs();

    $('pc-book').disabled = true;
    $('mo-book').disabled = true;
    startQueue();
}

document.addEventListener('click', onPick);
$('start-btn').addEventListener('click', startPractice);
$('pc-book').addEventListener('click', book);
$('mo-book').addEventListener('click', book);

// 뒤로가기(BFCache)로 돌아오면 카운트다운이 끝난 상태가 남는다
window.addEventListener('pageshow', (event) => {
    if (event.persisted) window.location.reload();
});

document.addEventListener('DOMContentLoaded', async () => {
    try {
        const res = await authFetch('/api/client');
        const client = await res.json();
        if (!client.name) {
            await showAlert({ title: '닉네임 필요', msg: '닉네임을 입력해주세요.' });
            window.location.replace('/practice');
        }
    } catch (e) {
        console.error('[Practicket] Failed to fetch client info:', e);
    }
});

renderDates();
render();
