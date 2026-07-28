# Contributing

PipeGuardian은 Pull Request 중심으로 협업합니다.

## 권한과 참여 방식

- 저장소는 Public이므로 누구나 열람·포크·Pull Request 생성이 가능합니다.
- 원본 저장소에 직접 브랜치를 푸시하려면 `min-1225`가 GitHub 사용자명을 Collaborator로 초대해야 합니다.
- `main`에는 직접 푸시하지 않고 Pull Request와 리뷰를 거칩니다.

## 브랜치 규칙

| 종류 | 예시 |
|---|---|
| 기능 | `feat/inspection-upload` |
| 버그 수정 | `fix/risk-score-null` |
| 문서 | `docs/api-contract` |
| 리팩터링 | `refactor/ai-client` |
| 테스트 | `test/temporal-engine` |

## 커밋 메시지

Conventional Commits 형식을 사용합니다.

```text
feat: 점검 이미지 업로드 API 추가
fix: 기준 이미지 미등록 시 오류 처리
docs: 로컬 실행 방법 보완
test: 신규 누유 Critical 규칙 검증
chore: 개발 환경 설정 정리
```

## Pull Request 체크리스트

- 담당 Issue를 연결했습니다.
- 변경 범위가 한 가지 목적에 집중되어 있습니다.
- 새 기능 또는 버그 수정에 필요한 테스트를 추가했습니다.
- API 또는 환경 변수가 바뀌면 관련 문서를 수정했습니다.
- 비밀키, 개인정보, 대용량 데이터, 모델 가중치가 포함되지 않았습니다.
- 로컬 테스트와 빌드 결과를 Pull Request에 기록했습니다.

## 개발 전 합의가 필요한 항목

- 데이터셋 출처와 사용 조건
- YOLOv8-seg와 YOLOv11-seg 중 1차 모델
- DB와 이미지 스토리지
- 인증 범위
- 위험도 정규화 공식과 임계값
- 공개 라이선스
