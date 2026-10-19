import { sec } from "./record-card.js";

const BACKGROUND = ["nav-section", "content-section", "chat-widget"];

export const escapeHtml = value => String(value).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

export function deltaLine(ms, previousMs) {
    if (previousMs == null) return `<p class="rb-delta flat">첫 기록입니다</p>`;
    const diff = Number(sec(previousMs)) - Number(sec(ms));
    if (Math.abs(diff) < 0.005) return `<p class="rb-delta flat">지난 판과 같습니다</p>`;
    return `<p class="rb-delta ${diff > 0 ? "up" : "down"}">지난 판보다 <b>${Math.abs(diff).toFixed(2)}초</b> ${diff > 0 ? "빨라졌습니다" : "느려졌습니다"}</p>`;
}

export const failBody = (title, why) =>
    `<div class="rb-main fail"><p class="rb-fail-t">${escapeHtml(title)}</p><p class="rb-fail-w">${escapeHtml(why)}</p><span class="rb-note">실패한 판은 기록에 남지 않습니다</span></div>`;

const unsavedLine = `<p class="rb-unsaved">이번 판 기록은 저장하지 못했습니다</p>`;

export function showUnsaved() {
    const main = document.querySelector("#pkt-complete-overlay .rb-main");
    if (main && !main.parentElement.querySelector(".rb-unsaved")) main.insertAdjacentHTML("afterend", unsavedLine);
}

/* 결과 모달. 뒤 화면은 inert 로 막고, Tab 은 모달 안에서만 돈다. */
export function openResultModal({ fail, meta, body, wide, onRecord, onRetry }) {
    closeResultModal();
    document.body.insertAdjacentHTML("beforeend", `
        <div id="pkt-complete-overlay" class="visible drill-result">
            <section class="pkt-modal-card${wide ? " has-round-notes" : ""}" role="dialog" aria-modal="true" aria-labelledby="drill-modal-title">
                <div class="pkt-modal-header${fail ? " pkt-modal-header--fail" : ""}">
                    <div class="pkt-header-tag">${fail ? "Time Over" : "Practice Complete"}</div>
                    <div class="pkt-header-title" id="drill-modal-title">${fail ? "아쉽네요!" : "연습 완료!"}</div>
                    <div class="pkt-header-meta strong">${meta}</div>
                </div>
                <div class="rb">${body}</div>
                <div class="pkt-btn-section">
                    <button type="button" class="pkt-btn-retry" data-act="record">기록 보기</button>
                    <button type="button" class="pkt-btn-rank" data-act="retry">${fail ? "다시 하기" : "한 번 더"}</button>
                </div>
            </section>
        </div>`);

    const overlay = document.getElementById("pkt-complete-overlay");
    BACKGROUND.forEach(id => { const el = document.getElementById(id); if (el) el.inert = true; });
    overlay.querySelector('[data-act="record"]').addEventListener("click", onRecord);
    overlay.querySelector('[data-act="retry"]').addEventListener("click", onRetry);
    overlay.addEventListener("keydown", e => {
        if (e.key === "Escape") { e.preventDefault(); onRecord(); return; }
        if (e.key !== "Tab") return;
        const buttons = [...overlay.querySelectorAll("button")], first = buttons[0], last = buttons.at(-1);
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    });
    overlay.querySelector('[data-act="retry"]').focus({ preventScroll: true });
}

export function closeResultModal() {
    document.getElementById("pkt-complete-overlay")?.remove();
    BACKGROUND.forEach(id => { const el = document.getElementById(id); if (el) el.inert = false; });
}
