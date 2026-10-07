package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequestDTO;
import com.example.taskmanager.dto.TaskResponseDTO;
import com.example.taskmanager.dto.TaskStatusUpdateDTO;
import com.example.taskmanager.dto.TaskSummaryDTO;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;

import java.util.List;

public interface TaskService {

    List<TaskResponseDTO> getAllTasks(TaskStatus status, TaskPriority priority, String category, String search, String sortBy, String direction);

    TaskResponseDTO getTaskById(Long id);

    TaskResponseDTO createTask(TaskRequestDTO requestDTO);

    TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDTO);

    TaskResponseDTO updateTaskStatus(Long id, TaskStatusUpdateDTO statusUpdateDTO);

    void deleteTask(Long id);

    TaskSummaryDTO getTaskSummary();

    List<String> getAllCategories();
}
