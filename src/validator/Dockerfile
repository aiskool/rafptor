FROM python:3.12-slim AS build
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential libjpeg-dev libpng-dev && \
    rm -rf /var/lib/apt/lists/*
COPY requirements.txt ./
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

FROM python:3.12-slim
LABEL org.opencontainers.image.source="https://github.com/aiskool/rafptor"
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.vendor="Rafptor"
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends libjpeg62-turbo libpng16-16 && \
    rm -rf /var/lib/apt/lists/* && \
    useradd -u 10002 -r -s /bin/false rafptor
COPY --from=build /install /usr/local
COPY pyproject.toml README.md ./
COPY src/ ./src/
RUN pip install --no-cache-dir --no-deps .
USER rafptor
ENTRYPOINT ["rafptor-qa"]
