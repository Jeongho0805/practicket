package com.practicket.ad.component;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/** 조각 하나가 그릴 것 전부. 데스크톱과 모바일을 따로 담는다. */
@Getter
@AllArgsConstructor
public class AdSlotRender {

    private final String code;
    private final AdFace pc;
    private final AdFace mobile;

    public static AdSlotRender empty(String code) {
        return new AdSlotRender(code, AdFace.none(), AdFace.none());
    }

    /**
     * 양쪽 다 같은 배너면 그림 두 장을 &lt;picture&gt; 하나로 묶는다. 브라우저가 화면 폭을 보고
     * 한 장만 내려받는다. 두 벌로 나누면 안 보이는 쪽까지 받아 간다.
     */
    public boolean isSinglePicture() {
        return pc.isBanner() && mobile.isBanner()
                && Objects.equals(pc.getBannerId(), mobile.getBannerId());
    }

    public String getPcClass() {
        return classOf("pc", pc);
    }

    public String getMobileClass() {
        return classOf("mo", mobile);
    }

    /**
     * 자리의 크기는 무엇이 들어가느냐로 갈린다. 배너는 규격 비율을 지켜야 하고,
     * 쿠팡 캐러셀 같은 위젯은 스스로 크기를 맞추므로 자리를 꽉 줘야 한다.
     * CSS 가 :has() 로 자식을 뒤지지 않게 서버가 답을 클래스로 적어 준다.
     */
    private String classOf(String prefix, AdFace face) {
        if (face.isBanner()) {
            return prefix + "-banner";
        }
        if (face.isFill()) {
            return prefix + "-fill " + prefix + "-net-" + face.getNetwork();
        }
        if (face.isEmpty()) {
            return prefix + "-empty";
        }
        return prefix + "-none";
    }
}
