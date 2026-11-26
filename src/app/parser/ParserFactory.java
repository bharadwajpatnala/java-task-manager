package app.parser;
import app.llm.LlmClient;
public final class ParserFactory {
    private ParserFactory(){}
    public static TaskParser create(boolean useLlm,LlmClient client){
        return useLlm?new LlmTaskParser(client):new RuleBasedTaskParser();
    }
}
