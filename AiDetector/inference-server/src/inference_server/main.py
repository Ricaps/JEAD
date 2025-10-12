import asyncio
from inference_server.server import grpc_server
from inference_server.configuration.logging import configure_logging


async def setup_and_run():
    configure_logging()
    await grpc_server.run_and_wait()


if __name__ == "__main__":
    import onnxruntime as ort

    # Get list of all available execution providers
    providers = ort.get_available_providers()

    print("Available providers:", providers)
    asyncio.run(setup_and_run())
