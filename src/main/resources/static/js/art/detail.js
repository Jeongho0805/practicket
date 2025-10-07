import {authFetch} from "../common.js";

class Detail {
    constructor() {
        this.artId = ART_ID;
        this.artData = null;
        this.currentClientId = null;

        this.titleElement = document.getElementById('art-title');
        this.authorElement = document.getElementById('art-author');
        this.createdAtElement = document.getElementById('created-at');
        this.likeCountElement = document.getElementById('like-count');
        this.commentCountElement = document.getElementById('comment-count');
        this.viewCountElement = document.getElementById('view-count');
        this.artCanvas = document.getElementById('art-canvas');
        this.artActions = document.getElementById('art-actions');
        this.editBtn = document.getElementById('edit-btn');
        this.deleteBtn = document.getElementById('delete-btn');

        this.commentInput = document.getElementById('comment-input');
        this.commentSubmitBtn = document.getElementById('comment-submit-btn');
        this.commentsList = document.getElementById('comments-list');

        this.init();
    }

    async init() {
        await this.loadArt();
        await this.loadComments();
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
        const { title, author_name, created_at, view_count, like_count, pixel_data, width, height } = this.artData;

        // 기본 정보 표시
        this.titleElement.textContent = title;
        this.authorElement.textContent = author_name;
        this.createdAtElement.textContent = this.formatDate(created_at);
        this.likeCountElement.textContent = this.formatCount(like_count || 0);
        this.commentCountElement.textContent = '0'; // 댓글 기능 미구현
        this.viewCountElement.textContent = this.formatCount(view_count || 0);

        // 캔버스 설정 및 렌더링
        this.artCanvas.width = 360;
        this.artCanvas.height = 360;

        this.renderPixelArt(this.artCanvas, pixel_data, width, height);
    }

    renderPixelArt(canvas, pixelData, gridWidth, gridHeight) {
        const ctx = canvas.getContext('2d');
        ctx.imageSmoothingEnabled = false;

        const cellSize = 12;
        const gap = 3;
        const corner = 3;
        const innerSize = cellSize - gap;

        // 배경
        ctx.fillStyle = '#12082a';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        if (!pixelData || typeof pixelData !== 'string') {
            return;
        }

        for (let i = 0; i < pixelData.length; i++) {
            const isActive = pixelData[i] === '1';
            const col = i % gridWidth;
            const row = Math.floor(i / gridWidth);

            const x = col * cellSize;
            const y = row * cellSize;

            const drawX = x + gap / 2;
            const drawY = y + gap / 2;
            const radius = Math.min(corner, innerSize / 2);

            ctx.save();
            ctx.beginPath();
            this.roundedRectPath(ctx, drawX, drawY, innerSize, innerSize, radius);
            ctx.fillStyle = isActive ? '#cbb5ff' : '#6633cc';
            ctx.shadowColor = isActive ? 'rgba(161,120,255,0.35)' : 'rgba(85,34,170,0.25)';
            ctx.shadowBlur = isActive ? 14 : 8;
            ctx.fill();
            ctx.restore();
        }
    }

    roundedRectPath(ctx, x, y, width, height, radius) {
        ctx.moveTo(x + radius, y);
        ctx.arcTo(x + width, y, x + width, y + height, radius);
        ctx.arcTo(x + width, y + height, x, y + height, radius);
        ctx.arcTo(x, y + height, x, y, radius);
        ctx.arcTo(x, y, x + width, y, radius);
        ctx.closePath();
    }

    formatCount(count) {
        if (count >= 1000000) {
            return (count / 1000000).toFixed(1).replace(/\.0$/, '') + 'm';
        }
        if (count >= 1000) {
            return (count / 1000).toFixed(1).replace(/\.0$/, '') + 'k';
        }
        return count.toString();
    }

    async checkOwnership() {
        try {
            const response = await fetch(`${HOST}/api/client/info`, {
                credentials: 'include'
            });

            if (response.ok) {
                const clientInfo = await response.json();
                this.currentClientId = clientInfo.id;

                // 작성자 확인
                if (this.artData.author_id === clientInfo.id) {
                    this.artActions.style.display = 'flex';
                }
            }
        } catch (error) {
            console.error('권한 확인 실패:', error);
        }
    }

    setupEventListeners() {
        if (this.editBtn) {
            this.editBtn.addEventListener('click', () => this.editArt());
        }

        if (this.deleteBtn) {
            this.deleteBtn.addEventListener('click', () => this.deleteArt());
        }

        if (this.commentSubmitBtn) {
            this.commentSubmitBtn.addEventListener('click', () => this.createComment());
        }

        if (this.commentInput) {
            this.commentInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.createComment();
                }
            });
        }
    }

    editArt() {
        const title = prompt('새 제목을 입력하세요:', this.artData.title);
        if (title === null || title.trim() === '') return;

        this.updateArt({ title: title.trim() });
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

    async loadComments() {
        try {
            const response = await authFetch(`${HOST}/api/arts/${this.artId}/comments?page=0&size=20`);

            if (response.ok) {
                const data = await response.json();
                this.renderComments(data.content);
                this.commentCountElement.textContent = data.total_elements || data.content.length;
            }
        } catch (error) {
            console.error('댓글 로딩 실패:', error);
        }
    }

    renderComments(comments) {
        this.commentsList.innerHTML = '';

        if (comments.length === 0) {
            const emptyMessage = document.createElement('div');
            emptyMessage.className = 'comments-empty';
            emptyMessage.textContent = '아직 댓글이 없습니다. 첫 댓글을 작성해보세요!';
            this.commentsList.appendChild(emptyMessage);
            return;
        }

        comments.forEach(comment => {
            const commentItem = document.createElement('div');
            commentItem.className = 'comment-item';
            commentItem.dataset.commentId = comment.id;

            commentItem.innerHTML = `
                <div class="comment-header">
                    <span class="comment-author">${comment.author_name}</span>
                    <span class="comment-date">${this.formatDate(comment.created_at)}</span>
                </div>
                <p class="comment-content">${comment.content}</p>
                ${this.currentClientId === comment.author_id ? `
                    <div class="comment-actions">
                        <button class="comment-edit-btn" data-id="${comment.id}">수정</button>
                        <button class="comment-delete-btn" data-id="${comment.id}">삭제</button>
                    </div>
                ` : ''}
            `;

            this.commentsList.appendChild(commentItem);
        });

        // 댓글 수정/삭제 이벤트 리스너
        this.commentsList.querySelectorAll('.comment-edit-btn').forEach(btn => {
            btn.addEventListener('click', (e) => this.editComment(e.target.dataset.id));
        });

        this.commentsList.querySelectorAll('.comment-delete-btn').forEach(btn => {
            btn.addEventListener('click', (e) => this.deleteComment(e.target.dataset.id));
        });
    }

    async createComment() {
        const content = this.commentInput.value.trim();
        if (!content) {
            alert('댓글 내용을 입력해주세요.');
            return;
        }

        try {
            const response = await authFetch(`${HOST}/api/arts/${this.artId}/comments`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ content })
            });

            if (response.ok) {
                alert('댓글이 작성되었습니다.');
                this.commentInput.value = '';
                await this.loadComments();
            } else {
                throw new Error('댓글 작성 실패');
            }
        } catch (error) {
            console.error('댓글 작성 실패:', error);
            alert('댓글 작성에 실패했습니다.');
        }
    }

    async editComment(commentId) {
        const commentItem = this.commentsList.querySelector(`[data-comment-id="${commentId}"]`);
        const currentContent = commentItem.querySelector('.comment-content').textContent;

        const newContent = prompt('댓글을 수정하세요:', currentContent);
        if (!newContent || newContent.trim() === '') return;

        try {
            const response = await authFetch(`${HOST}/api/arts/comments/${commentId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content: newContent.trim() })
            });

            if (response.ok) {
                await this.loadComments();
            } else {
                throw new Error('댓글 수정 실패');
            }
        } catch (error) {
            console.error('댓글 수정 실패:', error);
            alert('댓글 수정에 실패했습니다.');
        }
    }

    async deleteComment(commentId) {
        if (!confirm('정말 이 댓글을 삭제하시겠습니까?')) return;

        try {
            const response = await authFetch(`${HOST}/api/arts/comments/${commentId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                await this.loadComments();
            } else {
                throw new Error('댓글 삭제 실패');
            }
        } catch (error) {
            console.error('댓글 삭제 실패:', error);
            alert('댓글 삭제에 실패했습니다.');
        }
    }
}

// 페이지 로드 시 상세 페이지 초기화
document.addEventListener('DOMContentLoaded', () => {
    new Detail();
});