from pydantic import BaseModel, ConfigDict, Field


class NegativeReplyOutput(BaseModel):
    model_config = ConfigDict(populate_by_name=True, str_strip_whitespace=True)

    reply_content: str = Field(alias="replyContent", min_length=1, max_length=2_000)
