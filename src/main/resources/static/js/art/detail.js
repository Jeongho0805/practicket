import { authFetch, showAlert } from "../common.js";
import { GRID_SIZE, drawPixelArt, toStates } from "./pixel.js";

const VIEW_CELL = 12;
const DOWNLOAD_CELL = 16;

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
}

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
        this.artLikesElement = document.getElementById('art-likes');

        this.editBtn = document.getElementById('edit-btn');
        this.deleteBtn = document.getElementById('delete-btn');
        this.downloadBtn = document.getElementById('download-btn');

        this.commentInput = document.getElementById('comment-input');
        this.commentSubmitBtn = document.getElementById('comment-submit-btn');
        this.commentsList = document.getElementById('comments-list');
        this.scroller = document.getElementById('main-section');

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

            if (!response.ok) throw new Error('작품을 불러올 수 없습니다.');

            this.artData = await response.json();
            this.renderArt();
            this.updateUIBasedOnResponse();
        } catch (error) {
            console.error('작품 로딩 실패:', error);
            await showAlert({ title: '오류', msg: '작품을 불러오는데 실패했습니다.' });
            window.location.href = '/art';
        }
    }

    renderArt() {
        const { title, author_name, created_at, view_count, like_count } = this.artData;

        this.titleElement.textContent = title;
        this.authorElement.textContent = author_name;
        this.createdAtElement.textContent = this.formatDate(created_at);
        this.likeCountElement.textContent = this.formatCount(like_count || 0);
        this.viewCountElement.textContent = this.formatCount(view_count || 0);

        drawPixelArt(this.artCanvas, this.artStates(), VIEW_CELL, this.gridSize());
    }

    gridSize() {
        return this.artData.width ?? GRID_SIZE;
    }

    artStates() {
        return toStates(this.artData.pixel_data, this.gridSize());
    }

    updateUIBasedOnResponse() {
        if (this.artData.is_owned_by_current_user) {
            this.editBtn.hidden = false;
            this.deleteBtn.hidden = false;
        }

        this.paintLikeState(this.artData.is_liked_by_current_user);
    }

    paintLikeState(isLiked) {
        this.artLikesElement.classList.toggle('liked', !!isLiked);
        this.artLikesElement.querySelector('svg').setAttribute('fill', isLiked ? 'currentColor' : 'none');
    }

    setupEventListeners() {
        this.artLikesElement.addEventListener('click', () => this.toggleLike());
        this.editBtn.addEventListener('click', () => this.editArt());
        this.deleteBtn.addEventListener('click', () => this.deleteArt());
        this.downloadBtn.addEventListener('click', () => this.downloadImage());
        this.commentSubmitBtn.addEventListener('click', () => this.createComment());

        this.commentInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                this.createComment();
            }
        });
    }

    setupInfiniteScroll() {
        this.scroller.addEventListener('scroll', () => {
            const { scrollTop, scrollHeight, clientHeight } = this.scroller;
            if (scrollTop + clientHeight >= scrollHeight - 100) {
                this.loadComments();
            }
        });
    }

    downloadImage() {
        if (!this.artData) return;

        const canvas = document.createElement('canvas');
        drawPixelArt(canvas, this.artStates(), DOWNLOAD_CELL, this.gridSize());

        const link = document.createElement('a');
        link.download = `${this.artData.title || '포도아트'}.png`;
        link.href = canvas.toDataURL('image/png');
        link.click();
    }

    async toggleLike() {
        try {
            const response = await authFetch(`${HOST}/api/arts/${this.artId}/like`, {
                method: 'POST',
                credentials: 'include'
            });

            if (!response.ok) throw new Error('좋아요 실패');

            const result = await response.json();

            this.paintLikeState(result.is_liked);
            this.likeCountElement.textContent = this.formatCount(result.like_count);
            this.artData.is_liked_by_current_user = result.is_liked;
        } catch (error) {
            console.error('좋아요 토글 실패:', error);
            await showAlert({ title: '오류', msg: '좋아요 처리에 실패했습니다.' });
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

            if (!response.ok) throw new Error('작품 삭제에 실패했습니다.');

            await showAlert({ title: '삭제 완료', msg: '작품이 성공적으로 삭제되었습니다.' });
            window.location.href = '/art';
        } catch (error) {
            console.error('작품 삭제 실패:', error);
            await showAlert({ title: '오류', msg: '작품 삭제에 실패했습니다. 다시 시도해주세요.' });
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

    formatCount(count) {
        if (count >= 1000000) {
            return (count / 1000000).toFixed(1).replace(/\.0$/, '') + 'm';
        }
        if (count >= 1000) {
            return (count / 1000).toFixed(1).replace(/\.0$/, '') + 'k';
        }
        return count.toString();
    }

    async loadComments(isInitial = false) {
        if (this.isLoadingComments || (!isInitial && !this.hasMoreComments)) return;

        this.isLoadingComments = true;

        try {
            const response = await authFetch(`${HOST}/api/arts/${this.artId}/comments?page=${this.currentPage}&size=${this.commentsSize}`);

            if (response.ok) {
                const data = await response.json();

                this.renderComments(data.content, isInitial);
                if (isInitial) {
                    this.commentCountElement.textContent = data.total_elements ?? data.content.length;
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

            const author = escapeHtml(comment.author_name);
            const content = escapeHtml(comment.content);

            commentItem.innerHTML = `
                <div class="comment-avatar">${author.charAt(0)}</div>
                <div class="comment-body">
                    <div class="comment-header">
                        <span class="comment-author">${author}</span>
                        <span class="comment-date">${this.formatDate(comment.created_at)}</span>
                        ${comment.is_owned_by_current_user ? `
                            <div class="comment-actions">
                                <span class="comment-edit-link" data-id="${comment.id}">수정</span>
                                <span class="comment-separator">·</span>
                                <span class="comment-delete-link" data-id="${comment.id}">삭제</span>
                            </div>
                        ` : ''}
                    </div>
                    <p class="comment-content">${content}</p>
                    <textarea class="comment-edit-input" rows="2" style="display: none;">${content}</textarea>
                    <div class="comment-edit-actions" style="display: none;">
                        <button type="button" class="comment-save-btn" data-id="${comment.id}">저장</button>
                        <button type="button" class="comment-cancel-btn" data-id="${comment.id}">취소</button>
                    </div>
                </div>
            `;

            this.commentsList.appendChild(commentItem);
        });

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
            await showAlert({ title: '입력 오류', msg: '댓글 내용을 입력해주세요.' });
            return;
        }

        try {
            const response = await authFetch(`${HOST}/api/arts/${this.artId}/comments`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ content })
            });

            if (!response.ok) throw new Error('댓글 작성 실패');

            this.commentInput.value = '';
            this.commentInput.blur();
            this.reloadComments();
        } catch (error) {
            console.error('댓글 작성 실패:', error);
            await showAlert({ title: '오류', msg: '댓글 작성에 실패했습니다.' });
        }
    }

    async reloadComments() {
        this.currentPage = 0;
        this.hasMoreComments = true;
        await this.loadComments(true);
    }

    startEditComment(commentId) {
        const commentItem = this.commentsList.querySelector(`[data-comment-id="${commentId}"]`);
        commentItem.querySelector('.comment-content').style.display = 'none';
        commentItem.querySelector('.comment-edit-input').style.display = 'block';
        commentItem.querySelector('.comment-edit-actions').style.display = 'flex';
        commentItem.querySelector('.comment-edit-input').focus();
    }

    cancelEditComment(commentId) {
        const commentItem = this.commentsList.querySelector(`[data-comment-id="${commentId}"]`);
        const contentElement = commentItem.querySelector('.comment-content');
        const editInput = commentItem.querySelector('.comment-edit-input');

        contentElement.style.display = 'block';
        editInput.style.display = 'none';
        commentItem.querySelector('.comment-edit-actions').style.display = 'none';
        editInput.value = contentElement.textContent;
    }

    async saveEditComment(commentId) {
        const commentItem = this.commentsList.querySelector(`[data-comment-id="${commentId}"]`);
        const editInput = commentItem.querySelector('.comment-edit-input');
        const newContent = editInput.value.trim();

        if (!newContent) {
            await showAlert({ title: '입력 오류', msg: '댓글 내용을 입력해주세요.' });
            return;
        }

        try {
            const response = await authFetch(`${HOST}/api/arts/comments/${commentId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content: newContent })
            });

            if (!response.ok) throw new Error('댓글 수정 실패');

            editInput.blur();
            this.reloadComments();
        } catch (error) {
            console.error('댓글 수정 실패:', error);
            await showAlert({ title: '오류', msg: '댓글 수정에 실패했습니다.' });
        }
    }

    async deleteComment(commentId) {
        if (!confirm('정말 이 댓글을 삭제하시겠습니까?')) return;

        try {
            const response = await authFetch(`${HOST}/api/arts/comments/${commentId}`, {
                method: 'DELETE'
            });

            if (!response.ok) throw new Error('댓글 삭제 실패');

            this.reloadComments();
        } catch (error) {
            console.error('댓글 삭제 실패:', error);
            await showAlert({ title: '오류', msg: '댓글 삭제에 실패했습니다.' });
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new Detail();
});
