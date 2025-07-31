import * as util from "./common.js";

let name;

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

async function displayNickName() {
    name = await util.getNickname();
    const name_input_section = document.getElementById("name-input-section");
    const name_value = document.getElementById("name-value");
    const reset_button = document.getElementById("reset-button");
    if (!name) {
        const name_section = document.getElementById("name-section");
        name_section.style.display = "none";
        name_input_section.style.display = "flex";
        return;
    }
    name_input_section.style.display = "none";
    name_value.style.display = "block";
    name_value.textContent = name;
    reset_button.style.display = "block";
}

function isValidNickname(name) {
    if (!name) {
        alert("닉네임을 입력해주세요");
        return false;
    }
    if (name.trim() === "") {
        alert("공백 입력은 불가합니다")
        return false;
    }
    if (name.length > 10) {
        alert("닉네임은 최대 10 글자까지 입력가능합니다")
        return false;
    }
    return true;
}

function addHeaderEventList() {
    // 닉네임 입력 이벤트
    const name_button = document.getElementById("name-input-button");
    name_button.addEventListener("click", async () => {
        const name = document.getElementById("name-input-box").value;
        if (!isValidNickname(name)) {
            return;
        }
        await updateClient(name);
        location.reload();
    })

    // 엔터 키 이벤트 추가
    const name_input_box = document.getElementById("name-input-box");
    name_input_box.addEventListener("keyup", async (event) => {
        if (event.key === "Enter") {
            const name = document.getElementById("name-input-box").value;
            if (!isValidNickname(name)) {
                return;
            }
            await updateClient(name);
            location.reload();
        }
    });


    // 닉네임 변경 이벤트
    const name_reset_button = document.getElementById("reset-button");
    name_reset_button.addEventListener("click", async () => {
        await updateClient(null);
        location.reload();
    })
}

async function updateClient(name) {
    const response = await util.authFetch(`${HOST}/api/client`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            name: name
        })
    });

    if (!response.ok) {
        const errorResponse = await response.json();
        alert(errorResponse.message);
    }
}

function markCurrentPage() {
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

await util.getOrCreateToken();
displayNickName();
addHeaderEventList();
markCurrentPage();