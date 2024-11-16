const HOST = "https://practicket.com";
// const HOST = "http://localhost:8080";

function hasPermission(name) {
    const key = "permission";
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${key}=`);
    return parts.length === 2;

}

// 예매 페이지 접근 권한 확인
function permissionCheck() {
    window.onload = function() {
        if (hasPermission()) {
            alert("권한 통과")
            document.cookie = "permission=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        } else {
            alert("권한 없음")
            window.location.href = `${HOST}`;
        }
    };
}

permissionCheck();
