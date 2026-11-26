package app.parser;

import app.model.ParsedTask;

public interface TaskParser {

    ParsedTask parse(String text, String today);
}
