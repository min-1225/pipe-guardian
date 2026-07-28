package com.pipeguardian.service;

import com.pipeguardian.config.PipeGuardianProperties;
import com.pipeguardian.domain.DefectDetection;
import com.pipeguardian.domain.DefectType;
import com.pipeguardian.domain.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskScoringService {

    private final PipeGuardianProperties properties;

    public double score(DefectDetection defect) {
        double area = Math.max(0.0, defect.effectiveArea());
        double expansionRate = defect.getExpansionRateCm2PerDay() == null
                ? 0.0
                : Math.max(0.0, defect.getExpansionRateCm2PerDay());

        double gamma = properties.getRisk().getGamma();
        double normalizationK = Math.max(0.000001, properties.getRisk().getNormalizationK());
        double raw = defect.getDefectType().getWeight()
                * area
                * (1.0 + gamma * expansionRate);
        double score = clamp(100.0 * (1.0 - Math.exp(-raw / normalizationK)));

        RiskLevel level = classify(score);
        if (defect.getDefectType() == DefectType.LEAK) {
            level = defect.isNewlyDetected() ? RiskLevel.CRITICAL : max(level, RiskLevel.WARNING);
        }

        defect.setRiskScore(score);
        defect.setRiskLevel(level);
        return score;
    }

    public RiskLevel classify(double score) {
        if (score >= properties.getRisk().getCriticalThreshold()) {
            return RiskLevel.CRITICAL;
        }
        if (score >= properties.getRisk().getWarningThreshold()) {
            return RiskLevel.WARNING;
        }
        return RiskLevel.NORMAL;
    }

    private RiskLevel max(RiskLevel first, RiskLevel second) {
        return severity(first) >= severity(second) ? first : second;
    }

    private int severity(RiskLevel level) {
        return switch (level) {
            case NORMAL -> 0;
            case WARNING -> 1;
            case CRITICAL -> 2;
        };
    }

    private double clamp(double score) {
        return Math.max(0.0, Math.min(100.0, score));
    }
}
