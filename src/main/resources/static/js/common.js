// export const host = "https://practicket.com"
export const host = "http://localhost:8080"

// 닉네임 값 가져오기
export function getNickname() {
    const cookies = document.cookie.split(';');
    let cookie_key = "name";
    for (let i = 0; i < cookies.length; i++) {
        let cookie = cookies[i].trim(); // 공백 제거
        if (cookie.startsWith(cookie_key + '=')) {
            return cookie.substring(cookie_key.length + 1);
        }
    }
}