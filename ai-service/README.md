# AI Service

Python FastAPI 기반 이미지 정렬·결함 탐지 서비스입니다.

```bash
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
python -m unittest discover -s tests -v
```

구성:

- `main.py`: `/health`, `/analyze`
- `registration.py`: CLAHE, ORB/SIFT, BFMatcher, RANSAC Homography, Perspective Transform
- `detector.py`: YOLO-seg 추론과 결정적 Mock 탐지
- `tests/test_smoke.py`: API·정렬·면적 Smoke Test

환경 변수:

- `PIPE_SEG_MODEL`: YOLO-seg 가중치 경로
- `PIPE_SEG_CONF`: confidence threshold, 기본값 `0.25`
- `PIPE_SEG_IOU`: YOLO NMS IoU threshold, 기본값 `0.45`

`PIPE_SEG_MODEL`이 없거나 로드에 실패하면 `mock=true`로 응답합니다. 모델 가중치와 학습 데이터셋은 Git에 커밋하지 않습니다.
