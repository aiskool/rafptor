.PHONY: all build test test-race test-integration lint docker-build docker-build-sender cross-compile clean

all: build

build:
	go build -trimpath -ldflags="-s -w" ./...

test:
	go test ./... -cover

test-race:
	go test ./... -race -cover

test-integration:
	go test -tags=integration ./internal/transfer/ -race -v

lint:
	go vet ./...

docker-build:
	docker build -t rafptor/receiver:dev -f Dockerfile .

docker-build-sender:
	docker build -t rafptor/sender:dev -f Dockerfile.sender .

cross-compile:
	@mkdir -p dist
	GOOS=linux   GOARCH=amd64  go build -trimpath -ldflags="-s -w" -o dist/rafptor-receiver-linux-amd64 ./cmd/rafptor-receiver
	GOOS=linux   GOARCH=arm64  go build -trimpath -ldflags="-s -w" -o dist/rafptor-receiver-linux-arm64 ./cmd/rafptor-receiver
	# IBM i (PASE) target — best-effort; produces a binary runnable on PASE.
	GOOS=aix     GOARCH=ppc64  go build -trimpath -ldflags="-s -w" -o dist/rafptor-sender-aix-ppc64 ./cmd/rafptor-sender
	# z/OS USS target is not officially supported by upstream Go; a dedicated
	# zgo toolchain or IBM SDK for z/OS is required — see README.

clean:
	rm -rf dist/
