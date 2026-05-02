from inference_server.configuration.config import ServerConfig


def create_test_server_config(
    *, address: str = "0.0.0.0", port: str = "8888", models_root: str = "tests/resources/model_root"
) -> ServerConfig:
    return ServerConfig(address=address, port=port, models_root=models_root)
