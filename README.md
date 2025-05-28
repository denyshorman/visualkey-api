# VisualKey API

The API provides endpoints for Visual Keys NFT collection and token metadata, as well as on-the-fly image generation.  
It serves metadata and images for NFT marketplaces.

## Features

- Query NFT collection metadata
- Query NFT token metadata
- Generate on-the-fly SVG/PNG images for NFTs

## Getting Started

You can build and run the API using Java, Docker, or as a native executable.

### Option 1: Build and Run with Java

**Prerequisites:**  
- Java 23+

**Steps:**
```sh
./gradlew runFatJar
```

### Option 2: Build and Run with Docker (JVM)

1. Build the JAR:
    ```sh
    ./gradlew buildFatJar
    ```
2. Build the Docker image:
    ```sh
    docker build -t visualkey-api .
    ```
3. Run the Docker container:
    ```sh
    docker run -p 8080:8080 visualkey-api
    ```

### Option 3: Build and Run as Native Image

**Prerequisites:**  
- GraalVM JDK 24 with Native Image

**Steps:**
```sh
./gradlew nativeCompile
./build/native/nativeCompile/api
```

### Option 4: Build and Run with Docker (Native)

1. Build the native image:
    ```sh
    ./gradlew nativeCompile
    ```
2. Build the Docker image:
    ```sh
    docker build -f Dockerfile-native -t visualkey-api-native .
    ```
3. Run the Docker container:
    ```sh
    docker run -p 8080:8080 visualkey-api-native
    ```

#### Environment Variables

| Variable         | Description                                 | Default               |
|------------------|---------------------------------------------|-----------------------|
| `HTTP_HOST`      | Host to bind the server                     | 0.0.0.0               |
| `HTTP_PORT`      | Port to bind the server                     | 8080                  |
| `ENVIRONMENT`    | Server environment (`local`, `prod`)        | local                 |
| `BEHIND_PROXY`   | Set to `true` if behind a proxy             | false                 |
| `VISUAL_KEY_URL` | URL of the Visual Key frontend              | http://localhost:4200 |

Example:
```sh
docker run -e HTTP_PORT=80 -p 80:80 visualkey-api
```

## API Reference

The API is documented in [OpenAPI format](src/main/resources/openapi.yml).

When running locally, interactive API documentation is also available at the `/swagger` endpoint.

## License

[MIT](LICENSE)