import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from app import config


class ConfigTests(unittest.TestCase):
    def test_load_dotenv_uses_service_env_file_when_started_from_other_directory(self) -> None:
        expected_path = Path(config.__file__).resolve().parents[1] / ".env"

        original_cwd = os.getcwd()
        with tempfile.TemporaryDirectory() as temp_dir:
            os.chdir(temp_dir)
            try:
                with patch("dotenv.load_dotenv") as load_dotenv:
                    config._load_dotenv()
            finally:
                os.chdir(original_cwd)

        args, kwargs = load_dotenv.call_args
        actual_path = kwargs.get("dotenv_path") or args[0]
        self.assertEqual(Path(actual_path), expected_path)


if __name__ == "__main__":
    unittest.main()
