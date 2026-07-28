"""
[모듈 B] 결함 세그멘테이션 (Vision Segmentation)

YOLO-seg 인스턴스 세그멘테이션으로 배관 결함 5종을 탐지하고
마스크 폴리곤과 픽셀 면적을 산출한다.

모델 가중치가 없으면 Mock 모드로 동작한다. Mock 은 결정적(deterministic)이므로
동일 이미지를 두 번 올려도 같은 결과가 나오며, 백엔드 파이프라인(시계열 비교·
위험도 산정·리포트)을 모델 없이 단독 검증하는 용도로만 사용한다.
"""
import hashlib
import os
from typing import List, Dict, Any, Optional

import cv2
import numpy as np

CLASS_NAMES = ["corrosion", "leak", "crack", "insulation", "peeling"]
CONF_THRESHOLD = float(os.getenv("PIPE_SEG_CONF", "0.25"))
IOU_THRESHOLD = float(os.getenv("PIPE_SEG_IOU", "0.45"))

_model = None
_model_loaded = False


def _load_model():
    """가중치 경로(PIPE_SEG_MODEL)가 설정된 경우에만 Ultralytics 모델을 로드한다."""
    global _model, _model_loaded
    if _model_loaded:
        return _model
    _model_loaded = True

    weights = os.getenv("PIPE_SEG_MODEL")
    if not weights or not os.path.exists(weights):
        return None

    try:
        from ultralytics import YOLO
        _model = YOLO(weights)
    except Exception as exc:  # 의존성 미설치 등
        print(f"[detector] 모델 로드 실패, Mock 모드로 전환합니다: {exc}")
        _model = None
    return _model


def detect(image: np.ndarray) -> Dict[str, Any]:
    model = _load_model()
    if model is None:
        return {"mock": True, "defects": _mock_defects(image)}
    return {"mock": False, "defects": _run_yolo(model, image)}


def _run_yolo(model, image: np.ndarray) -> List[Dict[str, Any]]:
    results = model.predict(image, conf=CONF_THRESHOLD, iou=IOU_THRESHOLD, verbose=False)
    defects: List[Dict[str, Any]] = []

    for result in results:
        if result.masks is None:
            continue
        names = result.names
        for i, mask_xy in enumerate(result.masks.xy):
            box = result.boxes[i]
            cls_id = int(box.cls.item())
            confidence = float(box.conf.item())
            x1, y1, x2, y2 = [float(v) for v in box.xyxy[0].tolist()]

            polygon = [[float(p[0]), float(p[1])] for p in mask_xy]
            defects.append({
                "className": str(names.get(cls_id, cls_id)),
                "confidence": round(confidence, 4),
                "bbox": [x1, y1, x2 - x1, y2 - y1],
                "areaPx": _polygon_area(polygon),
                "polygon": polygon,
            })
    return defects


def _polygon_area(polygon: List[List[float]]) -> float:
    """OpenCV contourArea로 마스크 폴리곤의 픽셀 면적을 계산한다."""
    if len(polygon) < 3:
        return 0.0
    contour = np.asarray(polygon, dtype=np.float32).reshape((-1, 1, 2))
    return float(abs(cv2.contourArea(contour)))


def _mock_defects(image: np.ndarray) -> List[Dict[str, Any]]:
    """이미지 내용 해시를 시드로 사용하는 결정적 더미 탐지 결과."""
    h, w = image.shape[:2]
    seed = int(hashlib.md5(image.tobytes()[:100000]).hexdigest()[:8], 16)
    rng = np.random.default_rng(seed)

    defects = []
    for _ in range(int(rng.integers(1, 4))):
        cls = CLASS_NAMES[int(rng.integers(0, len(CLASS_NAMES)))]
        min_bw, max_bw = max(1, int(w * 0.05)), max(2, int(w * 0.2))
        min_bh, max_bh = max(1, int(h * 0.05)), max(2, int(h * 0.2))
        bw = float(min(w, rng.integers(min_bw, max_bw)))
        bh = float(min(h, rng.integers(min_bh, max_bh)))
        bx = float(rng.integers(0, max(1, int(w - bw))))
        by = float(rng.integers(0, max(1, int(h - bh))))

        polygon = [[bx, by], [bx + bw, by], [bx + bw, by + bh], [bx, by + bh]]
        defects.append({
            "className": cls,
            "confidence": round(float(rng.uniform(0.55, 0.95)), 4),
            "bbox": [bx, by, bw, bh],
            "areaPx": round(bw * bh * 0.7, 2),   # 마스크는 bbox보다 작다고 가정
            "polygon": polygon,
        })
    return defects
