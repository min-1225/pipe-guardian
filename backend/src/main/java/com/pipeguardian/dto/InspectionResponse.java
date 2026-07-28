package com.pipeguardian.dto;

import com.pipeguardian.domain.Inspection;

import java.time.LocalDateTime;
import java.util.List;

public record InspectionResponse(
        Long inspectionId,
        String pipeCode,
        LocalDateTime capturedAt,
        Double alignmentScore,
        boolean alignmentReliable,
        Double maxRiskScore,
        String riskLevel,
        String reportText,
        List<DefectResponse> defects
) {
    public static InspectionResponse from(Inspection i) {
        return new InspectionResponse(
                i.getId(),
                i.getPipe().getPipeCode(),
                i.getCapturedAt(),
                i.getAlignmentScore(),
                i.isAlignmentReliable(),
                i.getMaxRiskScore(),
                i.getRiskLevel() == null ? null : i.getRiskLevel().name(),
                i.getReportText(),
                i.getDefects().stream().map(DefectResponse::from).toList()
        );
    }
}
