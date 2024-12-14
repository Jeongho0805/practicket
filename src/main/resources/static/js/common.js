// export const host = "https://practicket.com"
export const HOST = "http://localhost:8080"

// 닉네임 값 가져오기
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