package com.pipeguardian.service;

import com.pipeguardian.domain.DefectDetection;
import com.pipeguardian.domain.DefectType;
import com.pipeguardian.domain.Inspection;
import com.pipeguardian.domain.RiskLevel;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

@Service
public class ReportService {

    public String generate(Inspection inspection) {
        Optional<DefectDetection> highestRisk = inspection.getDefects().stream()
                .max(Comparator.comparingDouble(d -> d.getRiskScore() == null ? 0.0 : d.getRiskScore()));

        if (highestRisk.isEmpty()) {
            return "배관 " + inspection.getPipe().getPipeCode()
                    + "에서 탐지된 결함이 없습니다. 다음 정기 점검을 유지하세요.";
        }

        DefectDetection defect = highestRisk.get();
        String pipeCode = inspection.getPipe().getPipeCode();
        String defectName = defect.getDefectType().getKoreanName();

        if (defect.getDefectType() == DefectType.LEAK && defect.isNewlyDetected()) {
            return "배관 " + pipeCode + "에서 신규 누유가 탐지되었습니다. 즉시 현장을 통제하고 긴급 점검하세요.";
        }

        String change = changeSummary(defect);
        String action = actionFor(defect.getRiskLevel());
        return String.format(
                Locale.ROOT,
                "배관 %s의 %s 위험도는 %.1f점(%s)입니다.%s %s",
                pipeCode,
                defectName,
                defect.getRiskScore(),
                defect.getRiskLevel().getDescription(),
                change,
                action
        );
    }

    private String changeSummary(DefectDetection defect) {
        if (defect.isNewlyDetected()) {
            return " 신규 결함입니다.";
        }
        if (defect.getGrowthRatePercent() == null) {
            return "";
        }
        return String.format(Locale.ROOT, " 이전 대비 면적 변화율은 %.1f%%입니다.", defect.getGrowthRatePercent());
    }

    private String actionFor(RiskLevel level) {
        return switch (level) {
            case CRITICAL -> "즉시 정밀 점검과 안전 조치를 권장합니다.";
            case WARNING -> "2주 이내 정밀 점검을 권장합니다.";
            case NORMAL -> "다음 정기 점검에서 추이를 확인하세요.";
        };
    }
}
