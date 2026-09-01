class AiServiceError(RuntimeError):
    status_code = 502

    def __init__(self, public_message: str) -> None:
        super().__init__(public_message)
        self.public_message = public_message


class AiConfigurationError(AiServiceError):
    status_code = 503


class AiAuthenticationError(AiServiceError):
    status_code = 502


class AiRateLimitError(AiServiceError):
    status_code = 502


class AiProviderTimeoutError(AiServiceError):
    status_code = 504


class AiProviderTemporaryError(AiServiceError):
    status_code = 502


class AiProviderRequestError(AiServiceError):
    status_code = 502


class AiOutputValidationError(AiServiceError):
    status_code = 502

    def __init__(
        self,
        public_message: str,
        *,
        input_tokens: int | None = None,
        output_tokens: int | None = None,
        total_tokens: int = 0,
        token_usage_estimated: bool = False,
        latency_ms: int = 0,
    ) -> None:
        super().__init__(public_message)
        self.input_tokens = input_tokens
        self.output_tokens = output_tokens
        self.total_tokens = total_tokens
        self.token_usage_estimated = token_usage_estimated
        self.latency_ms = latency_ms
