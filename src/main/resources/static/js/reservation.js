import { getNickname } from "./common.js";

const selected_seats = new Set();
let security_text;

function hasPermission() {
    const key = "permission";
    const cookies = document.cookie.split(';'); // 쿠키 문자열을 ;로 분리
    cookies.forEach((c) => console.log(c));
    return cookies.some(cookie => cookie.trim().startsWith(`${key}=`));
}

// 예매 페이지 접근 권한 확인
function permissionCheck() {
    if (!hasPermission()) {
        alert("비정상적인 경로를 통하여 접근하셨습니다\n 예매페이지로 돌아갑니다.")
        window.location.href = `${HOST}`;
    } else {
        document.cookie = "permission=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
    }
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
    const button = document.getElementById("complete-button");
    button.addEventListener("click", () => {
        requestReservation();
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
function requestReservation() {
    fetch(`${HOST}/api/ticket`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            name: getNickname(),
            seats: Array.from(selected_seats.keys())
        })
    }).then(response => {
        if (response.ok) {
            alert("예매 완료")
            window.location.href = `${HOST}/rank`;
            return Promise.resolve();
        } else {
            return response.json();
        }
    }).then(result => {
        if (result) {
            alert(result.message);
        }
    }).catch(error => {
        console.error(error);
        alert("일시적인 서버 오류로 예매에 실패하였습니다.")
    })
}

function createRandomSecurityText() {
    security_text = "";
    const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    for (let i = 0; i < 6; i++) {
        security_text += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    document.getElementById("security-text").textContent = security_text;
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
    if (security_input.value.toUpperCase() === security_text) {
        activateModalToggle();
    } else {
        security_input.value = "";
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
        permissionCheck();
    }
});

permissionCheck();
activateSecurityText();
await createSeat();
addButtonEventListener();