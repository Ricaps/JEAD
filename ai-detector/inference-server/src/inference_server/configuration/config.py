from pydantic_settings import BaseSettings, SettingsConfigDict


class ServerConfig(BaseSettings):
    __ENV_FILES_NAME = [".env"]

    model_config = SettingsConfigDict(env_file=__ENV_FILES_NAME, extra="allow")
    address: str
    port: str
    models_root: str
    models_venv_dir_name: str = ".venv"
    use_gpu: bool = False
    model_command_timeout: int = 120


server_config = ServerConfig()
