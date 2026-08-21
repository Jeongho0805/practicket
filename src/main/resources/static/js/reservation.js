import * as util from "./common.js";

let selected_seats = new Set();
let security_text;

function checkTimeToLeave() {
    setInterval(async () => {
        const serverTime = await util.getSyncTime();
        if (serverTime.getSeconds() >= 50) {
            await util.showAlert({ title: '예매 불가', msg: '예매 가능 시간이 지났습니다.\n예매페이지로 돌아갑니다.' });
            window.location.replace(`${HOST}/ticketing`);
        }
    }, 1000);
}

async function createSeat() {
    const row_size = 10;
    const col_size = 10;
    const seat_section = document.getElementById("seats");
    seat_section.innerHTML = "";
    const seats = await requestSeatInfo();
    console.log("seat 정보 =", seats);

    for (let i=0; i<row_size; i++) {
        for (let j=0; j<col_size; j++) {
            const seat_button = document.createElement('button');
            const seat_number = `${String.fromCharCode(65 + i)}${j + 1}`
            seat_button.classList.add('seat-button');
            seat_button.dataset.seatNumber = seat_number;
            if (seats.some(seat => seat === seat_number)) {
                seat_button.style.backgroundColor="lightsteelblue"
            } else {
                seat_button.style.cursor="pointer"
                seat_button.addEventListener('click', () => toggleSeat(seat_button));
            }
            seat_section.appendChild(seat_button);
        }
    }
}

function toggleSeat(seat_button) {
    const seat_number = seat_button.dataset.seatNumber;
    seat_button.classList.toggle('selected');

    if (seat_button.classList.contains('selected')) {
        seat_button.style.backgroundColor="darkslateblue"
        selected_seats.add(seat_number);
        displaySelectSeat();
    } else {
        seat_button.style.backgroundColor="white"
        selected_seats.delete(seat_number);
        displaySelectSeat();
    }

    console.log("현재 선택된 좌석:", selected_seats);
}

function displaySelectSeat() {
    let selected_seat_list = document.getElementById('selected-seat-list');
    selected_seat_list.innerHTML = "";
    for (const key of selected_seats.keys()) {
        const selected_seat_value = document.createElement('p');
        selected_seat_value.textContent = `${key.substring(0, 1)}열-${key.substring(1)}`
        selected_seat_list.append(selected_seat_value);
    }
}

function addButtonEventListener() {
    // 좌석 선택 완료 처리
    const completeButton = document.getElementById("complete-button");
    completeButton.addEventListener("click", async () => {
        if (selected_seats.size === 0) {
            await util.showAlert({ title: '좌석 선택 필요', msg: '좌석을 선택해주세요.' });
            return;
        }
        requestReservation();
    });
    // 좌석 초기화 처리
    const seatResetButton = document.getElementById("seat-reset-button");
    seatResetButton.addEventListener("click", async () => {
        selected_seats = new Set();
        displaySelectSeat();
        await createSeat();
    });
}

async function requestSeatInfo() {
    try {
        const response = await fetch(`${HOST}/api/ticket`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
            },
        });
        if (!response.ok) {
            throw new Error("서버 오류 발생");
        }

        const result = await response.json();
        console.log(result); //todo 추후 삭제
        if (Array.isArray(result)) {
            return result;
        }
        return [];
    } catch (error) {
        console.error("네트워크 오류:", error);
        return [];
    }
}
async function requestReservation() {
    const reservationToken = localStorage.getItem('reservationToken');
    if (!reservationToken) {
        await util.showAlert({ title: '권한 없음', msg: '권한이 없습니다.' });
        window.location.replace(`${HOST}/ticketing`);
        return;
    }

    try {
        const response = await util.authFetch(`${HOST}/api/ticket`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                seats: Array.from(selected_seats.keys()),
                reservation_token: reservationToken
            })
        });

        if (response.ok) {
            localStorage.removeItem('reservationToken');
            await util.showAlert({ title: '예매 완료', msg: '예매가 완료되었습니다!' });
            window.location.replace(`${HOST}/ticketing`);
        } else {
            const result = await response.json();
            await util.showAlert({ title: '예매 실패', msg: result.message });
            if (result.code === 'T05' || result.code === 'T06') {
                localStorage.removeItem('reservationToken');
                window.location.replace(`${HOST}/ticketing`);
            }
        }
    } catch (error) {
        console.error(error);
        await util.showAlert({ title: '서버 오류', msg: '일시적인 서버 오류로 예매에 실패하였습니다.' });
    }
}

function createRandomSecurityText() {
    security_text = "";
    const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    for (let i = 0; i < 6; i++) {
        security_text += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    document.getElementById("security-text").textContent = security_text;
    document.getElementById("security-error").classList.remove("show");
}

function activateModalToggle() {
    const modal = document.getElementById("modal-section");
    if (modal.style.display === "none" || modal.style.display === "") {
        modal.style.display = "flex";
        const input = document.getElementById("security-input");
        input.focus();
    } else {
        modal.style.display = "none";
    }
}

function checkSecurityText() {
    const security_input = document.getElementById("security-input");
    const security_error = document.getElementById("security-error");
    if (security_input.value.toUpperCase() === security_text) {
        security_error.classList.remove("show");
        activateModalToggle();
    } else {
        security_error.classList.add("show");
        security_input.value = "";
        security_input.focus();
    }
}

function addSecurityInputEvent() {
    const button = document.getElementById("security-input-button");
    button.addEventListener("click", () => {
        checkSecurityText();
    });
    const input = document.getElementById("security-input");
    input.addEventListener('keydown', function(event) {
        if (event.key === 'Enter') {
            event.preventDefault(); // 기본 Enter 동작 방지 (폼 제출 등)
            checkSecurityText();
        }
    });
}

function addSecurityResetEvent() {
    const button = document.getElementById("security-reset-button");
    button.addEventListener("click", () => {
        createRandomSecurityText();
    });
}

function activateSecurityText() {
    createRandomSecurityText();
    addSecurityInputEvent();
    addSecurityResetEvent();
    activateModalToggle();
}

window.addEventListener("pageshow", (event) => {
    if (event.persisted) {
        window.location.reload();
    }
});

checkTimeToLeave();
activateSecurityText();
await createSeat();
addButtonEventListener();