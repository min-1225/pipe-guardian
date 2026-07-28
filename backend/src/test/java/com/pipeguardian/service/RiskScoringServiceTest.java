package com.pipeguardian.service;

import com.pipeguardian.config.PipeGuardianProperties;
import com.pipeguardian.domain.DefectDetection;
import com.pipeguardian.domain.DefectType;
import com.pipeguardian.domain.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskScoringServiceTest {

    private RiskScoringService service;

    @BeforeEach
    void setUp() {
        service = new RiskScoringService(new PipeGuardianProperties());
    }

    @Test
    void movingSmallCrackOutranksStaticLargeInsulationDamage() {
        DefectDetection crack = defect(DefectType.CRACK, 3.0, 0.10, false);
        DefectDetection insulation = defect(DefectType.INSULATION, 40.0, 0.0, false);

        service.score(crack);
        service.score(insulation);

        assertEquals(48.9, crack.getRiskScore(), 0.1);
        assertEquals(RiskLevel.WARNING, crack.getRiskLevel());
        assertEquals(38.1, insulation.getRiskScore(), 0.1);
        assertEquals(RiskLevel.NORMAL, insulation.getRiskLevel());
        assertTrue(crack.getRiskScore() > insulation.getRiskScore());
    }

    @Test
    void newLeakIsAlwaysCritical() {
        DefectDetection leak = defect(DefectType.LEAK, 0.1, 0.0, true);

        service.score(leak);

        assertEquals(RiskLevel.CRITICAL, leak.getRiskLevel());
    }

    @Test
    void existingLeakIsAtLeastWarning() {
        DefectDetection leak = defect(DefectType.LEAK, 0.1, 0.0, false);

        service.score(leak);

        assertEquals(RiskLevel.WARNING, leak.getRiskLevel());
    }

    @Test
    void negativeExpansionDoesNotInflateRisk() {
        DefectDetection repaired = defect(DefectType.CORROSION, 10.0, -3.0, false);
        DefectDetection staticDefect = defect(DefectType.CORROSION, 10.0, 0.0, false);

        service.score(repaired);
        service.score(staticDefect);

        assertEquals(staticDefect.getRiskScore(), repaired.getRiskScore(), 0.0001);
    }

    private DefectDetection defect(
            DefectType type,
            double areaCm2,
            double expansionRate,
            boolean newlyDetected
    ) {
        return DefectDetection.builder()
                .defectType(type)
                .areaCm2(areaCm2)
                .expansionRateCm2PerDay(expansionRate)
                .newlyDetected(newlyDetected)
                .build();
    }
}
