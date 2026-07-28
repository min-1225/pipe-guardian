package com.pipeguardian.controller;

import com.pipeguardian.dto.AlertResponse;
import com.pipeguardian.dto.InspectionResponse;
import com.pipeguardian.dto.TrendPointResponse;
import com.pipeguardian.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    /** 핵심 엔드포인트: 업로드 → 정합 → 탐지 → 시계열 비교 → 위험도 → 리포트 */
    @PostMapping(value = "/pipes/{pipeId}/inspections", consumes = "multipart/form-data")
    public ResponseEntity<InspectionResponse> inspect(
            @PathVariable Long pipeId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "capturedAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime capturedAt) {
        return ResponseEntity.ok(inspectionService.runInspection(pipeId, image, capturedAt));
    }

    @GetMapping("/pipes/{pipeId}/inspections/latest")
    public InspectionResponse latest(@PathVariable Long pipeId) {
        return inspectionService.getLatest(pipeId);
    }

    @GetMapping("/pipes/{pipeId}/trend")
    public List<TrendPointResponse> trend(@PathVariable Long pipeId) {
        return inspectionService.getTrend(pipeId);
    }

    /** 위험도 내림차순 점검 우선순위 */
    @GetMapping("/alerts")
    public List<AlertResponse> alerts() {
        return inspectionService.getAlerts();
    }
}
