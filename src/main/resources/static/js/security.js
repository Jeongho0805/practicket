import  * as util from "./common.js";
import {authFetch} from "./common.js";
const mainSection = document.getElementById("main-section");
const titleSection = document.getElementById("title-section")
const guideSection = document.getElementById("guide-section");
const startButton = document.getElementById("start-button");
const securityLetterSection = document.getElementById("security-letter-section");
const correctCountTag = document.getElementById("correct-count");
const securityCountMessage = document.getElementById("security-letter-count-message");
const securityInput = document.getElementById("security-input")
const resultSection = document.getElementById("result-section");
const inputBox = document.getElementById("security-input");
const inputButton = document.getElementById("security-input-button");
const successMessage = "보안문자 입력에 성공하였습니다.";
const errorMessage = "보안문자를 잘못 입력하였습니다.";
let correctCount = 0;
let isStart = false;
let isToggleOpen = true;

function startCountDown() {
    const startButton = document.getElementById("start-button");
    const displaySetting = startButton.style.display;
    startButton.style.display = "none";
    const timeInfo = document.createElement("h3")
    timeInfo.id = "countdown";
    timeInfo.textContent = "3";
    guideSection.querySelectorAll(".guide-description").forEach(child => {
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
                startButton.style.display = displaySetting;
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
    if (!isToggleOpen) {
        if (window.matchMedia("(min-width: 769px)").matches) {
            titleSection.style.display = "block"
        }
        startButton.style.display = "inline-block";
        securityLetterSection.style.display = "none"
        const securitySection = document.getElementById("security-section");
        securitySection.style.display = "none";
        mainSection.style.display = "grid";
        mainSection.style.alignItems = "";
        mainSection.style.justifyContent = "";
        resultSection.style.display = "flex";
        guideSection.querySelectorAll(".guide-description").forEach(child => {
            child.style.display = "block";
        });
        guideSection.style.display = "flex";
        isToggleOpen = true;
    } else {
        titleSection.style.display = "none"
        guideSection.style.display = "none";
        startButton.style.display = "none";
        resultSection.style.display = "none";
        securityLetterSection.style.display = "grid"
        const securitySection = document.getElementById("security-section");
        securitySection.style.display = "flex";
        mainSection.style.display = "flex";
        mainSection.style.alignItems = "center";
        mainSection.style.justifyContent = "center";
        isToggleOpen = false;
    }
}

function markCorrectCount() {
    correctCountTag.innerText = correctCount;
}

function checkCaptcha() {
    if (!isStart) {
        return;
    }
    securityInput.focus();
    const userInput = securityInput.value.toUpperCase();
    const canvas = document.getElementById("security-letter-image");
    const correctText = canvas.dataset.captcha;

    if (userInput === correctText) {
        correctCount++;
        securityCountMessage.innerText = successMessage;
        securityCountMessage.className = "message-success";

        if (correctCount < 3) {
            markCorrectCount();
            generateCaptcha();
        } else {
            securityCountMessage.innerText = "";
            securityCountMessage.className = "";
            correctCount = 0;
            isStart = false;
        }
    } else {
        securityCountMessage.innerText = errorMessage;
        securityCountMessage.className = "message-error";
    }
    securityInput.value = "";
    securityInput.focus();
}

function startSecurityTest() {
    isStart = true;
    markCorrectCount();
    generateCaptcha();
    securityInput.focus();
    return new Promise(resolve => {
        const interval = setInterval(() => {
            if (isStart === false) {
                clearInterval(interval);
                resolve();
            }
        }, 200);
    });
}

async function fetchCreateElapsedTime(startTime, endTime) {
    const elapsedTime = ((endTime - startTime) / 1000).toFixed(2);
    const response = await authFetch("/api/captcha", {
        method: "POST",
        credentials: 'same-origin',
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ elapsed_time: elapsedTime }) // ✅ JSON 키를 snake_case로 변환
    });
    if (!response.ok) {
        await util.showAlert({ title: '서버 오류', msg: '서버 오류로 전송에 실패했습니다.' });
    }
}

async function fetchGetResult() {
    let response;
    try {
        response = await authFetch("/api/captcha", {
            method: "GET",
            credentials: 'same-origin',
            headers: {
                "Content-Type": "application/json"
            },
        });
    } catch (e) {
        return null;
    }
    if (!response.ok) {
        return;
    }
    return await response.json();
}

async function updateResultValue() {
    const result = await fetchGetResult();
    const resultSection = document.getElementById("result-section");
    if (!result || result.latest_result === 0) {
        resultSection.style.display = "none";
    }
    else  {
        const myLatestResult = document.getElementById("my-latest-result-value")
        const myAverageResult = document.getElementById("my-average-result-value")
        const totalAverageResult = document.getElementById("total-average-result-value")
        const myLatestPercentile = document.getElementById("my-latest-percentile-value")
        const myLatestRank = document.getElementById("my-latest-rank-value")
        const myBestResult = document.getElementById("my-best-result-value")
        myLatestResult.innerText = `${Number(result.latest_result).toFixed(2)}초`;
        myAverageResult.innerText = `${Number(result.my_avg_result).toFixed(2)}초`;
        totalAverageResult.innerText = `${Number(result.total_avg_result).toFixed(2)}초`;
        myLatestPercentile.innerText = `${Number(result.latest_percentile).toFixed(2)}%`;
        myLatestRank.innerText = `${result.latest_rank}위`;
        myBestResult.innerText = `${Number(result.best_result).toFixed(2)}초`;
        resultSection.style.display = "flex";
    }
}

function addEventList() {
    // 보안문자 입력 관련 이벤트 등록
    inputButton.addEventListener("click", () => checkCaptcha());
    inputBox.addEventListener("keypress", (event) => {
        if (event.key === "Enter") {
            event.preventDefault(); // 기본 엔터 키 동작 방지 (폼 제출 방지)
            checkCaptcha();
        }
    });

    // 보안문자 입력 테스트 시작 이벤트 등록
    startButton.addEventListener("click", async () => {
        if (!await util.getNickname()) {
            await util.showAlert({ title: '닉네임 필요', msg: '닉네임을 입력해주세요.' });
            return;
        }
        await startCountDown();
        toggleElements();
        const startTime = performance.now();
        await startSecurityTest();
        const endTime = performance.now();
        toggleElements();
        await fetchCreateElapsedTime(startTime, endTime);
        await updateResultValue();
    })
}

window.onload = async () => {
    await updateResultValue();
};
await updateResultValue();
addEventList();