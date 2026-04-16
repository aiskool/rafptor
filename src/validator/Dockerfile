FROM python:3.12-slim AS build
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential libjpeg-dev libpng-dev poppler-utils && \
    rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

FROM python:3.12-slim
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends libjpeg62-turbo libpng16-16 poppler-utils && \
    rm -rf /var/lib/apt/lists/* && \
    useradd -u 10002 -r -s /bin/false rafptor
COPY --from=build /install /usr/local
COPY src/ ./src/
USER rafptor
ENTRYPOINT ["python", "-m", "src"]
