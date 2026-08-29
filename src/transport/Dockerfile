FROM golang:1.26-alpine AS build
WORKDIR /src
COPY go.mod go.sum* ./
RUN go mod download 2>/dev/null || true
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/rafptor-receiver ./cmd/rafptor-receiver

FROM gcr.io/distroless/static:nonroot
LABEL org.opencontainers.image.source="https://github.com/aiskool/rafptor"
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.vendor="Rafptor"
COPY --from=build /out/rafptor-receiver /usr/local/bin/rafptor-receiver
USER nonroot:nonroot
EXPOSE 2222
ENTRYPOINT ["/usr/local/bin/rafptor-receiver"]
