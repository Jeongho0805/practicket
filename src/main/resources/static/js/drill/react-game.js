const LIMIT = 5000;
const OPEN_DELAY = 5000;
const AUTO_COUNTDOWN = 6000;
const RENDER_MS = 60;
const OPEN_AT = 20 * 3600 * 1000;

const REFRESH_ICON = `<svg viewBox="0 0 14 14"><path d="M11.5 7A4.5 4.5 0 1 1 10.2 3.8M11.5 1.5V4.5H8.5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

/* 결과: { ok: true, ms } 또는 { ok: false, title, why } */
export function playReact(box, { form, latency }) {
    let resolve;
    const done = new Promise(r => { resolve = r; });
    const timers = [];
    const stop = () => timers.forEach(clearInterval);
    const end = outcome => { stop(); resolve(outcome); };
    const fail = (title, why) => end({ ok: false, title, why });

    if (form === "n-ticket") playAuto(box, timers, end, fail);
    else playRefresh(box, timers, end, fail, form === "advanced" ? latency : 0);

    return { done, abort: stop };
}

/* N형: 남은 시간이 0이 되면 버튼이 저절로 열린다. */
function playAuto(box, timers, end, fail) {
    box.insertAdjacentHTML("beforeend", `
        <section class="gate"><h1>예매 준비가 완료됐어요</h1><div class="gposter"><img src="/image/drill/hoya-poster.svg" alt=""></div><p class="gname">HOYA LIVE 2026 IN SEOUL</p><button type="button" class="gbook">남은시간 00:05</button></section>`);
    const buy = box.querySelector(".gbook");
    const until = performance.now() + AUTO_COUNTDOWN;
    let openAt = 0, finished = false, shown = null;

    timers.push(setInterval(() => {
        const now = performance.now();
        if (!openAt) {
            const left = until - now;
            if (left > 0) {
                const sec = Math.max(0, Math.ceil(left / 1000) - 1);
                if (sec !== shown) { shown = sec; buy.textContent = `남은시간 00:${String(sec).padStart(2, "0")}`; }
                return;
            }
            openAt = now;
            buy.textContent = "예매하기";
            buy.classList.add("is-open");
            return;
        }
        if (now - openAt > LIMIT) {
            finished = true;
            fail("예매하기를 놓쳤습니다", "버튼이 열린 뒤 5초 안에 눌러야 합니다");
        }
    }, 10));

    buy.addEventListener("pointerdown", () => {
        if (finished || !openAt) return;
        finished = true;
        end({ ok: true, ms: Math.round(performance.now() - openAt) });
    });
}

/* 일반형·고급형: 오픈 뒤 새로고침해야 열린다. 고급형은 요청과 응답에 지연이 붙는다. 새로고침은 화면 상태만 바꾼다. */
function playRefresh(box, timers, end, fail, latency) {
    const advanced = latency > 0;
    const openIn = OPEN_DELAY + latency;
    box.insertAdjacentHTML("beforeend", `
        <div class="sclock"><small>서버 시간</small><div class="sc"></div>${advanced ? `<p class="sc-lat">요청이 서버까지 <b>${(latency / 1000).toFixed(2)}초</b> · 응답도 같은 시간</p>` : ""}</div>
        <section class="ydet">
            <p class="ycat">콘서트 &gt; 서울</p>
            <h1>HOYA LIVE 2026 IN SEOUL</h1>
            <p class="ysub">2026.11.21 ~ 2026.11.22 &nbsp;|&nbsp; 올림픽공원 올림픽홀</p>
            <div class="ybody"><div class="yposter"></div><div class="yinfo">
                <div class="yopen"><b>티켓오픈 20:00</b>오픈 시각이 지나면 새로고침해야 예매할 수 있습니다</div>
                <div class="ybtns"><button type="button" class="rf">${REFRESH_ICON}새로고침</button><button type="button" class="bk">예매하기</button></div>
                <p class="rlog"></p>
            </div></div>
            <div class="mload"><i></i></div>
        </section>`);

    const clock = box.querySelector(".sc"), gate = box.querySelector(".ydet");
    const buy = box.querySelector(".bk"), log = box.querySelector(".rlog");
    const t0 = performance.now(), openAt = t0 + openIn;
    const st = { loading: false, arrive: 0, hit: null, done: false };
    const pad = n => String(n).padStart(2, "0");

    const lose = (title, why) => { st.done = true; fail(title, why); };
    const tick = () => {
        const now = performance.now();
        const t = OPEN_AT - openIn + Math.floor(now - t0), sec = Math.floor(t / 1000);
        clock.classList.toggle("sc-on", now >= openAt);
        clock.innerHTML = `${pad(Math.floor(sec / 3600) % 24)}<i>시</i> ${pad(Math.floor(sec / 60) % 60)}<i>분</i> ${pad(sec % 60)}<i>초</i><sup>${String(t % 1000).padStart(3, "0")}</sup>`;
        if (st.done) return;
        if (!st.hit && now > openAt + LIMIT && !(st.loading && st.arrive <= openAt + LIMIT)) {
            lose("새로고침이 늦었습니다", advanced ? "요청이 오픈 뒤 5초 안에 서버에 닿아야 합니다" : "오픈 뒤 5초 안에 새로고침해야 합니다");
        } else if (st.hit && now - st.hit.got > LIMIT) {
            lose("예매하기를 놓쳤습니다", "판매가 열린 뒤 5초 안에 눌러야 합니다");
        }
    };
    timers.push(setInterval(tick, 16));

    box.querySelector(".rf").addEventListener("click", () => {
        if (st.loading || st.done || st.hit) return;
        const press = performance.now(), down = latency + (advanced ? RENDER_MS : 0), arrive = press + latency;
        st.loading = true;
        st.arrive = arrive;
        if (advanced) gate.classList.add("is-loading");
        timers.push(setTimeout(() => {
            if (st.done) return;
            st.loading = false;
            gate.classList.remove("is-loading");
            const took = ((latency + down) / 1000).toFixed(2);
            if (arrive >= openAt) {
                st.hit = { got: performance.now() };
                buy.classList.add("is-open");
                log.innerHTML = advanced ? `응답 ${took}초 · 판매가 열렸습니다` : "판매가 열렸습니다";
                return;
            }
            const gap = ((openAt - arrive) / 1000).toFixed(2);
            log.innerHTML = advanced
                ? `<b>아직 판매 전</b> · 요청이 서버에 오픈 ${gap}초 전에 도착했습니다 · 응답 ${took}초`
                : `<b>아직 판매 전</b> · 오픈 ${gap}초 전에 눌렀습니다`;
        }, latency + down));
    });

    buy.addEventListener("pointerdown", () => {
        if (!st.hit || st.done) return;
        st.done = true;
        end({ ok: true, ms: Math.round(performance.now() - openAt) });
    });

    tick();
}
