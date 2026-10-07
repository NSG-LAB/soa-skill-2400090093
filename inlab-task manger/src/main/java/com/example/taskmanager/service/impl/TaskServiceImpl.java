package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.TaskRequestDTO;
import com.example.taskmanager.dto.TaskResponseDTO;
import com.example.taskmanager.dto.TaskStatusUpdateDTO;
import com.example.taskmanager.dto.TaskSummaryDTO;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getAllTasks(TaskStatus status, TaskPriority priority, String category, String search, String sortBy, String direction) {
        String cleanCategory = (category != null && !category.isBlank()) ? category.trim() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        List<Task> tasks = taskRepository.searchTasks(status, priority, cleanCategory, cleanSearch);

        // Sorting
        Comparator<Task> comparator;
        String sortField = sortBy != null ? sortBy.toLowerCase() : "createdat";

        switch (sortField) {
            case "duedate":
                comparator = Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "priority":
                comparator = Comparator.comparing(Task::getPriority, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "title":
                comparator = Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
                break;
            case "status":
                comparator = Comparator.comparing(Task::getStatus);
                break;
            case "createdat":
            default:
                comparator = Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
        }

        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }

        return tasks.stream()
                .sorted(comparator)
                .map(TaskResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
        return TaskResponseDTO.fromEntity(task);
    }

    @Override
    public TaskResponseDTO createTask(TaskRequestDTO requestDTO) {
        Task task = new Task(
                requestDTO.getTitle().trim(),
                requestDTO.getDescription() != null ? requestDTO.getDescription().trim() : null,
                requestDTO.getStatus() != null ? requestDTO.getStatus() : TaskStatus.TODO,
                requestDTO.getPriority() != null ? requestDTO.getPriority() : TaskPriority.MEDIUM,
                (requestDTO.getCategory() != null && !requestDTO.getCategory().isBlank()) ? requestDTO.getCategory().trim() : "General",
                requestDTO.getDueDate()
        );

        Task saved = taskRepository.save(task);
        return TaskResponseDTO.fromEntity(saved);
    }

    @Override
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDTO) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        task.setTitle(requestDTO.getTitle().trim());
        task.setDescription(requestDTO.getDescription() != null ? requestDTO.getDescription().trim() : null);
        if (requestDTO.getStatus() != null) {
            task.setStatus(requestDTO.getStatus());
        }
        if (requestDTO.getPriority() != null) {
            task.setPriority(requestDTO.getPriority());
        }
        if (requestDTO.getCategory() != null && !requestDTO.getCategory().isBlank()) {
            task.setCategory(requestDTO.getCategory().trim());
        }
        task.setDueDate(requestDTO.getDueDate());

        Task updated = taskRepository.save(task);
        return TaskResponseDTO.fromEntity(updated);
    }

    @Override
    public TaskResponseDTO updateTaskStatus(Long id, TaskStatusUpdateDTO statusUpdateDTO) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        task.setStatus(statusUpdateDTO.getStatus());
        Task updated = taskRepository.save(task);
        return TaskResponseDTO.fromEntity(updated);
    }

    @Override
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
        taskRepository.delete(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskSummaryDTO getTaskSummary() {
        long total = taskRepository.count();
        long todo = taskRepository.countByStatus(TaskStatus.TODO);
        long inProgress = taskRepository.countByStatus(TaskStatus.IN_PROGRESS);
        long completed = taskRepository.countByStatus(TaskStatus.COMPLETED);
        long cancelled = taskRepository.countByStatus(TaskStatus.CANCELLED);
        long overdue = taskRepository.countOverdueTasks(LocalDate.now());
        long urgentOrHigh = taskRepository.countByPriorityIn(List.of(TaskPriority.HIGH, TaskPriority.URGENT));

        return new TaskSummaryDTO(total, todo, inProgress, completed, cancelled, overdue, urgentOrHigh);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return taskRepository.findAll().stream()
                .map(Task::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}
