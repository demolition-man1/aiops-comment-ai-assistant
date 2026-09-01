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

    def test_settings_supports_langchain_negative_reply_engine(self) -> None:
        with patch.dict(
            os.environ,
            {"AI_NEGATIVE_REPLY_ENGINE": "legacy", "AI_MAX_RETRIES": "1"},
            clear=False,
        ):
            settings = config.Settings()

        self.assertEqual(settings.ai_negative_reply_engine, "legacy")
        self.assertEqual(settings.ai_max_retries, 1)

    def test_default_deepseek_model_matches_the_supported_configuration_profile(self) -> None:
        self.assertEqual(config.Settings.__dataclass_fields__["ai_model"].default, "deepseek-v4-flash")

    def test_settings_default_to_fast_negative_reply_profile(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            settings = config.Settings()

        self.assertFalse(settings.ai_negative_reply_thinking_enabled)
        self.assertEqual(settings.ai_negative_reply_max_tokens, 320)
        self.assertEqual(settings.rag_reply_top_k, 2)
        self.assertEqual(settings.rag_reply_max_context_chars, 1800)

    def test_settings_rejects_unknown_negative_reply_engine(self) -> None:
        with patch.dict(os.environ, {"AI_NEGATIVE_REPLY_ENGINE": "unsupported"}, clear=False):
            with self.assertRaisesRegex(ValueError, "AI_NEGATIVE_REPLY_ENGINE"):
                config.Settings()

    def test_settings_rejects_negative_ai_retry_count(self) -> None:
        with patch.dict(os.environ, {"AI_MAX_RETRIES": "-1"}, clear=False):
            with self.assertRaisesRegex(ValueError, "AI_MAX_RETRIES"):
                config.Settings()

    def test_settings_supports_comment_ai_shadow_limits(self) -> None:
        with patch.dict(
            os.environ,
            {
                "COMMENT_AI_SHADOW_DEFAULT_SAMPLE_SIZE": "24",
                "COMMENT_AI_SHADOW_MAX_SAMPLE_SIZE": "80",
                "COMMENT_AI_SHADOW_DEFAULT_MAX_TOTAL_TOKENS": "5000",
                "COMMENT_AI_SHADOW_MAX_TOTAL_TOKENS": "9000",
            },
            clear=False,
        ):
            settings = config.Settings()

        self.assertEqual(settings.comment_ai_shadow_default_sample_size, 24)
        self.assertEqual(settings.comment_ai_shadow_max_sample_size, 80)
        self.assertEqual(settings.comment_ai_shadow_default_max_total_tokens, 5000)
        self.assertEqual(settings.comment_ai_shadow_max_total_tokens, 9000)

    def test_settings_rejects_inconsistent_comment_ai_shadow_limits(self) -> None:
        with patch.dict(
            os.environ,
            {
                "COMMENT_AI_SHADOW_DEFAULT_SAMPLE_SIZE": "61",
                "COMMENT_AI_SHADOW_MAX_SAMPLE_SIZE": "60",
            },
            clear=False,
        ):
            with self.assertRaisesRegex(ValueError, "COMMENT_AI_SHADOW_MAX_SAMPLE_SIZE"):
                config.Settings()


if __name__ == "__main__":
    unittest.main()
