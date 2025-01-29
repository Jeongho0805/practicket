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
            console.log(data)
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

const pageInfo = {
    ticketing: document.getElementById("ticketing-page-btn"),
    rank: document.getElementById("rank-page-btn"),
    security: document.getElementById("security-page-btn")
}

export function markCurrentPage() {
    let currentPath = window.location.pathname;
    currentPath = currentPath === "/" ? "ticketing" : currentPath.substring(1);
    const button = pageInfo[currentPath];

    Object.values(pageInfo).forEach(btn => {
        btn.style.backgroundColor = "white";
        button.style.color = "darkslateblue";
    });
    button.style.backgroundColor = "darkslateblue";
    button.style.color = "white";
}