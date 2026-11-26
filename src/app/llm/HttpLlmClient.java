package app.llm;

import app.exception.BadGatewayException;
import app.exception.ServiceUnavailableException;
import app.util.MiniJson;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public class HttpLlmClient implements LlmClient {
    private final String provider,key,model;
    private final HttpClient http;
    public HttpLlmClient(){
        this.provider=env("LLM_PROVIDER","anthropic").toLowerCase(Locale.ROOT);
        this.key=System.getenv("LLM_API_KEY");
        this.model=System.getenv("LLM_MODEL");
        this.http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }
    public HttpLlmClient(String provider,String key,String model){
        this.provider=provider.toLowerCase(Locale.ROOT);this.key=key;this.model=model;
        this.http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }
    private static String env(String k,String d){String v=System.getenv(k);return v==null||v.isBlank()?d:v;}
    @Override public String complete(String prompt){
        if(key==null||key.isBlank())throw new ServiceUnavailableException("LLM_API_KEY is not configured");
        try {
            String url;Map<String,String> headers=new LinkedHashMap<>();Map<String,Object> body=new LinkedHashMap<>();
            if("anthropic".equals(provider)){
                url="https://api.anthropic.com/v1/messages";
                headers.put("x-api-key",key);headers.put("anthropic-version","2023-06-01");
                body.put("model",model==null||model.isBlank()?"claude-3-5-haiku-latest":model);
                body.put("max_tokens",1024);body.put("messages",List.of(Map.of("role","user","content",prompt)));
            } else if("openai".equals(provider)){
                url="https://api.openai.com/v1/chat/completions";
                headers.put("Authorization","Bearer "+key);
                body.put("model",model==null||model.isBlank()?"gpt-4o-mini":model);
                body.put("messages",List.of(Map.of("role","user","content",prompt)));
            } else if("gemini".equals(provider)){
                String m=model==null||model.isBlank()?"gemini-2.0-flash":model;
                url="https://generativelanguage.googleapis.com/v1beta/models/"+m+":generateContent?key="+key;
                body.put("contents",List.of(Map.of("parts",List.of(Map.of("text",prompt)))));
            } else throw new ServiceUnavailableException("Unsupported LLM_PROVIDER: "+provider);
            headers.put("Content-Type","application/json");
            HttpRequest.Builder rb=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).POST(HttpRequest.BodyPublishers.ofString(MiniJson.stringify(body)));
            headers.forEach(rb::header);
            HttpResponse<String> response=http.send(rb.build(),HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()<200||response.statusCode()>=300)
                throw new BadGatewayException("LLM provider returned HTTP "+response.statusCode()+": "+response.body());
            return extractText(response.body());
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw new BadGatewayException("LLM request was interrupted");}
        catch(BadGatewayException|ServiceUnavailableException e){throw e;}
        catch(Exception e){throw new BadGatewayException("LLM request failed: "+e.getMessage());}
    }
    @SuppressWarnings("unchecked")
    private String extractText(String json){
        Object parsed=MiniJson.parse(json);if(!(parsed instanceof Map))throw new BadGatewayException("Unexpected LLM response");
        Map<String,Object> root=(Map<String,Object>)parsed;
        if("anthropic".equals(provider)){
            List<Object> content=(List<Object>)root.get("content");
            if(content!=null)for(Object item:content)if(item instanceof Map){Object t=((Map<?,?>)item).get("text");if(t instanceof String)return (String)t;}
        } else if("openai".equals(provider)){
            List<Object> choices=(List<Object>)root.get("choices");
            if(choices!=null&&!choices.isEmpty()){Object msg=((Map<?,?>)choices.get(0)).get("message");if(msg instanceof Map){Object c=((Map<?,?>)msg).get("content");if(c instanceof String)return (String)c;}}
        } else {
            List<Object> candidates=(List<Object>)root.get("candidates");
            if(candidates!=null&&!candidates.isEmpty()){Object content=((Map<?,?>)candidates.get(0)).get("content");if(content instanceof Map){List<Object> parts=(List<Object>)((Map<?,?>)content).get("parts");if(parts!=null&&!parts.isEmpty()){Object t=((Map<?,?>)parts.get(0)).get("text");if(t instanceof String)return (String)t;}}}
        }
        throw new BadGatewayException("LLM response did not contain answer text");
    }
}
