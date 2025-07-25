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

export async function getClientInfo() {
    return await this.authFetch(`${HOST}/api/client`, {
        method: "GET",
        credentials: 'same-origin',
        headers: {
            "Content-Type": "application/json",
        },
    })
        .then(response => response.json())
        .then(data => {
            return data;
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

export async function authFetch(url, options = {}) {
    const token = await getOrCreateToken();

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

export function getTokenValue() {
    return localStorage.getItem("token");
}

export async function getOrCreateToken() {
    // 1. LocalStorage 토큰이 존재하는 경우, 바로 토큰 반환
    let token = localStorage.getItem("token");
    if (token) {
        return token;
    }
    // 2. LocalStorage 토큰이 없는 경우, index DB에 토큰이 존재하면 복원 후 반환
    try {
        token = await getTokenFromIndexedDB();
    } catch (e) {
        console.error("indexDB 에서 토큰 조회 실패", e);
    }
    if (token) {
        localStorage.setItem("token", token);
        return token;
    }
    // 3. LocalStorage, indexedDB 전부 토큰이 존재하지 않는 경우 -> 토큰 발급 api 호출
    const response = await fetch(`${HOST}/api/client`, { method: "POST" });
    if (!response.ok) {
        const errorResponse = await response.json();
        alert(errorResponse.message);
        return;
    }
    const data = await response.json();
    token = data.token;
    try {
        await saveTokenToIndexedDB(token);
        localStorage.setItem("token", token);
    } catch (e) {
        alert("서버 에러로 사이트를 정상적으로 활용할 수 없습니다.")
    }
    return token;
}

const TOKEN_DB_NAME = "tokenDB";
const TOKEN_STORE_NAME = "tokenStore"
const TOKEN_DATA_KEY = "token";

function saveTokenToIndexedDB(token) {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(TOKEN_DB_NAME, 1);

        request.onupgradeneeded = function (event) {
            const db = event.target.result;
            if (!db.objectStoreNames.contains(TOKEN_STORE_NAME)) {
                db.createObjectStore(TOKEN_STORE_NAME);
            }
        };

        request.onsuccess = function (event) {
            const db = event.target.result;
            const tx = db.transaction(TOKEN_STORE_NAME, "readwrite");
            tx.onerror = function (event) {
                console.error("Transaction error:", event.target.error);
                reject(event.target.error);
            };

            const store = tx.objectStore(TOKEN_STORE_NAME);
            const putRequest = store.put(token, TOKEN_DATA_KEY);

            putRequest.onsuccess = function () {
                tx.oncomplete = function () {
                    db.close();
                    resolve();
                };
            };
            putRequest.onerror = function (event) {
                console.error("Put request error:", event.target.error);
                reject(event.target.error);
            };
        };

        request.onerror = function (event) {
            console.error("DB open error:", event.target.error);
            reject(event.target.error);
        };
    });
}

function getTokenFromIndexedDB() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(TOKEN_DB_NAME, 1);

        request.onupgradeneeded = function (event) {
            const db = event.target.result;
            if (!db.objectStoreNames.contains(TOKEN_STORE_NAME)) {
                db.createObjectStore(TOKEN_STORE_NAME);
            }
        };

        request.onsuccess = function (event) {
            const db = event.target.result;
            const tx = db.transaction(TOKEN_STORE_NAME, "readonly");
            const store = tx.objectStore(TOKEN_STORE_NAME);

            const getRequest = store.get(TOKEN_DATA_KEY);

            getRequest.onsuccess = function () {
                tx.oncomplete = function() {
                    db.close();
                    resolve(getRequest.result || null);
                };
            };

            getRequest.onerror = function (event) {
                console.error("Get request error:", event.target.error);
                reject(event.target.error);
            };
        };

        request.onerror = function (event) {
            console.error("DB open error:", event.target.error);
            reject(event.target.error);
        };
    });
}
