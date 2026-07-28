package com.pipeguardian.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Python AI 서비스가 반환하는 개별 결함. bbox = [x, y, w, h] */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiDefectDto(
        String className,
        Double confidence,
        List<Double> bbox,
        Double areaPx,
        List<List<Double>> polygon
) {
    public double bboxAt(int index) {
        if (bbox == null || index < 0 || bbox.size() <= index || bbox.get(index) == null) {
            return 0.0;
        }
        return bbox.get(index);
    }
}
