import { authFetch, showAlert } from "/js/common.js";

const section = document.getElementById("community-section");
const isEdit = section.dataset.isEdit === "true";
const postId = section.dataset.postId;

const titleInput = document.getElementById("title-input");
const contentInput = document.getElementById("content-input");
const passwordInput = document.getElementById("password-input");
const submitBtn = document.getElementById("submit-btn");
const titleCount = document.getElementById("title-count");
const contentCount = document.getElementById("content-count");

const tagBox = document.getElementById("tag-box");
const tagInput = document.getElementById("tag-input");
const tagSelected = document.getElementById("tag-selected");
const tagCount = document.getElementById("tag-count");

/** 서버 규칙(TagNormalizer)과 같은 모양으로 맞춘다. 여기서 안 맞추면 화면에 보이는 것과 저장된 것이 달라진다 */
const MAX_TAGS = 3;
const tags = [];

function normalizeTag(raw) {
    return raw.replace(/[^가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9]/g, "").toLowerCase().slice(0, 12);
}

function renderTags() {
    tagSelected.replaceChildren(...tags.map((tag) => {
        const chip = document.createElement("span");
        chip.className = "tag-selected-chip";
        chip.textContent = `#${tag}`;

        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "tag-remove";
        remove.textContent = "×";
        remove.setAttribute("aria-label", `${tag} 태그 빼기`);
        remove.addEventListener("click", () => {
            tags.splice(tags.indexOf(tag), 1);
            renderTags();
        });

        chip.appendChild(remove);
        return chip;
    }));

    // 세 개를 채우면 입력칸을 숨긴다. 열어두고 넣을 때 튕기면 왜 안 되는지 알 수 없다
    tagInput.hidden = tags.length >= MAX_TAGS;
    tagCount.textContent = tags.length;
}

function addTag(raw) {
    const tag = normalizeTag(raw);
    if (!tag || tags.includes(tag) || tags.length >= MAX_TAGS) {
        return;
    }
    tags.push(tag);
    renderTags();
}

tagInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter" || event.key === ",") {
        // Enter 로 폼이 제출되거나 쉼표가 그대로 남지 않게 막는다
        event.preventDefault();
        addTag(tagInput.value);
        tagInput.value = "";
        return;
    }
    if (event.key === "Backspace" && !tagInput.value && tags.length > 0) {
        tags.pop();
        renderTags();
    }
});

// 입력칸 밖을 눌러도 치던 태그가 사라지지 않게 한 번 더 담는다
tagInput.addEventListener("blur", () => {
    addTag(tagInput.value);
    tagInput.value = "";
});

tagBox.addEventListener("click", (event) => {
    if (event.target === tagBox) {
        tagInput.focus();
    }
});

document.querySelectorAll(".tag-suggestion").forEach((chip) => {
    chip.addEventListener("click", () => addTag(chip.dataset.tag));
});

function bindCounter(input, output) {
    const update = () => { output.textContent = input.value.length; };
    input.addEventListener("input", update);
    update();
}

bindCounter(titleInput, titleCount);
bindCounter(contentInput, contentCount);

// 비밀번호 칸에는 숫자만 들어가게 한다. 서버도 검증하지만 여기서 막아야 덜 답답하다.
if (passwordInput) {
    passwordInput.addEventListener("input", () => {
        passwordInput.value = passwordInput.value.replace(/\D/g, "");
    });
}

async function loadPostForEdit() {
    const response = await authFetch(`${HOST}/api/posts/${postId}`);
    if (!response.ok) {
        await showAlert({ title: "오류", msg: "글을 불러오지 못했습니다." });
        location.href = "/community";
        return;
    }
    const post = await response.json();
    if (!post.mine) {
        await showAlert({ title: "수정 불가", msg: "본인이 작성한 글만 수정할 수 있습니다." });
        location.href = `/community/${postId}`;
        return;
    }
    titleInput.value = post.title;
    contentInput.value = post.content;
    titleCount.textContent = post.title.length;
    contentCount.textContent = post.content.length;
    tags.push(...(post.tags ?? []));
    renderTags();
}

function validate() {
    if (!titleInput.value.trim()) {
        return "제목을 입력해주세요.";
    }
    if (!contentInput.value.trim()) {
        return "내용을 입력해주세요.";
    }
    if (!isEdit && !/^\d{4}$/.test(passwordInput.value)) {
        return "삭제 비밀번호를 숫자 네 자리로 입력해주세요.";
    }
    return null;
}

async function submit() {
    const invalidMessage = validate();
    if (invalidMessage) {
        await showAlert({ title: "확인해주세요", msg: invalidMessage });
        return;
    }

    submitBtn.disabled = true;
    try {
        const url = isEdit ? `${HOST}/api/posts/${postId}` : `${HOST}/api/posts`;
        const body = isEdit
            ? { title: titleInput.value, content: contentInput.value, tags }
            : {
                title: titleInput.value,
                content: contentInput.value,
                tags,
                delete_password: passwordInput.value,
            };

        const response = await authFetch(url, {
            method: isEdit ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            await showAlert({ title: "등록 실패", msg: error.message ?? "잠시 후 다시 시도해주세요." });
            return;
        }

        const post = await response.json();
        location.href = `/community/${post.id}`;
    } catch (e) {
        await showAlert({ title: "통신 오류", msg: "잠시 후 다시 시도해주세요." });
    } finally {
        submitBtn.disabled = false;
    }
}

renderTags();
submitBtn.addEventListener("click", submit);

if (isEdit) {
    loadPostForEdit();
}
