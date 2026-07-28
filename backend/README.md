# Backend

Java 17 / Spring Boot 3.2 기반 PipeGuardian 핵심 백엔드입니다.

담당 기능:

- 배관·점검·결함 JPA 도메인과 H2 저장
- Baseline과 점검 이미지 저장
- `VisionAiClient`를 통한 FastAPI 호출
- IoU 기반 시계열 결함 연결
- 면적 변화량·확장 속도·위험도 계산
- 점검 리포트와 우선순위 API
- Thymeleaf 대시보드

```bash
# macOS / Linux
bash ./gradlew bootRun
bash ./gradlew test

# Windows
gradlew.bat bootRun
gradlew.bat test
```

- API: `http://localhost:8080/api/v1`
- Dashboard: `http://localhost:8080/dashboard`
- H2 Console: `http://localhost:8080/h2-console`

환경 변수:

- `PIPE_AI_BASE_URL`: 기본값 `http://localhost:8000`
- `PIPE_IMAGE_ROOT`: 기본값 `./data/images`
