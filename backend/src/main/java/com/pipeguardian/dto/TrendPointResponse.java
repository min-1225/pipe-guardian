package com.pipeguardian.dto;

import java.time.LocalDateTime;

/** 회차별 열화 추이 */
public record TrendPointResponse(
        LocalDateTime capturedAt,
        Double maxRiskScore,
        Double totalDefectAreaCm2,
        int defectCount
) {}
