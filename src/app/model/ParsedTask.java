package app.model;

public class ParsedTask {
    private final String title;
    private final String description;
    private final String dueDate;
    private final String priority;

    public ParsedTask(
            String title,
            String description,
            String dueDate,
            String priority) {

        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDueDate() { return dueDate; }
    public String getPriority() { return priority; }
}
