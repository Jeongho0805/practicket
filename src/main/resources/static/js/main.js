import * as util from "./common.js";

function addEventList() {
    // 티켓 예매 이벤트
    const ticket_button = document.getElementById("action-button")
    ticket_button.addEventListener("click", async () => {
        const name = await util.getNickname();
        if (!name) {
            alert("닉네임을 입력해주세요.");
            return
        }

        const timeErrorMessage = "예매 가능 시간이 아닙니다. \n 예매는 매분 00초 부터 30초까지 가능합니다."

        const response = await util.authFetch(`${HOST}/api/order`, {
            method: "POST",
            credentials: 'same-origin',
            headers: {
                "Content-Type": "application/json",
            },
        });
        try {
            if(!response.ok) {
                const data = await response.json();
                if(data.code === "T02") {
                    alert(timeErrorMessage);
                    return;
                }
                throw new Error(data.message);
            }
            activateModalToggle();
            setWaitingOrderSse(name);
        } catch(error) {
            alert("일시적인 서버 장애로 예매에 실패하였습니다.")
        }
    })
}

function updateProgressBar(currentWaiting, totalCapacity) {
    const progressBar = document.getElementById('progress-bar');
    const progress = (totalCapacity - currentWaiting) / totalCapacity;
    progressBar.style.width = progress * 100 + '%';
}

function activateModalToggle() {
    const modal = document.getElementById("modal-section");
    if (modal.style.display === "none" || modal.style.display === "") {
        modal.style.display = "flex";
    } else {
        modal.style.display = "none";
    }
}

function setWaitingOrderSse(name) {
    const token = localStorage.getItem("token");
    const eventSource = new EventSource(`${HOST}/api/order?token=${encodeURIComponent(token)}`);
    eventSource.addEventListener("waiting-order", (event) => {
        const data = JSON.parse(event.data)
        if (data.is_complete && data.reservation_token) {
            eventSource.close();
            // 예매 권한 토큰을 localStorage에 저장
            localStorage.setItem('reservationToken', data.reservation_token);
            // 토큰을 쿼리 파라미터로 전달하여 서버에서 검증
            window.location.href = `${HOST}/reservation?token=${encodeURIComponent(data.reservation_token)}`;
            return;
        }
        console.log(event);
        const waiting_number = document.getElementById("waiting-number");
        waiting_number.innerText = data.current_waiting_order;
        updateProgressBar(data.current_waiting_order, data.first_waiting_order);
    });
    eventSource.onerror = (error) => {
        eventSource.close();
        window.location.href = `${HOST}/rank`;
    };
}

async function displayTime() {
    setInterval(async () => {
        const serverTime = await util.getSyncTime();
        const hours = serverTime.getHours().toString().padStart(2, '0');
        const minutes = serverTime.getMinutes().toString().padStart(2, '0');
        const seconds = serverTime.getSeconds().toString().padStart(2, '0');
        const displayElement = document.getElementById('countdown'); // 시간을 표시할 요소 선택
        displayElement.innerText = `${hours}시 ${minutes}분 ${seconds}초`;
        updateColor(seconds);
    }, 100)
}

function updateColor(second) {
    const button = document.getElementById('action-button');
    const buttonText = document.getElementById('button-text');
    const pointer = document.getElementById('timeline-pointer');

    // 타임라인 포인터 위치 업데이트 (0~60초 → 0~100%)
    const position = (second / 60) * 100;
    pointer.style.left = `${position}%`;

    // 0~30초: 예매 가능 (보라)
    if (second >= 0 && second <= 60) {
        button.className = 'active';
        buttonText.textContent = '지금 예매하세요!';
        button.disabled = false;
    }
    // // 50~59초: 준비 (연보라)
    // else if (second >= 50) {
    //     button.className = 'ready';
    //     buttonText.textContent = '준비하세요!';
    //     button.disabled = false;
    // }
    // // 31~49초: 대기 (회색)
    // else {
    //     button.className = 'waiting';
    //     buttonText.textContent = '잠시 대기';
    //     button.disabled = true;
    // }
}

//todo 시간 확인을 위한 임시 처리
async function checkServerTime() {
    const synTime = getSyncDate();
    const synTimeInfo = `${synTime.getHours()}:${synTime.getMinutes()}:${synTime.getSeconds()}.${synTime.getMilliseconds()}`

    const deviceTime = new Date();
    const deviceTimeInfo = `${deviceTime.getHours()}:${deviceTime.getMinutes()}:${deviceTime.getSeconds()}.${deviceTime.getMilliseconds()}`

    const response = await fetch(`${HOST}/api/server-time`);
    const data = await response.json();
    const serverDate = new Date(data.server_time);
    const serverDateInfo = `${serverDate.getHours()}:${serverDate.getMinutes()}:${serverDate.getSeconds()}.${serverDate.getMilliseconds()}`

    const displayElement = document.getElementById('countdown'); // 시간을 표시할 요소 선택

    console.log(
        `보정시간=${synTimeInfo} \n기기시간=${deviceTimeInfo}\n서버시간-${serverDateInfo}\n표시시간=${displayElement.innerText}\n차이=${serverOffset}`
    );
}

window.addEventListener("pageshow", async (event) => {
    if (event.persisted) {
        window.location.reload();
    }
});

addEventList();
displayTime();

