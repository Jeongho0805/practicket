package com.practicket.ad.application;

import com.practicket.ad.component.BannerImageStorage;
import com.practicket.ad.domain.AdSlot;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.Banner;
import com.practicket.ad.domain.BannerRepository;
import com.practicket.ad.exception.AdException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 배너 CRUD 로직. 이미지 저장은 {@link BannerImageStorage}에 위임.
 * Banner 엔티티는 필드 setter가 없어(불변 스타일) 수정 시 builder로 새 상태를 만들어 save(merge)한다.
 */
@Service
@RequiredArgsConstructor
public class AdminBannerService {

    private final BannerRepository bannerRepository;
    private final AdSlotRepository adSlotRepository;
    private final BannerImageStorage bannerImageStorage;

    @Transactional(readOnly = true)
    public List<Banner> getAllBanners() {
        return bannerRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Banner getBanner(Long id) {
        return findBannerOrThrow(id);
    }

    @Transactional
    public Banner createBanner(Long slotId, MultipartFile image, String linkUrl, String advertiserName,
                                LocalDate startAt, LocalDate endAt, boolean enabled) {
        AdSlot slot = findSlotOrThrow(slotId);
        String imagePath = bannerImageStorage.store(image);

        Banner banner = Banner.builder()
                .slot(slot)
                .imagePath(imagePath)
                .linkUrl(linkUrl)
                .advertiserName(advertiserName)
                .startAt(startAt)
                .endAt(endAt)
                .enabled(enabled)
                .reportToken(UUID.randomUUID().toString())
                .build();

        return bannerRepository.save(banner);
    }

    @Transactional
    public Banner updateBanner(Long id, Long slotId, MultipartFile image, String linkUrl, String advertiserName,
                                LocalDate startAt, LocalDate endAt, boolean enabled) {
        Banner existing = findBannerOrThrow(id);
        AdSlot slot = findSlotOrThrow(slotId);

        String imagePath = existing.getImagePath();
        if (image != null && !image.isEmpty()) {
            imagePath = bannerImageStorage.store(image);
        }

        Banner updated = Banner.builder()
                .id(existing.getId())
                .slot(slot)
                .imagePath(imagePath)
                .linkUrl(linkUrl)
                .advertiserName(advertiserName)
                .startAt(startAt)
                .endAt(endAt)
                .enabled(enabled)
                .reportToken(existing.getReportToken())
                .createdAt(existing.getCreatedAt())
                .build();

        return bannerRepository.save(updated);
    }

    @Transactional
    public void deleteBanner(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new AdException("존재하지 않는 배너입니다.");
        }
        bannerRepository.deleteById(id);
    }

    @Transactional
    public void toggleBanner(Long id) {
        Banner banner = findBannerOrThrow(id);
        if (Boolean.TRUE.equals(banner.getEnabled())) {
            banner.disable();
        } else {
            banner.enable();
        }
    }

    private Banner findBannerOrThrow(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new AdException("존재하지 않는 배너입니다."));
    }

    private AdSlot findSlotOrThrow(Long slotId) {
        return adSlotRepository.findById(slotId)
                .orElseThrow(() -> new AdException("존재하지 않는 슬롯입니다."));
    }
}
