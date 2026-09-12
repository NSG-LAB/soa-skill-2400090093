#!/usr/bin/env bash

set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND="$ROOT/backend"
FRONTEND="$ROOT/cors-demo"

BACKEND_LOG="$ROOT/backend.log"
FRONTEND_LOG="$ROOT/frontend.log"

cleanup() {
    echo
    echo "Stopping CORS Lab..."

    if [[ -n "${BACKEND_PID:-}" ]]; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi

    if [[ -n "${FRONTEND_PID:-}" ]]; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi
}

trap cleanup INT TERM EXIT

echo "=========================================="
echo "       SPRING BOOT + REACT CORS LAB"
echo "=========================================="
echo

echo "[1/6] Checking required software..."

for cmd in java mvn npm curl unzip; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "ERROR: $cmd is not installed."
        exit 1
    fi
done

echo "Java : $(java -version 2>&1 | head -n 1)"
echo "Maven: $(mvn -version 2>&1 | head -n 1)"
echo "Node : $(node --version)"
echo "npm  : $(npm --version)"
echo

echo "[2/6] Creating Spring Boot project..."

if [[ ! -f "$BACKEND/pom.xml" ]]; then

    rm -rf "$BACKEND"
    mkdir -p "$BACKEND"

    TEMP_ZIP="$(mktemp)"

    curl -fsSL \
      "https://start.spring.io/starter.zip?type=maven-project&language=java&groupId=com.klu&artifactId=cors-demo-backend&name=cors-demo-backend&packageName=com.klu&packaging=jar&javaVersion=21&dependencies=web" \
      -o "$TEMP_ZIP"

    unzip -q "$TEMP_ZIP" -d "$BACKEND"

    rm -f "$TEMP_ZIP"
fi

echo "[3/6] Creating Spring Boot CORS controller..."

mkdir -p "$BACKEND/src/main/java/com/klu"

cat > "$BACKEND/src/main/java/com/klu/DemoController.java" <<'JAVA'
package com.klu;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class DemoController {

    @GetMapping("/message")
    public String message() {
        return "Welcome to Spring Boot CORS Demo";
    }
}
JAVA

echo "[4/6] Creating React Vite project..."

if [[ ! -f "$FRONTEND/package.json" ]]; then
    rm -rf "$FRONTEND"

    npm create vite@latest cors-demo -- --template react --no-interactive
fi

cd "$FRONTEND"

npm install
npm install axios

cat > "$FRONTEND/src/App.jsx" <<'JS'
import { useEffect, useState } from "react";
import axios from "axios";

function App() {

    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {

        axios
            .get("http://localhost:8080/message")
            .then(response => {
                setMessage(response.data);
            })
            .catch(error => {
                console.error(error);
                setError("Unable to connect to Spring Boot API.");
            });

    }, []);

    return (
        <div>
            <h1>React Vite CORS Demo</h1>

            <h2>{message}</h2>

            {error && <p>{error}</p>}
        </div>
    );
}

export default App;
JS

cat > "$FRONTEND/vite.config.js" <<'JS'
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
    plugins: [react()],
    server: {
        host: "0.0.0.0",
        port: 5173
    }
});
JS

echo "[5/6] Building Spring Boot..."

cd "$BACKEND"

mvn clean package -DskipTests

echo
echo "Starting Spring Boot..."

mvn spring-boot:run \
    -Dspring-boot.run.arguments="--server.port=8080" \
    > "$BACKEND_LOG" 2>&1 &

BACKEND_PID=$!

echo "Waiting for Spring Boot..."

for i in {1..30}; do

    if curl -fsS \
        http://localhost:8080/message \
        >/dev/null 2>&1; then

        echo "Spring Boot is ready."
        break
    fi

    sleep 1

done

if ! curl -fsS \
    http://localhost:8080/message \
    >/dev/null 2>&1; then

    echo
    echo "ERROR: Spring Boot failed to start."
    echo
    echo "Last backend log:"
    tail -30 "$BACKEND_LOG"
    exit 1
fi

echo "[6/6] Starting React Vite..."

cd "$FRONTEND"

npm run dev \
    -- --host 0.0.0.0 --port 5173 \
    > "$FRONTEND_LOG" 2>&1 &

FRONTEND_PID=$!

sleep 3

if ! kill -0 "$FRONTEND_PID" 2>/dev/null; then

    echo "ERROR: React failed to start."
    echo
    echo "Last frontend log:"
    tail -30 "$FRONTEND_LOG"
    exit 1
fi

echo
echo "=========================================="
echo "          CORS LAB IS RUNNING"
echo "=========================================="
echo
echo "React Frontend:"
echo "http://localhost:5173"
echo
echo "Spring Boot API:"
echo "http://localhost:8080/message"
echo
echo "API Response:"
curl -fsS http://localhost:8080/message
echo
echo
echo "Backend log : $BACKEND_LOG"
echo "Frontend log: $FRONTEND_LOG"
echo
echo "Press Ctrl+C to stop both applications."
echo "=========================================="

wait
