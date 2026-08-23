package com.practicket.community.domain.repository;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostQueryCondition {

    private final String keyword;

    /** 정규화된 값이 들어온다 — 주소창의 {@code ?tag=IVE} 와 저장된 {@code ive} 를 맞추려면 모양이 같아야 한다 */
    private final String tag;

    /** "내 글" 화면에서만 채운다 */
    private final Long clientId;

    @Builder.Default
    private final PostSortType sort = PostSortType.LATEST;

    /** 수정일 정렬은 없다 — 자기 글을 계속 고쳐 맨 위로 끌어올리는 걸 막을 방법이 없다 */
    public enum PostSortType {
        LATEST, LIKE, VIEW, COMMENT;

        /** 알 수 없는 값은 에러 대신 기본값으로 떨어진다 — 주소창은 사용자가 고칠 수 있다 */
        public static PostSortType from(String value) {
            if (value == null) {
                return LATEST;
            }
            try {
                return PostSortType.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return LATEST;
            }
        }
    }
}
