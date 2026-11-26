package app.llm;
/** Deterministic offline client for tests. */
public class FakeLlmClient implements LlmClient {
    private final String response;

    public FakeLlmClient(String response){
        this.response=response;
    }

    @Override
    public String complete(String prompt){
        return response;
    }
}
