import * as util from "./common.js";

let name;
let editingFromUnset = false;

const mobilePageInfos = {
    ticketing: document.getElementById("ticketing-page-btn"),
    practice: document.getElementById("practice-page-btn"),
    security: document.getElementById("security-page-btn"),
    blog: document.getElementById("blog-page-btn"),
    art: document.getElementById("art-page-btn")
}

const desktopPageInfos = {
    ticketing: document.getElementById("ticketing-desktop-btn"),
    practice: document.getElementById("practice-desktop-btn"),
    security: document.getElementById("security-desktop-btn"),
    blog: document.getElementById("blog-desktop-btn"),
    art: document.getElementById("art-desktop-btn")
}

async function displayNickName() {
    name = await util.getNickname();
    const chip = document.getElementById("nickname-chip");
    const chipUnset = document.getElementById("nickname-chip-unset");
    const inputSection = document.getElementById("name-input-section");

    inputSection.style.display = "none";

    if (!name) {
        chip.style.display = "none";
        chipUnset.style.display = "flex";
        return;
    }

    chipUnset.style.display = "none";
    document.getElementById("name-value").textContent = name;
    chip.style.display = "flex";
}

function showInputSection(prefill = "") {
    const chip = document.getElementById("nickname-chip");
    const chipUnset = document.getElementById("nickname-chip-unset");
    const inputSection = document.getElementById("name-input-section");
    const inputBox = document.getElementById("name-input-box");

    chip.style.display = "none";
    chipUnset.style.display = "none";
    inputSection.style.display = "flex";
    inputBox.value = prefill;
    inputBox.focus();
    if (prefill) inputBox.select();
}

function hideInputSection() {
    const chip = document.getElementById("nickname-chip");
    const chipUnset = document.getElementById("nickname-chip-unset");
    const inputSection = document.getElementById("name-input-section");

    inputSection.style.display = "none";

    if (editingFromUnset) {
        chipUnset.style.display = "flex";
    } else {
        chip.style.display = "flex";
    }
    editingFromUnset = false;
}

async function isValidNickname(name) {
    if (!name) {
        await util.showAlert({ title: '입력 오류', msg: '닉네임을 입력해주세요.' });
        return false;
    }
    if (name.trim() === "") {
        await util.showAlert({ title: '입력 오류', msg: '공백 입력은 불가합니다.' });
        return false;
    }
    if (name.length > 10) {
        await util.showAlert({ title: '입력 오류', msg: '닉네임은 최대 10 글자까지 입력가능합니다.' });
        return false;
    }
    return true;
}

async function submitNickname() {
    const inputName = document.getElementById("name-input-box").value;
    if (!await isValidNickname(inputName)) return;
    await updateClient(inputName);
    location.reload();
}

function addHeaderEventList() {
    // chip 클릭 → 현재 닉네임 pre-fill 후 편집
    document.getElementById("nickname-chip").addEventListener("click", () => {
        editingFromUnset = false;
        showInputSection(name || "");
    });

    // chip-unset 클릭 → 빈 인풋으로 편집
    document.getElementById("nickname-chip-unset").addEventListener("click", () => {
        editingFromUnset = true;
        showInputSection("");
    });

    // 취소
    document.getElementById("name-input-cancel").addEventListener("click", () => {
        hideInputSection();
    });

    // 완료 버튼
    document.getElementById("name-input-button").addEventListener("click", submitNickname);

    // 엔터 / ESC 키
    document.getElementById("name-input-box").addEventListener("keydown", async (event) => {
        if (event.key === "Enter") await submitNickname();
        if (event.key === "Escape") hideInputSection();
    });
}

async function updateClient(name) {
    const response = await util.authFetch(`${HOST}/api/client`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            name: name
        })
    });

    if (!response.ok) {
        const errorResponse = await response.json();
        await util.showAlert({ title: '오류', msg: errorResponse.message });
    }
}

function markCurrentPage() {
    let currentPath = window.location.pathname;
    currentPath = currentPath.substring(1);

    let pageName = currentPath;
    if (currentPath.startsWith("art")) pageName = "art";
    if (currentPath.startsWith("practice")) pageName = "practice";
    if (currentPath.startsWith("security")) pageName = "security";
    if (currentPath.startsWith("blog")) pageName = "blog";

    // 모바일 버튼 처리
    Object.values(mobilePageInfos).forEach(btn => {
        if (btn) btn.classList.remove('nav-current');
    });
    const mobileButton = mobilePageInfos[pageName];
    if (mobileButton) mobileButton.classList.add('nav-current');

    // 데스크탑 버튼 처리 — 연한 틴트 pill 활성 표시(.nav-current)
    Object.values(desktopPageInfos).forEach(btn => {
        if (btn) btn.classList.remove('nav-current');
    });
    const desktopButton = desktopPageInfos[pageName];
    if (desktopButton) desktopButton.classList.add('nav-current');
}

function initNavPager() {
    const pagesEl = document.getElementById('nav-pages');
    const prevBtn = document.getElementById('nav-prev');
    const nextBtn = document.getElementById('nav-next');
    if (!pagesEl || !prevBtn || !nextBtn) return;

    const track = pagesEl.querySelector('.nav-track');
    const pages = Array.from(pagesEl.querySelectorAll('.nav-page'));
    const totalPages = pages.length;
    let currentPage = 0;

    function showPage(page, animated = true) {
        currentPage = page;
        if (!animated) track.style.transition = 'none';
        track.style.transform = `translateX(-${page * (100 / totalPages)}%)`;
        if (!animated) {
            track.getBoundingClientRect(); // force reflow
            track.style.transition = '';
        }
        prevBtn.disabled = page === 0;
        nextBtn.disabled = page === totalPages - 1;
    }

    prevBtn.addEventListener('click', () => {
        if (currentPage > 0) showPage(currentPage - 1);
    });
    nextBtn.addEventListener('click', () => {
        if (currentPage < totalPages - 1) showPage(currentPage + 1);
    });

    // 스와이프 지원
    let touchStartX = 0;
    pagesEl.addEventListener('touchstart', e => { touchStartX = e.touches[0].clientX; }, { passive: true });
    pagesEl.addEventListener('touchend', e => {
        const dx = touchStartX - e.changedTouches[0].clientX;
        if (Math.abs(dx) > 40) {
            if (dx > 0 && currentPage < totalPages - 1) showPage(currentPage + 1);
            else if (dx < 0 && currentPage > 0) showPage(currentPage - 1);
        }
    }, { passive: true });

    // 현재 활성 항목이 있는 페이지로 시작 (애니메이션 없이)
    const activeEl = pagesEl.querySelector('.nav-current');
    const startPage = activeEl ? pages.findIndex(p => p.contains(activeEl)) : 0;
    showPage(Math.max(0, startPage), false);
}

await util.getOrCreateToken();
displayNickName();
addHeaderEventList();
markCurrentPage();
initNavPager();
