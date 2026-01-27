FROM python:3.13
LABEL authors="martin"

ENV WORKER_PATH=./worker.py
ENV MODEL_PORT=5432
ENV MODEL_PORT="0.0.0.0"
RUN mkdir /usr/worker
WORKDIR /usr/worker

COPY requirements.txt ./
RUN pip3 install -r requirements.txt

COPY . .

ENTRYPOINT ["python3", "-u", "socket_worker.py"]

