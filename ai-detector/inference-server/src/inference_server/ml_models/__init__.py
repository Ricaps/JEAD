from inference_server.ml_models.comments_model import CommentsModel
from inference_server.ml_models.god_di_model import GodDiModel
from inference_server.ml_models.multiservice_model import MultiServiceModel
from inference_server.ml_models.test_model import TestModel

model_type_registry = {
    "comments-model": CommentsModel,
    "god-di-model": GodDiModel,
    "test-model": TestModel,
    "multi-service-model": MultiServiceModel,
}
