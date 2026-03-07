import { authFetch } from '/js/common.js';

let selectedDate = 6;
let selectedTimeSlot = true;

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

function startBooking() {
    if (!selectedDate) {
        alert('날짜를 선택해주세요.');
        return;
    }

    const reactionStart = parseInt(sessionStorage.getItem('pkt.reactionStartMs') || '0');
    if (reactionStart) {
        sessionStorage.setItem('pkt.reactionTimeMs', (Date.now() - reactionStart).toString());
    }

    sessionStorage.removeItem('iq.seat.decay.startedAt');
    sessionStorage.removeItem('iq.queue.payload');
    sessionStorage.removeItem('iq.queue.bookingEnabledAt');

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
            sessionStorage.setItem('pkt.sessionId', data.session_id);
        } else {
            alert('연습 세션을 시작할 수 없습니다.\n다시 로그인하거나 나중에 시도해주세요.');
            window.location.href = '/practice';
            return;
        }
    } catch (e) {
        console.error('[Practicket] Failed to start session:', e);
        alert('네트워크 오류가 발생했습니다.\n다시 시도해주세요.');
        window.location.href = '/practice';
        return;
    }

    modal.style.display = 'none';
    countdownUi.style.display = 'block';
    bookingUi.style.display = 'none';

    let seconds = 5;

    function updateTimer() {
        const m = Math.floor(seconds / 60).toString().padStart(2, '0');
        const s = (seconds % 60).toString().padStart(2, '0');
        timerDisplay.innerText = `${m}:${s}`;
    }

    updateTimer();

    const interval = setInterval(() => {
        seconds--;
        updateTimer();

        if (seconds <= 0) {
            clearInterval(interval);
            sessionStorage.setItem('pkt.reactionStartMs', Date.now().toString());
            countdownUi.style.display = 'none';
            bookingUi.style.display = 'block';
        }
    }, 1000);
}

window.changeMonth = changeMonth;
window.selectDate = selectDate;
window.selectTime = selectTime;
window.startBooking = startBooking;
window.startPractice = startPractice;

// 뒤로가기(BFCache)로 돌아왔을 때 초기 상태로 강제 새로고침
window.addEventListener('pageshow', function (event) {
    if (event.persisted) {
        window.location.reload();
    }
});
