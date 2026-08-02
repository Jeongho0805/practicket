import * as util from "./common.js";
import { authFetch } from "./common.js";

const CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
const CAPTCHA_LENGTH = 6;
const CAPTCHA_ROUNDS = 3;
const CAPTCHA_SCALE = 2;
const COUNTDOWN_SECONDS = 3;
const RANK_SLOTS = 5;

const homeSection = document.getElementById("home-section");
const playSection = document.getElementById("play-section");
const startButton = document.getElementById("start-button");
const canvas = document.getElementById("security-letter-image");
const input = document.getElementById("security-input");
const inputButton = document.getElementById("security-input-button");
const refreshButton = document.getElementById("captcha-refresh");
const playMessage = document.getElementById("play-message");
const timerValue = document.getElementById("timer-value");
const progressTag = document.getElementById("play-progress");
const stepTags = document.querySelectorAll("#play-steps i");

let answer = "";
let round = 0;
let startedAt = 0;
let timerId = null;
let justFinished = null;

function drawCaptcha() {
    const ctx = canvas.getContext("2d");
    canvas.width = 250 * CAPTCHA_SCALE;
    canvas.height = 80 * CAPTCHA_SCALE;

    const r = Math.floor(Math.random() * 100) + 50;
    const g = Math.floor(Math.random() * 100) + 50;
    const b = Math.floor(Math.random() * 100) + 50;
    ctx.fillStyle = `rgb(${r}, ${g}, ${b})`;
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // 보색은 색상만 반대일 뿐 밝기가 비슷해질 수 있어 글자가 배경에 묻힌다. 밝기 차이를 강제한다.
    const luminance = 0.299 * r + 0.587 * g + 0.114 * b;
    const pick = () => (luminance < 118
        ? 240 + Math.floor(Math.random() * 16)
        : Math.floor(Math.random() * 26));
    ctx.fillStyle = `rgb(${pick()}, ${pick()}, ${pick()})`;

    answer = "";
    for (let i = 0; i < CAPTCHA_LENGTH; i++) {
        answer += CAPTCHA_CHARS.charAt(Math.floor(Math.random() * CAPTCHA_CHARS.length));
    }

    ctx.font = `bold ${40 * CAPTCHA_SCALE}px Arial`;
    ctx.textBaseline = "middle";
    ctx.textAlign = "center";

    const textColor = ctx.fillStyle;
    for (let i = 0; i < answer.length; i++) {
        ctx.save();
        ctx.translate((40 + i * 35) * CAPTCHA_SCALE, (45 + Math.random() * 5 - 2.5) * CAPTCHA_SCALE);
        ctx.rotate(Math.random() * 0.1 - 0.05);
        ctx.fillStyle = textColor;
        ctx.fillText(answer[i], 0, 0);
        ctx.restore();
    }

    ctx.strokeStyle = "#FFFFFF";
    ctx.lineWidth = 1.5 * CAPTCHA_SCALE;
    for (let i = 0; i < 2; i++) {
        ctx.beginPath();
        ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height);
        ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height);
        ctx.stroke();
    }

    ctx.fillStyle = "yellow";
    for (let i = 0; i < 50; i++) {
        ctx.beginPath();
        ctx.arc(Math.random() * canvas.width, Math.random() * canvas.height, 1.5 * CAPTCHA_SCALE, 0, 2 * Math.PI);
        ctx.fill();
    }

    canvas.dataset.captcha = answer;
}

// 버튼을 감추고 다른 자리에 숫자를 띄우면 그만큼 화면이 출렁여서, 버튼 안에서 센다
function startCountDown() {
    let count = COUNTDOWN_SECONDS;
    startButton.disabled = true;
    startButton.classList.add("counting");
    startButton.textContent = count;

    return new Promise(resolve => {
        const interval = setInterval(() => {
            count--;
            if (count <= 0) {
                clearInterval(interval);
                startButton.disabled = false;
                startButton.classList.remove("counting");
                startButton.textContent = "START";
                resolve();
                return;
            }
            startButton.textContent = count;
        }, 1000);
    });
}

function showRound() {
    stepTags.forEach((tag, index) => tag.classList.toggle("on", index <= round));
    progressTag.textContent = `${round + 1} / ${CAPTCHA_ROUNDS}`;
}

function startTimer() {
    startedAt = performance.now();
    stopTimer();
    // 0.01초 단위로 표시하면 마지막 자리가 초당 100번 바뀌어 캡차를 읽는 데 방해가 된다.
    timerId = setInterval(() => {
        timerValue.textContent = ((performance.now() - startedAt) / 1000).toFixed(1);
    }, 100);
}

function stopTimer() {
    if (timerId) {
        clearInterval(timerId);
        timerId = null;
    }
}

function openPlay() {
    homeSection.hidden = true;
    playSection.hidden = false;
    round = 0;
    playMessage.innerHTML = "";
    timerValue.textContent = "0.0";
    showRound();
    drawCaptcha();
    input.value = "";
    input.focus();
    startTimer();
}

function openHome() {
    stopTimer();
    playSection.hidden = true;
    homeSection.hidden = false;
}

function checkAnswer() {
    const typed = input.value.trim().toUpperCase();
    if (!typed) {
        return;
    }

    if (typed !== answer) {
        playMessage.innerHTML = '<span class="no">다시 확인해주세요</span>';
        input.value = "";
        input.focus();
        return;
    }

    round++;
    input.value = "";

    if (round >= CAPTCHA_ROUNDS) {
        finish();
        return;
    }

    playMessage.innerHTML = `<span class="ok">정답! ${CAPTCHA_ROUNDS - round}개 남았어요</span>`;
    showRound();
    drawCaptcha();
    input.focus();
}

async function finish() {
    const elapsed = (performance.now() - startedAt) / 1000;
    stopTimer();
    justFinished = elapsed;

    await postResult(elapsed);
    openHome();
    await renderStatistic();
}

async function postResult(elapsed) {
    try {
        const response = await authFetch("/api/captcha", {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ elapsed_time: elapsed.toFixed(2) })
        });
        if (!response.ok) {
            await util.showAlert({ title: "서버 오류", msg: "기록 저장에 실패했습니다." });
        }
    } catch (e) {
        await util.showAlert({ title: "서버 오류", msg: "기록 저장에 실패했습니다." });
    }
}

async function fetchStatistic() {
    try {
        const response = await authFetch("/api/captcha", {
            method: "GET",
            credentials: "same-origin",
            headers: { "Content-Type": "application/json" }
        });
        return response.ok ? await response.json() : null;
    } catch (e) {
        return null;
    }
}

const seconds = value => `${Number(value).toFixed(2)}초`;

const escape = value => String(value ?? "").replace(/[&<>"']/g,
    ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[ch]));

function renderDistribution(distribution, hasRecord, totalCount) {
    const bars = document.getElementById("dist-bars");
    const axis = document.getElementById("dist-axis");
    const caption = document.getElementById("dist-caption");
    const hint = document.getElementById("dist-hint");

    if (!distribution || !distribution.counts || !distribution.counts.length) {
        bars.innerHTML = "";
        axis.innerHTML = "";
        return;
    }

    const counts = distribution.counts;
    const max = Math.max(...counts, 1);
    const emptySlot = hasRecord ? -1 : Math.floor(counts.length / 2);

    bars.innerHTML = counts.map((count, index) => {
        const height = Math.max(6, Math.round((count / max) * 100));
        if (index === distribution.my_bin) {
            return `<i class="me" style="height:${height}%"></i>`;
        }
        if (index === emptySlot) {
            return `<i class="slot" style="height:${height}%"></i>`;
        }
        return `<i style="height:${height}%"></i>`;
    }).join("");

    const lower = distribution.lower_bound;
    const middle = lower + distribution.bin_width * (counts.length / 2);
    const upper = lower + distribution.bin_width * counts.length;
    axis.innerHTML = `<span>${lower.toFixed(1)}초</span>`
        + `<span>${middle.toFixed(1)}초</span>`
        + `<span>${upper.toFixed(1)}초 이상</span>`;

    caption.textContent = "다른 사용자들은 이 정도 걸렸어요";
    caption.hidden = hasRecord;
    hint.hidden = hasRecord;
}

function statCard(label, value, note, highlight) {
    return `<div class="stat${highlight ? " hi" : ""}">`
        + `<span>${label}</span><b>${value}</b><em>${note}</em></div>`;
}

function renderStats(result, hasRecord) {
    const grid = document.getElementById("stat-grid");
    const totalCount = result.total_count ?? 0;

    if (!hasRecord) {
        const fastest = result.top_ranking?.[0];
        grid.innerHTML = statCard("전체 기록", `${totalCount.toLocaleString()}개`, "지금도 쌓이는 중", false)
            + statCard("전체 평균", seconds(result.total_avg_result), "모든 기록 기준", false)
            + statCard("최고 기록", fastest ? seconds(fastest.elapsed_second) : "—", fastest ? `${fastest.nickname} 님` : "", false);
        return;
    }

    const gap = result.total_avg_result - result.best_result;
    grid.innerHTML = statCard("내 최단", seconds(result.best_result), `${result.my_count}회 중`, false)
        + statCard("내 평균", seconds(result.my_avg_result), `${result.my_count}회`, false)
        + statCard("전체 평균", seconds(result.total_avg_result), gap > 0 ? `나보다 ${gap.toFixed(2)}초 느림` : "나보다 빠름", false);
}

// 자리 수가 고정이라 기록자가 몇 명이든 패널 높이가 변하지 않는다
function renderRanking(result) {
    const panel = document.getElementById("rank-panel");
    const rows = document.getElementById("rank-rows");
    const ranking = result.top_ranking ?? [];
    const people = result.today_people ?? 0;

    panel.hidden = false;
    document.getElementById("rank-total").textContent =
        people > 0 ? `오늘 ${people.toLocaleString()}명` : "오늘 첫 기록을 기다리는 중";

    rows.innerHTML = Array.from({ length: RANK_SLOTS }, (unused, index) => {
        const row = ranking[index];
        if (!row) {
            return `<div class="rank-row empty"><span class="no">${index + 1}</span>`
                + `<span class="who">—</span><span class="time">—</span></div>`;
        }
        return `<div class="rank-row${row.mine ? " mine" : ""}">`
            + `<span class="no${index < 3 ? " top" : ""}">${index + 1}</span>`
            + `<span class="who">${escape(row.nickname)}</span>`
            + `<span class="time">${seconds(row.elapsed_second)}</span></div>`;
    }).join("");
}

function renderTrend(result) {
    const panel = document.getElementById("trend-panel");
    const bars = document.getElementById("trend-bars");
    const recent = result.recent_results ?? [];

    if (recent.length < 2) {
        panel.hidden = true;
        return;
    }

    panel.hidden = false;
    const max = Math.max(...recent);
    const best = Math.min(...recent);

    document.getElementById("trend-title").textContent = `내 최근 ${recent.length}회`;
    document.getElementById("trend-note").textContent =
        recent[recent.length - 1] <= best ? "최고 기록 경신" : "꾸준히 도전 중";

    bars.innerHTML = recent.map(value => {
        const height = Math.round(16 + (value / max) * 36);
        return `<div><i class="${value === best ? "best" : ""}" style="height:${height}px"></i>`
            + `<span>${value.toFixed(1)}초</span></div>`;
    }).join("");
}

// 방금 낸 기록만 분포 카드 머리에 얹는다. 다시 들어오면 사라진다
function renderHero(result, hasRecord) {
    const hero = document.getElementById("dist-hero");
    const title = document.getElementById("dist-title");
    const show = justFinished !== null && hasRecord;

    hero.hidden = !show;
    title.hidden = show;
    if (!show) {
        return;
    }

    document.getElementById("dist-hero-time").textContent = seconds(result.latest_result);
    document.getElementById("dist-hero-meta").textContent =
        `전체 ${result.latest_rank.toLocaleString()}위 · 상위 ${result.latest_percentile.toFixed(1)}%`;
}

async function renderStatistic() {
    const result = await fetchStatistic();
    if (!result) {
        return;
    }

    const hasRecord = (result.my_count ?? 0) > 0;
    renderHero(result, hasRecord);
    renderDistribution(result.distribution, hasRecord, result.total_count);
    renderStats(result, hasRecord);
    renderRanking(result);
    renderTrend(result);
}

startButton.addEventListener("click", async () => {
    if (!await util.getNickname()) {
        await util.showAlert({ title: "닉네임 필요", msg: "닉네임을 입력해주세요." });
        return;
    }
    justFinished = null;
    await startCountDown();
    openPlay();
});

inputButton.addEventListener("click", checkAnswer);
input.addEventListener("keydown", event => {
    if (event.key === "Enter") {
        checkAnswer();
    }
});
refreshButton.addEventListener("click", () => {
    drawCaptcha();
    input.value = "";
    input.focus();
});

window.addEventListener("load", renderStatistic);
