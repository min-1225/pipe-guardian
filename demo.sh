#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080/api/v1}"
IMAGE_PATH="${1:-}"

if [[ -z "$IMAGE_PATH" || ! -f "$IMAGE_PATH" ]]; then
  echo "사용법: ./demo.sh <배관 이미지 경로>"
  exit 1
fi

echo "1) 데모 배관 목록 확인"
curl --fail --silent --show-error "$API_BASE/pipes"
echo

echo "2) A-203(id=1) Baseline 등록"
curl --fail --silent --show-error \
  -X POST "$API_BASE/pipes/1/baseline?pipeWidthPx=280" \
  -F "image=@$IMAGE_PATH"
echo

echo "3) 점검 실행"
curl --fail --silent --show-error \
  -X POST "$API_BASE/pipes/1/inspections" \
  -F "image=@$IMAGE_PATH"
echo

echo "4) 위험도 우선순위 확인"
curl --fail --silent --show-error "$API_BASE/alerts"
echo
