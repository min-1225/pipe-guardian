package com.pipeguardian.config;

import com.pipeguardian.domain.Pipe;
import com.pipeguardian.repository.PipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 데모용 배관 등록. 운영 프로파일에서는 제외한다. */
@Configuration
@RequiredArgsConstructor
public class DemoDataInitializer {

    @Bean
    public ApplicationRunner initDemoPipes(PipeRepository pipeRepository) {
        return args -> {
            if (pipeRepository.count() > 0) return;
            pipeRepository.save(Pipe.builder()
                    .pipeCode("A-203").location("제2공정동 북측")
                    .outerDiameterMm(219.1).baselinePipeWidthPx(280.0).build());
            pipeRepository.save(Pipe.builder()
                    .pipeCode("B-117").location("탱크야드 배관랙")
                    .outerDiameterMm(168.3).baselinePipeWidthPx(240.0).build());
        };
    }
}
