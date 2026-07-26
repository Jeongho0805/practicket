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
            ? { title: titleInput.value, content: contentInput.value }
            : {
                title: titleInput.value,
                content: contentInput.value,
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

submitBtn.addEventListener("click", submit);

if (isEdit) {
    loadPostForEdit();
}
