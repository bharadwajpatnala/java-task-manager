package app.parser;

import app.model.ParsedTask;

public class RuleBasedTaskParser implements TaskParser{
    @Override
    public ParsedTask parse(String text, String today) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Task description cannot be empty");
        }

        String title = text.trim();

        String lower = title.toLowerCase();

        if (lower.startsWith("remind me to ")) {
            title = title.substring(13).trim();
        }

        if (!title.isEmpty()) {
            title = Character.toUpperCase(title.charAt(0))
                    + title.substring(1);
        }

        String dueDate = null;

        if (lower.contains("tomorrow")) {
            dueDate = java.time.LocalDate.parse(today)
                    .plusDays(1).toString();
        }

        if (lower.contains("every month")) {
            // Recurrence is not represented by the current Task model.
            // Keep the date null until recurrence is implemented.
        }

        return new ParsedTask(
                title,
                "Created from natural language",
                dueDate,
                "MEDIUM");
    }
}
