import * as util from "./common.js";

// 모바일 화면 이동시 채팅 sse 연결 끊김에 대한 처리
document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") {
        location.reload();
    }
});

function appendChatElementAtBottom(chat, chatBox, tokenValue) {
    const lastChatUnit = chatBox.lastElementChild;

    const chatType = tokenValue === chat.key ? "sent" : "received";
    const chatUnit = document.createElement("div");
    chatUnit.classList.add("chat-unit", chatType);

    chatUnit.setAttribute("data-user-key", chat.key);
    chatUnit.setAttribute("data-send-at", chat.send_at);

    const user = document.createElement("p");
    user.classList.add("chat-user");
    user.textContent = chat.name;

    const chatContent = document.createElement("div");
    chatContent.classList.add("chat-content");

    const message = document.createElement("p");
    message.classList.add("chat-message");
    message.textContent = chat.text;

    const chatDateTime = new Date(chat.send_at);
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

function appendChatElementAtTop(chat, chatBox, authValue) {
    const firstChatUnit = chatBox.querySelector(".chat-unit");

    const chatType = authValue === chat.key ? "sent" : "received";
    const chatUnit = document.createElement("div");
    chatUnit.classList.add("chat-unit", chatType);

    chatUnit.setAttribute("data-user-key", chat.key);
    chatUnit.setAttribute("data-send-at", chat.send_at);

    const user = document.createElement("p");
    user.classList.add("chat-user");
    user.textContent = chat.name;

    const chatContent = document.createElement("div");
    chatContent.classList.add("chat-content");

    const message = document.createElement("p");
    message.classList.add("chat-message");
    message.textContent = chat.text;

    const chatDateTime = new Date(chat.send_at);
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

    // 첫번쨰 요소와 닉네임이 일치하지 않을 때
    if (!firstChatUnit || firstChatUnit.dataset.userKey !== chat.key) {
        chatUnit.appendChild(user);
    }
    // 첫번째 요소와 닉네임이 일치할 때(닉네임 라벨 삭제)
    if (firstChatUnit.dataset.userKey === chat.key) {
        chatUnit.appendChild(user);
        const existingUser = firstChatUnit.querySelector(".chat-user");
        existingUser.remove();
    }

    chatContent.appendChild(message);
    chatContent.appendChild(time);
    chatUnit.appendChild(chatContent);

    // 첫번째 요소와 날짜가 다를때
    if (!firstChatUnit || (new Date(firstChatUnit.dataset.sendAt).getDate() !== chatDateTime.getDate())) {
        chatBox.insertBefore(chatUnit, chatBox.firstChild);
        chatBox.insertBefore(date, chatBox.firstChild);
    }
    // 첫번쨰 요소와 날짜가 같을 때 (기존 날짜 라벨 제거 -> 채팅 추가 -> 날짜 라벨 추가)
    if (new Date(firstChatUnit.dataset.sendAt).getDate() === chatDateTime.getDate()) {
        const firstDateLabel = chatBox.querySelector(".chat-date");
        firstDateLabel.remove();
        chatBox.insertBefore(chatUnit, chatBox.firstChild);
        chatBox.insertBefore(date, chatBox.firstChild);
    }
}

function makeChatElementByScroll(chat, chatBox, authValue) {
    const lastChatUnit = chatBox.firstElementChild;

    const chatType = authValue === chat.key ? "sent" : "received";
    const chatUnit = document.createElement("div");
    chatUnit.classList.add("chat-unit", chatType);
}

function appendChatByScroll(data) {
    const authValue = util.getAuthValue();
    const chatBox = document.getElementById("chatting-box-section");

    const prevScrollHeight = chatBox.scrollHeight;
    const prevScrollTop = chatBox.scrollTop;

    data.reverse().forEach(chat => {
        appendChatElementAtTop(chat, chatBox, authValue);
    })

    const newScrollHeight = chatBox.scrollHeight;
    const addedHeight = newScrollHeight - prevScrollHeight;

    // 스크롤 위치 보정
    chatBox.scrollTop = prevScrollTop + addedHeight;
}

function renderingChatting(data, isFirstRendering) {
    const tokenValue = util.getTokenValue();
    const chatBox = document.getElementById("chatting-box-section");
    if (isFirstRendering) {
        chatBox.innerHTML = "";
        data.forEach(chat => {
            appendChatElementAtBottom(chat, chatBox, tokenValue);
        })
        chatBox.scrollTop = chatBox.scrollHeight;
    } else {
        appendChatElementAtBottom(data, chatBox, tokenValue);
    }
    if (chatBox.scrollHeight - chatBox.scrollTop - chatBox.clientHeight <= chatBox.clientHeight * 2) {
        chatBox.scrollTop = chatBox.scrollHeight;
    }
}

async function setChatting() {
    const date = new Date();
    const koreaTime = new Date(date.getTime() + 9 * 60 * 60 * 1000);
    const response = await fetch(`${HOST}/api/chat?cursor=${encodeURIComponent(koreaTime.toISOString())}`);
    if (response.ok) {
        const data = await response.json();
        renderingChatting(data, true);
    }
}

function setChattingSse() {
    const messageQueue = [];
    let timerId = null;
    const THROTTLE_MS = 100;

    function flushQueue() {
        const messages = messageQueue.splice(0);
        if (messages.length === 0) {
            timerId = null;
            return;
        }
        const tokenValue = util.getTokenValue();
        const chatBox = document.getElementById("chatting-box-section");
        const isNearBottom = chatBox.scrollHeight - chatBox.scrollTop - chatBox.clientHeight <= chatBox.clientHeight * 2;

        messages.forEach(chat => {
            appendChatElementAtBottom(chat, chatBox, tokenValue);
        });

        if (isNearBottom) {
            chatBox.scrollTop = chatBox.scrollHeight;
        }
        timerId = null;
    }

    const eventSource = new EventSource(`${HOST}/api/chat/connection`);
    eventSource.addEventListener("chat", (event) => {
        const chat = JSON.parse(event.data);
        messageQueue.push(chat);
        if (!timerId) {
            timerId = setTimeout(flushQueue, THROTTLE_MS);
        }
    });
    eventSource.onerror = () => {
        if (eventSource && eventSource.readyState !== EventSource.CLOSED) {
            eventSource.close();
        }
        setTimeout(() => {
            setChattingSse();
        }, 1000);
    };
}

async function isSendChatPossible(chatting) {
    if (!chatting) {
        alert("채팅을 입력해주세요.")
        return false;
    }
    if (chatting.trim().length === 0) {
        alert("공백 입력은 불가합니다.")
        return false;
    }
    if (chatting.length > 100) {
        alert("채팅은 최대 100 글자까지 가능합니다.")
        return false;
    }
    const clientInfo = await util.getClientInfo();
    console.log("clientInfo =", clientInfo);
    if (!clientInfo.name) {
        alert("채팅을 입력하려면 닉네임을 입력해주세요.")
        return false;
    }
    if (clientInfo.banned) {
        alert("채팅 전송이 불가합니다.")
        return false;
    }
    return true;
}

function getOldestChatDateTime() {
    const firstChat = document.querySelector(".chat-unit");
    const sendAt = firstChat?.getAttribute("data-send-at");
    const date = new Date(sendAt);
    const koreaTime = new Date(date.getTime() + 9 * 60 * 60 * 1000);
    return koreaTime.toISOString();
}

// 채팅 이벤트 설정
async function setChatEventListener() {
    const chatBox = document.getElementById("chatting-box-section");
    let isLoading = false;
    chatBox.addEventListener("scroll", async () => {
        if (chatBox.scrollTop < 100 && !isLoading) {
            isLoading = true;
            try {
                const dateTime = getOldestChatDateTime();
                const response = await fetch(`${HOST}/api/chat?cursor=${encodeURIComponent(dateTime)}`);
                const data = await response.json();
                appendChatByScroll(data);
            } finally {
                isLoading = false;
            }
        }
    });

    const inputBox = document.getElementById("chatting-input");
    inputBox.addEventListener("click", async () => {
        const name = await util.getNickname();
        if (!name) {
            alert("채팅을 입력하려면 닉네임을 입력해주세요.")
        }
    })

    const button = document.getElementById("chatting-send-button");
    button.addEventListener("click", async () => {
        const chatting = document.getElementById("chatting-input").value;
        if (!await isSendChatPossible(chatting)) {
            return;
        }
        const response = await util.authFetch(`${HOST}/api/chat`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                text: chatting,
            }),
            credentials: "same-origin"
        });
        document.getElementById("chatting-input").value = "";
        if (!response.ok) {
            const errorResponse = await response.json();
            alert(errorResponse.message);
        }
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