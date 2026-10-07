package com.example.taskmanager.dto;

public class TaskSummaryDTO {

    private long total;
    private long todo;
    private long inProgress;
    private long completed;
    private long cancelled;
    private long overdue;
    private long urgentOrHigh;

    public TaskSummaryDTO() {
    }

    public TaskSummaryDTO(long total, long todo, long inProgress, long completed, long cancelled, long overdue, long urgentOrHigh) {
        this.total = total;
        this.todo = todo;
        this.inProgress = inProgress;
        this.completed = completed;
        this.cancelled = cancelled;
        this.overdue = overdue;
        this.urgentOrHigh = urgentOrHigh;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTodo() {
        return todo;
    }

    public void setTodo(long todo) {
        this.todo = todo;
    }

    public long getInProgress() {
        return inProgress;
    }

    public void setInProgress(long inProgress) {
        this.inProgress = inProgress;
    }

    public long getCompleted() {
        return completed;
    }

    public void setCompleted(long completed) {
        this.completed = completed;
    }

    public long getCancelled() {
        return cancelled;
    }

    public void setCancelled(long cancelled) {
        this.cancelled = cancelled;
    }

    public long getOverdue() {
        return overdue;
    }

    public void setOverdue(long overdue) {
        this.overdue = overdue;
    }

    public long getUrgentOrHigh() {
        return urgentOrHigh;
    }

    public void setUrgentOrHigh(long urgentOrHigh) {
        this.urgentOrHigh = urgentOrHigh;
    }
}
