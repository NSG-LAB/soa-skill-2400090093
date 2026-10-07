package com.example.taskmanager;

import com.example.taskmanager.dto.TaskRequestDTO;
import com.example.taskmanager.dto.TaskStatusUpdateDTO;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void testCreateTaskSuccess() throws Exception {
        TaskRequestDTO requestDTO = new TaskRequestDTO(
                "Complete Unit Testing",
                "Ensure all service methods and controller endpoints are covered",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                "Testing",
                LocalDate.now().plusDays(3)
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Complete Unit Testing")))
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.category", is("Testing")));
    }

    @Test
    void testCreateTaskValidationFailure() throws Exception {
        TaskRequestDTO invalidDTO = new TaskRequestDTO(
                "", // Blank title violates @NotBlank and @Size
                "Description",
                TaskStatus.TODO,
                TaskPriority.LOW,
                "General",
                null
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.errors.title", notNullValue()));
    }

    @Test
    void testGetAllTasksAndFilter() throws Exception {
        TaskRequestDTO task1 = new TaskRequestDTO("Task 1", "Desc 1", TaskStatus.TODO, TaskPriority.LOW, "Dev", LocalDate.now());
        TaskRequestDTO task2 = new TaskRequestDTO("Task 2", "Desc 2", TaskStatus.COMPLETED, TaskPriority.HIGH, "Ops", LocalDate.now());

        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(task1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(task2))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/tasks").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Task 2")));
    }

    @Test
    void testUpdateTaskStatus() throws Exception {
        TaskRequestDTO initial = new TaskRequestDTO("Draft Documentation", "Write docs", TaskStatus.TODO, TaskPriority.MEDIUM, "Docs", null);
        String response = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        TaskStatusUpdateDTO updateDTO = new TaskStatusUpdateDTO(TaskStatus.IN_PROGRESS);

        mockMvc.perform(patch("/api/tasks/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.intValue())))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    void testDeleteTask() throws Exception {
        TaskRequestDTO task = new TaskRequestDTO("Task to delete", "Will be removed", TaskStatus.TODO, TaskPriority.LOW, "Misc", null);
        String response = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/tasks/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void testGetSummary() throws Exception {
        TaskRequestDTO task1 = new TaskRequestDTO("Task Alpha", "Alpha", TaskStatus.TODO, TaskPriority.URGENT, "Dev", LocalDate.now().minusDays(2));
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(task1))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.todo", is(1)))
                .andExpect(jsonPath("$.overdue", is(1)))
                .andExpect(jsonPath("$.urgentOrHigh", is(1)));
    }
}
