import pytest

from app.ai.chains.report import ReportChain
from app.ai.errors import AiOutputValidationError
from app.ai.results import AiInvocationResult
from app.ai.schemas import OperationReportOutput


def _payload() -> dict[str, str]:
    return {
        "reportTitle": "Product Operations Report",
        "consumerPainPoints": "Surface defects affect the customer experience.",
        "productAdvantages": "Core satisfaction remains stable.",
        "productDisadvantages": "Quality variation creates trust risk.",
        "operationSuggestions": "Improve inspection and packaging.",
        "copywritingSuggestions": "Describe verified quality controls only.",
        "serviceSuggestions": "Offer support according to the actual store policy.",
        "fullReport": "Validated report body.",
    }


class FakeProvider:
    def __init__(self) -> None:
        self.repair_prompts: list[object] = []

    def invoke_structured(self, _prompt: object, _schema: type[OperationReportOutput]) -> AiInvocationResult[OperationReportOutput]:
        raise AiOutputValidationError("invalid structured report", total_tokens=17)

    def invoke_text(self, prompt: object, *, max_retries: int | None = None) -> AiInvocationResult[str]:
        self.repair_prompts.append((prompt, max_retries))
        return AiInvocationResult(
            value='{"reportTitle":"Product Operations Report","consumerPainPoints":"Surface defects affect the customer experience.","productAdvantages":"Core satisfaction remains stable.","productDisadvantages":"Quality variation creates trust risk.","operationSuggestions":"Improve inspection and packaging.","copywritingSuggestions":"Describe verified quality controls only.","serviceSuggestions":"Offer support according to the actual store policy.","fullReport":"Validated report body."}',
            model_name="deepseek-v4-flash",
            input_tokens=9,
            output_tokens=8,
            total_tokens=17,
            token_usage_estimated=False,
        )


def test_report_chain_rejects_malformed_json_escape_from_raw_model_output() -> None:
    malformed = '{"reportTitle":"Report","consumerPainPoints":"Pain","productAdvantages":"Advantage","productDisadvantages":"Risk","operationSuggestions":"Action","copywritingSuggestions":"Copy","serviceSuggestions":"Service","fullReport":"First section.\\3. Broken section."}'

    with pytest.raises(AiOutputValidationError):
        ReportChain.parse_output(malformed)


def test_report_chain_normalizes_string_list_sections_to_text() -> None:
    payload = _payload()
    payload["consumerPainPoints"] = ["Surface defects reduce trust.", "Delivery handling can worsen scratches."]
    payload["productAdvantages"] = ["Core product use remains acceptable."]

    output = OperationReportOutput.model_validate(payload)

    assert output.consumer_pain_points == (
        "1. Surface defects reduce trust.\n2. Delivery handling can worsen scratches."
    )
    assert output.product_advantages == "1. Core product use remains acceptable."


def test_report_chain_repairs_invalid_structured_response_once() -> None:
    provider = FakeProvider()
    result = ReportChain(provider).generate("Generate a JSON report.")

    assert result.value.report_title == "Product Operations Report"
    assert result.total_tokens == 34
    assert provider.repair_prompts[0][1] == 0
