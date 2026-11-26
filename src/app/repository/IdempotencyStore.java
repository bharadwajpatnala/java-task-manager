package app.repository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class IdempotencyStore {
    private static final IdempotencyStore INSTANCE=new IdempotencyStore();
    private final Map<String,StoredRecord> records=new ConcurrentHashMap<>();
    private IdempotencyStore(){}
    public static IdempotencyStore getInstance(){
        return INSTANCE;
    }
    public StoredRecord find(String key){
        return key==null?null:records.get(key);
    }
    public void save(String key,String fingerprint,Object response){
        if(key!=null && !key.isBlank()) records.putIfAbsent(key,new StoredRecord(fingerprint,response));
    }
    public static class StoredRecord {
        private final String fingerprint; private final Object response;
        public StoredRecord(String f,Object r){
            fingerprint=f;response=r;
        }
        public String getFingerprint(){
            return fingerprint;
        }
        public Object getResponse(){
            return response;
        }
    }
}
