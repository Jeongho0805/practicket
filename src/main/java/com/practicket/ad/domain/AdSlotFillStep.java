package com.practicket.ad.domain;

import jakarta.persistence.*;
import lombok.*;

/** 자리의 채움 순서 중 2단계부터. 1단계는 {@link AdSlot} 의 fillNetwork·단위 칸이다 */
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_slot_fill_step")
public class AdSlotFillStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long slotId;

    private Integer stepOrder;

    private String network;

    private Long pcAdUnitId;

    private Long mobileAdUnitId;

    public AdSlotFillStep(Long slotId, int stepOrder, String network, Long pcAdUnitId, Long mobileAdUnitId) {
        this(null, slotId, stepOrder, network, pcAdUnitId, mobileAdUnitId);
    }
}
