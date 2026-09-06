package com.practicket.ad.application;

import com.practicket.ad.component.AdNetworkSettings;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.AdUnit;
import com.practicket.ad.domain.AdUnitRepository;
import com.practicket.ad.exception.AdException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 광고 설정 — 네트워크가 발급한 광고단위를 등록해 둔다. 슬롯이 팔리지 않았을 때 이 중 하나가 들어간다.
 *
 * 계정 값(쿠팡 trackingCode, 애드센스 client)은 여기 없다. 사이트에 하나뿐이고 바뀌지 않아
 * {@link AdNetworkSettings} 가 설정에서 읽는다.
 */
@Controller
@RequestMapping("/admin-hoya/ad/units")
@RequiredArgsConstructor
public class AdminUnitController {

    private final AdUnitRepository adUnitRepository;
    private final AdSlotRepository adSlotRepository;

    @InitBinder
    void trimEmptyToNull(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("units", adUnitRepository.findAllByOrderByNetworkAscNameAsc());
        model.addAttribute("networks", networks());
        return "admin/ad/unit-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("unit", null);
        model.addAttribute("networks", networks());
        return "admin/ad/unit-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return adUnitRepository.findById(id)
                .map(unit -> {
                    model.addAttribute("unit", unit);
                    model.addAttribute("networks", networks());
                    return "admin/ad/unit-form";
                })
                .orElse("redirect:/admin-hoya/ad/units");
    }

    @PostMapping
    @Transactional
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String network,
                       @RequestParam String unitId,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) Integer width,
                       @RequestParam(required = false) Integer height,
                       @RequestParam(required = false, defaultValue = "false") boolean isDefault,
                       RedirectAttributes redirectAttributes) {
        try {
            AdUnit saved = id == null
                    ? adUnitRepository.save(AdUnit.builder()
                            .network(network).unitId(unitId).name(name)
                            .width(width).height(height).isDefault(false)
                            .build())
                    : update(id, network, unitId, name, width, height);
            if (isDefault) {
                markDefault(saved);
            }
        } catch (AdException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return id == null ? "redirect:/admin-hoya/ad/units/new"
                    : "redirect:/admin-hoya/ad/units/" + id + "/edit";
        }
        return "redirect:/admin-hoya/ad/units";
    }

    @PostMapping("/{id}/default")
    @Transactional
    public String setDefault(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adUnitRepository.findById(id).ifPresentOrElse(this::markDefault,
                () -> redirectAttributes.addFlashAttribute("error", "존재하지 않는 광고단위입니다."));
        return "redirect:/admin-hoya/ad/units";
    }

    /** 슬롯이 직접 고른 단위는 지우지 않는다. 지우면 그 슬롯이 조용히 비어 버린다 */
    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean used = adSlotRepository.findAll().stream()
                .anyMatch(slot -> id.equals(slot.getPcAdUnitId()) || id.equals(slot.getMobileAdUnitId()));
        if (used) {
            redirectAttributes.addFlashAttribute("error", "슬롯이 직접 지정해 쓰고 있어 삭제할 수 없습니다.");
            return "redirect:/admin-hoya/ad/units";
        }
        adUnitRepository.deleteById(id);
        return "redirect:/admin-hoya/ad/units";
    }

    private AdUnit update(Long id, String network, String unitId, String name, Integer width, Integer height) {
        AdUnit unit = adUnitRepository.findById(id)
                .orElseThrow(() -> new AdException("존재하지 않는 광고단위입니다."));
        unit.update(network, unitId, width, height, name);
        return unit;
    }

    /** 네트워크마다 기본은 하나다. 새로 세우면 같은 네트워크의 나머지를 내린다 */
    private void markDefault(AdUnit unit) {
        adUnitRepository.findByNetwork(unit.getNetwork())
                .forEach(other -> other.markDefault(other.getId().equals(unit.getId())));
        unit.markDefault(true);
    }

    private List<String> networks() {
        return List.of(AdNetworkSettings.COUPANG, AdNetworkSettings.ADSENSE, AdNetworkSettings.ADFIT);
    }
}
