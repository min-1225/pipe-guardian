package com.pipeguardian.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;

/** 모듈 A(정합) + 모듈 B(세그멘테이션) 결과 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAnalysisResponse(
        boolean aligned,
        Double alignmentScore,
        List<List<Double>> homography,
        Integer imageWidth,
        Integer imageHeight,
        boolean mock,
        List<AiDefectDto> defects
) {
    public List<AiDefectDto> safeDefects() {
        return defects == null ? Collections.emptyList() : defects;
    }
}
