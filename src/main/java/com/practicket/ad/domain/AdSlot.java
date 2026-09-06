package com.practicket.ad.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ad_slot")
public class AdSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    private String recommendedSize;

    private Boolean enabled;

    private Integer pcWidth;

    private Integer pcHeight;

    private Integer mobileWidth;

    private Integer mobileHeight;

    private String groupName;

    private String groupPath;

    private String format;

    private Integer sortOrder;

    /** 팔리지 않았을 때 채울 네트워크. NULL 이면 그 자리를 아예 그리지 않는다. */
    private String fillNetwork;

    private Long pcAdUnitId;

    private Long mobileAdUnitId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean hasPcSize() {
        return pcWidth != null && pcHeight != null;
    }

    public boolean hasMobileSize() {
        return mobileWidth != null && mobileHeight != null;
    }
}
