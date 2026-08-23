import { authFetch, showAlert } from "/js/common.js";

const section = document.getElementById("community-section");
const postId = section.dataset.postId;

const ownerActions = document.getElementById("owner-actions");
const guestActions = document.getElementById("guest-actions");
const passwordPrompt = document.getElementById("password-prompt");
const passwordInput = document.getElementById("delete-password-input");

const likeBtn = document.getElementById("like-btn");
const likeCount = document.getElementById("like-count");
const viewCount = document.getElementById("view-count");

const postReportBtn = document.getElementById("post-report-btn");
const reportModal = document.getElementById("report-modal");
const reportModalCancel = document.getElementById("report-modal-cancel");
const reportReasonList = document.getElementById("report-reason-list");

const commentList = document.getElementById("comment-list");
const commentCount = document.getElementById("comment-count");
const commentEmpty = document.getElementById("comment-empty");
const commentForm = document.getElementById("comment-form");
const commentInput = document.getElementById("comment-input");
const commentSubmit = document.getElementById("comment-submit");
const commentLength = document.getElementById("comment-length");

commentInput.addEventListener("input", () => {
    commentLength.textContent = commentInput.value.length;
});

// 본문은 서버가 이미 그렸다. 여기서는 토큰을 알아야 판단되는 것만 채운다
async function resolveViewerState() {
    try {
        const response = await authFetch(`${HOST}/api/posts/${postId}`);
        if (!response.ok) {
            return;
        }
        const post = await response.json();

        if (post.mine) {
            ownerActions.hidden = false;
            guestActions.hidden = true;
            likeBtn.disabled = true;
            likeBtn.title = "본인이 쓴 글은 추천할 수 없습니다.";
            postReportBtn.hidden = true;
        }

        applyLike(post.liked, post.like_count);
        viewCount.textContent = `조회 ${post.view_count}`;
    } catch (e) {
        // 실패해도 본문은 이미 보인다
    }
}

function applyLike(liked, count) {
    likeBtn.classList.toggle("liked", Boolean(liked));
    likeCount.textContent = count;
}

async function requestDelete(deletePassword) {
    const options = {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
    };
    if (deletePassword) {
        options.body = JSON.stringify({ delete_password: deletePassword });
    }

    const response = await authFetch(`${HOST}/api/posts/${postId}`, options);

    if (response.status === 204) {
        location.href = "/community";
        return;
    }

    const error = await response.json().catch(() => ({}));
    await showAlert({ title: "삭제 실패", msg: error.message ?? "잠시 후 다시 시도해주세요." });
}

document.getElementById("delete-btn").addEventListener("click", async () => {
    await requestDelete(null);
});

document.getElementById("password-delete-btn").addEventListener("click", () => {
    guestActions.hidden = true;
    passwordPrompt.hidden = false;
    passwordInput.focus();
});

document.getElementById("password-cancel-btn").addEventListener("click", () => {
    passwordPrompt.hidden = true;
    guestActions.hidden = false;
    passwordInput.value = "";
});

passwordInput.addEventListener("input", () => {
    passwordInput.value = passwordInput.value.replace(/\D/g, "");
});

document.getElementById("password-confirm-btn").addEventListener("click", async () => {
    if (!/^\d{4}$/.test(passwordInput.value)) {
        await showAlert({ title: "확인해주세요", msg: "비밀번호를 숫자 네 자리로 입력해주세요." });
        return;
    }
    await requestDelete(passwordInput.value);
});

passwordInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        document.getElementById("password-confirm-btn").click();
    }
});

/* ---------- 추천 ---------- */

likeBtn.addEventListener("click", async () => {
    likeBtn.disabled = true;
    try {
        const response = await authFetch(`${HOST}/api/posts/${postId}/like`, { method: "POST" });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            await showAlert({ title: "추천 실패", msg: error.message ?? "잠시 후 다시 시도해주세요." });
            return;
        }
        const result = await response.json();
        applyLike(result.liked, result.like_count);
    } finally {
        likeBtn.disabled = false;
    }
});

/* ---------- 댓글 ---------- */

// 본문만 innerHTML 이다 — 서버가 escape 한 결과. 나머지는 전부 textContent 로 넣는다
function buildCommentItem(comment) {
    const item = document.createElement("li");
    item.className = "comment-item";
    item.dataset.commentId = comment.id;
    if (comment.blinded) {
        item.classList.add("blinded");
    }

    const avatar = document.createElement("span");
    // 색은 서버가 뽑아준다. 여기서 계산하면 서버 렌더링과 어긋난다
    avatar.className = `comment-avatar av${comment.avatar_color ?? 1}`;
    avatar.textContent = comment.nickname.charAt(0);

    const body = document.createElement("div");
    body.className = "comment-body";

    const meta = document.createElement("div");
    meta.className = "comment-meta";

    const name = document.createElement("span");
    name.textContent = comment.nickname;

    const ip = document.createElement("span");
    ip.className = "comment-ip";
    ip.textContent = comment.ip;

    const time = document.createElement("span");
    time.className = "comment-time";
    time.textContent = formatCommentTime(comment.created_at);

    meta.append(name, ip, time);

    const text = document.createElement("div");
    text.className = "comment-text";
    text.innerHTML = comment.rendered_content;

    body.append(meta, text);

    if (comment.blinded) {
        item.append(avatar, body);
        return item;
    }

    const tools = document.createElement("div");
    tools.className = "comment-tools";

    const reply = document.createElement("button");
    reply.type = "button";
    reply.className = "comment-reply";
    reply.textContent = "답글";
    tools.appendChild(reply);

    if (comment.mine) {
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "comment-delete";
        remove.textContent = "삭제";
        tools.appendChild(remove);
    } else {
        const report = document.createElement("button");
        report.type = "button";
        report.className = "comment-report";
        report.textContent = "신고";
        tools.appendChild(report);
    }

    body.appendChild(tools);
    item.append(avatar, body);
    return item;
}

// LocalDateTime 문자열을 목록·본문과 같은 모양으로
function formatCommentTime(isoString) {
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) {
        return "";
    }
    const pad = (value) => String(value).padStart(2, "0");
    return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} `
        + `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function renderComments(comments) {
    commentList.replaceChildren(...comments.map(buildCommentItem));
    commentCount.textContent = comments.length;
    commentEmpty.hidden = comments.length > 0;
}

// 어느 댓글이 내 것인지는 토큰을 아는 브라우저만 판단할 수 있어 다시 그린다
async function refreshComments() {
    try {
        const response = await authFetch(`${HOST}/api/posts/${postId}/comments`);
        if (!response.ok) {
            return;
        }
        renderComments(await response.json());
    } catch (e) {
    }
}

commentForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const content = commentInput.value.trim();
    if (!content) {
        await showAlert({ title: "확인해주세요", msg: "댓글을 입력해주세요." });
        return;
    }

    commentSubmit.disabled = true;
    try {
        const response = await authFetch(`${HOST}/api/posts/${postId}/comments`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ content }),
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            await showAlert({ title: "등록 실패", msg: error.message ?? "잠시 후 다시 시도해주세요." });
            return;
        }

        commentInput.value = "";
        commentLength.textContent = "0";
        // 새로고침 금지 — 애드센스 무효 트래픽
        await refreshComments();
    } finally {
        commentSubmit.disabled = false;
    }
});

// 버튼은 다시 그릴 때마다 새로 생기므로 목록 하나에만 붙인다
commentList.addEventListener("click", async (event) => {
    const item = event.target.closest(".comment-item");
    if (!item) {
        return;
    }

    if (event.target.classList.contains("comment-reply")) {
        quoteComment(item);
        return;
    }

    if (event.target.classList.contains("comment-delete")) {
        await deleteComment(item.dataset.commentId);
        return;
    }

    if (event.target.classList.contains("comment-report")) {
        openReportModal("COMMENT", item.dataset.commentId, event.target);
    }
});

// 대댓글 대신 쓰는 인용. 첫 줄만 따서 넣는다
function quoteComment(item) {
    const nickname = item.querySelector(".comment-meta span").textContent;
    const text = item.querySelector(".comment-text").textContent.trim();
    const summary = text.length > 40 ? `${text.slice(0, 40)}…` : text;

    commentInput.value = `>${nickname} : ${summary}\n`;
    commentLength.textContent = commentInput.value.length;
    commentInput.focus();
    commentInput.setSelectionRange(commentInput.value.length, commentInput.value.length);
}

async function deleteComment(commentId) {
    const response = await authFetch(`${HOST}/api/comments/${commentId}`, { method: "DELETE" });

    if (response.status === 204) {
        await refreshComments();
        return;
    }

    const error = await response.json().catch(() => ({}));
    await showAlert({ title: "삭제 실패", msg: error.message ?? "잠시 후 다시 시도해주세요." });
}

/* ---------- 신고 ---------- */

// 신고 진행 중인 대상. 사유 버튼을 누를 때 이걸 보고 보낼 곳을 정한다
let reportTarget = null;

function openReportModal(targetType, targetId, triggerBtn) {
    reportTarget = { targetType, targetId, triggerBtn };
    reportModal.hidden = false;
}

function closeReportModal() {
    reportModal.hidden = true;
    reportTarget = null;
}

postReportBtn.addEventListener("click", () => {
    openReportModal("POST", postId, postReportBtn);
});

reportModalCancel.addEventListener("click", closeReportModal);

// 바깥을 눌러도 닫는다. 사유 상자를 누른 건 걸러야 해서 target 을 본다
reportModal.addEventListener("click", (event) => {
    if (event.target === reportModal) {
        closeReportModal();
    }
});

reportReasonList.addEventListener("click", async (event) => {
    const button = event.target.closest(".report-reason-btn");
    if (!button || !reportTarget) {
        return;
    }
    await submitReport(button.dataset.reason);
});

// 새로고침 금지(애드센스). 대신 버튼을 "신고완료"로 바꿔 다시 못 누르게 한다
async function submitReport(reason) {
    const { targetType, targetId, triggerBtn } = reportTarget;
    const path = targetType === "POST" ? `posts/${targetId}` : `comments/${targetId}`;
    closeReportModal();

    try {
        const response = await authFetch(`${HOST}/api/${path}/reports`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ reason }),
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            await showAlert({ title: "신고 실패", msg: error.message ?? "잠시 후 다시 시도해주세요." });
            return;
        }

        triggerBtn.disabled = true;
        triggerBtn.textContent = "신고완료";
    } catch (e) {
        await showAlert({ title: "신고 실패", msg: "잠시 후 다시 시도해주세요." });
    }
}

resolveViewerState();
refreshComments();
