import { getNickname, HOST } from "./common.js";



function addEventList() {
    // 티켓 예매 이벤트
    const ticket_button = document.getElementById("round-button")
    ticket_button.addEventListener("click", () => {
        console.log(name);
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

function setChattingSse() {
    const eventSource = new EventSource(`${HOST}/api/sse-stream`);
    eventSource.addEventListener("chat-event", (event) => {
        // console.log("sse 이벤트 수신");
        const chattingBox = document.getElementById("chatting-box-section");
        const childCount = chattingBox.childElementCount;
        const chatData = JSON.parse(event.data);
        if (childCount !== chatData.length) {
            chattingBox.innerHTML = "";
            chatData.forEach(item => {
                const newParagraph = document.createElement("p");
                newParagraph.textContent = item.name + ": " + item.text + "  [" + item.sendAt + "]"
                chattingBox.appendChild(newParagraph);
            });
            chattingBox.scrollTop = chattingBox.scrollHeight;
        }
    });
    eventSource.onerror = (error) => {
        console.error("Error:", error);
    };
}

// 채팅 이벤트 설정
function setChatEventListener() {
    const button = document.getElementById("chatting-send-button");
    button.addEventListener("click", () => {
        const name = getNickname();
        if (!name) {
            alert("채팅을 입력하려면 닉네임을 입력해주세요.")
            return
        }
        const chatting = document.getElementById("chatting-input").value;
        if (!chatting) {
            alert("채팅을 입력해주세요.")
            return;
        }
        fetch(`${HOST}/api/chat`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                text: chatting,
                name: name
            })
        }).then(response => {
            if (!response.ok) {
                throw new Error("Request is failed");
            }
            document.getElementById("chatting-input").value = "";
        }).catch(e => {
            alert("채팅 전송 실패")
        })
    });
    document.getElementById("chatting-input").addEventListener("keypress", function (e) {
        if (e.key === "Enter") { // 엔터 키를 눌렀을 때
            e.preventDefault();   // 기본 동작 방지
            button.click();
        }
    });
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
// setChatEventListener();
// setChattingSse();