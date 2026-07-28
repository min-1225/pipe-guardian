package com.pipeguardian.client;

import com.pipeguardian.dto.AiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.file.Path;

/**
 * Python 비전 AI 서비스 연동 클라이언트.
 * 정합(모듈 A)과 세그멘테이션(모듈 B)은 AI 서비스가, 이후 판단은 모두 백엔드가 담당한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisionAiClient {

    private final WebClient visionWebClient;

    /**
     * @param currentImage  이번 회차 촬영 이미지
     * @param baselineImage 배관 기준 이미지(없으면 null → 정합 생략)
     */
    public AiAnalysisResponse analyze(Path currentImage, Path baselineImage) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("current", new FileSystemResource(currentImage));
        if (baselineImage != null) {
            builder.part("baseline", new FileSystemResource(baselineImage));
        }

        try {
            AiAnalysisResponse response = visionWebClient.post()
                    .uri("/analyze")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(AiAnalysisResponse.class)
                    .block();

            if (response == null) {
                throw new IllegalStateException("AI 서비스 응답이 비어 있습니다.");
            }
            if (response.mock()) {
                log.warn("AI 서비스가 Mock 모드로 동작 중입니다. 실제 모델 가중치를 설정하세요.");
            }
            return response;
        } catch (Exception e) {
            log.error("비전 AI 서비스 호출 실패", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "비전 AI 분석 서비스에 연결할 수 없습니다. 이미지는 저장되었으므로 이후 재분석이 가능합니다.");
        }
    }
}
