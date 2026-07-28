# 기술 개발 명세서

## 1. 목표

드론·휴대용 카메라·고정식 카메라로 촬영한 배관 이미지에서 결함을 탐지하고, 동일 배관의 과거 점검 결과와 비교해 열화 속도, 위험도, 점검 우선순위를 계산합니다.

주요 사용자는 화학·정유 공장의 설비 보전팀과 안전관리자입니다.

## 2. 기술 스택

### Spring Boot 백엔드

- Java 17
- Spring Boot 3.x
- Spring Web
- Spring WebFlux `WebClient`
- Spring Data JPA
- Bean Validation
- H2 개발 DB
- Thymeleaf
- JUnit 5

운영 전환 시 PostgreSQL과 Flyway 도입을 검토합니다.

### Python AI 서비스

- Python 3.10+
- FastAPI, Pydantic
- OpenCV
- PyTorch
- Ultralytics YOLOv8-seg 또는 YOLOv11-seg
- Albumentations
- NumPy

라이브러리가 제공하는 HTTP 처리, 검증, ORM, 이미지 특징점 추출, Homography, 세그멘테이션 추론 기능을 우선 사용합니다. 프로젝트에서 직접 만들 대상은 배관 도메인 규칙, 시계열 비교, 위험도 정책, 리포트 조합과 서비스 간 계약입니다.

## 3. 서비스 책임

### Spring Boot

- 배관과 점검 이력 관리
- 기준 이미지 선택과 버전 관리
- Python AI 서비스 호출
- 과거·현재 탐지 결과 연결
- 변화량과 위험도 계산
- 점검 우선순위와 리포트 생성
- 대시보드용 REST API

구현된 프로젝트 고유 구성요소는 다음과 같습니다. 아래 이름은 Spring 라이브러리에 존재하는 메서드가 아니라 이 프로젝트에서 만든 애플리케이션 코드입니다.

- 도메인: `Pipe`, `Inspection`, `DefectDetection`, `DefectType`, `RiskLevel`
- 서비스: `ImageStorageService`, `PipeService`, `InspectionService`, `TemporalAnalysisService`, `RiskScoringService`, `ReportService`
- AI 연동: `VisionAiClient`

### Python AI 서비스

- 이미지 품질 검사와 전처리
- 과거·현재 이미지 구도 정렬
- 결함 인스턴스 세그멘테이션
- 결함 마스크, 위치, 면적, 신뢰도 반환
- 모델 미준비 기간의 명시적 Mock 응답

전체 메서드와 라이브러리 API 구분은 [CODE_GUIDE.md](CODE_GUIDE.md)를 따릅니다.

## 4. 처리 파이프라인

### 4.1 데이터 입력

- 이미지 또는 영상 프레임
- 촬영 시각
- 배관 ID
- 위치 또는 구간 정보
- 촬영 장비와 작업자 메모

### 4.2 이미지 정렬

모든 점검 이미지는 직전 이미지가 아니라 배관별 Baseline 이미지에 맞춥니다. 이렇게 하면 회차마다 기준 평면이 바뀌어 오차가 누적되는 문제를 줄일 수 있습니다.

OpenCV에서 제공하는 기능을 조합합니다.

1. SIFT 또는 ORB로 특징점과 descriptor 추출
2. BFMatcher 또는 FLANN으로 특징점 매칭
3. RANSAC 기반 `findHomography`로 투시 변환 행렬 추정
4. `warpPerspective`로 현재 이미지를 Baseline 좌표계에 정렬
5. 필요 시 CLAHE로 국부 대비 보정

정렬 신뢰도가 임계값보다 낮으면 자동 비교를 중단하고 재촬영 또는 수동 확인 상태로 전환합니다.

### 4.3 결함 세그멘테이션

YOLO-seg가 다음 클래스를 탐지합니다.

| 코드 | 결함 | 기본 가중치 |
|---|---|---:|
| `LEAK` | 누유 | 1.0 |
| `CRACK` | 균열 | 0.8 |
| `CORROSION` | 부식 | 0.5 |
| `INSULATION` | 단열재 손상 | 0.3 |
| `PEELING` | 페인트 박리 | 0.3 |

추론 결과에는 클래스, confidence, bounding box, mask polygon, 픽셀 면적을 포함합니다.

### 4.4 시계열 연결

현재 결함과 과거 결함은 다음 정보를 조합해 연결합니다.

- Baseline 좌표계의 마스크 IoU
- 결함 클래스
- 중심점 거리
- 형태·면적 유사도

자동 연결 신뢰도가 낮으면 새 결함으로 확정하지 않고 검토 대상으로 표시합니다.

### 4.5 변화량과 위험도

기획안의 기본식은 다음과 같습니다.

```text
deltaArea = areaCurrent - areaPast
deltaDays = dateCurrent - datePast
expansionRate = deltaArea / deltaDays
rawRisk = typeWeight * areaCurrent * (1 + gamma * expansionRate)
```

- 점수는 최종적으로 `0..100` 범위로 정규화합니다.
- `gamma`와 정규화 기준은 실제 데이터 분포로 보정해야 합니다.
- Mock 데이터로 검토한 후보값은 `gamma=60`, 정규화 상수 후보 `K=25`이지만, 구현 공식과 데이터셋이 없는 상태에서 확정값으로 사용하지 않습니다.
- 누유는 최소 `Warning`으로 유지합니다.
- 이전에 없던 신규 누유는 `Critical`로 처리합니다.
- 날짜 차이가 0 이하이거나 정렬 신뢰도가 낮으면 점수를 계산하지 않습니다.

### 4.6 결과

- 과거·현재 이미지 Split View
- 결함 마스크와 확장 영역 Overlay
- 결함별 면적과 변화 속도
- 위험도 점수와 등급
- 전체 배관 점검 우선순위
- 자동 점검 리포트

## 5. 비기능 요구사항

- 이미지·추론 데이터의 접근 권한 분리
- API 키와 비밀번호의 환경 변수 관리
- 원본 이미지와 파생 이미지의 추적 가능성
- 요청 ID 기반 서비스 간 로그 연결
- AI 요청 timeout, retry, circuit breaker
- 재현 가능한 모델 버전과 confidence threshold 기록
- 이미지 업로드 형식·크기·악성 파일 검증

## 6. 안전 한계

AI가 생성한 위험도와 리포트는 점검 우선순위를 돕는 보조 정보입니다. 법정 검사, 안전관리자의 판단, 현장 정밀 진단을 대체하지 않습니다.

현재 구현은 AI 요청 timeout을 적용하고 있습니다. retry와 circuit breaker는 다음 단계입니다.
