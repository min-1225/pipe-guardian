package com.pipeguardian.repository;

import com.pipeguardian.domain.DefectDetection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefectDetectionRepository extends JpaRepository<DefectDetection, Long> {
    List<DefectDetection> findByInspectionId(Long inspectionId);
}
