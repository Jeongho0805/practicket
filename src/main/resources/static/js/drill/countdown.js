const STEPS = ["3", "2", "1", "시작"];

/* 3·2·1·시작 덮개. 보안문자·포도알·취소표가 같이 쓴다. 반환한 cancel 로 중간에 정리한다. */
export function startCountdown({ title, sub }, go) {
    const el = document.createElement("div");
    el.className = "drill-cd";
    document.body.appendChild(el);
    let id;
    const show = i => {
        el.innerHTML = `<div class="cd-in"><small>${title}</small><b class="${i === 3 ? "go" : ""}">${STEPS[i]}</b><p>${sub}</p></div>`;
        id = setTimeout(i < 3 ? () => show(i + 1) : () => { el.remove(); go(); }, i < 3 ? 1000 : 500);
    };
    show(0);
    return () => { clearTimeout(id); el.remove(); };
}
