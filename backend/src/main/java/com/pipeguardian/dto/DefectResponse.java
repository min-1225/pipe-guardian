package com.pipeguardian.dto;

import com.pipeguardian.domain.DefectDetection;

import java.util.List;

public record DefectResponse(
        Long id,
        String defectType,
        String defectTypeName,
        Double confidence,
        Double areaPx,
        Double areaCm2,
        Double deltaAreaCm2,
        Double growthRatePercent,
        Double expansionRateCm2PerDay,
        Double riskScore,
        String riskLevel,
        boolean newlyDetected,
        List<Double> bbox
) {
    public static DefectResponse from(DefectDetection d) {
        return new DefectResponse(
                d.getId(),
                d.getDefectType().name(),
                d.getDefectType().getKoreanName(),
                d.getConfidence(),
                d.getAreaPx(),
                d.getAreaCm2(),
                d.getDeltaAreaCm2(),
                d.getGrowthRatePercent(),
                d.getExpansionRateCm2PerDay(),
                d.getRiskScore(),
                d.getRiskLevel() == null ? null : d.getRiskLevel().name(),
                d.isNewlyDetected(),
                List.of(
                        nz(d.getBboxX()), nz(d.getBboxY()),
                        nz(d.getBboxW()), nz(d.getBboxH())
                )
        );
    }

    private static double nz(Double v) { return v == null ? 0.0 : v; }
}
