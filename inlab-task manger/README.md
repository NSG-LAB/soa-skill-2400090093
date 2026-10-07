# TaskFlow — In-Lab Task Manager Microservice

An enterprise-grade, Service-Oriented Architecture (SOA) Task Management Microservice built with **Spring Boot 3**, **Spring Data JPA**, **MySQL**, and a modern **Interactive Web UI**.

---

## 🚀 Key Features

- **SOA Microservice Architecture**: Clean layer separation across Controller, Service, DTO, Repository, and Model layers.
- **Full Task Lifecycle Management**:
  - Task Statuses: `TODO`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`
  - Task Priorities: `LOW`, `MEDIUM`, `HIGH`, `URGENT`
  - Due date tracking with automatic overdue calculation.
- **RESTful Endpoints & Validation**:
  - Full CRUD operations with Jakarta Bean Validation (`@NotBlank`, `@Size`, etc.).
  - Fast status transitions via `PATCH /api/tasks/{id}/status`.
  - Rich query parameters for search, filtering, and sorting (`status`, `priority`, `category`, `search`, `sortBy`, `direction`).
  - Real-time summary statistics via `GET /api/tasks/summary`.
- **Modern Interactive Web UI** (`http://localhost:8090/`):
  - Dark & Light mode toggle (with localStorage persistence).
  - Kanban Board view (drag/move between columns: To Do, In Progress, Completed).
  - Data Table / List view with quick toggle.
  - Metrics cards (Total, In Progress, Completed, Overdue).
  - Built-in live REST API Explorer & Tester right inside the UI.
- **Automated Testing Suite**:
  - Automated Bash test script (`./test.sh`) covering positive and negative edge cases.
  - Spring Boot MockMvc Integration tests (`mvn test`).

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 (OpenJDK) |
| **Framework** | Spring Boot 3.5.5 |
| **ORM / Persistence** | Spring Data JPA / Hibernate 6 |
| **Database** | MySQL / MariaDB (Database: `task_db`) |
| **Validation** | Jakarta Bean Validation |
| **Build Tool** | Apache Maven 3.9+ |
| **Frontend** | HTML5, Modern CSS (Glassmorphism, CSS Custom Properties), Vanilla JavaScript |

---

## 🗄️ Database Setup

Create the MySQL database if not already created:

```bash
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS task_db;"
```

Or execute the provided SQL script:

```bash
mysql -u root -proot < create_database.sql
```

---

## 🏃 Running the Application

### 1. Start the Spring Boot Service

```bash
mvn spring-boot:run
```

The service will start on:
👉 **`http://localhost:8090/`** (Access the Interactive UI in your browser)

### 2. Run the Automated API Test Script

In another terminal, run:

```bash
./test.sh
```

### 3. Run JUnit Integration Tests

```bash
mvn test
```

---

## 📡 REST API Reference

Base URL: `http://localhost:8090/api/tasks`

| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `GET` | `/api/tasks` | Get all tasks (supports `status`, `priority`, `category`, `search`, `sortBy`) | `200 OK` |
| `GET` | `/api/tasks/{id}` | Get single task by ID | `200 OK` / `404 Not Found` |
| `POST` | `/api/tasks` | Create a new task | `201 Created` / `400 Bad Request` |
| `PUT` | `/api/tasks/{id}` | Update existing task details | `200 OK` / `404 Not Found` |
| `PATCH` | `/api/tasks/{id}/status` | Update task status | `200 OK` / `404 Not Found` |
| `DELETE` | `/api/tasks/{id}` | Delete task | `204 No Content` / `404 Not Found` |
| `GET` | `/api/tasks/summary` | Get aggregated task statistics | `200 OK` |
| `GET` | `/api/tasks/categories` | Get distinct categories list | `200 OK` |

### Sample Payloads

#### Create Task (`POST /api/tasks`):
```json
{
  "title": "Implement Microservice Security",
  "description": "Add JWT authentication and RBAC roles",
  "status": "TODO",
  "priority": "HIGH",
  "category": "Backend",
  "dueDate": "2026-10-15"
}
```

#### Update Status (`PATCH /api/tasks/{id}/status`):
```json
{
  "status": "COMPLETED"
}
```

---

## 📁 Project Structure

```
inlab-task manger/
├── create_database.sql
├── pom.xml
├── README.md
├── test.sh
├── src/
│   ├── main/
│   │   ├── java/com/example/taskmanager/
│   │   │   ├── TaskManagerApplication.java
│   │   │   ├── config/
│   │   │   │   └── DataInitializer.java
│   │   │   ├── controller/
│   │   │   │   └── TaskController.java
│   │   │   ├── dto/
│   │   │   │   ├── TaskRequestDTO.java
│   │   │   │   ├── TaskResponseDTO.java
│   │   │   │   ├── TaskStatusUpdateDTO.java
│   │   │   │   └── TaskSummaryDTO.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── model/
│   │   │   │   ├── Task.java
│   │   │   │   ├── TaskPriority.java
│   │   │   │   └── TaskStatus.java
│   │   │   ├── repository/
│   │   │   │   └── TaskRepository.java
│   │   │   └── service/
│   │   │       ├── TaskService.java
│   │   │       └── impl/
│   │   │           └── TaskServiceImpl.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   │           ├── index.html
│   │           ├── style.css
│   │           └── app.js
│   └── test/
│       └── java/com/example/taskmanager/
│           └── TaskControllerTest.java
```
