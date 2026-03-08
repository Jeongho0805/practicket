import * as util from "./common.js";

let name;

const mobilePageInfos = {
    ticketing: document.getElementById("ticketing-page-btn"),
    rank: document.getElementById("rank-page-btn"),
    practice: document.getElementById("practice-page-btn"),
    security: document.getElementById("security-page-btn"),
    blog: document.getElementById("blog-page-btn"),
    art: document.getElementById("art-page-btn")
}

const desktopPageInfos = {
    ticketing: document.getElementById("ticketing-desktop-btn"),
    rank: document.getElementById("rank-desktop-btn"),
    practice: document.getElementById("practice-desktop-btn"),
    security: document.getElementById("security-desktop-btn"),
    blog: document.getElementById("blog-desktop-btn"),
    art: document.getElementById("art-desktop-btn")
}

async function displayNickName() {
    name = await util.getNickname();
    const name_input_section = document.getElementById("name-input-section");
    const name_value = document.getElementById("name-value");
    const reset_button = document.getElementById("reset-button");
    if (!name) {
        const name_section = document.getElementById("name-section");
        name_section.style.display = "none";
        name_input_section.style.display = "flex";
        return;
    }
    name_input_section.style.display = "none";
    name_value.style.display = "block";
    name_value.textContent = name;
    reset_button.style.display = "block";
}

function isValidNickname(name) {
    if (!name) {
        alert("닉네임을 입력해주세요");
        return false;
    }
    if (name.trim() === "") {
        alert("공백 입력은 불가합니다")
        return false;
    }
    if (name.length > 10) {
        alert("닉네임은 최대 10 글자까지 입력가능합니다")
        return false;
    }
    return true;
}

function addHeaderEventList() {
    // 닉네임 입력 이벤트
    const name_button = document.getElementById("name-input-button");
    name_button.addEventListener("click", async () => {
        const name = document.getElementById("name-input-box").value;
        if (!isValidNickname(name)) {
            return;
        }
        await updateClient(name);
        location.reload();
    })

    // 엔터 키 이벤트 추가
    const name_input_box = document.getElementById("name-input-box");
    name_input_box.addEventListener("keyup", async (event) => {
        if (event.key === "Enter") {
            const name = document.getElementById("name-input-box").value;
            if (!isValidNickname(name)) {
                return;
            }
            await updateClient(name);
            location.reload();
        }
    });


    // 닉네임 변경 이벤트
    const name_reset_button = document.getElementById("reset-button");
    name_reset_button.addEventListener("click", async () => {
        await updateClient(null);
        location.reload();
    })
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
        alert(errorResponse.message);
    }
}

function markCurrentPage() {
    let currentPath = window.location.pathname;
    currentPath = currentPath === "/" ? "ticketing" : currentPath.substring(1);

    let pageName = currentPath;
    if (currentPath.startsWith("art")) pageName = "art";
    if (currentPath.startsWith("practice")) pageName = "practice";
    if (currentPath.startsWith("rank")) pageName = "rank";
    if (currentPath.startsWith("security")) pageName = "security";
    if (currentPath.startsWith("blog")) pageName = "blog";

    // 모바일 버튼 처리
    Object.values(mobilePageInfos).forEach(btn => {
        if (btn) btn.classList.remove('nav-current');
    });
    const mobileButton = mobilePageInfos[pageName];
    if (mobileButton) mobileButton.classList.add('nav-current');

    // 데스크탑 버튼 처리
    const desktopButton = desktopPageInfos[pageName];
    if (desktopButton) desktopButton.style.color = "darkslateblue";
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