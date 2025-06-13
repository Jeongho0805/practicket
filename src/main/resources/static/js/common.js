export async function getNickname() {
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
        .catch();
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




async function calTimeOffset() {
    const localTime = new Date();
    const response = await fetch(`${HOST}/api/server-time`, {
        cache: 'no-store'
    });
    const data = await response.json();
    const serverTime = new Date(data.server_time);
    return serverTime.getTime() - localTime.getTime();
}

let isFirstCall = true;
let timeOffset = 0;
export async function getSyncTime() {
    if (isFirstCall) {
        timeOffset = await calTimeOffset()
        isFirstCall = false;
    }
    return new Date(new Date().getTime() + timeOffset);
}