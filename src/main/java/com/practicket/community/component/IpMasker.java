package com.practicket.community.component;

/** 화면에 표시할 IP 를 앞 두 마디로 자른다 (118.235.13.7 → 118.235) */
public final class IpMasker {

    private static final String UNKNOWN = "0.0";

    private IpMasker() {
    }

    public static String maskToTwoSegments(String ip) {
        if (ip == null || ip.isBlank()) {
            return UNKNOWN;
        }

        String separator = ip.contains(":") ? ":" : ".";
        String[] segments = ip.split(java.util.regex.Pattern.quote(separator));
        if (segments.length < 2) {
            return segments[0];
        }
        return segments[0] + separator + segments[1];
    }
}
