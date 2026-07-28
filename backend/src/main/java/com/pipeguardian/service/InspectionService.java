package com.pipeguardian.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipeguardian.client.VisionAiClient;
import com.pipeguardian.config.PipeGuardianProperties;
import com.pipeguardian.domain.DefectDetection;
import com.pipeguardian.domain.DefectType;
import com.pipeguardian.domain.Inspection;
import com.pipeguardian.domain.Pipe;
import com.pipeguardian.domain.RiskLevel;
import com.pipeguardian.dto.AiAnalysisResponse;
import com.pipeguardian.dto.AiDefectDto;
import com.pipeguardian.dto.AlertResponse;
import com.pipeguardian.dto.InspectionResponse;
import com.pipeguardian.dto.TrendPointResponse;
import com.pipeguardian.repository.InspectionRepository;
import com.pipeguardian.repository.PipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class InspectionService {

    private final PipeRepository pipeRepository;
    private final InspectionRepository inspectionRepository;
    private final ImageStorageService imageStorageService;
    private final VisionAiClient visionAiClient;
    private final TemporalAnalysisService temporalAnalysisService;
    private final RiskScoringService riskScoringService;
    private final ReportService reportService;
    private final PipeGuardianProperties properties;
    private final ObjectMapper objectMapper;

    public InspectionResponse runInspection(Long pipeId, MultipartFile image, LocalDateTime capturedAt) {
        Pipe pipe = pipeRepository.findById(pipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배관을 찾을 수 없습니다."));
        LocalDateTime inspectedAt = capturedAt == null ? LocalDateTime.now() : capturedAt;
        Optional<Inspection> previous = inspectionRepository.findFirstByPipeOrderByCapturedAtDesc(pipe);

        Path currentPath = imageStorageService.store(image, pipeId, "inspections");
        Path baselinePath = pipe.getBaselineImagePath() == null ? null : Path.of(pipe.getBaselineImagePath());
        AiAnalysisResponse analysis = visionAiClient.analyze(currentPath, baselinePath);

        boolean alignmentReliable = baselinePath != null
                && analysis.aligned()
                && valueOrZero(analysis.alignmentScore())
                >= properties.getMatching().getAlignmentScoreThreshold();

        Inspection inspection = Inspection.builder()
                .pipe(pipe)
                .capturedAt(inspectedAt)
                .imagePath(currentPath.toString())
                .alignmentScore(analysis.alignmentScore())
                .alignmentReliable(alignmentReliable)
                .build();

        analysis.safeDefects().stream()
                .map(aiDefect -> toDomain(pipe, aiDefect))
                .filter(Objects::nonNull)
                .forEach(inspection::addDefect);

        boolean previousReliable = previous.map(Inspection::isAlignmentReliable).orElse(true);
        temporalAnalysisService.analyze(
                inspection.getDefects(),
                previous,
                inspectedAt,
                alignmentReliable && previousReliable
        );
        inspection.getDefects().forEach(riskScoringService::score);
        updateInspectionRisk(inspection);
        inspection.setReportText(reportService.generate(inspection));

        return InspectionResponse.from(inspectionRepository.save(inspection));
    }

    @Transactional(readOnly = true)
    public InspectionResponse getLatest(Long pipeId) {
        Pipe pipe = pipeRepository.findById(pipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배관을 찾을 수 없습니다."));
        Inspection latest = inspectionRepository.findFirstByPipeOrderByCapturedAtDesc(pipe)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "점검 이력이 없습니다."));
        return InspectionResponse.from(latest);
    }

    @Transactional(readOnly = true)
    public List<TrendPointResponse> getTrend(Long pipeId) {
        Pipe pipe = pipeRepository.findById(pipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배관을 찾을 수 없습니다."));
        return inspectionRepository.findByPipeOrderByCapturedAtAsc(pipe).stream()
                .map(inspection -> new TrendPointResponse(
                        inspection.getCapturedAt(),
                        inspection.getMaxRiskScore(),
                        inspection.getDefects().stream()
                                .map(DefectDetection::getAreaCm2)
                                .filter(Objects::nonNull)
                                .mapToDouble(Double::doubleValue)
                                .sum(),
                        inspection.getDefects().size()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts() {
        return pipeRepository.findAll().stream()
                .map(pipe -> inspectionRepository.findFirstByPipeOrderByCapturedAtDesc(pipe))
                .flatMap(Optional::stream)
                .map(this::toAlert)
                .sorted(Comparator.comparingDouble(
                        (AlertResponse alert) -> valueOrZero(alert.riskScore())
                ).reversed())
                .toList();
    }

    private DefectDetection toDomain(Pipe pipe, AiDefectDto source) {
        DefectType type = DefectType.fromClassName(source.className());
        if (type == null) {
            return null;
        }

        double areaPx = Math.max(0.0, valueOrZero(source.areaPx()));
        return DefectDetection.builder()
                .defectType(type)
                .confidence(valueOrZero(source.confidence()))
                .bboxX(source.bboxAt(0))
                .bboxY(source.bboxAt(1))
                .bboxW(Math.max(0.0, source.bboxAt(2)))
                .bboxH(Math.max(0.0, source.bboxAt(3)))
                .areaPx(areaPx)
                .areaCm2(pipe.toAreaCm2(areaPx))
                .polygonJson(toJson(source.polygon()))
                .build();
    }

    private void updateInspectionRisk(Inspection inspection) {
        DefectDetection highest = inspection.getDefects().stream()
                .max(Comparator.comparingInt((DefectDetection d) -> severity(d.getRiskLevel()))
                        .thenComparingDouble(d -> valueOrZero(d.getRiskScore())))
                .orElse(null);

        if (highest == null) {
            inspection.setMaxRiskScore(0.0);
            inspection.setRiskLevel(RiskLevel.NORMAL);
            return;
        }
        inspection.setMaxRiskScore(highest.getRiskScore());
        inspection.setRiskLevel(highest.getRiskLevel());
    }

    private AlertResponse toAlert(Inspection inspection) {
        return new AlertResponse(
                inspection.getPipe().getId(),
                inspection.getPipe().getPipeCode(),
                inspection.getPipe().getLocation(),
                inspection.getMaxRiskScore(),
                inspection.getRiskLevel().name(),
                inspection.getRiskLevel().getBadge(),
                inspection.getCapturedAt(),
                inspection.getReportText()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석 결과를 저장할 수 없습니다.");
        }
    }

    private int severity(RiskLevel level) {
        if (level == null) {
            return -1;
        }
        return switch (level) {
            case NORMAL -> 0;
            case WARNING -> 1;
            case CRITICAL -> 2;
        };
    }

    private double valueOrZero(Double value) {
        return value == null || !Double.isFinite(value) ? 0.0 : value;
    }
}
