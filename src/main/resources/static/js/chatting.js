import {HOST} from "./common";

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

setChatEventListener();
setChattingSse();