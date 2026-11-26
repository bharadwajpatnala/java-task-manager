package app.llm;

public class LlmSmokeTest {
    public static void main(String[] args)
    {
        System.out.println("Testing Claude API");

        try{
            HttpLlmClient client = new HttpLlmClient();

            String response = client.complete(
                    "Reply with exactly one word: OK"
            );
            System.out.println("Model replied: " + response);
            System.out.println("LLM SMOKE TEST PASSED");
        }
        catch (Exception ex){
            System.out.println("LLM SMOKE TEST FAILED");
            System.out.println(ex.getMessage());
        }
    }
}
