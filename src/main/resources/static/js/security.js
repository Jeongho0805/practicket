const mainSection = document.getElementById("main-section");
const titleSection = document.getElementById("title-section")
const guideSection = document.getElementById("guide-section");
const startButton = document.getElementById("start-button");
const securityLetterSection = document.getElementById("security-letter-section");
const correctCountTag = document.getElementById("correct-count");
const securityCountMessage = document.getElementById("security-letter-count-message");
const securityInput = document.getElementById("security-input")
const resultSection = document.getElementById("result-section");
const successMessage = "보안문자 입력에 성공하였습니다.";
const errorMessage = "보안문자를 잘못 입력하였습니다.";
let correctCount = 0;

function startCountDown() {
    const timeInfo = document.createElement("h3")
    timeInfo.id = "countdown";
    timeInfo.textContent = "3";
    guideSection.querySelectorAll("*").forEach(child => {
        child.style.display = "none";
    });
    guideSection.appendChild(timeInfo);
    guideSection.style.border = "none";
    let count = 3;
    return new Promise(resolve => {
        const interval = setInterval(() => {
            count--;
            timeInfo.textContent = count.toString();
            if (count <= 0) {
                timeInfo.remove();
                clearInterval(interval);
                resolve();
            }
        }, 1000);
    });
}

function generateCaptcha() {
    const canvas = document.getElementById("security-letter-image");
    if (!canvas) {
        console.error("🚨 [오류] 'security-letter-section' ID를 가진 <canvas> 요소가 없음!");
        return;
    }

    const ctx = canvas.getContext("2d");
    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    let captchaText = "";

    // 캔버스 크기 설정
    canvas.width = 250;
    canvas.height = 80;

    // 🎨 랜덤한 배경색 생성 (너무 어둡거나 밝지 않도록)
    function getRandomBackgroundColor() {
        const r = Math.floor(Math.random() * 100) + 50; // 50~150 (어두운 색 피하기)
        const g = Math.floor(Math.random() * 100) + 50;
        const b = Math.floor(Math.random() * 100) + 50;
        return `rgb(${r}, ${g}, ${b})`;
    }

    // 배경색 설정
    const bgColor = getRandomBackgroundColor();
    ctx.fillStyle = bgColor;
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // 🎨 랜덤한 글자색 생성 (배경색과 대비되도록)
    function getRandomTextColor(bgColor) {
        const [r, g, b] = bgColor.match(/\d+/g).map(Number); // 배경색 RGB 값 가져오기
        const textR = 255 - r + Math.floor(Math.random() * 50) - 25; // 반대 계열 색상 조절
        const textG = 255 - g + Math.floor(Math.random() * 50) - 25;
        const textB = 255 - b + Math.floor(Math.random() * 50) - 25;
        return `rgb(${Math.abs(textR)}, ${Math.abs(textG)}, ${Math.abs(textB)})`;
    }

    // 글자색 설정
    const textColor = getRandomTextColor(bgColor);

    // 랜덤한 보안문자 생성
    for (let i = 0; i < 6; i++) {
        captchaText += chars.charAt(Math.floor(Math.random() * chars.length));
    }

    ctx.font = "bold 40px Arial";
    ctx.textBaseline = "middle";
    ctx.textAlign = "center";

    // 보안문자 출력 (약간의 회전 적용)
    for (let i = 0; i < captchaText.length; i++) {
        const x = 40 + i * 35; // 글자 간격 조정
        const y = 45 + Math.random() * 5 - 2.5; // 위치 랜덤화
        const angle = Math.random() * 0.1 - 0.05; // 회전 각도 (-0.05 ~ 0.05 radian)

        ctx.save(); // 현재 상태 저장
        ctx.translate(x, y);
        ctx.rotate(angle);
        ctx.fillStyle = textColor;
        ctx.fillText(captchaText[i], 0, 0);
        ctx.restore(); // 이전 상태 복구
    }

    // 랜덤한 선 추가 (적당한 개수로 조절)
    for (let i = 0; i < 2; i++) {
        ctx.strokeStyle = "#FFFFFF"; // 밝은 색 선
        ctx.lineWidth = 1.5; // 선 굵기 줄이기
        ctx.beginPath();
        ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height);
        ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height);
        ctx.stroke();
    }

    // 랜덤한 점 추가 (난이도 조절)
    for (let i = 0; i < 50; i++) {
        ctx.fillStyle = "yellow";
        ctx.beginPath();
        ctx.arc(Math.random() * canvas.width, Math.random() * canvas.height, 1.5, 0, 2 * Math.PI);
        ctx.fill();
    }

    // 보안문자 저장
    canvas.dataset.captcha = captchaText;
}

function toggleElements() {
    if (titleSection.style.display === "none") {
        titleSection.style.display = "block"
        startButton.style.display = "inline-block";
        securityLetterSection.style.display = "none"
        mainSection.style.display = "grid"
        resultSection.style.display = "flex";
        guideSection.querySelectorAll("*").forEach(child => {
            child.style.display = "block";
        });
        guideSection.style.display = "flex";
        guideSection.style.border = "2px solid darkslateblue";
    } else {
        titleSection.style.display = "none"
        guideSection.style.display = "none";
        startButton.style.display = "none";
        resultSection.style.display = "none";
        securityLetterSection.style.display = "grid"
        mainSection.style.display = "flex"
    }
}

function markCorrectCount() {
    correctCountTag.innerText = correctCount;
}

function startSecurityTest() {
    markCorrectCount();
    generateCaptcha();
    securityInput.focus();
    return new Promise(resolve => {
        const inputBox = document.getElementById("security-input");
        const inputButton = document.getElementById("security-input-button");

        inputButton.addEventListener("click", () => checkCaptcha(resolve));

        inputBox.addEventListener("keypress", (event) => {
            if (event.key === "Enter") {
                event.preventDefault(); // 기본 엔터 키 동작 방지 (폼 제출 방지)
                checkCaptcha(resolve);
            }
        });
    });
}

function checkCaptcha(resolve) {
    const userInput = securityInput.value.toUpperCase();
    const canvas = document.getElementById("security-letter-image");
    const correctText = canvas.dataset.captcha;
    securityInput.value = "";

    if (userInput === correctText) {
        correctCount++;
        securityCountMessage.innerText = successMessage;
        securityCountMessage.style.color = "#3CB371"

        if (correctCount < 3) {
            markCorrectCount();
            generateCaptcha();
        } else {
            correctCount = 0;
            resolve();
        }
    } else {
        securityCountMessage.innerText = errorMessage;
        securityCountMessage.style.color = "red";
    }
}

function addEventList() {
    startButton.addEventListener("click", async () => {
        await startCountDown();
        toggleElements();
        // 테스트 시작 시간 측정
        await startSecurityTest();
        // 테스트 끝 시간 측정
        toggleElements();
        // api 콜을 통해 서버에 소요시간 전송
        // 입력 결과 업데이트
    })
}

addEventList();
