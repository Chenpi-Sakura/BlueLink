"""用户 Pydantic Schema"""

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class UserInfoResponse(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    user_id: str
