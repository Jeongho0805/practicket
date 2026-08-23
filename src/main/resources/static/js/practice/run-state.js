/* 한 판의 진행 상태. 세 예매처가 같은 키를 쓴다.

   시각은 전부 Date.now() 로 찍어 sessionStorage 에 넣는다. performance.now() 는 페이지마다
   0 부터라 m-ticket 처럼 대기열과 좌석이 다른 페이지면 이어지지 않는다.

   진행 중인 구간은 시작 시각을, 끝난 구간은 걸린 시간을 저장한다.
   그래야 페이지가 새로 떠도 값 하나로 복원된다.

   규칙과 배경은 docs/practice-record-integrity.md. */

const K = {
    sessionId: 'pkt.sessionId',
    stage: 'pkt.stage',
    reactionStartMs: 'pkt.reactionStartMs',
    reactionTimeMs: 'pkt.reactionTimeMs',
    queueStartMs: 'pkt.queueStartMs',
    queueWaitMs: 'pkt.queueWaitMs',
    queueInitialRank: 'pkt.queueInitialRank',
    captchaMs: 'pkt.captchaMs',
    captchaOpenedAtMs: 'pkt.captchaOpenedAtMs',
};

export const STAGE = { QUEUE: 'QUEUE', SEAT: 'SEAT' };

const num = key => parseInt(sessionStorage.getItem(key) || '0', 10);
const put = (key, value) => sessionStorage.setItem(key, String(value));

/** 인트로가 뜨면 부른다. 인트로를 밟는 것은 판을 접는 것이다. */
export function clearRun() {
    Object.values(K).forEach(k => sessionStorage.removeItem(k));
    sessionStorage.removeItem('captcha_solved');
}

export const sessionId = () => sessionStorage.getItem(K.sessionId) || '';
export const setSessionId = id => put(K.sessionId, id);

export const stage = () => sessionStorage.getItem(K.stage) || '';
export const setStage = s => put(K.stage, s);

/** 카운트다운이 끝나 예매 버튼이 열린 순간. 반응 구간과 좌석 소진이 여기서 출발한다. */
export const openedAt = () => num(K.reactionStartMs);
export const markOpened = () => put(K.reactionStartMs, Date.now());

/** 예매하기를 누른 순간. 반응이 끝나고 대기가 시작된다 — 경계가 하나라 틈이 없다. */
export function markBooked() {
    const opened = openedAt();
    const now = Date.now();
    if (opened) put(K.reactionTimeMs, Math.max(0, now - opened));
    put(K.queueStartMs, now);
}

export const reactionMs = () => num(K.reactionTimeMs);

/** 대기 구간은 예매 클릭부터라 로딩 화면과 페이지 이동이 여기 포함된다. */
export function markQueuePassed() {
    const started = num(K.queueStartMs);
    if (started) put(K.queueWaitMs, Math.max(0, Date.now() - started));
    put(K.stage, STAGE.SEAT);
}

export const queueWaitMs = () => num(K.queueWaitMs);

export const initialRank = () => num(K.queueInitialRank);
export const setInitialRank = rank => put(K.queueInitialRank, rank);

/* 보안문자는 여러 번 열릴 수 있다(접어두기 · 나중에 입력하기).
   그래서 "창이 떠 있던 시간"을 누적한다. 새로고침으로 시간을 숨길 수 없도록
   열린 시각은 닫을 때까지 남겨둔다. */
export function captchaOpened() {
    if (!num(K.captchaOpenedAtMs)) put(K.captchaOpenedAtMs, Date.now());
}

export function captchaClosed() {
    const opened = num(K.captchaOpenedAtMs);
    if (!opened) return;
    put(K.captchaMs, num(K.captchaMs) + Math.max(0, Date.now() - opened));
    sessionStorage.removeItem(K.captchaOpenedAtMs);
}

/** 창이 열린 채 완료로 넘어가는 경로가 있어 마지막에 한 번 더 닫아준다. */
export function captchaMs() {
    captchaClosed();
    return num(K.captchaMs);
}

/** 대기열을 통과한 순간 서버에 알린다. 응답은 쓰지 않으므로 기다리지 않는다. */
export function sendCheckpoint(authFetch) {
    const id = sessionId();
    if (!id) return;
    authFetch('/api/practice/checkpoint', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ session_id: id }),
    }).catch(() => { /* 실패하면 complete 가 거부되고 미저장 안내가 뜬다 */ });
}
