FROM python:3.13
LABEL authors="martin"

RUN mkdir /usr/worker
WORKDIR /usr/worker

COPY requirements.txt ./
RUN pip3 install -r requirements.txt

COPY worker.py ./
COPY socket_worker.py ./

ENTRYPOINT ["python3", "-u", "socket_worker.py", "./worker.py", "0.0.0.0", "5432"]