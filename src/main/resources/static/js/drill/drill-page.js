import { closeResultModal, openResultModal } from "./result-modal.js";

/*
 * 준비 ⇄ 연습 ⇄ 결과 모달을 한 문서 안에서 바꾼다. 판마다 문서를 다시 불러오면 광고 요청이 판 수만큼 늘어난다.
 * start(box) 는 한 판을 그리고 { done: Promise<결과>, abort() } 를, finish(결과) 는 모달 내용을 돌려준다.
 * onReady 는 준비 화면이 보일 때 부른다. 가려진 동안에는 폭이 0이라 그래프를 그릴 수 없다.
 */
export function createDrillPage({ start, finish, onReady }) {
    const page = document.getElementById("drill-page");
    const playBox = document.getElementById("drill-play");
    const sticky = page.querySelector(".practice-sticky");
    let state = "ready", game = null;

    const setState = next => {
        state = next;
        page.classList.toggle("is-ready", next === "ready");
        page.classList.toggle("is-playing", next !== "ready");
        document.body.classList.toggle("drill-playing", next !== "ready");
    };

    function stopGame() {
        game?.abort();
        game = null;
        playBox.innerHTML = "";
    }

    function toReady() {
        stopGame();
        closeResultModal();
        setState("ready");
        page.scrollTop = 0;
        onReady();
    }

    function play(replace) {
        stopGame();
        closeResultModal();
        if (replace) history.replaceState({ drill: "play" }, "");
        else history.pushState({ drill: "play" }, "");
        setState("playing");
        page.scrollTop = 0;
        playBox.innerHTML = `<div class="crumb"><button type="button" class="quit">‹ 그만하기</button><span class="step"></span></div>`;
        playBox.querySelector(".quit").addEventListener("click", () => history.back());
        const current = game = start(playBox, text => { playBox.querySelector(".step").textContent = text; });
        current.done.then(async outcome => {
            if (game !== current) return;
            const modal = await finish(outcome);
            if (game !== current) return;
            history.replaceState({ drill: "modal" }, "");
            state = "modal";
            openResultModal({ ...modal, onRecord: () => history.back(), onRetry: () => play(true) });
        });
    }

    document.getElementById("drill-start").addEventListener("click", () => { if (state === "ready") play(false); });

    addEventListener("popstate", () => { if (state !== "ready") toReady(); });
    addEventListener("pageshow", e => {
        if (!e.persisted) return;
        if (history.state?.drill) history.replaceState(null, "");
        toReady();
    });
    if (history.state?.drill) history.replaceState(null, "");

    page.addEventListener("scroll", () => sticky.classList.toggle("stuck", page.scrollTop > 4), { passive: true });
    setState("ready");
}
