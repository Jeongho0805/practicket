import * as util from "./common.js";

let isFirst = true;
/* 매초 표를 통째로 다시 그리면 스크롤이 튄다. 그래서 명단이 실제로 달라졌을 때만 그린다.
   인원수만 비교하면 라운드가 바뀌어도 인원이 같을 때 옛 명단이 그대로 남는다. */
let renderedSignature = null;

function displayRank() {
    setInterval(() => {
        const rankBody = document.getElementById("rank-body");
        fetch(`${HOST}/api/rank`, {
            method: "GET",
            credentials: 'same-origin',
        })
            .then(response => response.json())
            .then(data => {
                if (data.length === 0) {
                    isFirst = true;
                }
                const signature = data.map(rank => `${rank.key}|${rank.name}|${rank.second}`).join(",");
                if (signature === renderedSignature) {
                    return;
                }
                renderedSignature = signature;
                rankBody.innerHTML = "";
                console.log(JSON.stringify(data));
                data.forEach((rank, index) => {
                    const row = document.createElement("tr");
                    row.setAttribute("data-user-key", rank.key);

                    const rankCell = document.createElement("td");
                    rankCell.textContent = index + 1;
                    row.appendChild(rankCell);

                    const nameCell = document.createElement("td");
                    nameCell.textContent = rank.name;
                    row.appendChild(nameCell);

                    const timeCell = document.createElement("td");
                    timeCell.textContent = rank.second;
                    row.appendChild(timeCell);

                    rankBody.appendChild(row);
                })
                markMyRank()
            }).catch(error => {
                console.log(error);
            })
    }, 1000)
}
function markMyRank() {
    const trElements = document.querySelectorAll('tr');
    trElements.forEach(tr => {
        const key = tr.dataset.userKey;
        if (key === util.getTokenValue()) {
            tr.classList.add('my-rank');

            if (!isFirst) {
                return;
            }
            tr.scrollIntoView({
                behavior: "smooth",
                block: "center"
            });
            isFirst = false;
        }
    });
}

displayRank();
