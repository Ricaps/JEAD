from pydantic_settings import BaseSettings, SettingsConfigDict


class ServerConfig(BaseSettings):
    __ENV_FILES_NAME = [".env"]

    model_config = SettingsConfigDict(env_file=__ENV_FILES_NAME)
    address: str
    port: str
    model_config_path: str = "models/models.yaml"
    docker_env: bool = False


server_config = ServerConfig()
