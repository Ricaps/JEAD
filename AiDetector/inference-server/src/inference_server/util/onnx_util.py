from aiopath import AsyncPath
from onnxruntime import get_available_providers, preload_dlls, InferenceSession
import logging

_LOGGER = logging.getLogger("onnx_util")
_CPU_PROVIDER = "CPUExecutionProvider"
_GPU_PROVIDER = "CUDAExecutionProvider"


def load_onnx(path: AsyncPath, load_gpu=True):
    _LOGGER.info(f"Available providers: {get_available_providers()}")

    loaded_providers = [_CPU_PROVIDER]
    available_providers = get_available_providers()

    if load_gpu and _GPU_PROVIDER in available_providers:
        loaded_providers.append(_GPU_PROVIDER)

    preload_dlls()

    return InferenceSession(path, providers=loaded_providers)
