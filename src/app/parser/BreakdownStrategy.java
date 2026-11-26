package app.parser;

import app.model.ParsedTask;

import java.util.List;

public interface BreakdownStrategy {
    List<ParsedTask> breakdown(String taskDescription);
}
