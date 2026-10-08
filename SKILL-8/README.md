# Microservices Architecture: Service Discovery, Dynamic Scaling & Load Balancing

This repository contains the complete implementation for a laboratory experiment demonstrating **Spring Boot 3.x**, **Spring Cloud Netflix Eureka**, and **Spring Cloud Gateway** with dynamic service discovery and client-side load balancing.

---

## 🏗️ Architecture

```
                    ┌─────────────────────┐
                    │       CLIENT        │
                    │ curl / Browser      │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │     API GATEWAY     │
                    │      :8080          │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   EUREKA SERVER     │
                    │      :8761          │
                    └──────────┬──────────┘
                               │
              ┌────────────────┴────────────────┐
              │                                 │
              ▼                                 ▼
     ┌──────────────────┐              ┌──────────────────┐
     │ INVENTORY        │              │ INVENTORY        │
     │ INSTANCE 1       │              │ INSTANCE 2       │
     │ :8081            │              │ :8082            │
     └──────────────────┘              └──────────────────┘
                               │
                               ▼
                      ┌──────────────────┐
                      │ INVENTORY        │
                      │ INSTANCE 3       │
                      │ :8083            │
                      └──────────────────┘
```

---

## 🛠️ Technology Stack

- **Java**: OpenJDK 21
- **Spring Boot**: 3.3.5
- **Spring Cloud**: 2023.0.3 (Leyton release train)
- **Eureka Server & Client**: Spring Cloud Netflix Eureka
- **Routing & Load Balancing**: Spring Cloud Gateway + Spring Cloud LoadBalancer
- **Build Tool**: Apache Maven

---

## 📁 Repository Structure

```
eureka-lab/
├── eureka-server/           # Netflix Eureka Service Discovery Server (:8761)
│   ├── pom.xml
│   └── src/main/java/com/example/eurekaserver/EurekaServerApplication.java
│   └── src/main/resources/application.yml
├── inventory-service/        # Multi-instance Inventory Microservice (:8081, :8082, :8083)
│   ├── pom.xml
│   └── src/main/java/com/example/inventoryservice/
│   │   ├── InventoryServiceApplication.java
│   │   └── InventoryController.java
│   └── src/main/resources/application.yml
└── api-gateway/              # Spring Cloud API Gateway with lb:// routing (:8080)
    ├── pom.xml
    └── src/main/java/com/example/apigateway/ApiGatewayApplication.java
    └── src/main/resources/application.yml
```

---

## 🚀 Execution Guide

Run services in separate terminal tabs in this exact order:

### 1. Start Eureka Server (Port 8761)
```bash
cd eureka-lab/eureka-server
mvn spring-boot:run
```
*Access dashboard at: [http://localhost:8761](http://localhost:8761)*

### 2. Start Multiple Inventory Instances
The same application code runs across three ports dynamically:

**Instance 1 (Port 8081):**
```bash
cd eureka-lab/inventory-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

**Instance 2 (Port 8082):**
```bash
cd eureka-lab/inventory-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

**Instance 3 (Port 8083):**
```bash
cd eureka-lab/inventory-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
```

### 3. Start API Gateway (Port 8080)
```bash
cd eureka-lab/api-gateway
mvn spring-boot:run
```

---

## 🧪 Testing & Verification

### Test Direct Endpoints
```bash
curl http://localhost:8081/inventory
curl http://localhost:8082/inventory
curl http://localhost:8083/inventory
```

### Test Load Balancing via API Gateway
Send multiple requests to port `8080`:
```bash
for i in {1..9}; do curl -s http://localhost:8080/inventory; echo ""; done
```
*Traffic rotates across `inventory-8081`, `inventory-8082`, and `inventory-8083`.*

### Failure & Self-Healing Testing
1. **Stop Instance 2**: Terminate terminal running on port `8082` (`Ctrl + C`).
2. **Observe Failover**: Requests via `http://localhost:8080/inventory` seamlessly route between healthy instances `8081` and `8083`.
3. **Restart Instance 2**: Run instance `8082` again; it re-registers with Eureka and automatically rejoins the load balancer rotation.
