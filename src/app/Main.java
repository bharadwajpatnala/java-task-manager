package app;
import app.api.*;
import app.event.*;
import app.llm.HttpLlmClient;
import app.parser.*;
import app.repository.*;
import app.service.*;
import app.model.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        boolean useLlm=Boolean.parseBoolean(System.getenv().getOrDefault("USE_LLM","false"));
        HttpLlmClient client=new HttpLlmClient();
        TaskParser parser=ParserFactory.create(useLlm,client);
        BreakdownStrategy breakdown=useLlm?new LlmBreakdownStrategy(client):new RuleBasedBreakdownStrategy();
        TaskService service=new TaskService(InMemoryTaskRepository.getInstance(),parser,breakdown);
        service.addListener(new ConsoleLogListener());
        service.addListener(new AuditTrailListener());
        TaskApi api=new TaskApi(service);

        System.out.println("Task Manager Service ready.");
        System.out.println("Example: createTask(title, description, completed, dueDate, priority)");
        ApiResponse response=api.createTask(new TaskRequest("Prepare assignment","Complete Java task manager","false",null,"HIGH"),"demo-001");
        System.out.println("Create status: "+response.getStatus());
        Task created=(Task)response.getBody();
        System.out.println("Created task ID: "+created.getId());
        System.out.println("Created task title: "+created.getTitle());
    }
}
