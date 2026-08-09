import * as util from "./common.js";

let cachedClientInfo = null;

function appendChatElementAtBottom(chat, chatBox, tokenValue) {
    const lastChatUnit = chatBox.lastElementChild;

    const chatType = tokenValue === chat.key ? "sent" : "received";
    const chatUnit = document.createElement("div");
    chatUnit.classList.add("chat-unit", chatType);

    chatUnit.setAttribute("data-user-key", chat.key);
    chatUnit.setAttribute("data-send-at", chat.send_at);

    const user = document.createElement("p");
    user.classList.add("chat-user");
    user.textContent = chat.name;

    const chatContent = document.createElement("div");
    chatContent.classList.add("chat-content");

    const message = document.createElement("p");
    message.classList.add("chat-message");
    message.textContent = chat.text;

    const chatDateTime = new Date(chat.send_at);
    const hour = chatDateTime.getHours();
    const period = hour < 12 ? "오전" : "오후";
    const timeValue = `${period} ${hour % 12 || 12}:${String(chatDateTime.getMinutes()).padStart(2, "0")}`;
    const dateValue = `${chatDateTime.getFullYear()}년 ${chatDateTime.getMonth() + 1}월 ${chatDateTime.getDate()}일`

    const date = document.createElement("div");
    const dateText = document.createElement("p");
    dateText.textContent = dateValue;
    date.appendChild(dateText);
    date.classList.add("chat-date");

    const time = document.createElement("p");
    time.classList.add("chat-time");
    time.textContent = timeValue;

    if (!lastChatUnit || lastChatUnit.dataset.userKey !== chat.key) {
        chatUnit.appendChild(user);
    }

    if (!lastChatUnit || (new Date(lastChatUnit.dataset.sendAt).getDate() !== chatDateTime.getDate())) {
        chatBox.appendChild(date);
    }

    chatContent.appendChild(message);
    chatContent.appendChild(time);
    chatUnit.appendChild(chatContent);
    chatBox.appendChild(chatUnit);
}

function appendChatElementAtTop(chat, chatBox, authValue) {
    const firstChatUnit = chatBox.querySelector(".chat-unit");

    const chatType = authValue === chat.key ? "sent" : "received";
    const chatUnit = document.createElement("div");
    chatUnit.classList.add("chat-unit", chatType);

    chatUnit.setAttribute("data-user-key", chat.key);
    chatUnit.setAttribute("data-send-at", chat.send_at);

    const user = document.createElement("p");
    user.classList.add("chat-user");
    user.textContent = chat.name;

    const chatContent = document.createElement("div");
    chatContent.classList.add("chat-content");

    const message = document.createElement("p");
    message.classList.add("chat-message");
    message.textContent = chat.text;

    const chatDateTime = new Date(chat.send_at);
    const hour = chatDateTime.getHours();
    const period = hour < 12 ? "오전" : "오후";
    const timeValue = `${period} ${hour % 12 || 12}:${String(chatDateTime.getMinutes()).padStart(2, "0")}`;
    const dateValue = `${chatDateTime.getFullYear()}년 ${chatDateTime.getMonth() + 1}월 ${chatDateTime.getDate()}일`

    const date = document.createElement("div");
    const dateText = document.createElement("p");
    dateText.textContent = dateValue;
    date.appendChild(dateText);
    date.classList.add("chat-date");

    const time = document.createElement("p");
    time.classList.add("chat-time");
    time.textContent = timeValue;

    // 첫번쨰 요소와 닉네임이 일치하지 않을 때
    if (!firstChatUnit || firstChatUnit.dataset.userKey !== chat.key) {
        chatUnit.appendChild(user);
    }
    // 첫번째 요소와 닉네임이 일치할 때(닉네임 라벨 삭제)
    if (firstChatUnit.dataset.userKey === chat.key) {
        chatUnit.appendChild(user);
        const existingUser = firstChatUnit.querySelector(".chat-user");
        existingUser.remove();
    }

    chatContent.appendChild(message);
    chatContent.appendChild(time);
    chatUnit.appendChild(chatContent);

    // 첫번째 요소와 날짜가 다를때
    if (!firstChatUnit || (new Date(firstChatUnit.dataset.sendAt).getDate() !== chatDateTime.getDate())) {
        chatBox.insertBefore(chatUnit, chatBox.firstChild);
        chatBox.insertBefore(date, chatBox.firstChild);
    }
    // 첫번쨰 요소와 날짜가 같을 때 (기존 날짜 라벨 제거 -> 채팅 추가 -> 날짜 라벨 추가)
    if (new Date(firstChatUnit.dataset.sendAt).getDate() === chatDateTime.getDate()) {
        const firstDateLabel = chatBox.querySelector(".chat-date");
        firstDateLabel.remove();
        chatBox.insertBefore(chatUnit, chatBox.firstChild);
        chatBox.insertBefore(date, chatBox.firstChild);
    }
}

function makeChatElementByScroll(chat, chatBox, authValue) {
    const lastChatUnit = chatBox.firstElementChild;

    const chatType = authValue === chat.key ? "sent" : "received";
    const chatUnit = document.createElement("div");
    chatUnit.classList.add("chat-unit", chatType);
}

function appendChatByScroll(data) {
    const authValue = util.getAuthValue();
    const chatBox = document.getElementById("chatting-box-section");

    const prevScrollHeight = chatBox.scrollHeight;
    const prevScrollTop = chatBox.scrollTop;

    data.reverse().forEach(chat => {
        appendChatElementAtTop(chat, chatBox, authValue);
    })

    const newScrollHeight = chatBox.scrollHeight;
    const addedHeight = newScrollHeight - prevScrollHeight;

    // 스크롤 위치 보정
    chatBox.scrollTop = prevScrollTop + addedHeight;
}

function renderingChatting(data, isFirstRendering) {
    const tokenValue = util.getTokenValue();
    const chatBox = document.getElementById("chatting-box-section");
    if (isFirstRendering) {
        chatBox.innerHTML = "";
        data.forEach(chat => {
            appendChatElementAtBottom(chat, chatBox, tokenValue);
        })
        chatBox.scrollTop = chatBox.scrollHeight;
    } else {
        appendChatElementAtBottom(data, chatBox, tokenValue);
    }
    if (chatBox.scrollHeight - chatBox.scrollTop - chatBox.clientHeight <= chatBox.clientHeight * 2) {
        chatBox.scrollTop = chatBox.scrollHeight;
    }
}

async function setChatting() {
    const date = new Date();
    const koreaTime = new Date(date.getTime() + 9 * 60 * 60 * 1000);
    const response = await fetch(`${HOST}/api/chat?cursor=${encodeURIComponent(koreaTime.toISOString())}`);
    if (response.ok) {
        const data = await response.json();
        renderingChatting(data, true);
    }
}

function updateParticipantCount(count) {
    const el = document.getElementById("chat-count-num");
    if (el) el.textContent = count;
}

async function fetchParticipantCount() {
    try {
        const res = await fetch(`${HOST}/api/chat/participants`, { credentials: "same-origin" });
        if (res.ok) updateParticipantCount((await res.text()).trim());
    } catch (e) { /* 무시 — SSE participants 이벤트가 곧 갱신 */ }
}

function setChattingSse() {
    const eventSource = new EventSource(`${HOST}/api/chat/connection`);
    // 연결이 수립돼야 서버가 내 연결을 인원에 반영한다. 그 전에 조회하면 나를 뺀 값이 온다.
    eventSource.onopen = () => fetchParticipantCount();
    eventSource.addEventListener("chat", (event) => {
        const chat = JSON.parse(event.data);
        renderingChatting(chat, false);
    });
    eventSource.addEventListener("participants", (event) => {
        updateParticipantCount(event.data);
    });
    eventSource.onerror = () => {
        if (eventSource && eventSource.readyState !== EventSource.CLOSED) {
            eventSource.close();
        }
        setTimeout(() => {
            setChattingSse();
        }, 1000);
    };
}

async function getOrFetchClientInfo() {
    if (!cachedClientInfo) {
        const info = await util.getClientInfo();
        if (info) cachedClientInfo = info;
    }
    return cachedClientInfo;
}

async function isSendChatPossible(chatting) {
    if (!chatting || chatting.trim().length === 0) {
        return false;
    }
    if (chatting.length > 100) {
        await util.showAlert({ title: '입력 오류', msg: '채팅은 최대 100 글자까지 가능합니다.' });
        return false;
    }
    if (!(await getOrFetchClientInfo())?.name) {
        await util.showAlert({ title: '닉네임 필요', msg: '채팅을 입력하려면 닉네임을 입력해주세요.' });
        return false;
    }
    return true;
}

function getOldestChatDateTime() {
    const firstChat = document.querySelector(".chat-unit");
    const sendAt = firstChat?.getAttribute("data-send-at");
    const date = new Date(sendAt);
    const koreaTime = new Date(date.getTime() + 9 * 60 * 60 * 1000);
    return koreaTime.toISOString();
}

// 채팅 이벤트 설정
async function setChatEventListener() {
    const chatBox = document.getElementById("chatting-box-section");
    let isLoading = false;
    chatBox.addEventListener("scroll", async () => {
        if (chatBox.scrollTop < 100 && !isLoading) {
            isLoading = true;
            try {
                const dateTime = getOldestChatDateTime();
                const response = await fetch(`${HOST}/api/chat?cursor=${encodeURIComponent(dateTime)}`);
                const data = await response.json();
                appendChatByScroll(data);
            } finally {
                isLoading = false;
            }
        }
    });

    const inputBox = document.getElementById("chatting-input");
    const button = document.getElementById("chatting-send-button");
    const charCount = document.getElementById("chatting-char-count");
    let isSending = false;

    const syncInputState = () => {
        const length = inputBox.value.length;
        button.disabled = isSending || inputBox.value.trim().length === 0;
        button.toggleAttribute("aria-busy", isSending);
        if (charCount) {
            charCount.textContent = `${length}/100`;
            charCount.hidden = length < 80;
        }
    };

    inputBox.addEventListener("click", async () => {
        if (!(await getOrFetchClientInfo())?.name) {
            await util.showAlert({ title: '닉네임 필요', msg: '채팅을 입력하려면 닉네임을 입력해주세요.' });
        }
    });
    inputBox.addEventListener("input", syncInputState);
    syncInputState();

    button.addEventListener("click", async () => {
        if (isSending) return;
        const chatting = document.getElementById("chatting-input").value;
        if (!await isSendChatPossible(chatting)) {
            return;
        }
        isSending = true;
        syncInputState();
        try {
            const response = await util.authFetch(`${HOST}/api/chat`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    text: chatting,
                }),
                credentials: "same-origin"
            });
            inputBox.value = "";
            if (!response.ok) {
                const errorResponse = await response.json();
                await util.showAlert({ title: '오류', msg: errorResponse.message });
            }
        } finally {
            isSending = false;
            syncInputState();
        }
    });
    document.getElementById("chatting-input").addEventListener("keypress", function (e) {
        if (e.key === "Enter") { // 엔터 키를 눌렀을 때
            e.preventDefault();   // 기본 동작 방지
            button.click();
        }
    });
}

async function initClientInfo() {
    cachedClientInfo = await util.getClientInfo();
}

// ===== 전역 플로팅 채팅 위젯 =====
// SSE·초기화는 부하를 줄이려 "패널을 처음 열 때" 한 번만 연결한다(lazy).
let chatInitialized = false;

function initChat() {
    if (chatInitialized) return;
    chatInitialized = true;
    // 모바일 탭 복귀 시 SSE 재연결 처리 (채팅을 연 사용자에게만 적용)
    document.addEventListener("visibilitychange", () => {
        if (document.visibilityState === "visible") {
            location.reload();
        }
    });
    initClientInfo();
    setChatting();
    setChatEventListener();
    setChattingSse(); // 초기 인원 seed 는 SSE onopen 에서 (이후 participants 이벤트로 실시간 갱신)
}

function setupChatWidget() {
    const widget = document.getElementById("chat-widget");
    if (!widget) return; // 위젯이 없는 페이지면 아무것도 안 함
    const launcher = document.getElementById("chat-launcher");
    const closeBtn = document.getElementById("chat-close");
    launcher.addEventListener("click", () => {
        const opened = widget.classList.toggle("open");
        if (opened) {
            initChat(); // 첫 열림에만 연결
            applyMobileOpenHeight(); // 모바일: 광고 보호 상한까지 열기
        }
    });
    if (closeBtn) closeBtn.addEventListener("click", () => widget.classList.remove("open"));
    setupChatVisibility(widget);
    setupChatDrag();
    setupMobileSheetDrag();
    setupViewportSync();
}

// 채팅 버튼이 화면을 가린다는 사람을 위해 전 페이지에서 접어둘 수 있게 한다.
// 완전히 없애면 되돌릴 길이 없어 작은 손잡이는 남긴다.
const CHAT_HIDDEN_KEY = "pk_chat_hidden";

function setupChatVisibility(widget) {
    const hideBtn = document.getElementById("chat-hide");
    const restoreBtn = document.getElementById("chat-restore");
    const toast = document.getElementById("chat-toast");
    const undoBtn = document.getElementById("chat-toast-undo");
    if (!hideBtn || !restoreBtn) return;

    let toastTimer;
    const showToast = () => {
        if (!toast) return;
        toast.hidden = false;
        requestAnimationFrame(() => toast.classList.add("on"));
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => toast.classList.remove("on"), 3000);
    };

    const apply = hidden => {
        widget.classList.toggle("hidden", hidden);
        restoreBtn.hidden = !hidden;
        if (hidden) widget.classList.remove("open");
    };

    const restore = () => {
        localStorage.removeItem(CHAT_HIDDEN_KEY);
        apply(false);
        clearTimeout(toastTimer);
        if (toast) toast.classList.remove("on");
    };

    hideBtn.addEventListener("click", () => {
        localStorage.setItem(CHAT_HIDDEN_KEY, "1");
        apply(true);
        showToast();
    });
    restoreBtn.addEventListener("click", restore);
    if (undoBtn) undoBtn.addEventListener("click", restore);

    apply(localStorage.getItem(CHAT_HIDDEN_KEY) === "1");
}

// 사용자가 원하는 시트 높이(px). 키보드 때문에 일시적으로 줄여도 이 값으로 되돌린다.
let mobileSheetHeight = 0;
let mobileAnchored = false;

// 키보드가 열린 상태에서 채팅을 끝까지 당기면 iOS의 rubber-band 값이
// visualViewport.offsetTop에 정상 범위를 벗어나 잠깐 들어온다. 그 값을 그대로
// 패널 위치에 쓰면 시트도 함께 튀면서 아래 배경이 빈 여백처럼 드러난다.
function mobileViewportMetrics() {
    const vv = window.visualViewport;
    const height = vv ? vv.height : window.innerHeight;
    const maxTop = Math.max(0, window.innerHeight - height);
    const top = vv ? Math.min(maxTop, Math.max(0, vv.offsetTop)) : 0;
    return { height, top, bottom: top + height };
}

// 모바일 시트가 커져도 상단 광고(ad-section)를 덮지 않는 최대 높이.
// 광고가 스크롤로 화면 위로 벗어나면 그만큼 상한이 자연히 커진다(가릴 광고가 없으므로).
// 기준은 window.innerHeight 가 아니라 visualViewport.height — 키보드가 가린 영역까지 높이로 쳐서는 안 된다.
function mobileMaxHeight() {
    const viewport = mobileViewportMetrics();
    const ad = document.getElementById("ad-section");
    const guardTop = ad
        ? Math.max(8, ad.getBoundingClientRect().bottom - viewport.top + 6)
        : Math.round(viewport.height * 0.12);
    return Math.max(120, viewport.height - guardTop);
}

function clearMobileAnchor(panel) {
    if (!mobileAnchored) return;
    mobileAnchored = false;
    panel.style.removeProperty("bottom");
    panel.style.removeProperty("height");
}

// iOS Safari의 fixed 좌표는 키보드 전환 중 visual viewport를 기준으로 바뀐다.
// top + translate로 보이는 바닥 좌표를 다시 적용하면 offsetTop이 이중 반영되어
// 입력줄이 키보드보다 훨씬 위로 뜬다. 레이아웃 뷰포트와 보이는 바닥의 차이만
// bottom에 적용하면 입력줄이 키보드 바로 위를 유지한다.
function syncPanelToViewport() {
    const panel = document.getElementById("chat-panel");
    if (!panel) return;
    if (window.innerWidth > 768) {
        clearMobileAnchor(panel);
        return;
    }
    const viewport = mobileViewportMetrics();
    const lift = Math.max(0, window.innerHeight - viewport.bottom);
    mobileAnchored = true;
    panel.style.removeProperty("top");
    panel.style.removeProperty("transform");
    panel.style.setProperty("bottom", lift + "px", "important");
    if (mobileSheetHeight) {
        panel.style.height = Math.min(mobileSheetHeight, mobileMaxHeight()) + "px";
    }
}

// WebKit 은 키보드 전환 도중 visualViewport 값을 늦게 갱신한다(offsetTop 이 잠깐 0으로 읽힘).
// 한 번만 재면 그 과도기 값이 그대로 굳으므로 프레임 뒤·시간차로 여러 번 다시 잰다.
let viewportSyncFrame = 0;
let viewportSyncTimers = [];

function scheduleSync() {
    cancelAnimationFrame(viewportSyncFrame);
    viewportSyncFrame = requestAnimationFrame(() => {
        viewportSyncFrame = requestAnimationFrame(syncPanelToViewport);
    });

    viewportSyncTimers.forEach(clearTimeout);
    viewportSyncTimers = [50, 150, 300].map(delay => setTimeout(syncPanelToViewport, delay));
}

function setupViewportSync() {
    const vv = window.visualViewport;
    if (vv) {
        vv.addEventListener("resize", scheduleSync);
        vv.addEventListener("scroll", scheduleSync);
    }
    window.addEventListener("resize", () => {
        if (window.innerWidth > 768) {
            mobileSheetHeight = 0;
            applyMobileOpenHeight();
            return;
        }
        scheduleSync();
    });
    window.addEventListener("scroll", scheduleSync);
    document.addEventListener("focusin", scheduleSync);
    document.addEventListener("focusout", scheduleSync);
}

// 열 때 높이 지정: 모바일은 광고 보호 상한까지, 데스크톱은 인라인 값 비워 CSS/resize 값 사용
function applyMobileOpenHeight() {
    const panel = document.getElementById("chat-panel");
    if (!panel) return;
    if (window.innerWidth > 768) {
        clearMobileAnchor(panel);
        panel.style.height = "";
        return;
    }
    mobileSheetHeight = mobileMaxHeight();
    syncPanelToViewport();
}

// 모바일 바텀시트: 헤더를 위/아래로 드래그해 높이 조절. 많이 내리면 닫힘.
function setupMobileSheetDrag() {
    const widget = document.getElementById("chat-widget");
    const panel = document.getElementById("chat-panel");
    const head = document.getElementById("chat-panel-head");
    if (!widget || !panel || !head) return;
    let dragging = false, startY = 0, startH = 0;

    head.addEventListener("pointerdown", (e) => {
        if (window.innerWidth > 768) return;         // 데스크톱은 별도 드래그(이동) 로직
        if (e.target.closest("#chat-close")) return;
        dragging = true;
        startY = e.clientY;
        startH = panel.getBoundingClientRect().height;
        panel.style.transition = "none";
        head.classList.add("dragging");
        head.setPointerCapture(e.pointerId);
    });
    head.addEventListener("pointermove", (e) => {
        if (!dragging) return;
        const max = mobileMaxHeight();
        const min = Math.round(window.innerHeight * 0.12);
        let h = startH - (e.clientY - startY);
        h = Math.max(min, Math.min(h, max));
        mobileSheetHeight = h;
        panel.style.height = h + "px";
    });
    const end = (e) => {
        if (!dragging) return;
        dragging = false;
        head.classList.remove("dragging");
        panel.style.transition = "";
        const h = panel.getBoundingClientRect().height;
        const closeAt = window.innerHeight * 0.22;
        const snapMin = Math.round(window.innerHeight * 0.35);
        if (h < closeAt) {
            widget.classList.remove("open");        // 충분히 내리면 닫힘 → 런처 복귀
        } else if (h < snapMin) {
            mobileSheetHeight = snapMin;
            panel.style.height = snapMin + "px";    // 너무 작으면 최소 높이로 스냅
        }
    };
    head.addEventListener("pointerup", end);
    head.addEventListener("pointercancel", end);
}

// 채팅 패널을 헤더로 드래그해 이동 (데스크톱만, 크기조절은 CSS resize)
function setupChatDrag() {
    const panel = document.getElementById("chat-panel");
    const head = document.getElementById("chat-panel-head");
    if (!panel || !head) return;
    let dragging = false, startX = 0, startY = 0, baseLeft = 0, baseTop = 0;

    head.addEventListener("mousedown", (e) => {
        if (e.target.closest("#chat-close")) return;
        if (window.innerWidth <= 768) return; // 모바일은 고정 시트
        const rect = panel.getBoundingClientRect();
        panel.style.left = rect.left + "px";
        panel.style.top = rect.top + "px";
        panel.style.right = "auto";
        panel.style.bottom = "auto";
        baseLeft = rect.left;
        baseTop = rect.top;
        startX = e.clientX;
        startY = e.clientY;
        dragging = true;
        document.body.style.userSelect = "none";
        e.preventDefault();
    });
    document.addEventListener("mousemove", (e) => {
        if (!dragging) return;
        let nextLeft = baseLeft + (e.clientX - startX);
        let nextTop = baseTop + (e.clientY - startY);
        // 광고 영역(좌·우 각 25vw)을 덮지 않도록 콘텐츠 영역(25~75vw) 안으로만 이동 허용
        const safeLeft = window.innerWidth * 0.25 + 4;
        const safeRight = window.innerWidth * 0.75 - 4;
        let maxLeft = safeRight - panel.offsetWidth;
        if (maxLeft < safeLeft) maxLeft = safeLeft;
        nextLeft = Math.max(safeLeft, Math.min(nextLeft, maxLeft));
        // 세로: 헤더 메뉴를 덮지 않도록 콘텐츠 영역 시작점(헤더 아래) 밑으로만 이동 허용
        const contentEl = document.getElementById("content-section");
        const topBound = (contentEl ? contentEl.getBoundingClientRect().top : 4) + 4;
        let maxTop = window.innerHeight - panel.offsetHeight - 4;
        if (maxTop < topBound) maxTop = topBound;
        nextTop = Math.max(topBound, Math.min(nextTop, maxTop));
        panel.style.left = nextLeft + "px";
        panel.style.top = nextTop + "px";
    });
    document.addEventListener("mouseup", () => {
        if (dragging) {
            dragging = false;
            document.body.style.userSelect = "";
        }
    });
}

setupChatWidget();
