import { getNickname, host as HOST } from "./common.js";

const selected_seats = new Set();

function hasPermission(name) {
    const key = "permission";
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${key}=`);
    return parts.length === 2;

}

// 예매 페이지 접근 권한 확인
function permissionCheck() {
    window.onload = function() {
        if (hasPermission()) {
            alert("권한 통과")
            document.cookie = "permission=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        } else {
            alert("권한 없음")
            window.location.href = `${HOST}`;
        }
    };
}

function createSeat() {
    const row_size = 10;
    const col_size = 10;
    const seat_section = document.getElementById("seats");

    for (let i=0; i<row_size; i++) {
        for (let j=0; j<col_size; j++) {
            const seat_button = document.createElement('button');
            seat_button.style.cursor="pointer"
            seat_button.classList.add('seat-button');
            seat_button.dataset.seatNumber = `${String.fromCharCode(65 + i)}${j + 1}`;
            seat_button.addEventListener('click', () => toggleSeat(seat_button));
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
        if (!response.ok) {
            throw new Error("Request is failed");
        }
    }).catch(error => {
        alert("에매 실패")
    })
}

// permissionCheck();
createSeat();
addButtonEventListener();