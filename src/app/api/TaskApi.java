package app.api;

import app.exception.*;
import app.model.*;
import app.repository.IdempotencyStore;
import app.service.TaskService;
import app.util.ErrorResponseFactory;
import java.util.*;
import java.util.function.Supplier;

public class TaskApi {
    private final TaskService service;
    private final IdempotencyStore idempotency=IdempotencyStore.getInstance();
    public TaskApi(TaskService service)
    {
        this.service=service;
    }

    private ApiResponse execute(Supplier<ApiResponse> operation){
        try{
            return operation.get();
        }
        catch(ApiException ex){
            return ErrorResponseFactory.from(ex);
        }
        catch(IllegalArgumentException ex){
            return ErrorResponseFactory.from(new ValidationException(List.of(Map.of("field","request","issue",ex.getMessage()))));
        }
        catch(Exception ex){
            return ErrorResponseFactory.internalError();
        }
    }

    private String fingerprint(Object... values){
        return app.util.MiniJson.stringify(Arrays.asList(values));
    }
    private synchronized ApiResponse idempotent(String key,String fingerprint,Supplier<ApiResponse> action){
        if(key==null||key.isBlank())return action.get();
        IdempotencyStore.StoredRecord old=idempotency.find(key);
        if(old!=null){
            if(!old.getFingerprint().equals(fingerprint))
                throw new ConflictException("Idempotency key reused with a different request");
            return (ApiResponse)old.getResponse();
        }
        ApiResponse response=action.get();
        if(response.getStatus()<400)idempotency.save(key,fingerprint,response);
        return response;
    }

    public ApiResponse createTask(TaskRequest request,String idempotencyKey){
        return execute(()->idempotent(idempotencyKey,fingerprint("createTask",request==null?null:request.getTitle(),request==null?null:request.getDescription(),request==null?null:request.getCompleted(),request==null?null:request.getDueDate(),request==null?null:request.getPriority()),
            ()->new ApiResponse(201,service.createTask(request))));
    }
    public ApiResponse getTask(int id){
        return execute(()->new ApiResponse(200,service.getTask(id)));
    }
    public ApiResponse listTasks(){
        return execute(()->new ApiResponse(200,service.getAllTasks()));
    }
    public ApiResponse updateTask(int id,TaskRequest request){
        return execute(()->new ApiResponse(200,service.updateTask(id,request)));
    }
    public ApiResponse deleteTask(int id){
        return execute(()->{
            service.deleteTask(id);
            return new ApiResponse(200,Map.of("message","Task deleted successfully"));
        });
    }
    public ApiResponse createTaskFromText(String text,String today,String idempotencyKey){
        return execute(()->idempotent(idempotencyKey,fingerprint("createTaskFromText",text,today),
            ()->new ApiResponse(201,service.createTaskFromText(text,today))));
    }

    public ApiResponse createSubtask(int parentId,TaskRequest request,String idempotencyKey){
        return execute(()->idempotent(idempotencyKey,fingerprint("createSubtask",parentId,request==null?null:request.getTitle(),request==null?null:request.getDescription(),request==null?null:request.getCompleted(),request==null?null:request.getDueDate(),request==null?null:request.getPriority()),
            ()->new ApiResponse(201,service.createSubtask(parentId,request))));
    }

    public ApiResponse listSubtasks(int parentId){
        return execute(()->new ApiResponse(200,service.listSubtasks(parentId)));
    }

    public ApiResponse breakDownTask(int id,String idempotencyKey){
        return execute(()->idempotent(idempotencyKey,fingerprint("breakDownTask",id),
            ()->new ApiResponse(201,service.breakDownTask(id))));
    }

    public ApiResponse queryTasks(TaskQuery query){
        return execute(()->new ApiResponse(200,service.query(query)));
    }

    public ApiResponse listByPriority(String priority){
        return execute(()->{
            Priority p;
            try{
                p=Priority.valueOf(priority);
            }
            catch(Exception ex){
                throw new ValidationException(List.of(Map.of("field","priority","issue","must be LOW, MEDIUM or HIGH")));
            }
            return new ApiResponse(200,service.listByPriority(p));
        });
    }
}
