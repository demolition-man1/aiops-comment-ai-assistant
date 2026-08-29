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
