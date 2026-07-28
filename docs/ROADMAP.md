# MVP Roadmap

## Milestone 0 — 협업 기반

- [x] 문제 정의와 MVP 범위 정리
- [x] Spring Boot와 Python AI 서비스 분리
- [x] API 계약 초안
- [x] 브랜치·커밋·PR 규칙
- [ ] 팀원 Collaborator 초대
- [ ] 라이선스 선택

## Milestone 1 — 전체 파이프라인 골격

- [x] Spring Boot 프로젝트 생성과 빌드 통과
- [x] FastAPI 프로젝트 생성과 health check
- [ ] Docker Compose로 DB·백엔드·AI 서비스 연결
- [x] Mock AI 응답으로 업로드 → 비교 → 위험도 → 리포트 연결
- [x] 최소 E2E 데모 스크립트

## Milestone 2 — 이미지 정렬

- [x] Baseline 이미지 정책 구현
- [x] SIFT/ORB 특징점 매칭
- [x] RANSAC Homography와 Perspective Transform
- [x] 정렬 confidence와 실패 처리
- [ ] 정렬 결과 시각화와 테스트 데이터

## Milestone 3 — 비전 AI

- [ ] 공개 데이터셋 후보와 라이선스 검토
- [ ] 5개 결함 클래스 라벨링 기준
- [ ] YOLO-seg 학습·검증 스크립트
- [x] Mock/실모델 모드 분리
- [ ] 모델 버전과 성능 지표 기록

## Milestone 4 — 시계열·위험도

- [x] Bounding Box IoU 기반 결함 instance 매칭
- [x] 면적 변화량
- [ ] 위험도 정규화 방식과 임계값
- [x] 신규 누유 `Critical` 정책
- [x] 검증표와 단위 테스트

## Milestone 5 — 데모

- [ ] 과거·현재 Split View
- [ ] 결함 Mask와 Heatmap Overlay
- [x] 위험도와 우선순위 화면
- [x] 자동 점검 리포트
- [ ] 발표용 시나리오와 샘플 데이터

## 권장 작업 분담

| 트랙 | 담당 범위 |
|---|---|
| Backend | 도메인, DB, API, AI client, 위험도, 리포트 |
| Vision AI | 정렬, 세그멘테이션, 학습, 추론 API |
| Frontend/Demo | 업로드, 비교 뷰, Overlay, 차트 |
| Data/QA | 데이터셋, 라벨 규칙, E2E, 성능·안전 검증 |
