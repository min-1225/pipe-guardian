"""
[모듈 A] 이미지 정합 (Image Registration)

드론/작업자의 촬영 각도·거리가 매번 달라지므로, 현재 이미지를 배관 기준 이미지의
좌표계로 투영해야 시계열 비교가 성립한다.

  1. CLAHE 로 조명/반사광 보정
  2. ORB(기본) 또는 SIFT 로 특징점 추출
  3. Lowe's ratio test 로 매칭 필터링
  4. RANSAC 기반 Homography 추정
  5. Perspective Transform 으로 워핑
"""
from dataclasses import dataclass
from typing import Optional, List

import cv2
import numpy as np

MIN_MATCH_COUNT = 10
RATIO_TEST = 0.75
RANSAC_REPROJ_THRESHOLD = 5.0


@dataclass
class AlignmentResult:
    image: np.ndarray                 # 정합된(또는 원본) 이미지
    aligned: bool                     # 정합 성공 여부
    score: float                      # inlier / matched (0~1)
    homography: Optional[List[List[float]]]


def apply_clahe(gray: np.ndarray) -> np.ndarray:
    """어두운 플랜트 환경과 금속 반사광 노이즈를 완화한다."""
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    return clahe.apply(gray)


def _to_prepared_gray(image: np.ndarray) -> np.ndarray:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY) if image.ndim == 3 else image
    return apply_clahe(gray)


def _create_detector(method: str):
    if method.lower() == "sift" and hasattr(cv2, "SIFT_create"):
        return cv2.SIFT_create(), cv2.NORM_L2
    return cv2.ORB_create(nfeatures=5000), cv2.NORM_HAMMING


def align(current: np.ndarray, baseline: np.ndarray, method: str = "orb") -> AlignmentResult:
    """current 를 baseline 좌표계로 투영한다. 실패 시 원본을 그대로 반환한다."""
    if baseline is None:
        return AlignmentResult(current, False, 0.0, None)

    detector, norm_type = _create_detector(method)
    gray_cur = _to_prepared_gray(current)
    gray_base = _to_prepared_gray(baseline)

    kp_cur, des_cur = detector.detectAndCompute(gray_cur, None)
    kp_base, des_base = detector.detectAndCompute(gray_base, None)

    if des_cur is None or des_base is None or len(kp_cur) < MIN_MATCH_COUNT:
        return AlignmentResult(current, False, 0.0, None)

    matcher = cv2.BFMatcher(norm_type)
    raw_matches = matcher.knnMatch(des_cur, des_base, k=2)

    good = []
    for pair in raw_matches:
        if len(pair) != 2:
            continue
        m, n = pair
        if m.distance < RATIO_TEST * n.distance:   # Lowe's ratio test
            good.append(m)

    if len(good) < MIN_MATCH_COUNT:
        return AlignmentResult(current, False, 0.0, None)

    src = np.float32([kp_cur[m.queryIdx].pt for m in good]).reshape(-1, 1, 2)
    dst = np.float32([kp_base[m.trainIdx].pt for m in good]).reshape(-1, 1, 2)

    H, mask = cv2.findHomography(src, dst, cv2.RANSAC, RANSAC_REPROJ_THRESHOLD)
    if H is None:
        return AlignmentResult(current, False, 0.0, None)

    inliers = int(mask.sum()) if mask is not None else 0
    score = inliers / len(good) if good else 0.0

    h, w = baseline.shape[:2]
    warped = cv2.warpPerspective(current, H, (w, h))

    return AlignmentResult(warped, True, round(float(score), 3), H.tolist())
