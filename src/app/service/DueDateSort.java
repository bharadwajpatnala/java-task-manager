package app.service;
import app.model.Task;
import java.time.LocalDate;
import java.util.Comparator;
public class DueDateSort implements SortStrategy {
    public Comparator<Task> getComparator(){
        return Comparator.comparing(t->t.getDueDate()==null?LocalDate.MAX:LocalDate.parse(t.getDueDate()));
    }
}
