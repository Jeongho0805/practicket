/* ============================================================
   n-ticket 준비 화면(NOL gates) — 카운트다운 + 반응속도 측정 시작점

   타이밍 정합성(서버 검증 대응):
   - /start 는 "연습 시작하기" 시점에 호출된다 (서버가 startAt 기록)
   - 카운트다운이 0 이 되는 순간을 pkt.reactionStartMs 로 찍고,
     예매하기 클릭 시각과의 차이를 pkt.reactionTimeMs 로 넘긴다
   - 서버 검증이 카운트다운을 5초로 고정 취급하므로(COUNTDOWN_MS) 초를 바꾸면 기록이 거부된다
   ============================================================ */
import { authFetch, showAlert } from '/js/common.js';

const COUNTDOWN_SEC = 5;
const NEXT_URL = '/practice/n-ticket';

const modal = document.getElementById('startModal');
const startBtn = document.getElementById('startBtn');
const bookBtn = document.getElementById('bookBtn');

function formatTime(sec) {
    const m = Math.floor(sec / 60).toString().padStart(2, '0');
    const s = (sec % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
}

async function startPractice() {
    startBtn.disabled = true;

    try {
        const res = await authFetch('/api/practice/start?type=N_TICKET', { method: 'POST' });
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

    modal.style.display = 'none';

    let seconds = COUNTDOWN_SEC;
    bookBtn.textContent = `남은시간 ${formatTime(seconds)}`;

    const interval = setInterval(() => {
        seconds--;
        if (seconds > 0) {
            bookBtn.textContent = `남은시간 ${formatTime(seconds)}`;
            return;
        }
        clearInterval(interval);
        sessionStorage.setItem('pkt.reactionStartMs', Date.now().toString());
        bookBtn.textContent = '예매하기';
        bookBtn.classList.add('is-open');
        bookBtn.disabled = false;
    }, 1000);
}

function book() {
    const reactionStart = parseInt(sessionStorage.getItem('pkt.reactionStartMs') || '0', 10);
    if (reactionStart) {
        sessionStorage.setItem('pkt.reactionTimeMs', (Date.now() - reactionStart).toString());
    }
    window.location.href = NEXT_URL;
}

startBtn.addEventListener('click', startPractice);
bookBtn.addEventListener('click', book);

// 뒤로가기(BFCache)로 돌아왔을 때 초기 상태로 강제 새로고침
window.addEventListener('pageshow', event => {
    if (event.persisted) window.location.reload();
});

document.addEventListener('DOMContentLoaded', async () => {
    try {
        const clientRes = await authFetch('/api/client');
        const clientData = await clientRes.json();
        if (!clientData.name) {
            await showAlert({ title: '닉네임 필요', msg: '닉네임을 입력해주세요.' });
            window.location.href = '/practice';
        }
    } catch (e) {
        console.error('[Practicket] Failed to fetch client info:', e);
    }
});
