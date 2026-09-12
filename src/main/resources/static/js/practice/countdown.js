/* ============================================================
   예매 오픈까지 남은 시간 — 세 연습 화면이 같이 쓴다

   실물(NOL 게이트·멜론 상품 페이지)은 남은 시간을 버려서 찍는다. 0.4초가 남아도
   "0초 남음"이라 마지막 1초가 통째로 00:00 으로 선다. 숫자를 1씩 빼면 그 1초가
   사라져 00:00 이 화면에 남지 않는다.

   그래서 뺄셈 대신 마감 시각에서 매번 다시 계산한다. 탭이 잠깐 멈춰도
   여는 시각이 밀리지 않는다 — 반응속도를 재는 화면이라 이게 중요하다.
   ============================================================ */

/* onTick 은 표시할 초가 바뀔 때만 부른다. onOpen 은 마감 시각에 한 번 부른다. */
export function startCountdown(seconds, onTick, onOpen) {
    const until = Date.now() + seconds * 1000;
    let shown = null;
    let timer = 0;

    const tick = () => {
        const left = until - Date.now();
        if (left <= 0) {
            clearInterval(timer);
            onOpen();
            return;
        }
        /* 올림에서 1 을 빼면 각 숫자가 정확히 1초씩 선다.
           6초면 05 부터 00 까지 여섯 개가 1초씩 지나간다. */
        const sec = Math.max(0, Math.ceil(left / 1000) - 1);
        if (sec === shown) return;
        shown = sec;
        onTick(sec);
    };

    tick();
    timer = setInterval(tick, 100);
    return () => clearInterval(timer);
}
