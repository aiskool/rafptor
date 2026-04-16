FROM golang:1.22-alpine AS build
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/rafptor-transfer ./cmd/rafptor-transfer

FROM gcr.io/distroless/static:nonroot
COPY --from=build /out/rafptor-transfer /usr/local/bin/rafptor-transfer
USER nonroot:nonroot
ENTRYPOINT ["/usr/local/bin/rafptor-transfer"]
