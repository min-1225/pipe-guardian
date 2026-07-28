# 코드 및 라이브러리 API 가이드

이 문서는 PipeGuardian이 직접 만든 코드와 외부 라이브러리가 제공하는 API를 구분합니다. 라이브러리에 이미 있는 기능은 다시 구현하지 않는 것을 원칙으로 합니다.

## 1. Python 프로젝트 고유 함수

### `ai-service/main.py`

| 함수 | 역할 |
|---|---|
| `_decode(raw)` | 업로드 bytes를 OpenCV 이미지로 변환하고 실패 시 `None` 반환 |
| `health()` | 서비스 상태와 Mock 모드를 반환하는 `/health` handler |
| `analyze(current, baseline)` | 디코딩 → 정렬 → 탐지 결과를 조합하는 `/analyze` handler |

### `ai-service/registration.py`

| 함수/클래스 | 역할 |
|---|---|
| `AlignmentResult` | 정렬 이미지, 성공 여부, 신뢰도, Homography를 묶는 프로젝트 DTO |
| `apply_clahe(gray)` | 프로젝트 CLAHE 설정값을 적용 |
| `_to_prepared_gray(image)` | 컬러 변환과 CLAHE를 묶은 전처리 |
| `_create_detector(method)` | 설정에 따라 SIFT 또는 ORB와 거리 norm 선택 |
| `align(current, baseline, method)` | 특징점 추출부터 Baseline 좌표계 warping까지 순서 제어 |

### `ai-service/detector.py`

| 함수 | 역할 |
|---|---|
| `_load_model()` | `PIPE_SEG_MODEL`을 한 번만 로드하고 실패 시 Mock 전환 |
| `detect(image)` | 실모델 또는 Mock 탐지 분기 |
| `_run_yolo(model, image)` | Ultralytics 결과를 백엔드 계약 JSON 구조로 변환 |
| `_polygon_area(polygon)` | OpenCV `contourArea` 호출에 필요한 배열 형태 변환 |
| `_mock_defects(image)` | 이미지 해시 기반의 재현 가능한 데모 결함 생성 |

`_polygon_area`는 면적 공식을 직접 구현하지 않고 OpenCV `cv2.contourArea`를 사용합니다.

## 2. Python 라이브러리 제공 API

### FastAPI

- `FastAPI(...)`: ASGI 애플리케이션 생성
- `@app.get`, `@app.post`: route 등록
- `UploadFile`, `File`: multipart 업로드 처리
- `JSONResponse`: 상태 코드가 포함된 JSON 응답
- `TestClient`: HTTP Smoke Test

### OpenCV

- `cv2.imdecode`: bytes → 이미지
- `cv2.cvtColor`: BGR → grayscale
- `cv2.createCLAHE`: 국부 대비 보정기 생성
- `cv2.ORB_create`, `cv2.SIFT_create`: 특징점 detector 생성
- `detectAndCompute`: keypoint와 descriptor 추출
- `cv2.BFMatcher`, `knnMatch`: descriptor 매칭
- `cv2.findHomography(..., cv2.RANSAC, ...)`: Homography 추정
- `cv2.warpPerspective`: Perspective Transform
- `cv2.contourArea`: polygon 면적

### NumPy / Ultralytics

- `np.frombuffer`, `np.asarray`: OpenCV 입력 배열 구성
- `np.random.default_rng`: 결정적 Mock 데이터 생성
- `ultralytics.YOLO(weights)`: 모델 로드
- `model.predict(...)`: 세그멘테이션 추론

## 3. Java 프로젝트 고유 메서드

### Entry / Config / Client

- `PipeGuardianApplication.main`: Spring Boot 시작
- `DemoDataInitializer.initDemoPipes`: 데모 배관 초기화
- `WebClientConfig.visionWebClient`: AI 호출용 WebClient 설정
- `VisionAiClient.analyze`: 이미지 multipart 요청과 AI 응답 변환

### Controller

- `DashboardController.dashboard`
- `InspectionController.inspect`, `latest`, `trend`, `alerts`
- `PipeController.create`, `list`, `registerBaseline`

이 메서드들은 HTTP 입력을 각 Service로 전달하는 프로젝트 handler입니다.

### Domain

- `DefectDetection.iou`: 두 Bounding Box의 교집합/합집합 비율
- `DefectDetection.effectiveArea`: cm² 우선, 없으면 px² 면적 선택
- `Inspection.addDefect`: 양방향 연관관계 설정
- `Pipe.toAreaCm2`: 배관 외경과 이미지 폭을 이용한 px² → cm² 환산
- `DefectType.fromClassName`: AI 문자열을 도메인 enum으로 변환
- `DefectType.getKoreanName`, `getWeight`
- `RiskLevel.getDescription`, `getBadge`

### DTO

- `AiAnalysisResponse.safeDefects`: null 목록을 빈 목록으로 변환
- `AiDefectDto.bboxAt`: Bounding Box 안전 조회
- `DefectResponse.from`, `InspectionResponse.from`: Entity → 응답 DTO 변환
- `DefectResponse.nz`: null 숫자를 `0.0`으로 변환

### Service

- `ImageStorageService.store`, `validateImage`, `safeExtension`
- `PipeService.create`, `getAll`, `registerBaseline`, `getRequired`
- `InspectionService.runInspection`, `getLatest`, `getTrend`, `getAlerts`
- `InspectionService.toDomain`, `updateInspectionRisk`, `toAlert`, `toJson`, `severity`, `valueOrZero`
- `TemporalAnalysisService.analyze`, `clearTemporalResult`, `markAsNew`
- `RiskScoringService.score`, `classify`, `max`, `severity`, `clamp`
- `ReportService.generate`, `changeSummary`, `actionFor`

위 메서드는 배관 도메인 규칙과 파이프라인 순서를 구현하기 위해 프로젝트에서 만든 코드입니다.

### Repository 선언

- `DefectDetectionRepository.findByInspectionId`
- `InspectionRepository.findFirstByPipeOrderByCapturedAtDesc`
- `InspectionRepository.findByPipeOrderByCapturedAtAsc`
- `InspectionRepository.findByPipeIdOrderByCapturedAtDesc`
- `PipeRepository.findByPipeCode`
- `PipeRepository.existsByPipeCode`

메서드 이름은 프로젝트에서 선언했지만 구현체를 직접 작성한 것은 아닙니다. Spring Data JPA가 이름을 해석해 SQL query 구현을 런타임에 생성합니다.

### Test 코드

- `RiskScoringServiceTest.setUp`, 위험도 정책 검증 메서드 4개, `defect` fixture helper
- `TemporalAnalysisServiceTest.setUp`, 시계열 정책 검증 메서드 3개, `defect` fixture helper
- `VisionAiSmokeTest.setUp`, AI API·정렬·면적 검증 메서드 4개, `_feature_rich_image` fixture helper

테스트 메서드와 fixture helper도 프로젝트에서 만든 코드이며 JUnit·`unittest`가 자동으로 실행합니다.

## 4. Java 라이브러리 제공 API

### Spring

- `SpringApplication.run`: 애플리케이션 부트스트랩
- `@RestController`, `@Controller`, `@GetMapping`, `@PostMapping`: HTTP route
- `ResponseEntity.ok`: HTTP 200 응답
- `MultipartFile`: 업로드 abstraction
- `WebClient`: reactive HTTP client
- `JpaRepository.save`, `findAll`, `findById`, `count`: 기본 CRUD
- `@Transactional`: transaction 경계
- `ResponseStatusException`: HTTP 오류 변환

### Jackson / Lombok / JPA

- `ObjectMapper.writeValueAsString`: polygon JSON 직렬화
- Lombok `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`: 반복 메서드와 생성자 자동 생성
- JPA `@Entity`, `@ManyToOne`, `@OneToMany`: entity mapping

### Java 표준 라이브러리

- `Path`, `Files`, `StandardCopyOption`: 파일 저장
- `UUID.randomUUID`: 충돌하기 어려운 저장 파일명
- `Duration.between`: 점검 간 날짜 차이
- `Optional`, Stream API, `Comparator`: null-safe 조회와 정렬
- `Math.exp`, `Math.max`, `Math.min`: 위험도 정규화

## 5. 구현 원칙

1. 이미지 처리 공식은 OpenCV에 있는 연산을 우선 사용합니다.
2. HTTP, validation, ORM, JSON 직렬화는 Spring/FastAPI/Jackson 기능을 사용합니다.
3. 직접 구현하는 영역은 배관 도메인 규칙, 서비스 연결, 시계열 매칭, 위험도 정책으로 제한합니다.
4. 새 유틸리티를 만들기 전 Java/Python 표준 라이브러리와 현재 의존성을 먼저 확인합니다.
5. Mock 결과와 실제 모델 결과를 항상 구분합니다.
