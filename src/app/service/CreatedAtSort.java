package app.service;
import app.model.Task;
import java.util.Comparator;
public class CreatedAtSort implements SortStrategy {
    public Comparator<Task> getComparator(){
        return Comparator.comparingLong(Task::getCreatedAt);
    }
}
