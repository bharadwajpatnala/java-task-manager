package app.service;

import app.model.Task;

public interface TaskFilter {

    boolean matches(Task task);
}
