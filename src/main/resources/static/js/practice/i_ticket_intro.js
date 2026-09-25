import { authFetch, showAlert } from '/js/common.js';
import * as run from '/js/practice/run-state.js';
import { startCountdown } from '/js/practice/countdown.js';

/* 인트로가 뜨면 이전 판을 접는다. 뒤로가기로 돌아와도 진행 상태가 남아 있으면
   앞으로가기 한 번에 끝난 판이 되살아난다. */
run.clearRun();

let selectedDate = 6;
let selectedTimeSlot = true;
let stopCountdown = null;

function changeMonth(direction) {
    console.log('Change month:', direction);
}

function selectDate(day) {
    if (day !== 6) return;

    document.querySelectorAll('.calendar-day.selected').forEach(el => {
        el.classList.remove('selected');
    });

    event.target.classList.add('selected');
    selectedDate = day;
}

function selectTime(element) {
    document.querySelectorAll('.time-slot.selected').forEach(el => {
        el.classList.remove('selected');
    });
    element.classList.add('selected');
    selectedTimeSlot = true;
}

async function startBooking() {
    if (!selectedDate) {
        await showAlert({ title: '날짜 선택 필요', msg: '날짜를 선택해주세요.' });
        return;
    }

    run.markBooked();
    window.location.href = '/practice/i-ticket';
}

async function startPractice() {
    const modal = document.getElementById('start-modal');
    const countdownUi = document.getElementById('countdown-ui');
    const bookingUi = document.getElementById('booking-ui');
    const timerDisplay = document.getElementById('timer-display');

    try {
        const res = await authFetch('/api/practice/start?type=I_TICKET_OLD', { method: 'POST' });
        if (res.ok) {
            const data = await res.json();
            run.setSessionId(data.session_id);
        } else {
            await showAlert({ title: '세션 오류', msg: '연습 세션을 시작할 수 없습니다.\n다시 로그인하거나 나중에 시도해주세요.' });
            window.location.replace('/practice');
            return;
        }
    } catch (e) {
        console.error('[Practicket] Failed to start session:', e);
        await showAlert({ title: '네트워크 오류', msg: '네트워크 오류가 발생했습니다.\n다시 시도해주세요.' });
        window.location.replace('/practice');
        return;
    }

    modal.style.display = 'none';

    const isMobile = window.innerWidth <= 768;
    const bookingActions = document.querySelector('.booking-actions');
    const COUNTDOWN_SEC = 6;

    function formatTime(s) {
        const m = Math.floor(s / 60).toString().padStart(2, '0');
        const sec = (s % 60).toString().padStart(2, '0');
        return `${m}:${sec}`;
    }

    if (isMobile) {
        const bookingBtn = document.querySelector('.mobile-btn-booking');
        bookingBtn.disabled = true;
        bookingBtn.style.background = '#ccc';
        bookingBtn.style.cursor = 'not-allowed';

        stopCountdown = startCountdown(COUNTDOWN_SEC, (sec) => {
            bookingBtn.textContent = `남은시간 ${formatTime(sec)}`;
        }, () => {
            run.markOpened();
            bookingBtn.disabled = false;
            bookingBtn.textContent = '예매하기';
            bookingBtn.style.background = '';
            bookingBtn.style.cursor = '';
        });
    } else {
        countdownUi.style.display = 'block';
        bookingUi.style.display = 'none';

        stopCountdown = startCountdown(COUNTDOWN_SEC, (sec) => {
            timerDisplay.innerText = formatTime(sec);
        }, () => {
            run.markOpened();
            countdownUi.style.display = 'none';
            bookingUi.style.display = 'block';
            bookingActions.style.display = 'block';
        });
    }
}

window.changeMonth = changeMonth;
window.selectDate = selectDate;
window.selectTime = selectTime;
window.startBooking = startBooking;
window.startPractice = startPractice;

/* 뒤로가기(BFCache)로 돌아오면 카운트다운이 끝난 화면이 그대로 살아난다.
   새로고침으로 지우면 페이지뷰와 광고 요청이 한 번 더 나가므로 제자리에서 되돌린다. */
function resetIntro() {
    if (stopCountdown) {
        stopCountdown();
        stopCountdown = null;
    }
    run.clearRun();

    document.getElementById('start-modal').style.display = '';
    document.getElementById('countdown-ui').style.display = '';
    document.getElementById('booking-ui').style.display = 'none';
    document.getElementById('timer-display').innerText = '00:05';
    document.querySelector('.booking-actions').style.display = '';

    const bookingBtn = document.querySelector('.mobile-btn-booking');
    bookingBtn.disabled = false;
    bookingBtn.textContent = '예매하기';
    bookingBtn.style.background = '';
    bookingBtn.style.cursor = '';
}

window.addEventListener('pageshow', (event) => {
    if (event.persisted) resetIntro();
});

// 페이지 진입 시점에 닉네임 검증
document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('start-btn').addEventListener('click', startPractice);

    try {
        const clientRes = await authFetch('/api/client');
        const clientData = await clientRes.json();
        if (!clientData.name) {
            await showAlert({ title: '닉네임 필요', msg: '닉네임을 입력해주세요.' });
            window.location.replace('/practice');
        }
    } catch (e) {
        console.error('[Practicket] Failed to fetch client info:', e);
    }
});
