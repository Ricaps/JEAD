from pydantic_settings import BaseSettings, SettingsConfigDict


class ServerConfig(BaseSettings):
    __ENV_FILES_NAME = [".env"]

    model_config = SettingsConfigDict(env_file=__ENV_FILES_NAME)
    address: str
    port: str
    model_config_path: str = "models/models.yaml"
    models_host: str = "127.0.0.1"


server_config = ServerConfig()
