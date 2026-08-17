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

// 패널이 열려 있는 동안만 유지한다. 인원 수가 곧 연결 수라, 닫으면 접속자에서 빠진다.
let eventSource = null;
let reconnectTimer = null;

function setChattingSse() {
    const es = new EventSource(`${HOST}/api/chat/connection`);
    eventSource = es;
    // 연결이 수립돼야 서버가 내 연결을 인원에 반영한다. 그 전에 조회하면 나를 뺀 값이 온다.
    es.onopen = () => fetchParticipantCount();
    es.addEventListener("chat", (event) => {
        const chat = JSON.parse(event.data);
        renderingChatting(chat, false);
    });
    es.addEventListener("participants", (event) => {
        updateParticipantCount(event.data);
    });
    es.onerror = () => {
        if (es.readyState !== EventSource.CLOSED) {
            es.close();
        }
        // 그 사이 패널이 닫혔거나 다른 연결로 교체됐으면 되살리지 않는다.
        if (eventSource !== es) return;
        reconnectTimer = setTimeout(setChattingSse, 1000);
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
// 패널을 열 때 붙이고 닫을 때 끊는다. 닫는 경로가 여러 개라(닫기 버튼·숨기기·시트 드래그·런처)
// 열고 닫기를 이 두 함수로만 하고, 각 경로는 이것만 부른다.
let chatWidget = null;
let chatBound = false;

function isChatOpen() {
    return !!chatWidget && chatWidget.classList.contains("open");
}

// 입력 핸들러처럼 다시 걸면 중복되는 것들은 최초 1회만
function bindChatOnce() {
    if (chatBound) return;
    chatBound = true;
    initClientInfo();
    setChatEventListener();
    // 모바일은 백그라운드로 가면 연결을 끊는다. 그때 onerror·타이머도 멈춰 스스로 못 살아나므로
    // 복귀 시점에 직접 확인한다. 페이지를 새로고침하던 예전 방식은 페이지뷰를 부풀렸다.
    document.addEventListener("visibilitychange", () => {
        if (document.visibilityState !== "visible") return;
        if (!isChatOpen()) return;
        if (eventSource && eventSource.readyState !== EventSource.CLOSED) return;
        disconnectChat();
        connectChat();
    });
}

function connectChat() {
    bindChatOnce();
    if (eventSource) return;
    setChatting(); // 끊긴 동안 놓친 대화를 다시 받는다
    setChattingSse();
}

function disconnectChat() {
    clearTimeout(reconnectTimer); // 예약된 재연결이 닫은 뒤에 되살리지 못하게
    reconnectTimer = null;
    if (!eventSource) return;
    eventSource.close();
    eventSource = null;
}

function openChatPanel() {
    if (!chatWidget) return;
    chatWidget.classList.add("open");
    connectChat();
    applyMobileOpenHeight(); // 모바일: 광고 보호 상한까지 열기
}

function closeChatPanel() {
    if (!chatWidget) return;
    chatWidget.classList.remove("open");
    disconnectChat();
}

function setupChatWidget() {
    chatWidget = document.getElementById("chat-widget");
    if (!chatWidget) return; // 위젯이 없는 페이지면 아무것도 안 함
    const launcher = document.getElementById("chat-launcher");
    const closeBtn = document.getElementById("chat-close");
    launcher.addEventListener("click", () => {
        if (isChatOpen()) closeChatPanel();
        else openChatPanel();
    });
    if (closeBtn) closeBtn.addEventListener("click", closeChatPanel);
    setupChatVisibility(chatWidget);
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
        if (hidden) closeChatPanel();
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

// 사용자가 원하는 시트 높이(px). 키보드 때문에 잠시 줄어도 이 값으로 복원한다.
let mobileSheetHeight = 0;
let mobileAnchored = false;

// iOS Safari는 키보드 애니메이션 중 visualViewport에 순간적인 오측정값을 보낸다.
// 마지막 이벤트 뒤 잠시 기다리고 두 프레임이 같은 경우에만 패널을 한 번 갱신한다.
const MOBILE_VIEWPORT_SETTLE_MS = 120;
const MOBILE_VIEWPORT_EPSILON = 2;
let viewportSyncFrame = 0;
let viewportSyncTimer = 0;
let mobileFocusSnapshot = null;

function mobileViewportMetrics() {
    const vv = window.visualViewport;
    const height = vv ? vv.height : window.innerHeight;
    const top = vv ? Math.max(0, vv.offsetTop) : 0;
    return { height, top, bottom: top + height };
}

function isChatInputFocused() {
    return document.activeElement?.id === "chatting-input";
}

// 포커스 직전의 안정된 광고 위치와 시트 높이를 저장한다. 키보드가 움직이는 동안
// getBoundingClientRect()를 다시 읽으면 WebKit의 흔들리는 좌표가 크기 계산에 섞인다.
function captureMobileFocusSnapshot() {
    if (window.innerWidth > 768 || mobileFocusSnapshot) return;
    const panel = document.getElementById("chat-panel");
    if (!panel) return;
    const viewport = mobileViewportMetrics();
    const ad = document.getElementById("ad-section");
    const guardTop = ad
        ? Math.max(8, ad.getBoundingClientRect().bottom - viewport.top + 6)
        : Math.round(viewport.height * 0.12);
    mobileFocusSnapshot = {
        guardTop,
        viewportHeight: viewport.height,
        sheetHeight: mobileSheetHeight || panel.getBoundingClientRect().height
    };
    panel.dataset.viewportState = "frozen";
}

// 평소에는 상단 광고를 보호한다. 키보드가 열리면 뒤쪽 광고보다 채팅 사용성이
// 우선이므로 보이는 viewport를 거의 전부 사용한다. 그렇지 않으면 짧은 화면에서
// 헤더와 입력줄만 남고 메시지 영역이 사라진다.
function mobileMaxHeight(viewport = mobileViewportMetrics()) {
    const keyboardOpen = mobileFocusSnapshot
        && viewport.height < mobileFocusSnapshot.viewportHeight - 80;
    if (keyboardOpen) return Math.max(120, viewport.height - 8);

    let guardTop = mobileFocusSnapshot?.guardTop;
    if (guardTop == null) {
        const ad = document.getElementById("ad-section");
        guardTop = ad
            ? Math.max(8, ad.getBoundingClientRect().bottom - viewport.top + 6)
            : Math.round(viewport.height * 0.12);
    }
    return Math.max(120, viewport.height - guardTop);
}

function clearMobileAnchor(panel) {
    mobileAnchored = false;
    mobileFocusSnapshot = null;
    clearTimeout(viewportSyncTimer);
    cancelAnimationFrame(viewportSyncFrame);
    panel.style.removeProperty("bottom");
    panel.style.removeProperty("height");
    panel.removeAttribute("data-viewport-state");
}

// 안정된 최종 viewport만 반영한다. 키보드가 열려도 사용자가 정한 원래 높이는
// mobileSheetHeight에 남겨두고, 실제 보이는 높이만 현재 viewport에 맞춰 줄인다.
function syncPanelToViewport(viewport = mobileViewportMetrics()) {
    const panel = document.getElementById("chat-panel");
    if (!panel) return;
    if (window.innerWidth > 768) {
        clearMobileAnchor(panel);
        return;
    }

    const lift = Math.max(0, window.innerHeight - viewport.bottom);
    const preferredHeight = mobileFocusSnapshot?.sheetHeight || mobileSheetHeight;
    mobileAnchored = true;
    panel.style.removeProperty("top");
    panel.style.removeProperty("transform");
    panel.style.setProperty("bottom", lift + "px", "important");
    if (preferredHeight) {
        panel.style.height = Math.min(preferredHeight, mobileMaxHeight(viewport)) + "px";
    }
    panel.dataset.viewportState = "stable";

    // 키보드가 완전히 닫힌 뒤부터는 다음 포커스를 위해 새 위치를 측정할 수 있다.
    if (!isChatInputFocused()) mobileFocusSnapshot = null;
}

function viewportIsStable(first, second) {
    return Math.abs(first.height - second.height) <= MOBILE_VIEWPORT_EPSILON
        && Math.abs(first.top - second.top) <= MOBILE_VIEWPORT_EPSILON
        && Math.abs(first.innerHeight - second.innerHeight) <= MOBILE_VIEWPORT_EPSILON;
}

function settleViewport() {
    const first = { ...mobileViewportMetrics(), innerHeight: window.innerHeight };
    cancelAnimationFrame(viewportSyncFrame);
    viewportSyncFrame = requestAnimationFrame(() => {
        const second = { ...mobileViewportMetrics(), innerHeight: window.innerHeight };
        if (!viewportIsStable(first, second)) {
            scheduleSync();
            return;
        }
        syncPanelToViewport(second);
    });
}

function scheduleSync() {
    if (window.innerWidth > 768) {
        const panel = document.getElementById("chat-panel");
        if (panel) clearMobileAnchor(panel);
        return;
    }
    const panel = document.getElementById("chat-panel");
    if (panel) panel.dataset.viewportState = "waiting";
    clearTimeout(viewportSyncTimer);
    viewportSyncTimer = setTimeout(settleViewport, MOBILE_VIEWPORT_SETTLE_MS);
}

function setupViewportSync() {
    const vv = window.visualViewport;
    const input = document.getElementById("chatting-input");
    if (input) {
        // pointerdown은 focusin/키보드 표시보다 먼저 실행되므로 안정된 좌표를 얻을 수 있다.
        input.addEventListener("pointerdown", (event) => {
            captureMobileFocusSnapshot();
            if (window.innerWidth > 768 || isChatInputFocused()) return;

            // iOS가 기본 포커스 과정에서 입력창을 화면 중앙까지 과하게 밀어 올리는 것을 막는다.
            // 사용자 제스처 안에서 직접 focus해야 소프트웨어 키보드는 그대로 열린다.
            event.preventDefault();
            input.focus({ preventScroll: true });
        });
    }
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
    document.addEventListener("focusin", (event) => {
        if (event.target.id !== "chatting-input") return;
        captureMobileFocusSnapshot();
        scheduleSync();
    });
    document.addEventListener("focusout", (event) => {
        if (event.target.id === "chatting-input") scheduleSync();
    });
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
            closeChatPanel();                       // 충분히 내리면 닫힘 → 런처 복귀
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

if (new URLSearchParams(location.search).has("vvdebug")) {
    import("./chatting-debug.js");
}
