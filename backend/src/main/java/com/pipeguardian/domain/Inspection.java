package com.pipeguardian.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inspection")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Inspection {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipe_id")
    private Pipe pipe;

    @Column(nullable = false)
    private LocalDateTime capturedAt;

    private String imagePath;
    private String alignedImagePath;

    /** 정합 신뢰도(0~1). 낮으면 시계열 비교 결과를 참고용으로 강등한다. */
    private Double alignmentScore;
    private boolean alignmentReliable;

    private Double maxRiskScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(length = 2000)
    private String reportText;

    @Builder.Default
    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DefectDetection> defects = new ArrayList<>();

    public void addDefect(DefectDetection defect) {
        defects.add(defect);
        defect.setInspection(this);
    }
}
