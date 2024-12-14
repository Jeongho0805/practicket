import { getNickname, HOST } from "./common.js";


// 페이지 로드시 마다 실행 (새로고침 포함)
let name;

window.onload = async () => {
    try {
        name = await getNickname();
        const name_input_section = document.getElementById("name-input-section");
        const name_value = document.getElementById("name-value");
        if (name) {
            name_input_section.style.display = "none";
            name_value.style.display = "block";
            name_value.textContent = name;
        } else {
            const name_section = document.getElementById("name-section");
            name_section.style.display = "none";
        }
    } catch (e) {
        console.log(e);
    }
};

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

    // 닉네임 입력 이벤트
    const name_button = document.getElementById("name-input-button");
    name_button.addEventListener("click", async () => {
        const name = document.getElementById("name-input-box").value;
        if (name === "" || !name) {
            alert("닉네임을 입력해주세요.");
            return
        }
        await requestCreateSession(name);
        location.reload();
    })

    // 닉네임 변경 이벤트
    const name_reset_button = document.getElementById("reset-button");
    name_reset_button.addEventListener("click", async () => {
        await requestDeleteSession();
        document.cookie = "auth=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        location.reload();
    })
}

function requestCreateSession(name) {
    return fetch(`${HOST}/api/auth`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            name: name
        })
    }).then(response => {
        if (!response.ok) {
            console.log("requestCreateSession 요청 실패");
            throw new Error("Request is failed");
        }
    }).catch(e => {
        console.log("에러 =", e.getMessage())
        alert("서버 에러가 발생하였습니다.\n 닉네임을 다시 입력해주세요.")
    })
}

function requestUpdateSession(name) {
    fetch(`${HOST}/api/auth`, {
        method: "PATCH",
        credentials: 'same-origin',
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            name: name
        })
    }).then(response => {
        if (!response.ok) {
            throw new Error("Request is failed");
        }
    }).catch(e => {
        alert("서버 에러가 발생하였습니다.\n 닉네임을 다시 입력해주세요.")
    })
}

function requestDeleteSession() {
    return fetch(`${HOST}/api/auth`, {
        method: "DELETE",
        credentials: 'same-origin',
        headers: {
            "Content-Type": "application/json",
        },
    }).then(response => {
        console.log("세션 삭제 완료");
        if (!response.ok) {
            throw new Error("Request is failed");
        }
    }).catch(e => {
        alert("서버 에러가 발생하였습니다.")
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