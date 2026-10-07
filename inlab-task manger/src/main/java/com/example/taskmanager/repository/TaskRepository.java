package com.example.taskmanager.repository;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(TaskPriority priority);

    List<Task> findByCategoryIgnoreCase(String category);

    @Query("SELECT t FROM Task t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:category IS NULL OR LOWER(t.category) = LOWER(:category)) AND " +
           "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Task> searchTasks(@Param("status") TaskStatus status,
                           @Param("priority") TaskPriority priority,
                           @Param("category") String category,
                           @Param("search") String search);

    long countByStatus(TaskStatus status);

    long countByPriorityIn(List<TaskPriority> priorities);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.status NOT IN ('COMPLETED', 'CANCELLED') AND t.dueDate < :currentDate")
    long countOverdueTasks(@Param("currentDate") LocalDate currentDate);
}
