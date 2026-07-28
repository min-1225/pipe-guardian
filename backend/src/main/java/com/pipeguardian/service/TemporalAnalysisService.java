package com.pipeguardian.service;

import com.pipeguardian.config.PipeGuardianProperties;
import com.pipeguardian.domain.DefectDetection;
import com.pipeguardian.domain.Inspection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TemporalAnalysisService {

    private final PipeGuardianProperties properties;

    public void analyze(
            List<DefectDetection> currentDefects,
            Optional<Inspection> previousInspection,
            LocalDateTime capturedAt,
            boolean comparisonAllowed
    ) {
        if (!comparisonAllowed) {
            currentDefects.forEach(this::clearTemporalResult);
            return;
        }

        if (previousInspection.isEmpty()) {
            currentDefects.forEach(this::markAsNew);
            return;
        }

        Inspection previous = previousInspection.get();
        long days = Duration.between(previous.getCapturedAt(), capturedAt).toDays();
        Set<Long> matchedPastIds = new HashSet<>();

        for (DefectDetection current : currentDefects) {
            Optional<DefectDetection> matched = previous.getDefects().stream()
                    .filter(past -> past.getDefectType() == current.getDefectType())
                    .filter(past -> past.getId() == null || !matchedPastIds.contains(past.getId()))
                    .filter(past -> current.iou(past) >= properties.getMatching().getIouThreshold())
                    .max(Comparator.comparingDouble(current::iou));

            if (matched.isEmpty()) {
                markAsNew(current);
                continue;
            }

            DefectDetection past = matched.get();
            if (past.getId() != null) {
                matchedPastIds.add(past.getId());
            }
            current.setMatchedPastDefectId(past.getId());
            current.setNewlyDetected(false);

            double pastArea = past.effectiveArea();
            double currentArea = current.effectiveArea();
            if (pastArea > 0) {
                current.setGrowthRatePercent((currentArea - pastArea) / pastArea * 100.0);
            }

            if (current.getAreaCm2() != null && past.getAreaCm2() != null) {
                double deltaCm2 = current.getAreaCm2() - past.getAreaCm2();
                current.setDeltaAreaCm2(deltaCm2);
                if (days > 0) {
                    current.setExpansionRateCm2PerDay(deltaCm2 / days);
                }
            }
        }
    }

    private void clearTemporalResult(DefectDetection defect) {
        defect.setMatchedPastDefectId(null);
        defect.setDeltaAreaCm2(null);
        defect.setExpansionRateCm2PerDay(null);
        defect.setGrowthRatePercent(null);
        defect.setNewlyDetected(false);
    }

    private void markAsNew(DefectDetection defect) {
        clearTemporalResult(defect);
        defect.setNewlyDetected(true);
    }
}
