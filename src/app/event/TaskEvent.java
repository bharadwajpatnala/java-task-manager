package app.event;

import app.model.Task;

public class TaskEvent {

    private final String eventType;
    private final Task task;

    public TaskEvent(String eventType, Task task) {
        this.eventType = eventType;
        this.task = task;
    }

    public String getEventType() {
        return eventType;
    }

    public Task getTask() {
        return task;
    }
}
