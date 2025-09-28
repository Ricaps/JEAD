from pydantic_settings import BaseSettings, SettingsConfigDict


class ServerConfig(BaseSettings):
    __ENV_FILES_NAME = [".env", ".env.test"]

    model_config = SettingsConfigDict(env_file=__ENV_FILES_NAME)
    address: str
    port: str
    models_root: str


server_config = ServerConfig()
