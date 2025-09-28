from pydantic_settings import BaseSettings, SettingsConfigDict


class ServerConfig(BaseSettings):
    __ENV_FILE_NAME = ".env"

    model_config = SettingsConfigDict(env_file=__ENV_FILE_NAME)
    address: str
    port: str
    models_root: str


server_config = ServerConfig()
