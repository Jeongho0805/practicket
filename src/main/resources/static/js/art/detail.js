import {authFetch} from "../common.js";

class Detail {
    constructor() {
        this.artId = ART_ID;
        this.artData = null;

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
        this.artLikesElement = document.getElementById('art-likes');

        this.commentInput = document.getElementById('comment-input');
        this.commentSubmitBtn = document.getElementById('comment-submit-btn');
        this.commentsList = document.getElementById('comments-list');

        // 무한스크롤 관련 변수
        this.currentPage = 0;
        this.isLoadingComments = false;
        this.hasMoreComments = true;
        this.commentsSize = 20;

        this.init();
    }

    async init() {
        await this.loadArt();
        await this.loadComments(true);
        this.setupEventListeners();
        this.setupInfiniteScroll();
    }

    async loadArt() {
        try {
            const response = await authFetch(`${HOST}/api/arts/${this.artId}`, {
                credentials: 'include'
            });

            if (response.ok) {
                this.artData = await response.json();
                this.renderArt();
                this.updateUIBasedOnResponse();
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
        const gap = 2;
        const corner = 1.5;
        const innerSize = cellSize - gap;
        const radius = Math.min(corner, innerSize / 2);

        // 배경
        ctx.fillStyle = '#251b3c';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        if (!pixelData || typeof pixelData !== 'string') {
            return;
        }

        // 성능 최적화: roundRect 사용 (그림자 제거)
        // 비활성 픽셀
        ctx.fillStyle = '#6633cc';
        ctx.beginPath();
        for (let i = 0; i < pixelData.length; i++) {
            if (pixelData[i] === '1') continue; // 활성은 스킵

            const col = i % gridWidth;
            const row = Math.floor(i / gridWidth);
            const x = col * cellSize + gap / 2;
            const y = row * cellSize + gap / 2;
            ctx.roundRect(x, y, innerSize, innerSize, radius);
        }
        ctx.fill();

        // 활성 픽셀
        ctx.fillStyle = '#cbb5ff';
        ctx.beginPath();
        for (let i = 0; i < pixelData.length; i++) {
            if (pixelData[i] !== '1') continue; // 비활성은 스킵

            const col = i % gridWidth;
            const row = Math.floor(i / gridWidth);
            const x = col * cellSize + gap / 2;
            const y = row * cellSize + gap / 2;
            ctx.roundRect(x, y, innerSize, innerSize, radius);
        }
        ctx.fill();
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

    updateUIBasedOnResponse() {
        // 소유 여부에 따라 수정/삭제 링크 표시
        const artActions = document.getElementById('art-actions');
        if (this.artData.is_owned_by_current_user && artActions) {
            artActions.style.display = 'flex';
        }

        // 좋아요 여부에 따라 UI 업데이트
        if (this.artData.is_liked_by_current_user) {
            const svgPath = this.artLikesElement.querySelector('svg path');
            svgPath.setAttribute('fill', '#ed4956');
            svgPath.setAttribute('stroke', '#ed4956');
        }
    }

    setupEventListeners() {
        // 좋아요 버튼
        if (this.artLikesElement) {
            this.artLikesElement.addEventListener('click', () => this.toggleLike());
        }

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

    setupInfiniteScroll() {
        const artDetailSection = document.getElementById('art-detail-section');
        if (!artDetailSection) return;

        artDetailSection.addEventListener('scroll', () => {
            const { scrollTop, scrollHeight, clientHeight } = artDetailSection;

            // 스크롤이 하단 100px 이내로 왔을 때 다음 페이지 로드
            if (scrollTop + clientHeight >= scrollHeight - 100) {
                this.loadComments();
            }
        });
    }

    async toggleLike() {
        try {
            const response = await authFetch(`${HOST}/api/arts/${this.artId}/like`, {
                method: 'POST',
                credentials: 'include'
            });

            if (!response.ok) throw new Error('좋아요 실패');

            const result = await response.json();
            const isLiked = result.is_liked;
            const likeCount = result.like_count;

            // UI 업데이트
            const svgPath = this.artLikesElement.querySelector('svg path');

            if (isLiked) {
                svgPath.setAttribute('fill', '#ed4956');
                svgPath.setAttribute('stroke', '#ed4956');
            } else {
                svgPath.setAttribute('fill', 'none');
                svgPath.setAttribute('stroke', 'currentColor');
            }

            this.likeCountElement.textContent = this.formatCount(likeCount);
            this.artData.is_liked_by_current_user = isLiked;
        } catch (error) {
            console.error('좋아요 토글 실패:', error);
            alert('좋아요 처리에 실패했습니다.');
        }
    }

    editArt() {
        window.location.href = `/art/edit/${this.artId}`;
    }

    async deleteArt() {
        if (!confirm('정말로 이 작품을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
            return;
        }

        try {
            const response = await authFetch(`${HOST}/api/arts/${this.artId}`, {
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
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');

        return `${year}.${month}.${day} ${hours}:${minutes}`;
    }

    async loadComments(isInitial = false) {
        if (this.isLoadingComments || (!isInitial && !this.hasMoreComments)) return;

        this.isLoadingComments = true;

        try {
            const response = await authFetch(`${HOST}/api/arts/${this.artId}/comments?page=${this.currentPage}&size=${this.commentsSize}`);

            if (response.ok) {
                const data = await response.json();

                if (isInitial) {
                    this.renderComments(data.content, true);
                    this.commentCountElement.textContent = data.total_elements || data.content.length;
                } else {
                    this.renderComments(data.content, false);
                }

                this.hasMoreComments = !data.last;
                this.currentPage++;
            }
        } catch (error) {
            console.error('댓글 로딩 실패:', error);
        } finally {
            this.isLoadingComments = false;
        }
    }

    renderComments(comments, isInitial = true) {
        if (isInitial) {
            this.commentsList.innerHTML = '';
        }

        if (isInitial && comments.length === 0) {
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
                    ${comment.is_owned_by_current_user ? `
                        <div class="comment-actions">
                            <span class="comment-edit-link" data-id="${comment.id}">수정</span>
                            <span class="comment-separator">·</span>
                            <span class="comment-delete-link" data-id="${comment.id}">삭제</span>
                        </div>
                    ` : ''}
                </div>
                <div class="comment-body">
                    <div class="comment-content-wrapper">
                        <p class="comment-content">${comment.content}</p>
                        <span class="comment-date">${this.formatDate(comment.created_at)}</span>
                        <textarea class="comment-edit-input" style="display: none;">${comment.content}</textarea>
                        <div class="comment-edit-actions" style="display: none;">
                            <button class="comment-save-btn" data-id="${comment.id}">저장</button>
                            <button class="comment-cancel-btn" data-id="${comment.id}">취소</button>
                        </div>
                    </div>
                </div>
            `;

            this.commentsList.appendChild(commentItem);
        });

        // 댓글 수정/삭제 이벤트 리스너
        this.commentsList.querySelectorAll('.comment-edit-link').forEach(link => {
            link.addEventListener('click', (e) => this.startEditComment(e.target.dataset.id));
        });

        this.commentsList.querySelectorAll('.comment-delete-link').forEach(link => {
            link.addEventListener('click', (e) => this.deleteComment(e.target.dataset.id));
        });

        this.commentsList.querySelectorAll('.comment-save-btn').forEach(btn => {
            btn.addEventListener('click', (e) => this.saveEditComment(e.target.dataset.id));
        });

        this.commentsList.querySelectorAll('.comment-cancel-btn').forEach(btn => {
            btn.addEventListener('click', (e) => this.cancelEditComment(e.target.dataset.id));
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
                this.commentInput.blur(); // 키보드 내리기
                this.currentPage = 0;
                this.hasMoreComments = true;
                await this.loadComments(true);
            } else {
                throw new Error('댓글 작성 실패');
            }
        } catch (error) {
            console.error('댓글 작성 실패:', error);
            alert('댓글 작성에 실패했습니다.');
        }
    }

    startEditComment(commentId) {
        const commentItem = this.commentsList.querySelector(`[data-comment-id="${commentId}"]`);
        const contentElement = commentItem.querySelector('.comment-content');
        const editInput = commentItem.querySelector('.comment-edit-input');
        const editActions = commentItem.querySelector('.comment-edit-actions');

        contentElement.style.display = 'none';
        editInput.style.display = 'block';
        editActions.style.display = 'flex';
        editInput.focus();
    }

    cancelEditComment(commentId) {
        const commentItem = this.commentsList.querySelector(`[data-comment-id="${commentId}"]`);
        const contentElement = commentItem.querySelector('.comment-content');
        const editInput = commentItem.querySelector('.comment-edit-input');
        const editActions = commentItem.querySelector('.comment-edit-actions');

        contentElement.style.display = 'block';
        editInput.style.display = 'none';
        editActions.style.display = 'none';
        editInput.value = contentElement.textContent;
    }

    async saveEditComment(commentId) {
        const commentItem = this.commentsList.querySelector(`[data-comment-id="${commentId}"]`);
        const editInput = commentItem.querySelector('.comment-edit-input');
        const newContent = editInput.value.trim();

        if (!newContent) {
            alert('댓글 내용을 입력해주세요.');
            return;
        }

        try {
            const response = await authFetch(`${HOST}/api/arts/comments/${commentId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content: newContent })
            });

            if (response.ok) {
                editInput.blur();
                this.currentPage = 0;
                this.hasMoreComments = true;
                await this.loadComments(true);
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
                this.currentPage = 0;
                this.hasMoreComments = true;
                await this.loadComments(true);
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