package app.repository;

import app.model.Task;
import java.util.*;

public class InMemoryTaskRepository implements TaskRepository {
    private static final InMemoryTaskRepository INSTANCE = new InMemoryTaskRepository();
    private final Map<Integer,Task> tasks = new LinkedHashMap<>();
    private int nextId=1;
    private InMemoryTaskRepository(){}
    public static InMemoryTaskRepository getInstance(){
        return INSTANCE;
    }

    @Override
    public synchronized Task save(Task task) {
        Task saved = copy(task, nextId++);
        tasks.put(saved.getId(),saved);
        return saved;
    }
    private Task copy(Task t,int id) {
        return new Task.Builder()
                .id(id)
                .title(t.getTitle())
                .description(t.getDescription())
                .completed(t.isCompleted())
                .dueDate(t.getDueDate())
                .priority(t.getPriority())
                .parentId(t.getParentId())
                .createdAt(t.getCreatedAt())
                .build();
    }

    @Override
    public synchronized Optional<Task> findById(int id){
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public synchronized List<Task> findAll(){
        return new ArrayList<>(tasks.values());
    }

    @Override
    public synchronized boolean delete(int id){
        return tasks.remove(id)!=null;
    }

    @Override
    public synchronized Task saveReplacement(Task task) {
        if(!tasks.containsKey(task.getId()))
            throw new IllegalArgumentException("Unknown task ID");
        tasks.put(task.getId(),task);
        return task;
    }
}
