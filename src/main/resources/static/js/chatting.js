import { getAuthValue, getNickname} from "./common.js";

function makeChatElements(chat, chatBox, authValue) {
    const lastChatUnit = chatBox.lastElementChild;

    const chatType = authValue === chat.key ? "sent" : "received";
    const chatUnit = document.createElement("div");
    chatUnit.classList.add("chat-unit", chatType);

    chatUnit.setAttribute("data-user-key", chat.key);
    chatUnit.setAttribute("data-send-at", chat.sendAt);

    const user = document.createElement("p");
    user.classList.add("chat-user");
    user.textContent = chat.name;

    const chatContent = document.createElement("div");
    chatContent.classList.add("chat-content");

    const message = document.createElement("p");
    message.classList.add("chat-message");
    message.textContent = chat.text;

    const chatDateTime = new Date(chat.sendAt);
    const hour = chatDateTime.getHours();
    const period = hour < 12 ? "오전" : "오후";
    const timeValue = `${period} ${hour % 12 || 12}:${String(chatDateTime.getMinutes()).padStart(2, "0")}`;
    const dateValue = `${chatDateTime.getFullYear()}년 ${chatDateTime.getMonth() + 1}월 ${chatDateTime.getDate()}일`

    const date = document.createElement("div");
    const dateText = document.createElement("p");
    dateText.textContent = dateValue;
    date.appendChild(dateText);
    date.classList.add("chat-date");

    const time = document.createElement("p");
    time.classList.add("chat-time");
    time.textContent = timeValue;

    if (!lastChatUnit || lastChatUnit.dataset.userKey !== chat.key) {
        chatUnit.appendChild(user);
    }

    if (!lastChatUnit || (new Date(lastChatUnit.dataset.sendAt).getDate() !== chatDateTime.getDate())) {
        chatBox.appendChild(date);
    }

    chatContent.appendChild(message);
    chatContent.appendChild(time);
    chatUnit.appendChild(chatContent);
    chatBox.appendChild(chatUnit);
}

function renderingChatting(data, isFirstRendering) {
    const authValue = getAuthValue();
    const chatBox = document.getElementById("chatting-box-section");
    if (isFirstRendering) {
        chatBox.innerHTML = "";
        data.forEach(chat => {
            makeChatElements(chat, chatBox, authValue);
        })
    } else {
        makeChatElements(data, chatBox, authValue);
    }
    chatBox.scrollTop = chatBox.scrollHeight;
}

function setChatting() {
    fetch(`${HOST}/api/chat`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
        },
    }).then(response => {
        if (!response.ok) {
            throw new Error("Request is failed");
        }
        return response.json();
    }).then(data => {
        console.log("채팅 데이터 추후 삭제", data);
        renderingChatting(data, true);
    }).catch(e => {
        alert("채팅 전송 실패")
    })
}

function setChattingSse() {
    const eventSource = new EventSource(`${HOST}/api/chat/connection`);
    eventSource.addEventListener("chat", (event) => {
        console.log("sse 이벤트 수신");
        const chat = JSON.parse(event.data);
        renderingChatting(chat, false);
    });
    eventSource.onerror = (error) => {
        console.error("Error:", error);
    };
}

// 채팅 이벤트 설정
async function setChatEventListener() {
    const inputBox = document.getElementById("chatting-input");
    inputBox.addEventListener("click", async () => {
        const name = await getNickname();
        if (!name) {
            alert("채팅을 입력하려면 닉네임을 입력해주세요.")
            return
        }
    })

    const button = document.getElementById("chatting-send-button");
    button.addEventListener("click", async () => {
        const name = await getNickname();
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
            }),
            credentials: "same-origin"
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

setChatting();
setChatEventListener();
setChattingSse();