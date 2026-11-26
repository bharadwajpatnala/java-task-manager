package app.parser;
import app.llm.LlmClient;
import app.model.ParsedTask;
import app.util.MiniJson;
import app.exception.BadGatewayException;
import app.exception.ApiException;
import java.util.*;

public class LlmBreakdownStrategy implements BreakdownStrategy {
    private final LlmClient client;
    public LlmBreakdownStrategy(LlmClient client){this.client=client;}
    @SuppressWarnings("unchecked")
    public List<ParsedTask> breakdown(String description){
        String prompt="Break this task into 2 to 6 actionable subtasks. Return only a JSON array. Each item must have string fields title, description, priority (LOW, MEDIUM, HIGH) and dueDate (null if unknown). Task: "+description;
        try{
            Object parsed=MiniJson.parse(LlmTaskParser.stripFences(client.complete(prompt).trim()));
            if(!(parsed instanceof List))throw new IllegalArgumentException("Expected JSON array");
            List<ParsedTask> result=new ArrayList<>();
            for(Object item:(List<?>)parsed){
                if(!(item instanceof Map))throw new IllegalArgumentException("Each subtask must be an object");
                Map<String,Object> m=(Map<String,Object>)item;
                result.add(new ParsedTask(value(m.get("title")),value(m.get("description")),nullable(m.get("dueDate")),value(m.get("priority"))));
            }
            return result;
        }catch(ApiException e){throw e;}
        catch(Exception e){throw new BadGatewayException("LLM returned invalid breakdown JSON: "+e.getMessage());}
    }
    private String value(Object o){return o==null?null:String.valueOf(o);}
    private String nullable(Object o){return o==null||"null".equals(o)?null:String.valueOf(o);}
}
