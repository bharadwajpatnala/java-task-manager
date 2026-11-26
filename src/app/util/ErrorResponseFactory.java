package app.util;

import app.api.ApiResponse;
import app.exception.ApiException;
import app.exception.ValidationException;
import java.util.*;

public final class ErrorResponseFactory {
    private ErrorResponseFactory()
    {

    }
    public static ApiResponse from(ApiException ex) {
        Map<String,Object> error=new LinkedHashMap<>();
        error.put("code",ex.getCode());
        error.put("message",ex.getMessage());
        if(ex instanceof ValidationException) error.put("details",((ValidationException)ex).getDetails());
        Map<String,Object> body=new LinkedHashMap<>(); body.put("error",error);
        return new ApiResponse(ex.getStatus(),body);
    }
    public static ApiResponse internalError() {
        Map<String,Object> error=Map.of("code","INTERNAL_ERROR","message","An unexpected error occurred");
        return new ApiResponse(500,Map.of("error",error));
    }
}
