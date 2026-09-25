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
@Table(name = "ad_unit")
public class AdUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String network;

    @Column(nullable = false)
    private String unitId;

    private Integer width;

    private Integer height;

    private String name;

    /** 네트워크 전용 값(JSON). 모비센스 frameCode·settings 처럼 단위마다 달라 코드에 박히는 것 */
    private String extra;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void update(String network, String unitId, Integer width, Integer height, String name, String extra) {
        this.network = network;
        this.unitId = unitId;
        this.width = width;
        this.height = height;
        this.name = name;
        this.extra = extra;
    }

    /** 규격을 안 가리는 반응형 단위. 애드센스·쿠팡이 여기 해당한다. */
    public boolean isResponsive() {
        return width == null || height == null;
    }

    public boolean fitsIn(Integer slotWidth, Integer slotHeight) {
        if (slotWidth == null || slotHeight == null) {
            return false;
        }
        return isResponsive() || (width <= slotWidth && height <= slotHeight);
    }
}
