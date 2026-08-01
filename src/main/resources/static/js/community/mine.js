import { authFetch, showAlert } from "/js/common.js";

// 이 화면만 목록을 브라우저가 그린다 — 누가 나인지는 토큰을 가진 브라우저만 안다
const PAGE_SIZE = 20;

// PageBlock.BLOCK_SIZE 와 같은 값이어야 한다
const BLOCK_SIZE = 5;

const loading = document.getElementById("mine-loading");
const list = document.getElementById("post-list");
const empty = document.getElementById("post-empty");
const total = document.getElementById("mine-total");
const pagination = document.getElementById("pagination");

function buildRow(post) {
    const row = document.createElement("li");
    row.className = "post-row";

    const link = document.createElement("a");
    link.className = "post-link";
    link.href = `/community/${post.id}`;

    const badge = document.createElement("span");
    badge.className = post.like_count > 0 ? "like-badge has-like" : "like-badge";
    const likeCount = document.createElement("span");
    likeCount.className = "like-count";
    likeCount.textContent = post.like_count;
    const likeLabel = document.createElement("span");
    likeLabel.className = "like-label";
    likeLabel.textContent = "추천";
    badge.append(likeCount, likeLabel);

    const main = document.createElement("span");
    main.className = "post-main";

    // 사용자가 정한 값이라 문자열로 HTML 을 만들면 그 자리가 XSS 가 된다
    const title = document.createElement("span");
    title.className = "post-title";
    title.textContent = post.title;
    if (post.comment_count > 0) {
        const commentCount = document.createElement("span");
        commentCount.className = "post-comment-count";
        commentCount.textContent = `[${post.comment_count}]`;
        title.append(" ", commentCount);
    }

    const metaRow = document.createElement("span");
    metaRow.className = "meta-row";

    const meta = document.createElement("span");
    meta.className = "post-meta";
    meta.append(
        span("post-author", post.nickname),
        span("post-ip", post.ip),
        span("meta-dot", "·"),
        span("post-date", formatDate(post.created_at)),
        span("meta-dot", "·"),
        span("post-view", `조회 ${post.view_count}`),
    );
    metaRow.appendChild(meta);

    if (post.tags?.length) {
        const tags = document.createElement("span");
        tags.className = "row-tags";
        post.tags.forEach((tag) => tags.appendChild(span("row-tag", `#${tag}`)));
        metaRow.appendChild(tags);
    }

    main.append(title, metaRow);
    link.append(badge, main);
    row.appendChild(link);
    return row;
}

function span(className, text) {
    const element = document.createElement("span");
    element.className = className;
    element.textContent = text;
    return element;
}

function formatDate(isoString) {
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) {
        return "";
    }
    const pad = (value) => String(value).padStart(2, "0");
    return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} `
        + `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function pageButton(label, targetPage, { disabled = false, current = false, isMove = false } = {}) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = isMove ? "page-move" : "page-num";
    if (disabled) {
        button.classList.add("disabled");
        button.disabled = true;
    }
    if (current) {
        button.classList.add("current");
    }
    button.textContent = label;
    if (!disabled) {
        button.addEventListener("click", () => load(targetPage));
    }
    return button;
}

// 규칙을 바꾸면 PageBlock.java 도 같이 고쳐야 한다
function renderPagination(result) {
    pagination.hidden = result.total_pages <= 1;
    if (pagination.hidden) {
        pagination.replaceChildren();
        return;
    }

    const lastPage = result.total_pages - 1;
    const blockStart = Math.floor(result.number / BLOCK_SIZE) * BLOCK_SIZE;
    const blockEnd = Math.min(blockStart + BLOCK_SIZE - 1, lastPage);

    const buttons = [
        // « 첫 페이지
        pageButton("«", 0, { disabled: result.number === 0, isMove: true }),
        // ‹ 이전 블록 (5개씩)
        pageButton("‹", blockStart - 1, { disabled: blockStart === 0, isMove: true }),
    ];

    for (let i = blockStart; i <= blockEnd; i += 1) {
        buttons.push(pageButton(String(i + 1), i, { current: i === result.number }));
    }

    buttons.push(
        pageButton("›", blockEnd + 1, { disabled: blockEnd === lastPage, isMove: true }),
        pageButton("»", lastPage, { disabled: result.number === lastPage, isMove: true }),
    );

    pagination.replaceChildren(...buttons);
}

async function load(page = 0) {
    try {
        const response = await authFetch(`${HOST}/api/posts/mine?page=${page}&size=${PAGE_SIZE}`);
        if (!response.ok) {
            throw new Error("failed");
        }

        const result = await response.json();
        loading.hidden = true;

        // 이어 붙이지 않고 갈아끼운다
        list.replaceChildren(...result.content.map(buildRow));

        total.textContent = result.total_elements;
        list.hidden = result.total_elements === 0;
        empty.hidden = result.total_elements > 0;
        renderPagination(result);
    } catch (e) {
        loading.hidden = true;
        await showAlert({ title: "불러오기 실패", msg: "잠시 후 다시 시도해주세요." });
    }
}

load();
