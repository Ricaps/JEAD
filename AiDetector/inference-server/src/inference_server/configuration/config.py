from pydantic_settings import BaseSettings, SettingsConfigDict


class ServerConfig(BaseSettings):
    model_config = SettingsConfigDict(env_file="../../.env")
    address: str
    port: str


server_config = ServerConfig()
