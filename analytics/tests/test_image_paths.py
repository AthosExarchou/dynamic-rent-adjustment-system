"""Tests for offline image path normalization used by scraper pushes."""

# Imported Libraries
import tempfile
import unittest
from pathlib import Path
from analytics.scrapers.image_paths import to_app_image_paths


class TestToAppImagePaths(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.base = Path(self.temp_dir.name)

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_normalizes_absolute_and_relative_paths_inside_base(self):
        raw = "|".join([
            str(self.base / "data/images/123/abc.jpg"),
            "data/images/456/def.webp",
        ])

        self.assertEqual(
            to_app_image_paths(raw, self.base),
            ["data/images/123/abc.jpg", "data/images/456/def.webp"]
        )

    def test_drops_missing_sentinel_and_paths_outside_base(self):
        raw = "|".join(["nan", "None", "/etc/passwd", ""])

        self.assertEqual(to_app_image_paths(raw, self.base), [])


if __name__ == "__main__":
    unittest.main()
