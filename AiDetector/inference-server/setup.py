from setuptools import setup
from setuptools.command.build_py import build_py as _build_py
import subprocess

class build_py(_build_py):
    def run(self):
        print("aaaa")
        subprocess.check_call([
            "source", "scripts/generate-server.sh"
        ])

        super().run()

setup()