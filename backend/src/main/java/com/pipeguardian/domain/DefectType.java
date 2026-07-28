package com.pipeguardian.domain;

/**
 * 결함 유형과 심각도 가중치(W_type).
 * 누유 > 균열 > 부식 > 단열재/박리 순으로 사고 직결성이 높다.
 */
public enum DefectType {
    LEAK("누유", 1.0),
    CRACK("균열", 0.8),
    CORROSION("부식", 0.5),
    INSULATION("단열재 손상", 0.3),
    PEELING("페인트 박리", 0.3);

    private final String koreanName;
    private final double weight;

    DefectType(String koreanName, double weight) {
        this.koreanName = koreanName;
        this.weight = weight;
    }

    public String getKoreanName() { return koreanName; }
    public double getWeight() { return weight; }

    /** AI 서비스가 반환한 클래스명을 도메인 enum으로 변환. 미지의 클래스는 null. */
    public static DefectType fromClassName(String className) {
        if (className == null) return null;
        switch (className.trim().toLowerCase()) {
            case "leak": case "oil_leak": return LEAK;
            case "crack": return CRACK;
            case "corrosion": case "rust": return CORROSION;
            case "insulation": case "insulation_damage": return INSULATION;
            case "peeling": case "paint_peeling": return PEELING;
            default: return null;
        }
    }
}
