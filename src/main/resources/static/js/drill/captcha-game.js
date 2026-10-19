import { startCountdown } from "./countdown.js";

export const LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
export const IME = "한";

const ROUNDS = 3;
const TIME_LIMIT_MS = 60_000;
const MAX_TYPOS = 30;
const HANGUL = /[ㄱ-ㅎㅏ-ㅣ가-힣]/;

/*
 * 보안문자 한 판. 결과: { ok: true, problems: [{ answer, elapsed_ms, typos: [{ position, typed }] }], keys }
 * 또는 { ok: false, title, why }.
 * 시작 버튼을 누른 순간(사용자 제스처 안)에 입력창 포커스를 줘야 모바일 키보드가 올라온다. 카운트다운은 그 위에 덮는다.
 * 키보드가 뜨면 iOS 는 입력창이 보일 만큼만 화면을 밀어 위가 잘린다. 그만하기 줄을 화면 맨 위에 맞춘다.
 */
export function playCaptcha(box, setStep) {
    let resolve;
    const done = new Promise(r => { resolve = r; });
    const timers = [];
    let cancelCountdown = () => {};
    const vv = window.visualViewport;
    const pin = () => {
        if (vv.height < innerHeight - 120) box.querySelector(".crumb").scrollIntoView({ block: "start" });
        else scrollTo(0, 0);
    };
    vv?.addEventListener("resize", pin);
    const stop = () => { timers.forEach(clearInterval); cancelCountdown(); vv?.removeEventListener("resize", pin); scrollTo(0, 0); };

    box.insertAdjacentHTML("beforeend", `
        <div class="ptop"><div class="psteps">${"<i></i>".repeat(ROUNDS)}</div><div class="ptimer"><b>0.0</b><small>초</small></div></div>
        <div class="ccard"><canvas width="500" height="160"></canvas><div class="slots">${"<i></i>".repeat(6)}</div></div>
        <div class="ibar2"><input maxlength="6" placeholder="문자 입력" autocomplete="off" autocapitalize="characters" spellcheck="false" aria-label="보안문자 입력"><button type="button">확인</button></div>
        <p class="pmsg"></p>`);
    const segs = box.querySelector(".psteps").children, live = box.querySelector(".ptimer b");
    const canvas = box.querySelector("canvas"), slots = box.querySelector(".slots").children;
    const bar = box.querySelector(".ibar2"), input = bar.querySelector("input"), msg = box.querySelector(".pmsg");
    input.focus();

    let answer = "", cur = null, round = 0, prev = "", keys = 0, over = false;
    const problems = [];
    const finish = outcome => { over = true; stop(); resolve(outcome); };

    function draw() {
        const ctx = canvas.getContext("2d");
        const r = 50 + Math.floor(Math.random() * 100), g = 50 + Math.floor(Math.random() * 100), b = 50 + Math.floor(Math.random() * 100);
        ctx.fillStyle = `rgb(${r},${g},${b})`;
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        const lum = 0.299 * r + 0.587 * g + 0.114 * b, pick = () => lum < 118 ? 240 + Math.floor(Math.random() * 16) : Math.floor(Math.random() * 26);
        const color = `rgb(${pick()},${pick()},${pick()})`;
        answer = Array.from({ length: 6 }, () => LETTERS[Math.floor(Math.random() * LETTERS.length)]).join("");
        ctx.font = "bold 80px Arial";
        ctx.textBaseline = "middle";
        ctx.textAlign = "center";
        [...answer].forEach((c, i) => {
            ctx.save();
            ctx.translate((40 + i * 35) * 2, (45 + Math.random() * 5 - 2.5) * 2);
            ctx.rotate(Math.random() * 0.1 - 0.05);
            ctx.fillStyle = color;
            ctx.fillText(c, 0, 0);
            ctx.restore();
        });
        ctx.strokeStyle = "#fff";
        ctx.lineWidth = 3;
        for (let i = 0; i < 2; i++) {
            ctx.beginPath();
            ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height);
            ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height);
            ctx.stroke();
        }
        ctx.fillStyle = "yellow";
        for (let i = 0; i < 50; i++) {
            ctx.beginPath();
            ctx.arc(Math.random() * canvas.width, Math.random() * canvas.height, 3, 0, Math.PI * 2);
            ctx.fill();
        }
        input.value = "";
        prev = "";
        paintSlots();
    }

    function paintSlots() {
        const v = input.value;
        [...slots].forEach((el, i) => { el.className = i >= v.length ? "" : v[i] === answer[i] ? "ok" : "bad"; });
    }

    /*
     * 새로 들어온 글자를 자리마다 정답과 비교해 다르면 오타 1회. 앞뒤가 그대로인 구간은 건너뛰어 삭제·이동은 세지 않는다.
     * 한글은 조합 중간 상태마다 입력 이벤트가 와서, 조합이 끝났을 때 한 번만 센다.
     */
    function count(v) {
        let k = 0, s = 0;
        while (k < v.length && k < prev.length && v[k] === prev[k]) k++;
        while (s < v.length - k && s < prev.length - k && v[v.length - 1 - s] === prev[prev.length - 1 - s]) s++;
        for (let p = k; p < v.length - s && p < answer.length; p++) {
            keys++;
            if (v[p] !== answer[p] && cur.typos.length < MAX_TYPOS) {
                cur.typos.push({ position: p, typed: HANGUL.test(v[p]) ? IME : v[p] });
            }
        }
        prev = v;
    }

    function onInput(e) {
        const v = input.value.toUpperCase().replace(/[^A-Zㄱ-ㅎㅏ-ㅣ가-힣]/g, "");
        if (input.value !== v) input.value = v;
        if (!cur) prev = v;
        else if (!e?.isComposing) count(v);
        paintSlots();
        if (HANGUL.test(v)) msg.innerHTML = `<span class="no">한글로 입력되고 있습니다 · 한/영 키를 눌러주세요</span>`;
    }

    function next() {
        if (round >= ROUNDS) return finish({ ok: true, problems, keys });
        setStep(`문제 ${round + 1} / ${ROUNDS}`);
        segs[round].className = "now";
        msg.innerHTML = "";
        draw();
        cur = { t0: performance.now(), typos: [] };
        input.focus();
    }

    function submit() {
        if (!cur || over) return;
        onInput();
        const v = input.value.trim().toUpperCase();
        if (!v) return;
        if (v === answer) {
            const ms = Math.round(performance.now() - cur.t0);
            problems.push({ answer, elapsed_ms: ms, typos: cur.typos });
            segs[round].className = cur.typos.length ? "bad" : "good";
            cur = null;
            msg.innerHTML = `<span class="ok">정답 · ${(ms / 1000).toFixed(2)}초</span>`;
            round++;
            timers.push(setTimeout(next, 500));
            return;
        }
        msg.innerHTML = `<span class="no">입력한 문자를 다시 확인해주세요</span>`;
        bar.classList.add("wrong");
        timers.push(setTimeout(() => bar.classList.remove("wrong"), 300));
        input.value = "";
        prev = "";
        paintSlots();
        input.focus();
    }

    function watch() {
        if (over) return;
        const spent = problems.reduce((a, p) => a + p.elapsed_ms, 0) + (cur ? performance.now() - cur.t0 : 0);
        if (cur) live.textContent = ((performance.now() - cur.t0) / 1000).toFixed(1);
        if (spent > TIME_LIMIT_MS) {
            input.disabled = true;
            finish({ ok: false, title: "시간이 초과됐습니다", why: "3문제를 1분 안에 입력해야 합니다" });
        }
    }

    input.addEventListener("input", onInput);
    input.addEventListener("compositionend", () => onInput());
    bar.querySelector("button").addEventListener("click", submit);
    input.addEventListener("keydown", e => { if (e.key === "Enter" && !e.isComposing) submit(); });

    setStep(`문제 1 / ${ROUNDS}`);
    cancelCountdown = startCountdown({ title: "보안문자", sub: `${ROUNDS}문제` }, () => { timers.push(setInterval(watch, 100)); next(); });

    return { done, abort: stop };
}
