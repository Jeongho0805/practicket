import * as util from "../common.js";
import { GRID_SIZE, drawPixelArt, toStates } from "./pixel.js";

const PREVIEW_CELL = 8;

const ICON = {
	heart: (filled) => `<svg viewBox="0 0 24 24" fill="${filled ? "currentColor" : "none"}" stroke="currentColor" stroke-width="2" aria-label="좋아요"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>`,
	comment: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" aria-label="댓글"><path d="M20.656 17.008a9.993 9.993 0 1 0-3.59 3.615L22 22Z"/></svg>`,
	view: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-label="조회수"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>`
};

function escapeHtml(text) {
	const div = document.createElement("div");
	div.textContent = text ?? "";
	return div.innerHTML;
}

class Gallery {
	constructor() {
		this.currentPage = 0;
		this.size = 20;
		this.hasMore = true;
		this.isLoading = false;

		this.grid = document.getElementById("gallery-grid");
		this.emptyMessage = document.getElementById("gallery-empty");
		this.scroller = document.getElementById("main-section");

		this.keyword = "";
		this.sortBy = "LATEST";
		this.sortDirection = "DESC";
		this.filterType = "ALL";

		this.init();
	}

	init() {
		this.setupControls();
		this.loadArts();
		this.setupInfiniteScroll();
	}

	setupControls() {
		const searchForm = document.getElementById("art-search");
		const searchInput = document.getElementById("search-input");

		searchForm.addEventListener("submit", (e) => {
			e.preventDefault();
			this.keyword = searchInput.value.trim();
			this.resetAndReload();
		});

		this.filterChips = [...document.querySelectorAll("[data-filter]")];
		this.filterChips.forEach((chip) => {
			chip.addEventListener("click", () => {
				if (this.filterType === chip.dataset.filter) return;
				this.filterType = chip.dataset.filter;
				this.syncChips();
				this.resetAndReload();
			});
		});

		this.sortChips = [...document.querySelectorAll("[data-sort]")];
		this.sortChips.forEach((chip) => {
			chip.addEventListener("click", () => {
				// 이미 걸린 정렬을 다시 누르면 방향만 뒤집는다
				if (this.sortBy === chip.dataset.sort) {
					this.sortDirection = this.sortDirection === "DESC" ? "ASC" : "DESC";
				} else {
					this.sortBy = chip.dataset.sort;
					this.sortDirection = "DESC";
				}
				this.syncChips();
				this.resetAndReload();
			});
		});

		this.syncChips();
	}

	syncChips() {
		this.filterChips.forEach((chip) => {
			chip.classList.toggle("current", chip.dataset.filter === this.filterType);
		});

		this.sortChips.forEach((chip) => {
			const isCurrent = chip.dataset.sort === this.sortBy;
			chip.classList.toggle("current", isCurrent);
			chip.querySelector(".sort-dir").textContent =
				isCurrent ? (this.sortDirection === "DESC" ? "↓" : "↑") : "";
		});
	}

	resetAndReload() {
		this.currentPage = 0;
		this.hasMore = true;
		this.grid.innerHTML = "";
		this.emptyMessage.hidden = true;
		this.loadArts();
	}

	setupInfiniteScroll() {
		this.scroller.addEventListener("scroll", () => {
			if (this.scroller.scrollHeight - this.scroller.scrollTop <= this.scroller.clientHeight + 500) {
				this.loadArts();
			}
		});
	}

	buildQueryString() {
		const params = new URLSearchParams();
		params.append("page", this.currentPage);
		params.append("size", this.size);
		params.append("sortBy", this.sortBy);
		params.append("sortDirection", this.sortDirection);
		params.append("filterType", this.filterType);

		if (this.keyword) {
			params.append("keyword", this.keyword);
		}

		return params.toString();
	}

	async loadArts() {
		if (!this.hasMore || this.isLoading) return;

		this.isLoading = true;

		try {
			const response = await util.authFetch(`${HOST}/api/arts?${this.buildQueryString()}`);
			const data = await response.json();

			if (data.content && data.content.length > 0) {
				this.renderArts(data.content);
				this.currentPage++;
				this.hasMore = !data.last;
			} else {
				this.hasMore = false;
				if (this.currentPage === 0) {
					this.emptyMessage.hidden = false;
				}
			}
		} catch (error) {
			console.error("failed to load arts", error);
			await util.showAlert({ title: '오류', msg: '작품을 불러오는데 실패했습니다.' });
		} finally {
			this.isLoading = false;
		}
	}

	renderArts(arts) {
		arts.forEach((art) => this.grid.appendChild(this.createArtCard(art)));
	}

	createArtCard(art) {
		const card = document.createElement("div");
		card.className = "art-card";
		card.addEventListener("click", () => {
			window.location.href = `/art/${art.id}`;
		});

		const preview = document.createElement("div");
		preview.className = "art-preview";

		const canvas = document.createElement("canvas");
		const size = art.width ?? GRID_SIZE;
		drawPixelArt(canvas, toStates(art.pixel_data, size), PREVIEW_CELL, size);
		preview.appendChild(canvas);

		const info = document.createElement("div");
		info.className = "art-info";

		const isLiked = art.is_liked_by_current_user || false;

		info.innerHTML = `
			<div class="art-card-title">${escapeHtml(art.title)}</div>
			<div class="art-card-meta">
				<span class="art-author">${escapeHtml(art.author_name)}<i class="meta-dot">·</i>${this.formatDate(art.created_at)}</span>
				<span class="art-stats">
					<button type="button" class="art-likes ${isLiked ? 'liked' : ''}">
						${ICON.heart(isLiked)}<span class="like-count">${this.formatCount(art.like_count || 0)}</span>
					</button>
					<span>${ICON.comment}${this.formatCount(art.comment_count || 0)}</span>
					<span>${ICON.view}${this.formatCount(art.view_count || 0)}</span>
				</span>
			</div>
		`;

		const likeBtn = info.querySelector(".art-likes");
		likeBtn.addEventListener("click", (e) => {
			e.stopPropagation();
			this.toggleLike(art.id, likeBtn);
		});

		card.appendChild(preview);
		card.appendChild(info);

		return card;
	}

	async toggleLike(artId, likeBtn) {
		try {
			const response = await util.authFetch(`${HOST}/api/arts/${artId}/like`, {
				method: "POST",
				credentials: "same-origin"
			});

			if (!response.ok) throw new Error("좋아요 실패");

			const result = await response.json();

			likeBtn.classList.toggle("liked", result.is_liked);
			likeBtn.querySelector("svg").setAttribute("fill", result.is_liked ? "currentColor" : "none");
			likeBtn.querySelector(".like-count").textContent = this.formatCount(result.like_count);
		} catch (error) {
			console.error("좋아요 토글 실패:", error);
			await util.showAlert({ title: '오류', msg: '좋아요 처리에 실패했습니다.' });
		}
	}

	formatDate(dateString) {
		const date = new Date(dateString);
		const now = new Date();

		const diffSec = Math.floor((now - date) / 1000);
		const diffMin = Math.floor(diffSec / 60);
		const diffHour = Math.floor(diffMin / 60);

		if (now.toDateString() === date.toDateString()) {
			if (diffSec < 60) return `${diffSec}초 전`;
			if (diffMin < 60) return `${diffMin}분 전`;
			return `${diffHour}시간 전`;
		}

		return date.toLocaleDateString("ko-KR", { year: "numeric", month: "short", day: "numeric" });
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
}

document.addEventListener("DOMContentLoaded", () => {
	new Gallery();
});
