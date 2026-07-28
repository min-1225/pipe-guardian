package com.pipeguardian.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 점검 대상 배관.
 * baselineImage 는 모든 회차 결함 좌표가 투영되는 "기준 좌표계" 역할을 한다.
 */
@Entity
@Table(name = "pipe")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Pipe {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pipeCode;          // 예: A-203

    private String location;          // 설치 구역

    /** 배관 표준 외경(mm) — px → cm² 스케일 환산의 기준 */
    private Double outerDiameterMm;

    private String baselineImagePath;

    /** 기준 이미지에서 배관이 차지하는 폭(px) */
    private Double baselinePipeWidthPx;

    /**
     * 픽셀 면적을 실제 면적(cm²)으로 환산.
     * mmPerPx = 외경(mm) / 배관폭(px),  areaCm2 = areaPx * mmPerPx^2 / 100
     * 스케일 정보가 없으면 null 반환(면적은 px 기준으로만 비교).
     */
    public Double toAreaCm2(double areaPx) {
        if (outerDiameterMm == null || baselinePipeWidthPx == null || baselinePipeWidthPx <= 0) {
            return null;
        }
        double mmPerPx = outerDiameterMm / baselinePipeWidthPx;
        return areaPx * mmPerPx * mmPerPx / 100.0;
    }
}
