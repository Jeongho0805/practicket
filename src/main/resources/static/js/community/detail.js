import { authFetch, showAlert } from "/js/common.js";

const section = document.getElementById("community-section");
const postId = section.dataset.postId;

const ownerActions = document.getElementById("owner-actions");
const guestActions = document.getElementById("guest-actions");
const passwordPrompt = document.getElementById("password-prompt");
const passwordInput = document.getElementById("delete-password-input");

/**
 * 본문은 서버가 이미 그렸다. 여기서는 "이 글이 내 글인지"만 확인해
 * 수정·삭제 버튼을 열어준다. 토큰은 브라우저에만 있어서 서버가 알 수 없다.
 */
async function resolveOwnership() {
    try {
        const response = await authFetch(`${HOST}/api/posts/${postId}`);
        if (!response.ok) {
            return;
        }
        const post = await response.json();
        if (post.mine) {
            ownerActions.hidden = false;
            guestActions.hidden = true;
        }
    } catch (e) {
        // 소유 확인에 실패해도 본문은 이미 보인다. 조용히 둔다.
    }
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
    // 브라우저 confirm 대신 두 번 누르게 하지 않고, 작성자 본인이므로 바로 지운다.
    // 실수 방지는 삭제 후 되돌릴 수 있다는 사실(soft delete)로 보완한다.
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

resolveOwnership();
