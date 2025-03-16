import { getNickname } from "./common.js";

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

function addHeaderEventList() {
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

    // 엔터 키 이벤트 추가
    const name_input_box = document.getElementById("name-input-box");
    name_input_box.addEventListener("keyup", async (event) => {
        if (event.key === "Enter") {
            const name = document.getElementById("name-input-box").value;
            if (name === "" || !name) {
                alert("닉네임을 입력해주세요.");
                return
            }
            await requestCreateSession(name);
            location.reload();
        }
    });


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

const mobilePageInfos = {
    ticketing: document.getElementById("ticketing-page-btn"),
    rank: document.getElementById("rank-page-btn"),
    security: document.getElementById("security-page-btn"),
    blog: document.getElementById("blog-page-btn")
}

const desktopPageInfos = {
    ticketing: document.getElementById("ticketing-desktop-btn"),
    rank: document.getElementById("rank-desktop-btn"),
    security: document.getElementById("security-desktop-btn"),
    blog: document.getElementById("blog-desktop-btn")
}

export function markCurrentPage() {
    let currentPath = window.location.pathname;
    currentPath = currentPath === "/" ? "ticketing" : currentPath.substring(1);

    // 모바일 버튼 처리
    const mobileButton = mobilePageInfos[currentPath];
    Object.values(mobilePageInfos).forEach(btn => {
        btn.style.backgroundColor = "white";
    });
    mobileButton.style.backgroundColor = "darkslateblue";
    mobileButton.style.color = "white";

    // 데스크탑 버튼 처리
    const desktopButton = desktopPageInfos[currentPath];
    desktopButton.style.color = "darkslateblue";
}

addHeaderEventList();
markCurrentPage();