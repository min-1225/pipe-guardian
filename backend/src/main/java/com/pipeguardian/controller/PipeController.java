package com.pipeguardian.controller;

import com.pipeguardian.domain.Pipe;
import com.pipeguardian.dto.PipeRequest;
import com.pipeguardian.service.PipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pipes")
@RequiredArgsConstructor
public class PipeController {

    private final PipeService pipeService;

    @PostMapping
    public ResponseEntity<Pipe> create(@Valid @RequestBody PipeRequest request) {
        return ResponseEntity.ok(pipeService.create(request));
    }

    @GetMapping
    public List<Pipe> list() {
        return pipeService.getAll();
    }

    /** 기준 좌표계가 될 Baseline 이미지 등록 */
    @PostMapping(value = "/{pipeId}/baseline", consumes = "multipart/form-data")
    public ResponseEntity<Pipe> registerBaseline(
            @PathVariable Long pipeId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "pipeWidthPx", required = false) Double pipeWidthPx) {
        return ResponseEntity.ok(pipeService.registerBaseline(pipeId, image, pipeWidthPx));
    }
}
