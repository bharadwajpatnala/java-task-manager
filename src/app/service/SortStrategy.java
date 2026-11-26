package app.service;

import app.model.Task;
import java.util.Comparator;

public interface SortStrategy {

    Comparator<Task> getComparator();
}
