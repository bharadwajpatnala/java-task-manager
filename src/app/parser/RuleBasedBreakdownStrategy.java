package app.parser;

import app.model.ParsedTask;
import java.util.*;

public class RuleBasedBreakdownStrategy
        implements BreakdownStrategy {

    @Override
    public List<ParsedTask> breakdown(String description) {

        List<ParsedTask> result = new ArrayList<>();

        String[] parts = description.split("[,]|\\band\\b");

        for (String part : parts) {

            String value = part.trim();

            if (!value.isEmpty()) {
                result.add(new ParsedTask(
                        value,
                        "Subtask of parent task",
                        null,
                        "MEDIUM"));
            }
        }

        return result;
    }
}
