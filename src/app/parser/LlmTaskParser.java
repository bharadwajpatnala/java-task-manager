package app.parser;
import app.llm.LlmClient;
import app.model.ParsedTask;
import app.util.MiniJson;
import app.exception.BadGatewayException;
import app.exception.ApiException;
import java.util.Map;

public class LlmTaskParser implements TaskParser {
    private final LlmClient client;
    public LlmTaskParser(LlmClient client){this.client=client;}
    @SuppressWarnings("unchecked")
    public ParsedTask parse(String text,String today){
        String prompt="Today is "+today+". Convert the user's request into exactly one JSON object with string fields title, description, dueDate, priority. Use null for unknown dueDate. priority must be LOW, MEDIUM, or HIGH. Return only JSON, no markdown. User request: "+text;
        String raw=client.complete(prompt).trim();
        raw=stripFences(raw);
        try{
            Object obj=MiniJson.parse(raw);
            if(!(obj instanceof Map))throw new IllegalArgumentException("Expected JSON object");
            Map<String,Object> m=(Map<String,Object>)obj;
            return new ParsedTask(str(m.get("title")),str(m.get("description")),nullable(m.get("dueDate")),str(m.get("priority")));
        }catch(ApiException ex){throw ex;}
        catch(Exception ex){throw new BadGatewayException("LLM returned invalid task JSON: "+ex.getMessage());}
    }
    static String stripFences(String s){if(s.startsWith("```")){int n=s.indexOf('\n');if(n>=0)s=s.substring(n+1);if(s.endsWith("```"))s=s.substring(0,s.length()-3); }return s.trim();}
    private String str(Object o){return o==null?null:String.valueOf(o);}
    private String nullable(Object o){return o==null||"null".equals(o)?null:String.valueOf(o);}
}
