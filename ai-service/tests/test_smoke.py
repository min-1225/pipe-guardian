import unittest

import cv2
import numpy as np
from fastapi.testclient import TestClient

import detector
import main
import registration


class VisionAiSmokeTest(unittest.TestCase):

    def setUp(self):
        self.client = TestClient(main.app)
        self.image = self._feature_rich_image()
        success, encoded = cv2.imencode(".png", self.image)
        self.assertTrue(success)
        self.image_bytes = encoded.tobytes()

    def test_health_reports_mock_mode_without_weights(self):
        response = self.client.get("/health")

        self.assertEqual(200, response.status_code)
        self.assertEqual("ok", response.json()["status"])
        self.assertTrue(response.json()["mockMode"])

    def test_analyze_returns_deterministic_mock_defects(self):
        files = {"current": ("pipe.png", self.image_bytes, "image/png")}

        first = self.client.post("/analyze", files=files)
        second = self.client.post("/analyze", files=files)

        self.assertEqual(200, first.status_code)
        self.assertTrue(first.json()["mock"])
        self.assertEqual(first.json()["defects"], second.json()["defects"])
        self.assertGreaterEqual(len(first.json()["defects"]), 1)

    def test_identical_images_can_be_aligned(self):
        result = registration.align(self.image, self.image)

        self.assertTrue(result.aligned)
        self.assertGreater(result.score, 0.9)

    def test_polygon_area_uses_opencv_result(self):
        polygon = [[0.0, 0.0], [10.0, 0.0], [10.0, 10.0], [0.0, 10.0]]

        self.assertAlmostEqual(100.0, detector._polygon_area(polygon))

    @staticmethod
    def _feature_rich_image():
        image = np.zeros((300, 400, 3), dtype=np.uint8)
        for x in range(20, 380, 40):
            cv2.line(image, (x, 10), (x, 290), (255, 255, 255), 2)
        for y in range(20, 280, 40):
            cv2.line(image, (10, y), (390, y), (255, 255, 255), 2)
        cv2.circle(image, (200, 150), 70, (0, 200, 255), 5)
        cv2.putText(image, "A-203", (120, 160), cv2.FONT_HERSHEY_SIMPLEX, 1.2, (255, 0, 0), 3)
        return image


if __name__ == "__main__":
    unittest.main()
