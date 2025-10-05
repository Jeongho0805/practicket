import * as util from "../common.js";

const PIXEL_THEME = {
	gridSize: 30,
	cellSize: 24,
	gap: 6,
	corner: 6,
	previewScale: 12,
	colors: {
		background: "#281d43",
		base: "#6633cc",
		hover: "#7c4dff",
		active: "#cbb5ff",
		activeHover: "#e4daff",
		border: "rgba(255,255,255,0.55)",
		shadow: "rgba(85,34,170,0.25)",
		shadowActive: "rgba(161,120,255,0.35)"
	}
};

class Gallery {
	constructor() {
		this.currentPage = 0;
		this.size = 20;
		this.hasMore = true;
		this.galleryContainer = document.getElementById("gallery-grid");

		// 검색/필터 상태
		this.searchKeyword = "";
		this.sortBy = "latest";
		this.sortDirection = "desc";
		this.onlyMine = false;

		this.init();
	}

	init() {
		this.setupControls();
		this.loadArts();
		this.setupInfiniteScroll();
	}

	setupControls() {
		const searchInput = document.getElementById("search-input");
		const sortSelect = document.getElementById("sort-select");
		const directionToggle = document.getElementById("direction-toggle");
		const onlyMineCheckbox = document.getElementById("only-mine-checkbox");

		// 검색 입력 (디바운스 적용)
		let searchTimeout;
		searchInput.addEventListener("input", (e) => {
			clearTimeout(searchTimeout);
			searchTimeout = setTimeout(() => {
				this.searchKeyword = e.target.value.trim();
				this.resetAndReload();
			}, 500);
		});

		// 정렬 기준 변경
		sortSelect.addEventListener("change", (e) => {
			this.sortBy = e.target.value;
			this.resetAndReload();
		});

		// 정렬 방향 토글
		directionToggle.addEventListener("click", () => {
			if (this.sortDirection === "desc") {
				this.sortDirection = "asc";
				directionToggle.setAttribute("data-direction", "asc");
				directionToggle.setAttribute("title", "오름차순");
				directionToggle.innerHTML = `
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<path d="M12 19V5M5 12l7-7 7 7"/>
					</svg>
				`;
			} else {
				this.sortDirection = "desc";
				directionToggle.setAttribute("data-direction", "desc");
				directionToggle.setAttribute("title", "내림차순");
				directionToggle.innerHTML = `
					<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
						<path d="M12 5v14M19 12l-7 7-7-7"/>
					</svg>
				`;
			}
			this.resetAndReload();
		});

		// 내 작품만 보기
		onlyMineCheckbox.addEventListener("change", (e) => {
			this.onlyMine = e.target.checked;
			this.resetAndReload();
		});
	}

	resetAndReload() {
		this.currentPage = 0;
		this.hasMore = true;
		this.galleryContainer.innerHTML = "";
		this.loadArts();
	}

	setupInfiniteScroll() {
		window.addEventListener("scroll", () => {
			if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 1000) {
				this.loadArts();
			}
		});
	}

	buildQueryString() {
		const params = new URLSearchParams();
		params.append("page", this.currentPage);
		params.append("size", this.size);

		if (this.searchKeyword) {
			params.append("keyword", this.searchKeyword);
		}

		params.append("sortBy", this.sortBy);
		params.append("sortDirection", this.sortDirection);

		if (this.onlyMine) {
			params.append("onlyMine", "true");
		}

		return params.toString();
	}

	async loadArts() {
		if (!this.hasMore) {
			return;
		}
		try {
			const queryString = this.buildQueryString();
			const response = await util.authFetch(`${HOST}/api/arts?${queryString}`);
			const data = await response.json();

			if (data.content && data.content.length > 0) {
				this.renderArts(data.content);
				this.currentPage++;
				this.hasMore = !data.last;
			} else {
				this.hasMore = false;
			}
		} catch (error) {
			console.error("failed to load arts", error);
			alert("작품을 불러오는데 실패했습니다.");
		}
	}

	renderArts(arts) {
		arts.forEach((art) => {
			const artCard = this.createArtCard(art);
			this.galleryContainer.appendChild(artCard);
		});
	}

	createArtCard(art) {
		const card = document.createElement("div");
		card.className = "art-card";

		const artPreview = document.createElement("div");
		artPreview.className = "art-preview";
		artPreview.onclick = () => {
			window.location.href = `/art/${art.id}`;
		};

		const canvas = document.createElement("canvas");
		canvas.className = "art-canvas";

		const pixelSize = PIXEL_THEME.previewScale;
		const width = art.width ?? PIXEL_THEME.gridSize;
		const height = art.height ?? PIXEL_THEME.gridSize;
		canvas.width = width * pixelSize;
		canvas.height = height * pixelSize;

		this.renderPixelArt(canvas, art.pixel_data, width, height, pixelSize);
		artPreview.appendChild(canvas);

		const artInfo = document.createElement("div");
		artInfo.className = "art-info";

		const isLiked = art.is_liked_by_current_user || false;
		const likeIconPath = 'M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41 0.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z';

		const likeFill = isLiked ? '#ed4956' : 'none';
		const likeStroke = isLiked ? '#ed4956' : 'currentColor';

		artInfo.innerHTML = `
			<div class="art-stats">
				<div class="art-likes ${isLiked ? 'liked' : ''}" data-art-id="${art.id}">

					<svg aria-label="좋아요" height="20" width="20" viewBox="0 0 24 24">
						<path d="${likeIconPath}" fill="${likeFill}" stroke="${likeStroke}"></path>
					</svg>
					<span class="art-stats-number like-count">${this.formatCount(art.like_count || 0)}</span>
				</div>
				<div class="art-comments" onclick="window.location.href='/art/${art.id}'">
					<svg aria-label="댓글" height="20" width="20" viewBox="0 0 24 24">
						<path d="M20.656 17.008a9.993 9.993 0 1 0-3.59 3.615L22 22Z" fill="none" stroke="currentColor" stroke-linejoin="round" stroke-width="2"></path>
					</svg>
					<span class="art-stats-number">${this.formatCount(art.comment_count || 0)}</span>
				</div>
				<div class="art-views">
					<svg aria-label="조회수" height="20" width="20" viewBox="0 0 24 24" fill="none">
						<path d="M12 5C7 5 2.73 8.11 1 12c1.73 3.89 6 7 11 7s9.27-3.11 11-7c-1.73-3.89-6-7-11-7Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
						<circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.8"/>
					</svg>
					<span class="art-stats-number">${this.formatCount(art.view_count || 0)}</span>
				</div>
			</div>
			<div class="art-caption">
				<span class="art-author">${art.author_name}</span>
				<span class="art-title">${art.title}</span>
			</div>
			<div>
				<span class="art-date">${this.formatDate(art.created_at)}</span>
			</div>
		`;

		// 좋아요 버튼 이벤트 리스너
		const likeBtn = artInfo.querySelector(".art-likes");
		likeBtn.addEventListener("click", (e) => {
			e.stopPropagation();
			this.toggleLike(art.id, likeBtn);
		});

		card.appendChild(artPreview);
		card.appendChild(artInfo);

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
			const isLiked = result.is_liked;
			const likeCount = result.like_count;

			// UI 업데이트
			const svgPath = likeBtn.querySelector("svg path");
			const countSpan = likeBtn.querySelector(".like-count");

			if (isLiked) {
				likeBtn.classList.add("liked");
				svgPath.setAttribute("fill", "#ed4956");
				svgPath.setAttribute("stroke", "#ed4956");
			} else {
				likeBtn.classList.remove("liked");
				svgPath.setAttribute("fill", "none");
				svgPath.setAttribute("stroke", "currentColor");
			}

			countSpan.textContent = this.formatCount(likeCount);
		} catch (error) {
			console.error("좋아요 토글 실패:", error);
			alert("좋아요 처리에 실패했습니다.");
		}
	}

	renderPixelArt(canvas, rawPixelData, width, height, pixelSize = 1) {
		const ctx = canvas.getContext("2d");
		ctx.imageSmoothingEnabled = false;

		// 배경 작업
		ctx.fillStyle = PIXEL_THEME.colors.background;
		ctx.fillRect(0, 0, canvas.width, canvas.height);

		// 문자열 데이터 가공 -> boolean 배열 반환
		const pixels = this.normalizePixelData(rawPixelData, width, height);
		if (!pixels.length) return;

		const gapRatio = PIXEL_THEME.gap / PIXEL_THEME.cellSize;
		const cornerRatio = PIXEL_THEME.corner / PIXEL_THEME.cellSize;
		const gap = pixelSize * gapRatio;
		const innerSize = pixelSize - gap;
		const radius = Math.min(pixelSize * cornerRatio, innerSize / 2);

		pixels.forEach((isActive, index) => {
			const col = index % width;
			const row = Math.floor(index / width);
			const x = col * pixelSize + gap / 2;
			const y = row * pixelSize + gap / 2;

			ctx.save();
			ctx.beginPath();
			this.roundedRectPath(ctx, x, y, innerSize, innerSize, radius);
			ctx.fillStyle = isActive ? PIXEL_THEME.colors.active : PIXEL_THEME.colors.base;
			ctx.shadowColor = isActive ? PIXEL_THEME.colors.shadowActive : PIXEL_THEME.colors.shadow;
			ctx.shadowBlur = isActive ? 14 : 8;
			ctx.fill();
			ctx.restore();
		});
	}

	normalizePixelData(pixelData, width, height) {
		if (!pixelData || typeof pixelData !== "string") return [];

		const total = width * height;
		const cleaned = pixelData.replace(/[^01]/g, "");
		const pixels = [...cleaned].map((digit) => digit === "1");

		return this.fillToSize(pixels, total);
	}

	// 픽셀 데이터가 canvas 사이즈에 맞지 않은 예외 케이스 처리
	fillToSize(pixels, total) {
		if (pixels.length >= total) {
			return pixels.slice(0, total);
		}
		const padding = Array(total - pixels.length).fill(false);
		return pixels.concat(padding);
	}

	roundedRectPath(ctx, x, y, width, height, radius) {
		ctx.moveTo(x + radius, y);
		ctx.arcTo(x + width, y, x + width, y + height, radius);
		ctx.arcTo(x + width, y + height, x, y + height, radius);
		ctx.arcTo(x, y + height, x, y, radius);
		ctx.arcTo(x, y, x + width, y, radius);
		ctx.closePath();
	}

	escapeHtml(text) {
		const div = document.createElement("div");
		div.textContent = text ?? "";
		return div.innerHTML;
	}

	formatDate(dateString) {
		const date = new Date(dateString);
		const now = new Date();

		const diffMs = now - date; // 시간 차 (ms)
		const diffSec = Math.floor(diffMs / 1000);
		const diffMin = Math.floor(diffSec / 60);
		const diffHour = Math.floor(diffMin / 60);

		// 오늘 하루 안에 속하는 경우
		if (
			now.toDateString() === date.toDateString() // 같은 날인지 비교
		) {
			if (diffSec < 60) {
				return `${diffSec}초 전`;
			} else if (diffMin < 60) {
				return `${diffMin}분 전`;
			} else {
				return `${diffHour}시간 전`;
			}
		}

		// 하루 이상 차이나면 날짜로 표시
		return date.toLocaleDateString("ko-KR", {
			year: "numeric",
			month: "short",
			day: "numeric",
		});;
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
