package com.pipeguardian.dto;

import java.time.LocalDateTime;

/** 점검 우선순위 목록 항목 */
public record AlertResponse(
        Long pipeId,
        String pipeCode,
        String location,
        Double riskScore,
        String riskLevel,
        String badge,
        LocalDateTime lastInspectedAt,
        String reportText
) {}
