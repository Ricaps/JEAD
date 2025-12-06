from abc import abstractmethod
from typing import Protocol, runtime_checkable


@runtime_checkable
class ShutdownAware(Protocol):
    @abstractmethod
    async def on_shutdown(self) -> None:
        pass
