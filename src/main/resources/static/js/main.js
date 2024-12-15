import { getNickname, HOST } from "./common.js";



function addEventList() {
    // 티켓 예매 이벤트
    const ticket_button = document.getElementById("round-button")
    ticket_button.addEventListener("click", () => {
        const name = getNickname();
        if (!name) {
            alert("닉네임을 입력해주세요.");
            return
        }

        fetch(`${HOST}/api/order`, {
            method: "POST",
            credentials: 'same-origin',
            headers: {
                "Content-Type": "application/json",
            },
        }).then(response => {
            if (!response.ok) {
                throw new Error("Request is failed");
            }
            activateModalToggle();
            setWaitingOrderSse(name);
        }).catch(error => {
            alert("에매 실패")
        })
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
    const eventSource = new EventSource(`${HOST}/api/order`);
    eventSource.addEventListener("waiting-order", (event) => {
        const data = JSON.parse(event.data)
        if (data.isComplete) {
            eventSource.close();
            document.cookie = `permission=reservation; path=/`;
            window.location.href = `${HOST}/reservation`;
            return;
        }
        console.log(event);
        const waiting_number = document.getElementById("waiting-number");
        waiting_number.innerText = data.currentWaitingOrder;
        updateProgressBar(data.currentWaitingOrder, data.firstWaitingOrder);
    });
    eventSource.onerror = (error) => {
        eventSource.close();
        window.location.href = `${HOST}/rank`;
    };
}

function startCountdown() {
    let isCountDownOn = false;
    setInterval(() => {
        const seconds = new Date().getSeconds();
        if (seconds >= 30 && !isCountDownOn) {
            displayCountDown(60 - seconds);
            isCountDownOn = true;
        }
        if (seconds < 30) {
            isCountDownOn = false;
        }
    }, 1000)
}

function displayCountDown() {
    const displayElement = document.getElementById('countdown'); // 시간을 표시할 요소 선택
    const countdownInterval = setInterval(() => {
        const remainingSecond = 60 - new Date().getSeconds();
        if (remainingSecond === 60) {
            displayElement.innerText = "Ticketing Start!";
            clearInterval(countdownInterval);
        } else {
            displayElement.innerText = remainingSecond.toString();
        }
    }, 1000);
}

addEventList();
startCountdown();