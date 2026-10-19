let request;

/* 모두에게 같은 값이라 토큰 없이 부른다. 페이지마다 한 번만 부르고, 연습 판을 오가도 다시 부르지 않는다. */
export function loadActivity() {
    request ??= fetch(`${HOST}/api/drill/activity`)
        .then(response => response.ok ? response.json() : null)
        .catch(() => null);
    return request;
}

export function showActivity(chip, stat) {
    if (!stat?.players) return;
    chip.innerHTML = `<i></i><span>오늘 <b>${stat.players.toLocaleString()}명</b>이 <b>${stat.rounds.toLocaleString()}회</b> 연습했어요</span>`;
    chip.hidden = false;
}
