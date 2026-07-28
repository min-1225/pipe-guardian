package com.pipeguardian.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 단일 결함 인스턴스. 좌표는 모두 배관 기준 이미지(Baseline) 좌표계 기준이다.
 */
@Entity
@Table(name = "defect_detection")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DefectDetection {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DefectType defectType;

    private Double confidence;

    // Bounding Box (기준 좌표계)
    private Double bboxX;
    private Double bboxY;
    private Double bboxW;
    private Double bboxH;

    private Double areaPx;
    private Double areaCm2;

    @Lob
    private String polygonJson;

    // ---- 시계열 분석 결과 ----
    private Long matchedPastDefectId;
    private Double deltaAreaCm2;
    private Double expansionRateCm2PerDay;   // cm² / day
    private Double growthRatePercent;
    private boolean newlyDetected;

    // ---- 위험도 ----
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    /** 다른 결함과의 Bounding Box IoU. 동일 결함 여부 판정에 사용. */
    public double iou(DefectDetection other) {
        if (other == null
                || bboxX == null || bboxY == null || bboxW == null || bboxH == null
                || other.bboxX == null || other.bboxY == null
                || other.bboxW == null || other.bboxH == null) {
            return 0.0;
        }
        double x1 = Math.max(bboxX, other.bboxX);
        double y1 = Math.max(bboxY, other.bboxY);
        double x2 = Math.min(bboxX + bboxW, other.bboxX + other.bboxW);
        double y2 = Math.min(bboxY + bboxH, other.bboxY + other.bboxH);

        double interW = Math.max(0.0, x2 - x1);
        double interH = Math.max(0.0, y2 - y1);
        double intersection = interW * interH;
        if (intersection <= 0) return 0.0;

        double union = (bboxW * bboxH) + (other.bboxW * other.bboxH) - intersection;
        return union <= 0 ? 0.0 : intersection / union;
    }

    /** 스케일 환산이 불가한 경우 px 면적으로 대체 비교. */
    public double effectiveArea() {
        return areaCm2 != null ? areaCm2 : (areaPx != null ? areaPx : 0.0);
    }
}
