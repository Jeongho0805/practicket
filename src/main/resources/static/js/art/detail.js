class Detail {
    constructor() {
        this.artId = ART_ID;
        this.artData = null;

        this.titleElement = document.getElementById('artTitle');
        this.descriptionElement = document.getElementById('artDescription');
        this.authorElement = document.getElementById('authorName');
        this.createdAtElement = document.getElementById('createdAt');
        this.viewCountElement = document.getElementById('viewCount');
        this.artCanvas = document.getElementById('artCanvas');
        this.artActions = document.getElementById('artActions');
        this.editBtn = document.getElementById('editBtn');
        this.deleteBtn = document.getElementById('deleteBtn');

        this.init();
    }

    async init() {
        await this.loadArt();
        this.setupEventListeners();
    }

    async loadArt() {
        try {
            const response = await fetch(`${HOST}/api/arts/${this.artId}`, {
                credentials: 'include'
            });

            if (response.ok) {
                this.artData = await response.json();
                this.renderArt();
                this.checkOwnership();
            } else {
                throw new Error('작품을 불러올 수 없습니다.');
            }
        } catch (error) {
            console.error('작품 로딩 실패:', error);
            alert('작품을 불러오는데 실패했습니다.');
            window.location.href = '/art';
        }
    }

    renderArt() {
        const { title, description, author_name, created_at, view_count, pixel_data, width, height } = this.artData;

        // 기본 정보 표시
        this.titleElement.textContent = title;
        this.descriptionElement.textContent = description;
        this.authorElement.textContent = author_name;
        this.createdAtElement.textContent = this.formatDate(created_at);
        this.viewCountElement.textContent = view_count.toLocaleString();

        // 캔버스 설정 및 렌더링
        const pixelSize = Math.max(8, Math.min(20, Math.floor(400 / Math.max(width, height))));
        this.artCanvas.width = width * pixelSize;
        this.artCanvas.height = height * pixelSize;

        this.renderPixelArt(this.artCanvas, pixel_data, pixelSize);
    }

    renderPixelArt(canvas, pixelData, pixelSize) {
        const ctx = canvas.getContext('2d');
        ctx.imageSmoothingEnabled = false;

        // 배경을 부드러운 그라데이션으로
        const gradient = ctx.createLinearGradient(0, 0, 0, canvas.height);
        gradient.addColorStop(0, '#f8fafc');
        gradient.addColorStop(1, '#e2e8f0');
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        if (!pixelData || !Array.isArray(pixelData)) {
            return;
        }

        const seatSize = pixelSize * 0.7; // 좌석 크기 (여백 포함)
        const seatGap = pixelSize * 0.3; // 좌석 간 여백

        for (let y = 0; y < pixelData.length; y++) {
            for (let x = 0; x < pixelData[y].length; x++) {
                const isSelected = pixelData[y][x];
                const seatX = x * pixelSize + seatGap / 2;
                const seatY = y * pixelSize + seatGap / 2;

                if (isSelected === true) {
                    // 선택된 좌석 (모던한 보라색)
                    this.drawModernSeat(ctx, seatX, seatY, seatSize, true, false);
                } else {
                    // 매진 좌석 (모던한 회색)
                    this.drawModernSeat(ctx, seatX, seatY, seatSize, false, false);
                }
            }
        }
    }

    drawModernSeat(ctx, x, y, size, isSelected, isHovered) {
        const seatSize = size * 0.9;
        const offset = (size - seatSize) / 2;
        const radius = seatSize * 0.25;

        // 그림자 효과
        ctx.shadowColor = 'rgba(0, 0, 0, 0.1)';
        ctx.shadowBlur = 4;
        ctx.shadowOffsetX = 0;
        ctx.shadowOffsetY = 2;

        if (isSelected) {
            // 선택된 좌석 - 단색 + 테두리
            ctx.fillStyle = '#e11d48'; // 로즈 핑크
            this.drawRoundedRect(ctx, x + offset, y + offset, seatSize, seatSize, radius);

            // 테두리 추가
            ctx.shadowColor = 'transparent';
            ctx.strokeStyle = '#9f1239'; // 더 진한 색상으로 테두리
            ctx.lineWidth = 2;
            this.strokeRoundedRect(ctx, x + offset, y + offset, seatSize, seatSize, radius);

        } else {
            // 매진 좌석 - 단색 + 테두리
            ctx.fillStyle = '#64748b'; // 그레이
            this.drawRoundedRect(ctx, x + offset, y + offset, seatSize, seatSize, radius);

            // 테두리 추가
            ctx.shadowColor = 'transparent';
            ctx.strokeStyle = '#334155'; // 더 진한 그레이로 테두리
            ctx.lineWidth = 1.5;
            this.strokeRoundedRect(ctx, x + offset, y + offset, seatSize, seatSize, radius);
        }

        // 그림자 리셋
        ctx.shadowColor = 'transparent';
    }

    drawSeat(ctx, x, y, size, isOccupied) {
        const seatSize = size * 0.85;
        const offset = (size - seatSize) / 2;
        const radius = seatSize * 0.2;

        if (isOccupied) {
            // 선택된 좌석 - 색상이 있는 좌석
            ctx.fillStyle = ctx.fillStyle;
            this.drawRoundedRect(ctx, x + offset, y + offset, seatSize, seatSize, radius);

            // 테두리 추가
            ctx.strokeStyle = this.darkenColor(ctx.fillStyle, 0.2);
            ctx.lineWidth = 2;
            this.strokeRoundedRect(ctx, x + offset, y + offset, seatSize, seatSize, radius);
        } else {
            // 빈 좌석 - 밝은 배경에 테두리
            ctx.fillStyle = '#f8f9fa';
            this.drawRoundedRect(ctx, x + offset, y + offset, seatSize, seatSize, radius);

            ctx.strokeStyle = '#dee2e6';
            ctx.lineWidth = 1.5;
            this.strokeRoundedRect(ctx, x + offset, y + offset, seatSize, seatSize, radius);
        }
    }

    drawRoundedRect(ctx, x, y, width, height, radius) {
        ctx.beginPath();
        ctx.roundRect(x, y, width, height, radius);
        ctx.fill();
    }

    strokeRoundedRect(ctx, x, y, width, height, radius) {
        ctx.beginPath();
        ctx.roundRect(x, y, width, height, radius);
        ctx.stroke();
    }

    darkenColor(color, factor) {
        // RGB 색상을 어둡게 만드는 함수
        if (color.startsWith('#')) {
            const r = parseInt(color.substr(1, 2), 16);
            const g = parseInt(color.substr(3, 2), 16);
            const b = parseInt(color.substr(5, 2), 16);

            return `rgb(${Math.floor(r * (1 - factor))}, ${Math.floor(g * (1 - factor))}, ${Math.floor(b * (1 - factor))})`;
        }
        return color;
    }

    async checkOwnership() {
        try {
            const response = await fetch(`${HOST}/api/arts/my?page=0&size=1`, {
                credentials: 'include'
            });

            if (response.ok) {
                const data = await response.json();
                const isOwner = data.content.some(art => art.id === parseInt(this.artId));

                if (isOwner) {
                    this.artActions.style.display = 'block';
                }
            }
        } catch (error) {
            console.error('소유권 확인 실패:', error);
        }
    }

    setupEventListeners() {
        if (this.editBtn) {
            this.editBtn.addEventListener('click', () => this.editArt());
        }

        if (this.deleteBtn) {
            this.deleteBtn.addEventListener('click', () => this.deleteArt());
        }
    }

    editArt() {
        const title = prompt('새 제목을 입력하세요:', this.artData.title);
        if (title === null) return;

        const description = prompt('새 설명을 입력하세요:', this.artData.description);
        if (description === null) return;

        const isPublic = confirm('공개 작품으로 설정하시겠습니까?');

        this.updateArt({ title: title.trim(), description: description.trim(), is_public: isPublic });
    }

    async updateArt(updateData) {
        try {
            const response = await fetch(`${HOST}/api/arts/${this.artId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(updateData)
            });

            if (response.ok) {
                const updatedArt = await response.json();
                this.artData = updatedArt;
                this.renderArt();
                alert('작품이 성공적으로 수정되었습니다.');
            } else {
                throw new Error('작품 수정에 실패했습니다.');
            }
        } catch (error) {
            console.error('작품 수정 실패:', error);
            alert('작품 수정에 실패했습니다. 다시 시도해주세요.');
        }
    }

    async deleteArt() {
        if (!confirm('정말로 이 작품을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
            return;
        }

        try {
            const response = await fetch(`${HOST}/api/arts/${this.artId}`, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (response.ok) {
                alert('작품이 성공적으로 삭제되었습니다.');
                window.location.href = '/art';
            } else {
                throw new Error('작품 삭제에 실패했습니다.');
            }
        } catch (error) {
            console.error('작품 삭제 실패:', error);
            alert('작품 삭제에 실패했습니다. 다시 시도해주세요.');
        }
    }

    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
}

// 페이지 로드 시 상세 페이지 초기화
document.addEventListener('DOMContentLoaded', () => {
    new Detail();
});