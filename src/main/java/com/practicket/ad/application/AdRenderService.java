package com.practicket.ad.application;

import com.practicket.ad.domain.Banner;
import com.practicket.ad.domain.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 슬롯 code로 지금 노출할 배너를 고른다.
 * 활성 배너가 여러 개면 분·시 기반으로 단순 로테이션한다.
 */
@Service
@RequiredArgsConstructor
public class AdRenderService {

    private final BannerRepository bannerRepository;

    public Optional<Banner> pick(String slotCode) {
        List<Banner> candidates = bannerRepository.findActiveBySlotCode(slotCode, LocalDate.now());
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (candidates.size() == 1) {
            return Optional.of(candidates.get(0));
        }
        int index = LocalDateTime.now().getMinute() % candidates.size();
        return Optional.of(candidates.get(index));
    }
}
