package com.practicket.practice.share;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 링크 미리보기에 뜨는 1200x630 카드를 그린다.
 *
 * 모든 글자를 Jalnan 으로 그린다. 서버에 한글 폰트가 깔려 있다는 보장이 없어
 * 논리 폰트로 두면 한글이 두부로 나온다.
 */
@Component
public class ShareCardRenderer {

    private static final int W = 1200;
    private static final int H = 630;
    private static final int LEFT_W = 528;

    private static final Color DEEP = new Color(0x51, 0x48, 0x97);
    private static final Color MID = new Color(0x84, 0x70, 0xb3);
    private static final Color INK = new Color(0x3d, 0x34, 0x62);
    private static final Color SUB = new Color(0x9a, 0x94, 0xab);
    private static final Color TRACK = new Color(0xf1, 0xef, 0xf5);
    private static final Color SEG1 = new Color(0xd6, 0xcb, 0xe9);

    private final Font base;

    public ShareCardRenderer() {
        this.base = loadFont();
    }

    private Font loadFont() {
        try (InputStream in = getClass().getResourceAsStream("/static/fonts/Jalnan.ttf")) {
            if (in == null) return new Font(Font.SANS_SERIF, Font.BOLD, 12);
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Exception e) {
            return new Font(Font.SANS_SERIF, Font.BOLD, 12);
        }
    }

    public byte[] render(ShareResult r) throws IOException {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        drawLeft(g, r);
        drawRight(g, r);

        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private void drawLeft(Graphics2D g, ShareResult r) {
        g.setPaint(new GradientPaint(0, 0, MID, LEFT_W, H, DEEP));
        g.fillRect(0, 0, LEFT_W, H);

        g.setColor(new Color(255, 255, 255, 16));
        g.fillOval(-90, H - 210, 300, 300);

        g.setColor(new Color(255, 255, 255, 160));
        g.setFont(base.deriveFont(26f));
        g.drawString("프랙티켓", 56, 92);

        g.setColor(Color.WHITE);
        g.setFont(base.deriveFont(104f));
        g.drawString(r.totalSeconds(), 52, 390);
        g.setFont(base.deriveFont(38f));
        g.setColor(new Color(255, 255, 255, 150));
        g.drawString("초", 52 + g.getFontMetrics(base.deriveFont(104f)).stringWidth(r.totalSeconds()) + 10, 390);

        if (r.percentile() != null) {
            g.setColor(Color.WHITE);
            g.setFont(base.deriveFont(30f));
            g.drawString("상위 " + r.percentile() + "%", 56, 448);
        }

        if (r.queueInitialRank() > 0) {
            String txt = "대기 순번 " + NumberFormat.getInstance(Locale.KOREA).format(r.queueInitialRank()) + "번에서 출발";
            g.setColor(new Color(255, 255, 255, 34));
            g.fillRoundRect(52, 486, LEFT_W - 108, 62, 16, 16);
            g.setColor(new Color(255, 255, 255, 220));
            g.setFont(base.deriveFont(21f));
            g.drawString(txt, 74, 525);
        }
    }

    private void drawRight(Graphics2D g, ShareResult r) {
        g.setColor(Color.WHITE);
        g.fillRect(LEFT_W, 0, W - LEFT_W, H);

        int x = LEFT_W + 56;
        g.setColor(INK);
        g.setFont(base.deriveFont(25f));
        g.drawString(r.label() + " 구간 기록", x, 96);

        int sum = Math.max(1, r.segmentSum());
        int barMax = W - x - 190;
        String[] names = {"반응", "대기열", "좌석 선택"};
        int[] values = {r.reactionMs(), r.queueMs(), r.seatMs()};
        Color[] colors = {SEG1, MID, DEEP};

        int y = 170;
        for (int i = 0; i < 3; i++) {
            g.setColor(SUB);
            g.setFont(base.deriveFont(21f));
            g.drawString(names[i], x, y + 6);

            int bx = x + 120;
            g.setColor(TRACK);
            g.fillRoundRect(bx, y - 10, barMax, 16, 8, 8);
            g.setColor(colors[i]);
            g.fillRoundRect(bx, y - 10, Math.max(8, Math.round(barMax * (values[i] / (float) sum))), 16, 8, 8);

            g.setColor(DEEP);
            g.setFont(base.deriveFont(21f));
            String v = String.format("%.3f초", values[i] / 1000f);
            g.drawString(v, W - 56 - g.getFontMetrics().stringWidth(v), y + 6);
            y += 62;
        }

        g.setColor(TRACK);
        g.fillRoundRect(x, 372, W - x - 56, 66, 14, 14);
        g.setColor(DEEP);
        g.setFont(base.deriveFont(21f));
        g.drawString("세 구간 중 " + r.slowestShare() + "%를 " + r.slowestLabel() + "에 썼어요", x + 22, 413);

        g.setColor(INK);
        g.setFont(base.deriveFont(25f));
        g.drawString("몇 초 만에 잡을 수 있나요?", x, 528);
        g.setColor(SUB);
        g.setFont(base.deriveFont(19f));
        g.drawString("실전과 똑같은 티켓팅 연습 · practicket.com", x, 562);
    }
}
