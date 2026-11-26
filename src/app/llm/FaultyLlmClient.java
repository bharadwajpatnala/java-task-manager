package app.llm;
/** Test double that simulates malformed output or provider failure. */
public class FaultyLlmClient implements LlmClient {
    private final RuntimeException failure;

    public FaultyLlmClient(RuntimeException failure){
        this.failure=failure;
    }

    @Override
    public String complete(String prompt){
        throw failure;
    }
}
