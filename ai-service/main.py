"""
비전 AI 추론 서비스 (FastAPI)

Spring Boot 백엔드가 호출하는 무상태 추론 엔드포인트.
정합(모듈 A)과 세그멘테이션(모듈 B)만 담당하며,
시계열 판단·위험도 산정·이력 관리는 전부 백엔드가 소유한다.
"""
from typing import Optional

import cv2
import numpy as np
from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse

import detector
import registration

app = FastAPI(title="PipeGuardian Vision AI Service", version="0.1.0")


def _decode(raw: bytes) -> Optional[np.ndarray]:
    if not raw:
        return None
    buffer = np.frombuffer(raw, dtype=np.uint8)
    return cv2.imdecode(buffer, cv2.IMREAD_COLOR)


@app.get("/health")
def health():
    return {"status": "ok", "mockMode": detector._load_model() is None}


@app.post("/analyze")
async def analyze(
    current: UploadFile = File(...),
    baseline: Optional[UploadFile] = File(None),
):
    current_img = _decode(await current.read())
    if current_img is None:
        return JSONResponse(status_code=400, content={"message": "현재 이미지를 디코딩할 수 없습니다."})

    baseline_img = _decode(await baseline.read()) if baseline is not None else None

    # 모듈 A: 기준 좌표계로 정합
    alignment = registration.align(current_img, baseline_img)

    # 모듈 B: 정합된 이미지에서 결함 탐지
    result = detector.detect(alignment.image)

    h, w = alignment.image.shape[:2]
    return {
        "aligned": alignment.aligned,
        "alignmentScore": alignment.score,
        "homography": alignment.homography,
        "imageWidth": int(w),
        "imageHeight": int(h),
        "mock": result["mock"],
        "defects": result["defects"],
    }
