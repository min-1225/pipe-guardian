package com.pipeguardian.service;

import com.pipeguardian.config.PipeGuardianProperties;
import com.pipeguardian.domain.DefectDetection;
import com.pipeguardian.domain.DefectType;
import com.pipeguardian.domain.Inspection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class TemporalAnalysisServiceTest {

    private TemporalAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new TemporalAnalysisService(new PipeGuardianProperties());
    }

    @Test
    void matchesSameDefectByTypeAndIou() {
        LocalDateTime pastAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        DefectDetection past = defect(10L, DefectType.CORROSION, 0, 0, 100, 100, 10.0);
        Inspection previous = Inspection.builder()
                .capturedAt(pastAt)
                .defects(List.of(past))
                .build();
        DefectDetection current = defect(null, DefectType.CORROSION, 10, 10, 100, 100, 12.0);

        service.analyze(
                List.of(current),
                Optional.of(previous),
                pastAt.plusDays(10),
                true
        );

        assertFalse(current.isNewlyDetected());
        assertEquals(10L, current.getMatchedPastDefectId());
        assertEquals(2.0, current.getDeltaAreaCm2(), 0.0001);
        assertEquals(0.2, current.getExpansionRateCm2PerDay(), 0.0001);
        assertEquals(20.0, current.getGrowthRatePercent(), 0.0001);
    }

    @Test
    void skipsComparisonWhenAlignmentIsUnreliable() {
        DefectDetection current = defect(null, DefectType.LEAK, 0, 0, 20, 20, 1.0);

        service.analyze(List.of(current), Optional.empty(), LocalDateTime.now(), false);

        assertFalse(current.isNewlyDetected());
        assertNull(current.getExpansionRateCm2PerDay());
    }

    @Test
    void doesNotCalculateRateWhenDatesAreNotIncreasing() {
        LocalDateTime capturedAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        DefectDetection past = defect(10L, DefectType.CRACK, 0, 0, 100, 100, 3.0);
        Inspection previous = Inspection.builder()
                .capturedAt(capturedAt)
                .defects(List.of(past))
                .build();
        DefectDetection current = defect(null, DefectType.CRACK, 0, 0, 100, 100, 4.0);

        service.analyze(List.of(current), Optional.of(previous), capturedAt, true);

        assertEquals(1.0, current.getDeltaAreaCm2());
        assertNull(current.getExpansionRateCm2PerDay());
    }

    private DefectDetection defect(
            Long id,
            DefectType type,
            double x,
            double y,
            double width,
            double height,
            double areaCm2
    ) {
        return DefectDetection.builder()
                .id(id)
                .defectType(type)
                .bboxX(x)
                .bboxY(y)
                .bboxW(width)
                .bboxH(height)
                .areaCm2(areaCm2)
                .build();
    }
}
