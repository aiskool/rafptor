FROM python:3.12-slim AS build
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential libfreetype6-dev libjpeg-dev libpng-dev && \
    rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

FROM python:3.12-slim
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends libfreetype6 libjpeg62-turbo libpng16-16 && \
    rm -rf /var/lib/apt/lists/* && \
    useradd -u 10001 -r -s /bin/false rafptor
COPY --from=build /install /usr/local
COPY src/ ./src/
USER rafptor
ENTRYPOINT ["python", "-m", "src"]
