package app;

import app.api.*;
import app.event.*;
import app.model.*;
import app.parser.*;
import app.repository.*;
import app.service.*;
import app.llm.*;
import app.exception.ServiceUnavailableException;
import java.util.*;

public class TestRunner {
    private static int passed=0;
    private static void check(boolean condition,String name){if(!condition)throw new AssertionError("FAIL: "+name);passed++;System.out.println("PASS: "+name);}
    public static void main(String[] args){
        TaskRepository repo=InMemoryTaskRepository.getInstance();
        TaskService service=new TaskService(repo);
        TaskApi api=new TaskApi(service);
        AuditTrailListener audit=new AuditTrailListener();
        service.addListener(audit);

        TaskRequest valid=new TaskRequest("Prepare report","Prepare monthly report","false","2026-12-01","HIGH");
        ApiResponse created=api.createTask(valid,"test-key-1");
        check(created.getStatus()==201,"create task");
        Task task=(Task)created.getBody();
        check(task.getId()>0 && task.getPriority()==Priority.HIGH,"created task fields");

        ApiResponse retry=api.createTask(valid,"test-key-1");
        check(retry.getStatus()==201 && ((Task)retry.getBody()).getId()==task.getId(),"idempotent retry returns original response");
        ApiResponse conflict=api.createTask(new TaskRequest("Different","Different description","false",null,"LOW"),"test-key-1");
        check(conflict.getStatus()==422,"idempotency key conflict");

        check(api.getTask(task.getId()).getStatus()==200,"get task");
        check(api.listTasks().getStatus()==200,"list tasks");
        check(api.createSubtask(task.getId(),new TaskRequest("Collect data","Collect source data","false",null,"MEDIUM"),"sub-1").getStatus()==201,"create subtask");
        check(api.listSubtasks(task.getId()).getStatus()==200,"list subtasks");
        check(api.listByPriority("HIGH").getStatus()==200,"list by priority");
        check(api.listByPriority("URGENT").getStatus()==400,"invalid priority validation");

        ApiResponse invalid=api.createTask(new TaskRequest(" ","desc","false","2026-02-30","BAD"),null);
        check(invalid.getStatus()==400,"invalid request validation");

        check(api.updateTask(task.getId(),new TaskRequest("Updated report","Updated description","true",null,"LOW")).getStatus()==200,"update task");
        check(api.deleteTask(task.getId()).getStatus()==200,"delete parent and subtasks");
        check(api.getTask(task.getId()).getStatus()==404,"deleted task not found");

        check(audit.getEventCounts().getOrDefault("CREATED",0)==1,"observer create event");
        check(audit.getEventCounts().getOrDefault("UPDATED",0)==1,"observer update event");

        TaskQuery query=new TaskQuery.Builder().search("rent").page(1).pageSize(10).sortBy("createdAt").build();
        check(api.queryTasks(query).getStatus()==200,"search and pagination query");

        LlmTaskParser llmParser=new LlmTaskParser(new FakeLlmClient("{\"title\":\"Pay rent\",\"description\":\"Monthly rent\",\"dueDate\":\"2026-12-01\",\"priority\":\"HIGH\"}"));
        ParsedTask parsed=llmParser.parse("Pay rent on Dec 1","2026-11-17");
        check("Pay rent".equals(parsed.getTitle()),"fake LLM task parser");
        LlmBreakdownStrategy breakdown=new LlmBreakdownStrategy(new FakeLlmClient("[{\"title\":\"Step one\",\"description\":\"Do step one\",\"dueDate\":null,\"priority\":\"MEDIUM\"}]"));
        check(breakdown.breakdown("A task").size()==1,"fake LLM breakdown");
        check(new RuleBasedTaskParser().parse("Remind me to pay rent tomorrow","2026-11-17").getDueDate().equals("2026-11-18"),"offline natural language parser");
        TaskService unavailableService=new TaskService(repo,
            new LlmTaskParser(new FaultyLlmClient(new ServiceUnavailableException("missing key"))),
            new RuleBasedBreakdownStrategy());
        TaskApi unavailableApi=new TaskApi(unavailableService);
        check(unavailableApi.createTaskFromText("Pay rent","2026-11-17",null).getStatus()==503,"missing LLM key maps to 503");

        TaskService brokenService=new TaskService(repo,
            new LlmTaskParser(new FakeLlmClient("not json")),
            new RuleBasedBreakdownStrategy());
        TaskApi brokenApi=new TaskApi(brokenService);
        check(brokenApi.createTaskFromText("Pay rent","2026-11-17",null).getStatus()==502,"invalid LLM response maps to 502");

        System.out.println("ALL TESTS PASSED: "+passed);
    }
}
