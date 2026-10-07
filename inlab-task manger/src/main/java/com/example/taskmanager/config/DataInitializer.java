package com.example.taskmanager.config;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final TaskRepository taskRepository;

    public DataInitializer(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void run(String... args) {
        if (taskRepository.count() == 0) {
            List<Task> initialTasks = List.of(
                    new Task(
                            "Design SOA Task Manager Microservice",
                            "Define architecture, domain entities, REST API contracts, and database schema for in-lab assignment.",
                            TaskStatus.COMPLETED,
                            TaskPriority.HIGH,
                            "Architecture",
                            LocalDate.now().minusDays(1)
                    ),
                    new Task(
                            "Implement REST CRUD Endpoints",
                            "Create controller, DTOs, service layer, and validation for tasks resource.",
                            TaskStatus.IN_PROGRESS,
                            TaskPriority.URGENT,
                            "Backend",
                            LocalDate.now().plusDays(2)
                    ),
                    new Task(
                            "Build Interactive Kanban & List UI",
                            "Develop responsive modern dashboard with dark/light theme, filters, and drag/drop cards.",
                            TaskStatus.IN_PROGRESS,
                            TaskPriority.HIGH,
                            "Frontend",
                            LocalDate.now().plusDays(3)
                    ),
                    new Task(
                            "Write Automated Integration Tests",
                            "Add curl test script and Spring MockMvc integration tests for all REST endpoints.",
                            TaskStatus.TODO,
                            TaskPriority.MEDIUM,
                            "Testing",
                            LocalDate.now().plusDays(5)
                    ),
                    new Task(
                            "Prepare In-Lab Viva Presentation",
                            "Document microservices principles, data flow, validation, and error handling mechanisms.",
                            TaskStatus.TODO,
                            TaskPriority.LOW,
                            "Documentation",
                            LocalDate.now().plusDays(7)
                    )
            );

            taskRepository.saveAll(initialTasks);
        }
    }
}
