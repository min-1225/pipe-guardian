# PipeGuardian — 비전 AI 기반 배관 열화 추적 및 예지 보전 시스템

화학·정유 공장의 배관을 드론/카메라로 촬영한 영상에서 **결함을 자동 탐지**하고,
동일 배관의 **과거 점검 데이터와 시계열 비교**하여 열화 속도를 정량화하고
**위험도(Risk Score)와 점검 우선순위**를 산출하는 예지 보전 시스템입니다.

> 핵심 차별점은 "탐지"가 아니라 **"시간에 따른 변화량 추적"** 입니다.
> 단일 시점의 부식 발견이 아니라, *같은 부위가 얼마나 빠르게 나빠지고 있는가* 를 계산합니다.

---

## 1. 아키텍처 결정 (왜 Spring + Python 하이브리드인가)

기획안의 비전 AI 스택(YOLOv8/v11-seg, OpenCV SIFT/ORB, Albumentations)은 Python 생태계 전용입니다.
반면 점검 이력 관리·시계열 분석·위험도 산정·대시보드 API는 트랜잭션과 도메인 모델이 중요한 영역으로 Spring이 강점을 가집니다.

따라서 **역할을 분리**합니다.

| 계층 | 기술 | 책임 |
|---|---|---|
| **Backend (Core)** | Spring Boot 3 / Java 17 | 배관·점검 이력 도메인, 시계열 변화량 계산, Risk Score 엔진, 리포트 생성, REST API, 대시보드 |
| **AI Inference Service** | Python 3.10 / FastAPI | 이미지 정합(Registration), YOLO-seg 세그멘테이션, 마스크·면적 추출 |
| **연동** | HTTP(multipart) + JSON | Spring `VisionAiClient` → Python `/analyze` |

AI 서비스는 **무상태(stateless)** 입니다. 모든 이력과 판단은 Spring이 소유합니다.
따라서 AI 모델을 교체해도(YOLOv8 → v11 → 자체 모델) 백엔드는 영향을 받지 않습니다.

---

## 2. 전체 파이프라인

```
[드론/작업자 촬영]
      │  이미지 + 배관ID(QR/RFID) + 촬영일시
      ▼
┌─────────────────────────── Spring Boot ───────────────────────────┐
│ 1) InspectionController : 업로드 수신, 메타데이터 검증             │
│ 2) ImageStorageService  : 원본 저장, 배관별 기준(Baseline) 조회    │
└───────────────────────────────┬───────────────────────────────────┘
                                │ multipart (current + baseline)
                                ▼
┌────────────────────── Python AI Service ──────────────────────────┐
│ 3) registration.py : ORB/SIFT 특징점 → RANSAC Homography          │
│                      → Perspective Transform (현재→기준 좌표계)   │
│                      + CLAHE 조명 보정                            │
│ 4) detector.py     : YOLO-seg 추론                                │
│                      Corrosion / Leak / Crack / Insulation / Peeling│
│                      → mask polygon, area_px, confidence          │
└───────────────────────────────┬───────────────────────────────────┘
                                │ JSON (기준 좌표계 기준 결함 목록)
                                ▼
┌─────────────────────────── Spring Boot ───────────────────────────┐
│ 5) TemporalAnalysisService : 이전 점검 결함과 IoU 매칭            │
│                              ΔArea, Δt, 확장 속도, 신규 결함 판정 │
│                              px² → cm² 스케일 환산                │
│ 6) RiskScoringService      : W_type × Area × (1 + γ × Rate)       │
│                              → 0~100 정규화 → Normal/Warning/Critical│
│ 7) ReportService           : 자연어 점검 리포트 자동 생성          │
│ 8) DashboardController     : Before/After 비교 뷰, 경보 목록      │
└───────────────────────────────────────────────────────────────────┘
```

### 좌표계 설계 (중요)
모든 결함 좌표는 **배관별 기준 이미지(Baseline)의 좌표계**로 정규화되어 저장됩니다.
촬영 각도·거리가 매번 달라져도 Homography로 기준 좌표계에 투영하므로,
서로 다른 날짜의 결함을 **같은 평면 위에서 IoU 비교**할 수 있습니다.
이것이 시계열 비교의 정확도를 보장하는 전제 조건입니다.

---

## 3. 기술 스택

**Backend**
- Java 17, Spring Boot 3.2.x, Gradle
- Spring Web / Spring WebFlux(WebClient) / Spring Data JPA / Validation
- H2 (개발·데모) → PostgreSQL (운영 전환 시 설정만 변경)
- Thymeleaf (MVP 대시보드)

**AI Service**
- Python 3.10+, FastAPI, Uvicorn
- OpenCV(정합·CLAHE), Ultralytics YOLO-seg, NumPy
- 모델 미탑재 시 **Mock 모드**로 동작 (백엔드 파이프라인 단독 검증용)

---

## 4. 프로젝트 구조

```
pipe-guardian/
├─ README.md
├─ CONTRIBUTING.md
├─ demo.sh
├─ docs/
│  ├─ TECHNICAL_SPEC.md          # 개발 명세서
│  ├─ API_CONTRACT.md            # 실제 API 계약
│  ├─ CODE_GUIDE.md              # 직접 구현 코드와 라이브러리 API 구분
│  └─ ROADMAP.md                 # 팀 작업 로드맵
├─ backend/                       # Spring Boot
│  ├─ build.gradle
│  ├─ gradlew / gradlew.bat
│  └─ src/main/java/com/pipeguardian/
│     ├─ domain/      Pipe, Inspection, DefectDetection, DefectType, RiskLevel
│     ├─ repository/  JPA Repositories
│     ├─ dto/         AI 연동 및 응답 DTO
│     ├─ client/      VisionAiClient (Python 서비스 호출)
│     ├─ service/     Storage / Temporal / RiskScoring / Report / Inspection
│     ├─ controller/  REST API + Dashboard
│     └─ config/      WebClient, 데모 데이터 초기화
├─ ai-service/                    # Python FastAPI
│  ├─ main.py                     # /analyze 엔드포인트
│  ├─ registration.py             # 모듈 A: 정합
│  ├─ detector.py                 # 모듈 B: 세그멘테이션
│  └─ requirements.txt
```

테스트: `backend/src/test/.../RiskScoringServiceTest`, `TemporalAnalysisServiceTest`
(위험도 단조성, 결함 매칭, Δt=0, 면적 감소 등 엣지 케이스 검증)

코드의 프로젝트 고유 메서드와 Spring/OpenCV/FastAPI 등 라이브러리가 제공하는 API는
[`docs/CODE_GUIDE.md`](docs/CODE_GUIDE.md)에 구분해 두었습니다.

---

## 5. 실행 방법

### 5.1 AI 서비스
```bash
cd ai-service
pip install -r requirements.txt
# 모델이 있으면 환경변수로 지정, 없으면 자동 Mock 모드
export PIPE_SEG_MODEL=./weights/pipe-seg.pt
uvicorn main:app --host 0.0.0.0 --port 8000
```

### 5.2 Backend
```bash
cd backend
./gradlew bootRun
```
- 대시보드: http://localhost:8080/dashboard
- H2 콘솔: http://localhost:8080/h2-console

> AI 서비스가 내려가 있어도 백엔드는 기동됩니다. 분석 요청 시 503을 반환합니다.

### 5.3 Test

```bash
# Backend
cd backend
./gradlew test

# AI Service
cd ../ai-service
python -m unittest discover -s tests -v
```

---

## 6. API 요약

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/v1/pipes` | 배관 등록 |
| `POST` | `/api/v1/pipes/{pipeId}/baseline` | 기준 이미지 등록 (좌표계 기준) |
| `POST` | `/api/v1/pipes/{pipeId}/inspections` | **점검 이미지 업로드 → 전체 파이프라인 실행** |
| `GET` | `/api/v1/pipes/{pipeId}/inspections/latest` | 최신 점검 결과 + 리포트 |
| `GET` | `/api/v1/pipes/{pipeId}/trend` | 결함별 면적 변화 추이 |
| `GET` | `/api/v1/alerts` | 위험도 순 점검 우선순위 목록 |

상세 스펙은 [`docs/TECHNICAL_SPEC.md`](docs/TECHNICAL_SPEC.md) 참조.

---

## 7. 위험도 산정 로직

```
mm_per_px      = 배관 표준 외경(mm) / 이미지상 배관 폭(px)
area_cm²       = area_px × mm_per_px² / 100

ΔArea          = area_current - area_past          (cm²)
ExpansionRate  = ΔArea / Δt                        (cm²/day)

raw            = W_type × area_current × (1 + γ × max(0, ExpansionRate))
RiskScore      = 100 × (1 - e^(-raw / K))          → 0~100 정규화
                 (γ = 60.0, K = 25.0 — application.yml 에서 조정)
```

면적이 줄어든 경우(보수 완료)는 `max(0, rate)` 로 위험도를 부풀리지 않습니다.
γ·K는 "정지한 대면적 단열재 손상 < 확장 중인 소형 균열"이 되도록 보정했습니다.

| 결함 유형 | 가중치 W_type |
|---|---|
| Leak (누유) | 1.0 |
| Crack (균열) | 0.8 |
| Corrosion (부식) | 0.5 |
| Insulation (단열재 손상) | 0.3 |
| Peeling (박리) | 0.3 |

**경보 등급** — 🟢 Normal(< 40) / 🟡 Warning(40~69) / 🔴 Critical(≥ 70)

등급 오버라이드 (도메인 규칙)
- **신규 누유(Leak) 탐지 → 점수와 무관하게 즉시 Critical** (즉시 출동 대상)
- **기존 누유는 최소 Warning 유지** — 면적이 작고 진행이 없어도 유증기·인화 위험이 있어 Normal로 내리지 않음

**보정 결과 검증**

| 상황 | Risk Score | 등급 |
|---|---|---|
| 소형 부식 10cm², 진행 정지 | 18.1 | 🟢 Normal |
| 단열재 손상 40cm², 진행 정지 | 38.1 | 🟢 Normal |
| 소형 균열 3cm², 0.10 cm²/day 확장 | 48.9 | 🟡 Warning |
| 부식 12.8cm², 0.04 cm²/day 확장 | 58.1 | 🟡 Warning |
| 부식 12.8cm², 0.42 cm²/day 급속 확장 | 99.9 | 🔴 Critical |

**리포트 출력 예시**
> 배관 A-203의 부식 면적이 이전 대비 27.4% 증가(0.04 cm²/day)하여 2주 내 정밀 점검을 권장합니다.

---

## 8. MVP 범위

**포함** — 업로드 → 정합 → 탐지 → 시계열 비교 → 위험도 → 리포트 → 대시보드 (End-to-End 골격)

**미포함(다음 단계)** — 실제 학습 데이터셋 구축 및 모델 파인튜닝, 드론 자동 항로 연동,
RFID/QR 자동 인식, 3D 배관 도면 매핑, 알림 채널(SMS/Slack) 연동, 사용자 권한 관리

---

## 9. 현재 구현 상태

- Spring Boot 도메인, Repository, REST/대시보드 Controller
- 이미지 저장, AI 연동, 시계열 비교, 위험도, 리포트 Service
- FastAPI 이미지 정합, YOLO-seg 연동 지점, 결정적 Mock 탐지
- H2 개발 DB와 Thymeleaf 우선순위 대시보드
- Gradle Wrapper
- Backend 단위 테스트 7개, AI Smoke Test 4개
- 업로드 → 정합 → 탐지 → 위험도 → 알림 E2E 확인

실제 YOLO-seg 가중치는 아직 포함하지 않습니다. `PIPE_SEG_MODEL`을 설정하지 않으면
AI 응답의 `mock` 필드가 `true`이며 데모 화면에서도 실모델 결과와 구분해야 합니다.

## 10. 협업

저장소는 Public이므로 누구나 포크 후 Pull Request를 보낼 수 있습니다.
원본 저장소에 브랜치를 직접 푸시할 팀원은 저장소 소유자가 GitHub Collaborator로 초대해야 합니다.

```bash
git switch -c feat/<작업명>
git add .
git commit -m "feat: 작업 내용"
git push -u origin feat/<작업명>
```

브랜치, 커밋, 리뷰 규칙은 [`CONTRIBUTING.md`](CONTRIBUTING.md)를 따릅니다.

## 11. 공개 저장소 주의사항

- `.env`, 토큰, 비밀번호, 개인정보를 커밋하지 않습니다.
- 원본 현장 이미지에 얼굴, 위치, 설비 식별정보가 있으면 비식별화합니다.
- 모델 가중치와 데이터셋은 GitHub Release 또는 별도 스토리지를 사용합니다.
- AI 위험도는 산업 안전 판정의 보조 정보이며 법정 검사와 현장 전문가 판단을 대체하지 않습니다.

## 12. 라이선스

아직 라이선스를 선택하지 않았습니다. 팀 합의 후 MIT, Apache-2.0 또는 별도 이용 조건을 추가합니다.
