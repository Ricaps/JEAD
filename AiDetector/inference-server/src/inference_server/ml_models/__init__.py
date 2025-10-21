from inference_server.ml_models.comments_model import CommentsModel
from inference_server.ml_models.test_model import TestModel

model_type_registry = {"comments-model": CommentsModel, "test-model": TestModel}
