import os
from pathlib import Path
from inference_server.configuration.config import ServerConfig
from aiopath import AsyncPath


class PathResolver:
    REQUIREMENTS_FILE = "requirements.txt"
    REQUIREMENTS_GPU_FILE = "requirements.gpu.txt"
    PIP_SCRIPT_NAME = "get-pip.py"

    def __init__(self, server_config: ServerConfig) -> None:
        self.models_root = AsyncPath(server_config.models_root)
        self.venv_dir_name: str = server_config.models_venv_dir_name
        self.use_gpu = server_config.use_gpu

        self._venv_path = self.models_root / self.venv_dir_name
        self._python_path = self.__get_os_specific_venv_folder(self._venv_path, "python")
        self._pip_path = self.__get_os_specific_venv_folder(self._venv_path, "pip")
        self._requirements_path = self.models_root / self.__get_requirements_file_name()
        src_root = Path(__file__).resolve().parents[2]
        self._socket_worker_path: AsyncPath = AsyncPath(
            src_root / "inference_server" / "module_worker" / "socket_worker.py"
        )
     
    def get_python_paths(self) -> tuple[AsyncPath, AsyncPath, AsyncPath]:
        """
        Returns paths to venv folder, Python executable and pip executable
        """
        return self._venv_path, self._python_path, self._pip_path
    
    def get_pip_script_path(self):
        """
        Gets get-pip.py script path in venv folder
        """
        return self._venv_path / PathResolver.PIP_SCRIPT_NAME

    def get_requirements_path(self) -> AsyncPath:
        """
        Gets path to the requirements.txt (requirements.gpu.txt)
        """
        return self._requirements_path
    
    def get_socket_worker_path(self) -> AsyncPath:
        """
        Gets path to the socket worker
        """

        return self._socket_worker_path

    @staticmethod
    def __get_os_specific_venv_folder(venv_path: AsyncPath, executable_name: str) -> AsyncPath:
        if os.name == "nt":
            return venv_path / "Scripts" / (executable_name + ".exe")
        
        return venv_path / "bin" / executable_name
    
    def __get_requirements_file_name(self) -> str:
        return PathResolver.REQUIREMENTS_GPU_FILE if self.use_gpu else PathResolver.REQUIREMENTS_FILE
