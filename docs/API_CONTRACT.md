# API 계약

현재 Spring Boot와 FastAPI 구현을 기준으로 정리한 계약입니다.

## Spring Boot API

Base URL: `http://localhost:8080/api/v1`

| Method | Path | Content-Type | 설명 |
|---|---|---|---|
| `POST` | `/pipes` | `application/json` | 배관 등록 |
| `GET` | `/pipes` | - | 배관 목록 |
| `POST` | `/pipes/{pipeId}/baseline` | `multipart/form-data` | Baseline 이미지 등록 |
| `POST` | `/pipes/{pipeId}/inspections` | `multipart/form-data` | 점검 전체 파이프라인 실행 |
| `GET` | `/pipes/{pipeId}/inspections/latest` | - | 최신 점검 결과 |
| `GET` | `/pipes/{pipeId}/trend` | - | 회차별 위험도·결함 추이 |
| `GET` | `/alerts` | - | 위험도 내림차순 점검 우선순위 |

### 배관 등록

```http
POST /api/v1/pipes
Content-Type: application/json
```

```json
{
  "pipeCode": "A-203",
  "location": "제2공정동 북측",
  "outerDiameterMm": 219.1
}
```

### Baseline 등록

```http
POST /api/v1/pipes/1/baseline?pipeWidthPx=280
Content-Type: multipart/form-data
```

- `image`: 기준 이미지 파일
- `pipeWidthPx`: 기준 이미지에서 배관이 차지하는 폭

### 점검 실행

```http
POST /api/v1/pipes/1/inspections
Content-Type: multipart/form-data
```

- `image`: 현재 점검 이미지
- `capturedAt`: 선택, ISO-8601 날짜·시간

```json
{
  "inspectionId": 1,
  "pipeCode": "A-203",
  "capturedAt": "2026-07-28T20:01:35",
  "alignmentScore": 1.0,
  "alignmentReliable": true,
  "maxRiskScore": 58.1,
  "riskLevel": "WARNING",
  "reportText": "배관 A-203의 부식 위험도는 58.1점(열화 진행 중)입니다.",
  "defects": []
}
```

## Python AI 내부 API

Base URL: `http://localhost:8000`

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/health` | 서비스와 모델 모드 |
| `POST` | `/analyze` | 이미지 정렬과 결함 세그멘테이션 |

### Health

```json
{
  "status": "ok",
  "mockMode": true
}
```

### Analyze 요청

`multipart/form-data`

- `current`: 필수, 현재 이미지
- `baseline`: 선택, 기준 이미지

### Analyze 응답

```json
{
  "aligned": true,
  "alignmentScore": 0.91,
  "homography": [[1.0, 0.0, 0.0], [0.0, 1.0, 0.0], [0.0, 0.0, 1.0]],
  "imageWidth": 1280,
  "imageHeight": 720,
  "mock": true,
  "defects": [
    {
      "className": "corrosion",
      "confidence": 0.88,
      "bbox": [104.0, 83.0, 194.0, 163.0],
      "areaPx": 2410.0,
      "polygon": [[104.0, 83.0], [291.0, 90.0], [298.0, 246.0]]
    }
  ]
}
```

`mock=true`인 결과는 실모델 탐지가 아닙니다.
