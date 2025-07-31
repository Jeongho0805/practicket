import * as util from "./common.js";

let isFirst = true;

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
                if (data.length === rankBody.children.length ) {
                    return;
                }
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
            tr.style.backgroundColor = "darkslateblue"
            tr.style.color = "white"
            if (!isFirst) {
                return;
            }
            tr.scrollIntoView({
                behavior: "smooth", // 부드럽게 스크롤 이동
                block: "center"     // 화면 중앙에 해당 요소가 오도록 설정
            });
            isFirst = false;
        }
    });
}

displayRank();
