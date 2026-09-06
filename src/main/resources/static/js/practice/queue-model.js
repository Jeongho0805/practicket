/* 대기열 순번 공식. 세 예매처가 같은 값을 쓴다.
   대기 시간이 기록의 일부라 종목마다 다르면 순위를 나란히 놓고 볼 수 없다.
   난이도를 조정할 때 여기만 고친다 — 예매처별로 따로 두면 반드시 어긋난다. */

export const QUEUE = {
    MIN: 5_000,
    MAX: 200_000,
    DEQ: 20_000,
    MAX_REACTION: 3_000,
    STEP_MS: 100,
    LOADING_MS: 800,
    TICK_MS: 200,
};

const STEPS = QUEUE.MAX_REACTION / QUEUE.STEP_MS;

/* 0.1초 단위로 끊는다. 그 안쪽은 겨눠서 맞추는 구간이 아니라 손이 떨리는 폭이다. */
export function initialRank(reactionMs) {
    const step = Math.min(Math.floor(reactionMs / QUEUE.STEP_MS), STEPS);
    return Math.round(QUEUE.MIN + (step / STEPS) * (QUEUE.MAX - QUEUE.MIN));
}
