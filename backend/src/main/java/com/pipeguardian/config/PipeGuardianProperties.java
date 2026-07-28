package com.pipeguardian.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pipeguardian")
@Getter @Setter
public class PipeGuardianProperties {

    private Ai ai = new Ai();
    private Storage storage = new Storage();
    private Risk risk = new Risk();
    private Matching matching = new Matching();

    @Getter @Setter
    public static class Ai {
        private String baseUrl = "http://localhost:8000";
        private int timeoutSeconds = 60;
    }

    @Getter @Setter
    public static class Storage {
        private String root = "./data/images";
    }

    @Getter @Setter
    public static class Risk {
        /** 확장 속도 가속 계수 γ */
        private double gamma = 60.0;
        /** 0~100 정규화 상수 K */
        private double normalizationK = 25.0;
        private double warningThreshold = 40.0;
        private double criticalThreshold = 70.0;
    }

    @Getter @Setter
    public static class Matching {
        private double iouThreshold = 0.3;
        private double alignmentScoreThreshold = 0.3;
    }
}
