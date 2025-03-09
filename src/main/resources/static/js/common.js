export async function getNickname() {
    console.log("api 요청")
    return await fetch(`${HOST}/api/auth`, {
        method: "GET",
        credentials: 'same-origin',
        headers: {
            "Content-Type": "application/json",
        },
    })
        .then(response => response.json())
        .then(data => {
            return data.name;
        })
        .catch(e => {console.log(e)});
}

export function getAuthValue() {
    const cookies = document.cookie.split('; '); // 모든 쿠키를 배열로 분리
    for (let cookie of cookies) {
        const [name, value] = cookie.split('='); // 쿠키 이름과 값을 분리
        if (name === "auth") {
            return value;
        }
    }
    return null;
}

const mobilePageInfos = {
    ticketing: document.getElementById("ticketing-page-btn"),
    rank: document.getElementById("rank-page-btn"),
    security: document.getElementById("security-page-btn"),
    blog: document.getElementById("blog-page-btn")
}

const desktopPageInfos = {
    ticketing: document.getElementById("ticketing-desktop-btn-btn"),
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
    Object.values(desktopPageInfos).forEach(btn => {
        btn.style.backgroundColor = "white";
    });
    desktopButton.style.color = "darkslateblue";
}