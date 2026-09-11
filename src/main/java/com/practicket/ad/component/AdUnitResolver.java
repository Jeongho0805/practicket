package com.practicket.ad.component;

import com.practicket.ad.domain.AdSlot;
import com.practicket.ad.domain.AdUnit;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 팔리지 않은 슬롯에 넣을 광고단위를 고른다. 렌더와 어드민이 같은 답을 내야 해서 한 곳에 둔다 —
 * 어드민이 "단위 없음" 이라고 경고한 슬롯은 렌더도 비워야 한다.
 */
@Component
public class AdUnitResolver {

    public Optional<AdUnit> resolve(AdSlot slot, boolean pc, List<AdUnit> units) {
        return resolve(slot.getFillNetwork(),
                pc ? slot.getPcAdUnitId() : slot.getMobileAdUnitId(),
                pc ? slot.getPcWidth() : slot.getMobileWidth(),
                pc ? slot.getPcHeight() : slot.getMobileHeight(),
                units);
    }

    /**
     * 사람이 고른 단위가 있으면 그것을 쓴다. 없으면 규격이 들어가는 것 중 슬롯을 가장 꽉 채우는 것을 고른다.
     * 슬롯이 그 기기 규격을 안 가졌으면 그 기기에서는 아예 안 나가므로 단위도 없다.
     */
    public Optional<AdUnit> resolve(String network, Long explicitUnitId,
                                    Integer width, Integer height, List<AdUnit> units) {
        if (network == null || width == null || height == null) {
            return Optional.empty();
        }
        if (explicitUnitId != null) {
            Optional<AdUnit> explicit = units.stream()
                    .filter(unit -> unit.getId().equals(explicitUnitId) && unit.getNetwork().equals(network))
                    .findFirst();
            if (explicit.isPresent()) {
                return explicit;
            }
        }
        return units.stream()
                .filter(unit -> unit.getNetwork().equals(network))
                .filter(unit -> unit.fitsIn(width, height))
                .max(Comparator.comparingInt(unit -> unit.isResponsive() ? 0 : unit.getWidth() * unit.getHeight()));
    }
}
