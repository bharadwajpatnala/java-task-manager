package app.api;
public class ApiResponse {
    private final int status;
    private final Object body;
    public ApiResponse(int status,Object body)
    {
        this.status=status;this.body=body;
    }
    public int getStatus(){return status;}
    public Object getBody(){return body;}
}
