package com.practicket.ad.application;

import com.practicket.ad.component.BannerImageStorage;
import com.practicket.ad.domain.AdCampaign;
import com.practicket.ad.domain.AdCampaignRepository;
import com.practicket.ad.domain.AdSlot;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.Advertiser;
import com.practicket.ad.domain.AdvertiserRepository;
import com.practicket.ad.domain.Banner;
import com.practicket.ad.domain.BannerRepository;
import com.practicket.ad.domain.BannerStatus;
import com.practicket.ad.domain.CampaignStatus;
import com.practicket.ad.exception.AdException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 캠페인(계약) 한 건과 그 아래 배너들을 함께 다룬다.
 *
 * 배너를 캠페인과 떼어 놓지 않는 이유는 저장 단위가 계약이기 때문이다. 슬롯을 셋 판 계약이면
 * 배너 셋이 한 화면에서 같이 만들어지고, 계약을 지우면 배너도 같은 트랜잭션에서 사라져야 한다 —
 * 외래키를 안 쓰므로 그 책임이 전부 여기 있다.
 */
@Service
@RequiredArgsConstructor
public class AdminCampaignService {

    /** 리포트 주소는 로그인 없이 열린다. 남이 맞힐 수 없도록 128비트 난수를 쓴다. */
    private static final int REPORT_TOKEN_BYTES = 16;
    /** 계약 종료가 이 날짜 안으로 들어오면 화면이 남은 일수를 강조한다 */
    private static final int ENDING_SOON_DAYS = 7;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdCampaignRepository adCampaignRepository;
    private final AdvertiserRepository advertiserRepository;
    private final AdSlotRepository adSlotRepository;
    private final BannerRepository bannerRepository;
    private final BannerImageStorage bannerImageStorage;
    private final AdminAdStatService adminAdStatService;

    @Transactional(readOnly = true)
    public List<CampaignRow> getCampaignRows() {
        LocalDate today = LocalDate.now();
        Map<Long, Advertiser> advertisers = advertisersById();
        Map<Long, AdSlot> slots = slotsById();
        Map<Long, AdminAdStatService.Totals> totals = adminAdStatService.totalsByBanner();
        Map<Long, List<Banner>> bannersByCampaign = bannerRepository.findAllWithSlotOrderByCreatedAtDesc().stream()
                .filter(banner -> banner.getCampaignId() != null)
                .collect(Collectors.groupingBy(Banner::getCampaignId));

        return adCampaignRepository.findAllByOrderByStartAtDesc().stream()
                .map(campaign -> toRow(campaign, advertisers, slots, totals,
                        bannersByCampaign.getOrDefault(campaign.getId(), List.of()), today))
                .sorted(Comparator.comparingInt((CampaignRow row) -> row.getStatus().getOrder())
                        .thenComparing(CampaignRow::getStartAt, Comparator.reverseOrder()))
                .toList();
    }

    /** 슬롯마다 지금 걸려 있는 계약 수. 슬롯 화면과 대시보드가 같이 쓴다. */
    @Transactional(readOnly = true)
    public Map<Long, Long> countLiveBannersBySlot() {
        LocalDate today = LocalDate.now();
        Map<Long, AdCampaign> campaigns = campaignsById();
        return bannerRepository.findAllWithSlotOrderByCreatedAtDesc().stream()
                .filter(banner -> BannerStatus.of(banner, campaigns.get(banner.getCampaignId()), today)
                        == BannerStatus.LIVE)
                .collect(Collectors.groupingBy(banner -> banner.getSlot().getId(), Collectors.counting()));
    }

    @Transactional(readOnly = true)
    public Optional<CampaignDetail> findDetail(Long id) {
        return adCampaignRepository.findById(id).map(this::toDetail);
    }

    /** 새 캠페인 폼. 슬롯 하나짜리 거래도 배너 한 줄로 시작한다 */
    @Transactional(readOnly = true)
    public CampaignForm blankForm() {
        CampaignForm form = new CampaignForm();
        form.setStartAt(LocalDate.now());
        form.setEndAt(LocalDate.now().plusDays(30));
        form.setAmount(0L);
        form.setAdvertiserId(advertiserRepository.findAllByOrderByNameAsc().stream()
                .findFirst().map(Advertiser::getId).orElse(null));
        form.getBanners().add(blankBannerForm());
        return form;
    }

    @Transactional(readOnly = true)
    public Optional<CampaignForm> findForm(Long id) {
        return adCampaignRepository.findById(id).map(campaign -> {
            CampaignForm form = new CampaignForm();
            form.setId(campaign.getId());
            form.setAdvertiserId(campaign.getAdvertiserId());
            form.setName(campaign.getName());
            form.setStartAt(campaign.getStartAt());
            form.setEndAt(campaign.getEndAt());
            form.setAmount(campaign.getAmount());
            form.setLinkUrl(campaign.getLinkUrl());
            form.setMemo(campaign.getMemo());
            form.setBanners(bannerRepository.findByCampaignIdWithSlot(campaign.getId()).stream()
                    .sorted(Comparator.comparing(Banner::getId))
                    .map(this::toBannerForm)
                    .collect(Collectors.toCollection(ArrayList::new)));
            if (form.getBanners().isEmpty()) {
                form.getBanners().add(blankBannerForm());
            }
            return form;
        });
    }

    @Transactional
    public Long save(CampaignForm form) {
        validate(form);

        AdCampaign campaign = form.getId() == null
                ? adCampaignRepository.save(newCampaign(form))
                : updateCampaign(form);

        saveBanners(campaign, form.getBanners());
        return campaign.getId();
    }

    /** 계약을 지우면 배너도 같은 트랜잭션에서 지운다. 통계는 남긴다 — 지난 성과까지 사라질 이유가 없다. */
    @Transactional
    public void delete(Long id) {
        if (!adCampaignRepository.existsById(id)) {
            throw new AdException("존재하지 않는 캠페인입니다.");
        }
        bannerRepository.deleteAll(bannerRepository.findByCampaignId(id));
        adCampaignRepository.deleteById(id);
    }

    @Transactional
    public void toggleBanner(Long bannerId) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new AdException("존재하지 않는 배너입니다."));
        if (Boolean.TRUE.equals(banner.getEnabled())) {
            banner.disable();
        } else {
            banner.enable();
        }
    }

    // ── 저장 ──────────────────────────────────────────────

    private void validate(CampaignForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new AdException("캠페인 이름을 입력해주세요.");
        }
        if (form.getAdvertiserId() == null || !advertiserRepository.existsById(form.getAdvertiserId())) {
            throw new AdException("광고주를 선택해주세요.");
        }
        if (form.getStartAt() == null || form.getEndAt() == null) {
            throw new AdException("계약 기간을 입력해주세요.");
        }
        if (form.getEndAt().isBefore(form.getStartAt())) {
            throw new AdException("계약 종료일이 시작일보다 앞설 수 없습니다.");
        }
        for (BannerForm banner : form.getBanners()) {
            if (banner.isDeleted()) {
                continue;
            }
            if (banner.getSlotId() == null) {
                throw new AdException("배너마다 슬롯을 골라주세요.");
            }
            LocalDate bannerStart = banner.getStartAt() == null ? form.getStartAt() : banner.getStartAt();
            LocalDate bannerEnd = banner.getEndAt() == null ? form.getEndAt() : banner.getEndAt();
            if (bannerEnd.isBefore(bannerStart)) {
                throw new AdException("배너 게재 기간이 뒤집힙니다. 비운 쪽은 계약 기간을 따릅니다.");
            }
        }
    }

    private AdCampaign newCampaign(CampaignForm form) {
        return AdCampaign.builder()
                .advertiserId(form.getAdvertiserId())
                .name(form.getName().trim())
                .startAt(form.getStartAt())
                .endAt(form.getEndAt())
                .amount(form.getAmount() == null ? 0L : form.getAmount())
                .linkUrl(blankToNull(form.getLinkUrl()))
                .reportToken(issueReportToken())
                .memo(blankToNull(form.getMemo()))
                .build();
    }

    private AdCampaign updateCampaign(CampaignForm form) {
        AdCampaign campaign = adCampaignRepository.findById(form.getId())
                .orElseThrow(() -> new AdException("존재하지 않는 캠페인입니다."));
        campaign.update(form.getAdvertiserId(), form.getName().trim(), form.getStartAt(), form.getEndAt(),
                form.getAmount() == null ? 0L : form.getAmount(),
                blankToNull(form.getLinkUrl()), blankToNull(form.getMemo()));
        return campaign;
    }

    /**
     * 폼에서 사라진 배너는 지운다. 화면이 계약 전체를 통째로 보내므로, 목록에 없다는 것이 곧 삭제다.
     */
    private void saveBanners(AdCampaign campaign, List<BannerForm> forms) {
        Map<Long, Banner> existing = bannerRepository.findByCampaignId(campaign.getId()).stream()
                .collect(Collectors.toMap(Banner::getId, Function.identity()));

        for (BannerForm form : forms) {
            if (form.isDeleted()) {
                continue;
            }
            AdSlot slot = adSlotRepository.findById(form.getSlotId())
                    .orElseThrow(() -> new AdException("존재하지 않는 슬롯입니다."));

            Banner banner = form.getId() == null ? null : existing.remove(form.getId());
            String pcImagePath = resolveImage(form.getPcImage(), form.getPcImagePath(), form.isClearPcImage());
            String mobileImagePath =
                    resolveImage(form.getMobileImage(), form.getMobileImagePath(), form.isClearMobileImage());

            if (banner == null) {
                bannerRepository.save(Banner.builder()
                        .campaignId(campaign.getId())
                        .slot(slot)
                        .pcImagePath(pcImagePath)
                        .mobileImagePath(mobileImagePath)
                        .linkUrl(blankToNull(form.getLinkUrl()))
                        .startAt(form.getStartAt())
                        .endAt(form.getEndAt())
                        .enabled(form.isEnabled())
                        .build());
                continue;
            }

            bannerRepository.save(Banner.builder()
                    .id(banner.getId())
                    .campaignId(campaign.getId())
                    .slot(slot)
                    .imagePath(banner.getImagePath())
                    .pcImagePath(pcImagePath)
                    .mobileImagePath(mobileImagePath)
                    .linkUrl(blankToNull(form.getLinkUrl()))
                    .advertiserName(banner.getAdvertiserName())
                    .startAt(form.getStartAt())
                    .endAt(form.getEndAt())
                    .enabled(form.isEnabled())
                    .reportToken(banner.getReportToken())
                    .createdAt(banner.getCreatedAt())
                    .build());
        }

        bannerRepository.deleteAll(existing.values());
    }

    /** 새 파일이 오면 갈아 끼우고, 떼기를 눌렀으면 비우고, 아니면 쓰던 것을 그대로 둔다. */
    private String resolveImage(MultipartFile file, String currentPath, boolean clear) {
        if (file != null && !file.isEmpty()) {
            return bannerImageStorage.store(file);
        }
        return clear ? null : blankToNull(currentPath);
    }

    private String issueReportToken() {
        byte[] bytes = new byte[REPORT_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }

    // ── 조회 모델 ─────────────────────────────────────────

    private CampaignRow toRow(AdCampaign campaign, Map<Long, Advertiser> advertisers, Map<Long, AdSlot> slots,
                              Map<Long, AdminAdStatService.Totals> totals, List<Banner> banners, LocalDate today) {
        AdminAdStatService.Totals sum = banners.stream()
                .map(banner -> totals.getOrDefault(banner.getId(), AdminAdStatService.Totals.empty()))
                .reduce(AdminAdStatService.Totals.empty(), AdminAdStatService.Totals::plus);

        return new CampaignRow(
                campaign.getId(),
                campaign.getName(),
                advertiserNameOf(campaign, advertisers),
                campaign.getStartAt(),
                campaign.getEndAt(),
                campaign.getAmount(),
                CampaignStatus.of(campaign, today),
                banners.size(),
                banners.stream()
                        .map(banner -> slotName(slots, banner))
                        .collect(Collectors.joining(" · ")),
                banners.stream().filter(this::hasOwnPeriod).count(),
                sum.getImpressions(), sum.getClicks(), sum.getCtr(),
                ChronoUnit.DAYS.between(today, campaign.getEndAt()));
    }

    private CampaignDetail toDetail(AdCampaign campaign) {
        LocalDate today = LocalDate.now();
        Map<Long, AdminAdStatService.Totals> totals = adminAdStatService.totalsByBanner();
        Map<Long, AdSlot> slots = slotsById();

        List<Banner> banners = bannerRepository.findByCampaignIdWithSlot(campaign.getId()).stream()
                .sorted(Comparator.comparing(Banner::getId))
                .toList();

        Map<Long, List<String>> overlaps = findOverlaps(campaign, banners);

        List<BannerRow> rows = banners.stream()
                .map(banner -> toBannerRow(campaign, banner, slots, totals, overlaps, today))
                .toList();

        AdminAdStatService.Totals sum = rows.stream()
                .map(row -> new AdminAdStatService.Totals(row.getImpressions(), row.getClicks()))
                .reduce(AdminAdStatService.Totals.empty(), AdminAdStatService.Totals::plus);

        return new CampaignDetail(
                campaign,
                advertiserRepository.findById(campaign.getAdvertiserId()).orElse(null),
                CampaignStatus.of(campaign, today),
                rows,
                sum.getImpressions(), sum.getClicks(), sum.getCtr(),
                ChronoUnit.DAYS.between(today, campaign.getEndAt()),
                todayPercent(campaign, today));
    }

    private BannerRow toBannerRow(AdCampaign campaign, Banner banner, Map<Long, AdSlot> slots,
                                  Map<Long, AdminAdStatService.Totals> totals,
                                  Map<Long, List<String>> overlaps, LocalDate today) {
        AdSlot slot = slots.get(banner.getSlot().getId());
        LocalDate start = BannerStatus.effectiveStart(banner, campaign);
        LocalDate end = BannerStatus.effectiveEnd(banner, campaign);
        AdminAdStatService.Totals sum = totals.getOrDefault(banner.getId(), AdminAdStatService.Totals.empty());

        long days = ChronoUnit.DAYS.between(start, end) + 1;
        long total = ChronoUnit.DAYS.between(campaign.getStartAt(), campaign.getEndAt()) + 1;
        double left = Math.max(0, ChronoUnit.DAYS.between(campaign.getStartAt(), start)) * 100.0 / total;
        double width = Math.max(1.5, Math.min(100.0 - left, days * 100.0 / total));

        return new BannerRow(
                banner.getId(),
                slot == null ? banner.getSlot().getCode() : slot.getCode(),
                slot == null ? banner.getSlot().getCode() : slot.getName(),
                banner.getPcImagePath(),
                banner.getMobileImagePath(),
                slot != null && slot.hasPcSize() ? slot.getPcWidth() + "x" + slot.getPcHeight() : null,
                slot != null && slot.hasMobileSize() ? slot.getMobileWidth() + "x" + slot.getMobileHeight() : null,
                start, end, hasOwnPeriod(banner), days,
                Boolean.TRUE.equals(banner.getEnabled()),
                BannerStatus.of(banner, campaign, today),
                sum.getImpressions(), sum.getClicks(), sum.getCtr(),
                left, width,
                overlaps.getOrDefault(banner.getId(), List.of()));
    }

    /**
     * 같은 슬롯에 기간이 겹치는 다른 계약의 배너를 찾는다. 겹치면 노출이 무작위로 나뉘므로
     * 단독 노출로 판 슬롯이라면 사고다. 막지는 않고 알려만 준다.
     */
    private Map<Long, List<String>> findOverlaps(AdCampaign campaign, List<Banner> mine) {
        Map<Long, AdCampaign> campaigns = campaignsById();
        List<Banner> others = bannerRepository.findAllWithSlotOrderByCreatedAtDesc().stream()
                .filter(banner -> Boolean.TRUE.equals(banner.getEnabled()))
                .filter(banner -> !Objects.equals(banner.getCampaignId(), campaign.getId()))
                .toList();

        Map<Long, List<String>> found = new HashMap<>();
        for (Banner banner : mine) {
            if (!Boolean.TRUE.equals(banner.getEnabled())) {
                continue;
            }
            LocalDate start = BannerStatus.effectiveStart(banner, campaign);
            LocalDate end = BannerStatus.effectiveEnd(banner, campaign);

            List<String> names = others.stream()
                    .filter(other -> other.getSlot().getId().equals(banner.getSlot().getId()))
                    .filter(other -> {
                        AdCampaign otherCampaign = campaigns.get(other.getCampaignId());
                        LocalDate otherStart = BannerStatus.effectiveStart(other, otherCampaign);
                        LocalDate otherEnd = BannerStatus.effectiveEnd(other, otherCampaign);
                        return otherStart != null && otherEnd != null
                                && !otherStart.isAfter(end) && !start.isAfter(otherEnd);
                    })
                    .map(other -> {
                        AdCampaign otherCampaign = campaigns.get(other.getCampaignId());
                        return otherCampaign == null ? "(소속 없는 배너)" : otherCampaign.getName();
                    })
                    .distinct()
                    .toList();
            if (!names.isEmpty()) {
                found.put(banner.getId(), names);
            }
        }
        return found;
    }

    /** 계약 기간 중 오늘이 어디쯤인지. 기간 밖이면 표시하지 않는다 */
    private Double todayPercent(AdCampaign campaign, LocalDate today) {
        if (today.isBefore(campaign.getStartAt()) || today.isAfter(campaign.getEndAt())) {
            return null;
        }
        long total = ChronoUnit.DAYS.between(campaign.getStartAt(), campaign.getEndAt()) + 1;
        return (ChronoUnit.DAYS.between(campaign.getStartAt(), today) + 0.5) * 100.0 / total;
    }

    private BannerForm toBannerForm(Banner banner) {
        BannerForm form = new BannerForm();
        form.setId(banner.getId());
        form.setSlotId(banner.getSlot().getId());
        form.setLinkUrl(banner.getLinkUrl());
        form.setStartAt(banner.getStartAt());
        form.setEndAt(banner.getEndAt());
        form.setEnabled(Boolean.TRUE.equals(banner.getEnabled()));
        form.setPcImagePath(banner.getPcImagePath());
        form.setMobileImagePath(banner.getMobileImagePath());
        return form;
    }

    private BannerForm blankBannerForm() {
        BannerForm form = new BannerForm();
        form.setSlotId(adSlotRepository.findAll().stream()
                .min(Comparator.comparing(slot -> slot.getSortOrder() == null ? 0 : slot.getSortOrder()))
                .map(AdSlot::getId).orElse(null));
        form.setEnabled(true);
        return form;
    }

    private boolean hasOwnPeriod(Banner banner) {
        return banner.getStartAt() != null || banner.getEndAt() != null;
    }

    private String slotName(Map<Long, AdSlot> slots, Banner banner) {
        AdSlot slot = slots.get(banner.getSlot().getId());
        return slot == null ? banner.getSlot().getCode() : slot.getName();
    }

    private String advertiserNameOf(AdCampaign campaign, Map<Long, Advertiser> advertisers) {
        Advertiser advertiser = advertisers.get(campaign.getAdvertiserId());
        return advertiser == null ? "(삭제된 광고주)" : advertiser.getName();
    }

    private Map<Long, Advertiser> advertisersById() {
        return advertiserRepository.findAll().stream()
                .collect(Collectors.toMap(Advertiser::getId, Function.identity()));
    }

    private Map<Long, AdCampaign> campaignsById() {
        return adCampaignRepository.findAll().stream()
                .collect(Collectors.toMap(AdCampaign::getId, Function.identity()));
    }

    private Map<Long, AdSlot> slotsById() {
        return adSlotRepository.findAll().stream()
                .collect(Collectors.toMap(AdSlot::getId, Function.identity()));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    // ── 화면 모델 ─────────────────────────────────────────

    @Getter
    @AllArgsConstructor
    public static class CampaignRow {
        private final Long id;
        private final String name;
        private final String advertiserName;
        private final LocalDate startAt;
        private final LocalDate endAt;
        private final long amount;
        private final CampaignStatus status;
        private final int bannerCount;
        private final String slotNames;
        private final long ownPeriodCount;
        private final long impressions;
        private final long clicks;
        private final double ctr;
        private final long daysLeft;

        public boolean isEndingSoon() {
            return status == CampaignStatus.LIVE && daysLeft >= 0 && daysLeft <= ENDING_SOON_DAYS;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class BannerRow {
        private final Long id;
        private final String slotCode;
        private final String slotName;
        private final String pcImagePath;
        private final String mobileImagePath;
        private final String pcSize;
        private final String mobileSize;
        private final LocalDate startAt;
        private final LocalDate endAt;
        private final boolean ownPeriod;
        private final long days;
        private final boolean enabled;
        private final BannerStatus status;
        private final long impressions;
        private final long clicks;
        private final double ctr;
        private final double leftPercent;
        private final double widthPercent;
        private final List<String> overlappingCampaigns;

        /** 그림이 하나도 없으면 렌더에서 빠진다 — 슬롯만 잡고 아무것도 안 나간다 */
        public boolean isMissingCreative() {
            return pcImagePath == null && mobileImagePath == null;
        }

        /** 그 기기 규격이 있는데 그림이 없으면 그 기기에서는 네트워크가 대신 채운다 */
        public boolean isPcUnfilled() {
            return pcSize != null && pcImagePath == null;
        }

        public boolean isMobileUnfilled() {
            return mobileSize != null && mobileImagePath == null;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class CampaignDetail {
        private final AdCampaign campaign;
        private final Advertiser advertiser;
        private final CampaignStatus status;
        private final List<BannerRow> banners;
        private final long impressions;
        private final long clicks;
        private final double ctr;
        private final long daysLeft;
        private final Double todayPercent;

        public String getReportPath() {
            return "/ad/report/campaign/" + campaign.getReportToken();
        }

        public List<BannerRow> getMissingCreatives() {
            return banners.stream().filter(BannerRow::isMissingCreative).toList();
        }

        public List<BannerRow> getOverlapping() {
            return banners.stream().filter(row -> !row.getOverlappingCampaigns().isEmpty()).toList();
        }
    }

    // ── 폼 ───────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CampaignForm {
        private Long id;
        private Long advertiserId;
        private String name;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startAt;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endAt;
        private Long amount;
        private String linkUrl;
        private String memo;
        private List<BannerForm> banners = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class BannerForm {
        private Long id;
        private Long slotId;
        private String linkUrl;
        /** 비우면 계약 기간을 따른다 */
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startAt;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endAt;
        private boolean enabled = true;
        private boolean deleted;
        private String pcImagePath;
        private String mobileImagePath;
        private MultipartFile pcImage;
        private MultipartFile mobileImage;
        private boolean clearPcImage;
        private boolean clearMobileImage;
    }
}
