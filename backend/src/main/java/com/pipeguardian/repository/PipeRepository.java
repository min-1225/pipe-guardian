package com.pipeguardian.repository;

import com.pipeguardian.domain.Pipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PipeRepository extends JpaRepository<Pipe, Long> {
    Optional<Pipe> findByPipeCode(String pipeCode);
    boolean existsByPipeCode(String pipeCode);
}
