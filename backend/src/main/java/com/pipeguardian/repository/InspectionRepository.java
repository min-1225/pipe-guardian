package com.pipeguardian.repository;

import com.pipeguardian.domain.Inspection;
import com.pipeguardian.domain.Pipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    /** 시계열 비교 대상: 해당 배관의 가장 최근 점검 회차 */
    Optional<Inspection> findFirstByPipeOrderByCapturedAtDesc(Pipe pipe);

    List<Inspection> findByPipeOrderByCapturedAtAsc(Pipe pipe);

    List<Inspection> findByPipeIdOrderByCapturedAtDesc(Long pipeId);
}
