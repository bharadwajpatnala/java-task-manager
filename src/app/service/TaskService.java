package app.service;

import app.event.*;
import app.exception.*;
import app.model.*;
import app.parser.*;
import app.repository.TaskRepository;
import app.util.DateUtil;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class TaskService {
    private final TaskRepository repository;
    private final List<TaskEventListener> listeners=new CopyOnWriteArrayList<>();
    private final TaskParser parser;
    private final BreakdownStrategy breakdownStrategy;

    public TaskService(TaskRepository repository) {
        this(repository,new RuleBasedTaskParser(),new RuleBasedBreakdownStrategy());
    }

    public TaskService(TaskRepository repository,TaskParser parser,BreakdownStrategy breakdownStrategy) {
        this.repository=Objects.requireNonNull(repository);
        this.parser=Objects.requireNonNull(parser);
        this.breakdownStrategy=Objects.requireNonNull(breakdownStrategy);
    }

    private List<Map<String,String>> validate(TaskRequest r) {
        List<Map<String,String>> errors=new ArrayList<>();
        if(r==null) {
            errors.add(issue("request","must not be null"));
            return errors;
        }
        if(blank(r.getTitle())) errors.add(issue("title","must not be empty or blank"));
        if(blank(r.getDescription())) errors.add(issue("description","must not be empty or blank"));
        if(!"true".equals(r.getCompleted()) && !"false".equals(r.getCompleted()))
            errors.add(issue("completed","must be exactly true or false"));
        if(!DateUtil.isValidDate(r.getDueDate()))
            errors.add(issue("dueDate","must be a real calendar date in YYYY-MM-DD format"));
        if(!blank(r.getPriority()) && !isPriority(r.getPriority()))
            errors.add(issue("priority","must be LOW, MEDIUM or HIGH"));
        return errors;
    }

    private boolean blank(String s){return s==null || s.isBlank();}

    private boolean isPriority(String s)
    {
        try{
            Priority.valueOf(s);
            return true;
        }catch(Exception e)
        {
            return false;
        }
    }

    private Map<String,String> issue(String f,String i)
    {
        Map<String,String> m=new LinkedHashMap<>();
        m.put("field",f);
        m.put("issue",i);
        return m;
    }

    private void validateOrThrow(TaskRequest r)
    {
        List<Map<String,String>> e=validate(r);
        if(!e.isEmpty())
            throw new ValidationException(e);
    }

    private Task toTask(TaskRequest r,Integer parentId) {
        validateOrThrow(r);
        Priority p=blank(r.getPriority())?Priority.MEDIUM:Priority.valueOf(r.getPriority());
        return new Task.Builder().title(r.getTitle().trim()).description(r.getDescription().trim())
          .completed(Boolean.parseBoolean(r.getCompleted())).dueDate(blank(r.getDueDate())?null:r.getDueDate())
          .priority(p).parentId(parentId).build();
    }

    private TaskRequest requestFromParsed(ParsedTask p) {
        return new TaskRequest(p.getTitle(),p.getDescription(),"false",p.getDueDate(),p.getPriority());
    }

    public Task createTask(TaskRequest request)
    {
        Task t=repository.save(toTask(request,null))
                ;notifyListeners("CREATED",t);return t;
    }

    public Task getTask(int id)
    {
        return repository.findById(id).orElseThrow(()->new NotFoundException("Task "+id+" not found"));
    }

    public List<Task> getAllTasks(){
        return repository.findAll();
    }

    public List<Task> getAllTasksIncludingSubtasks(){
        return repository.findAll();
    }

    public Task updateTask(int id,TaskRequest request) {
        Task old=getTask(id); Task input=toTask(request,old.getParentId());
        Task updated=new Task.Builder().id(old.getId()).title(input.getTitle()).description(input.getDescription())
          .completed(input.isCompleted()).dueDate(input.getDueDate()).priority(input.getPriority())
          .parentId(old.getParentId()).createdAt(old.getCreatedAt()).build();
        repository.saveReplacement(updated);notifyListeners("UPDATED",updated);return updated;
    }

    public void deleteTask(int id) {
        Task target=getTask(id);
        // Delete descendants first, so no orphaned subtasks remain.
        deleteChildrenRecursively(id);
        repository.delete(id);notifyListeners("DELETED",target);
    }

    private void deleteChildrenRecursively(int parentId) {
        List<Task> children=repository.findAll().stream()
          .filter(t->Objects.equals(t.getParentId(),parentId)).collect(Collectors.toList());
        for(Task child:children){deleteChildrenRecursively(child.getId());repository.delete(child.getId());notifyListeners("DELETED",child);}
    }

    public Task createSubtask(int parentId,TaskRequest request) {
        Task parent=getTask(parentId);
        if(parent.getParentId()!=null) throw new ConflictException("A subtask cannot have its own subtasks");
        Task saved=repository.save(toTask(request,parentId));notifyListeners("SUBTASK_CREATED",saved);return saved;
    }

    public List<Task> listSubtasks(int parentId) {
        getTask(parentId);
        return repository.findAll().stream().filter(t->Objects.equals(t.getParentId(),parentId)).collect(Collectors.toList());
    }

    public Task createTaskFromText(String text,String today) {
        if(blank(text)) throw new ValidationException(List.of(issue("text","must not be empty or blank")));
        if(today!=null && !DateUtil.isValidDate(today))
            throw new ValidationException(List.of(issue("today","must be a real date in YYYY-MM-DD format")));
        ParsedTask parsed;
        try { parsed=parser.parse(text,today==null?LocalDate.now().toString():today); }
        catch(ApiException ex){throw ex;}
        catch(Exception ex){throw new BadGatewayException("Could not parse task text: "+ex.getMessage());}
        if(parsed==null) throw new BadGatewayException("LLM returned an empty task");
        return createTask(requestFromParsed(parsed));
    }

    public List<Task> breakDownTask(int id) {
        Task parent=getTask(id);
        if(parent.getParentId()!=null) throw new ConflictException("A subtask cannot be broken down");
        List<ParsedTask> parsed;
        try {parsed=breakdownStrategy.breakdown(parent.getTitle()+". "+parent.getDescription());}
        catch(Exception ex){throw new BadGatewayException("Could not break down task: "+ex.getMessage());}
        if(parsed==null || parsed.isEmpty()) throw new ValidationException(List.of(issue("subtasks","breakdown must contain at least one subtask")));
        List<TaskRequest> requests=new ArrayList<>();
        for(ParsedTask p:parsed) {
            if(p==null) throw new ValidationException(List.of(issue("subtasks","contains an empty item")));
            TaskRequest r=requestFromParsed(p);
            validateOrThrow(r);
            requests.add(r);
        }
        List<Task> saved=new ArrayList<>();
        for(TaskRequest r:requests) {Task t=repository.save(toTask(r,id));saved.add(t);notifyListeners("SUBTASK_CREATED",t);}
        return saved;
    }

    public List<Task> listByPriority(Priority priority) {
        if(priority==null) throw new ValidationException(List.of(issue("priority","must be LOW, MEDIUM or HIGH")));
        return repository.findAll().stream().filter(t->t.getPriority()==priority).collect(Collectors.toList());
    }

    public List<Task> query(TaskQuery q) {
        if(q==null) throw new ValidationException(List.of(issue("query","must not be null")));
        List<TaskFilter> filters=new ArrayList<>();
        if(!blank(q.getSearch())) filters.add(new SearchFilter(q.getSearch()));
        if(q.getCompleted()!=null) filters.add(new CompletedFilter(q.getCompleted()));
        if(q.getPriority()!=null) filters.add(new PriorityFilter(q.getPriority()));
        SortStrategy sort;
        if("dueDate".equalsIgnoreCase(q.getSortBy())) sort=new DueDateSort();
        else if("createdAt".equalsIgnoreCase(q.getSortBy())) sort=new CreatedAtSort();
        else throw new ValidationException(List.of(issue("sortBy","must be createdAt or dueDate")));
        return query(filters,sort,q.getPage(),q.getPageSize());
    }

    public List<Task> query(List<TaskFilter> filters,SortStrategy sort,int page,int pageSize) {
        if(page<1 || pageSize<1 || pageSize>1000)
            throw new ValidationException(List.of(issue("pagination","page must be >= 1 and pageSize must be between 1 and 1000")));
        java.util.stream.Stream<Task> stream=repository.findAll().stream();
        if(filters!=null) for(TaskFilter f:filters) if(f!=null) stream=stream.filter(f::matches);
        List<Task> result=stream.collect(Collectors.toList());
        if(sort!=null) result.sort(sort.getComparator());
        long requestedStart=(long)(page-1)*pageSize;
        int from=(int)Math.min(requestedStart,result.size());
        int to=Math.min(from+pageSize,result.size());
        return new ArrayList<>(result.subList(from,to));
    }

    public void addListener(TaskEventListener listener){
        if(listener!=null)listeners.add(listener);
    }

    public void removeListener(TaskEventListener listener){
        listeners.remove(listener);
    }

    private void notifyListeners(String type,Task task){
        TaskEvent event=new TaskEvent(type,task);for(TaskEventListener l:listeners){
            try{l.onEvent(event);
            }
            catch(RuntimeException ex)
            {
                System.err.println("Task listener failed: "+ex.getMessage());
            }
        }
    }
}
