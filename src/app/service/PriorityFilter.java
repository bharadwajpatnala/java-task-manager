package app.service;

import app.model.Task;
import app.model.Priority;

public class PriorityFilter implements TaskFilter {

    private final Priority priority;

    public PriorityFilter(Priority priority) {
        this.priority = priority;
    }

    @Override
    public boolean matches(Task task) {
        return task.getPriority() == priority;
    }
}
