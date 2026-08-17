import { authFetch, showAlert } from '/js/common.js';

const COUNTDOWN_SECONDS = 5;

/*
  대기열 상수는 i_ticket_new.js 와 같은 값을 쓴다.
  종목이 달라도 같은 공식이어야 랭킹을 나란히 놓고 볼 수 있다.
  반응이 느릴수록 초기 순번이 커지고, 초당 DEQ 만큼 줄어 최대 10초 안에 통과한다.
*/
const QUEUE = { MIN: 5000, MAX: 200000, DEQ: 20000, MAX_REACTION: 3000, TICK_MS: 200 };

/* 오픈 안내 문구. 데스크톱과 모바일이 형식이 다른 것도 실물 그대로다. */
const OPEN_NOTICE = {
    pc: '2026.02.08(일) 오후 18:00 티켓오픈!',
    mo: '2월 8일 18:00 티켓오픈!',
};

/*
  실물은 카운트가 0이 되어도 바로 열리지 않는다 — 서버에 다시 물어보는 왕복이 있다.
  0초에 맞춰 미리 누를 수 없고 버튼이 뜨는 것을 보고 눌러야 하는 구간이라 그대로 둔다.
  고정값이면 타이밍을 외워 화면을 안 보고도 좋은 기록이 나오므로 범위로 흔든다.
  반응 시간은 이 구간이 끝난 뒤부터 재므로 기록에는 들어가지 않는다.
*/
const OPEN_DELAY = { MIN: 400, MAX: 1400 };

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
    sessionStorage.setItem('pkt.reactionStartMs', Date.now().toString());

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
    let seconds = COUNTDOWN_SECONDS;

    const paint = () => {
        const left = `(남은시간 ${formatCounter(seconds)})`;
        $('open-txt').textContent = `${OPEN_NOTICE.pc} ${left}`;
        $('mo-book').textContent = `${OPEN_NOTICE.mo} ${left}`;
    };

    $('mo-book').classList.add('pre-open');
    paint();

    const timer = setInterval(() => {
        seconds -= 1;
        paint();
        if (seconds > 0) return;

        clearInterval(timer);
        setTimeout(unlock, OPEN_DELAY.MIN + Math.random() * (OPEN_DELAY.MAX - OPEN_DELAY.MIN));
    }, 1000);
}

async function startPractice() {
    try {
        const res = await authFetch('/api/practice/start?type=M_TICKET', { method: 'POST' });
        if (!res.ok) {
            await showAlert({ title: '세션 오류', msg: '연습 세션을 시작할 수 없습니다.\n다시 로그인하거나 나중에 시도해주세요.' });
            window.location.href = '/practice';
            return;
        }
        const data = await res.json();
        sessionStorage.setItem('pkt.sessionId', data.session_id);
    } catch (e) {
        console.error('[Practicket] Failed to start session:', e);
        await showAlert({ title: '네트워크 오류', msg: '네트워크 오류가 발생했습니다.\n다시 시도해주세요.' });
        window.location.href = '/practice';
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
    const step = Math.min(Math.floor(Math.min(reactionMs, QUEUE.MAX_REACTION) / 100), 30);
    const initialRank = Math.round(QUEUE.MIN + (step / 30) * (QUEUE.MAX - QUEUE.MIN));
    sessionStorage.setItem('pkt.queueInitialRank', initialRank.toString());

    // 실물의 "뒤에 N명" 자리. 우리는 서버 대기열이 없으므로 초기 순번에서 파생시킨다.
    const behind = Math.round(initialRank * 0.37);
    $('q-behind').textContent = behind.toLocaleString();

    $('queue-dim').style.display = 'block';
    $('queue-pop').style.display = 'block';

    const startedAt = now();
    // 좌석은 이 순간부터 팔리기 시작한다. 대기열에서 끈 만큼 자리가 없어야 한다(n-ticket 과 같다)
    sessionStorage.setItem('pkt.queueStartAt', Date.now().toString());

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
            sessionStorage.setItem('pkt.queueWaitMs', Math.round(now() - startedAt).toString());
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
    const startedAt = parseInt(sessionStorage.getItem('pkt.reactionStartMs') || '0', 10);
    reactionMs = startedAt ? Date.now() - startedAt : 0;
    sessionStorage.setItem('pkt.reactionTimeMs', reactionMs.toString());

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
            window.location.href = '/practice';
        }
    } catch (e) {
        console.error('[Practicket] Failed to fetch client info:', e);
    }
});

renderDates();
render();
