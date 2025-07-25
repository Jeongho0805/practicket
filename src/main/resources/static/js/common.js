export async function getNickname() {
    return await this.authFetch(`${HOST}/api/client`, {
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

export function getTokenValue() {
    return localStorage.getItem("token");
}

async function ensureToken() {
    let token = localStorage.getItem("token");
    if (token) return token;
    const response = await fetch(`${HOST}/api/client`, { method: "POST" });

    if (!response.ok) {
        const errorResponse = await response.json();
        alert(errorResponse.message);
        return;
    }

    const data = await response.json();
    token = data.token;
    localStorage.setItem("token", token);
    return token;
}

export async function authFetch(url, options = {}) {
    const token = await ensureToken();

    options.headers = {
        ...(options.headers || {}),
        "Authorization": `Bearer ${token}`
    };

    return fetch(url, options);
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