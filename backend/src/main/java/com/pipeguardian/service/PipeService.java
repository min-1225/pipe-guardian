package com.pipeguardian.service;

import com.pipeguardian.domain.Pipe;
import com.pipeguardian.dto.PipeRequest;
import com.pipeguardian.repository.PipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PipeService {

    private final PipeRepository pipeRepository;
    private final ImageStorageService imageStorageService;

    public Pipe create(PipeRequest request) {
        String pipeCode = request.pipeCode().trim();
        if (pipeRepository.existsByPipeCode(pipeCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 배관 코드입니다.");
        }

        Pipe pipe = Pipe.builder()
                .pipeCode(pipeCode)
                .location(request.location())
                .outerDiameterMm(request.outerDiameterMm())
                .build();
        return pipeRepository.save(pipe);
    }

    @Transactional(readOnly = true)
    public List<Pipe> getAll() {
        return pipeRepository.findAll();
    }

    public Pipe registerBaseline(Long pipeId, MultipartFile image, Double pipeWidthPx) {
        Pipe pipe = getRequired(pipeId);
        if (pipeWidthPx != null && pipeWidthPx <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배관 이미지 폭은 0보다 커야 합니다.");
        }

        Path stored = imageStorageService.store(image, pipeId, "baseline");
        pipe.setBaselineImagePath(stored.toString());
        if (pipeWidthPx != null) {
            pipe.setBaselinePipeWidthPx(pipeWidthPx);
        }
        return pipeRepository.save(pipe);
    }

    @Transactional(readOnly = true)
    public Pipe getRequired(Long pipeId) {
        return pipeRepository.findById(pipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배관을 찾을 수 없습니다."));
    }
}
