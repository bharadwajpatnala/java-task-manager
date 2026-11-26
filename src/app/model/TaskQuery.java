package app.model;

public final class TaskQuery {
    private final String search;
    private final Boolean completed;
    private final Priority priority;
    private final String sortBy;
    private final int page;
    private final int pageSize;

    private TaskQuery(Builder b) {
        search=b.search; completed=b.completed; priority=b.priority;
        sortBy=b.sortBy; page=b.page; pageSize=b.pageSize;
    }
    public String getSearch(){
        return search;
    }
    public Boolean getCompleted(){
        return completed;
    }
    public Priority getPriority(){
        return priority;
    }
    public String getSortBy(){
        return sortBy;
    }
    public int getPage(){
        return page;
    }
    public int getPageSize(){
        return pageSize;
    }

    public static class Builder {
        private String search;
        private Boolean completed;
        private Priority priority;
        private String sortBy="createdAt";
        private int page=1,pageSize=20;
        public Builder search(String v){
            search=v;return this;
        }
        public Builder completed(Boolean v){
            completed=v;return this;
        }
        public Builder priority(Priority v){
            priority=v;return this;
        }
        public Builder sortBy(String v){
            sortBy=v;return this;
        }
        public Builder page(int v){
            page=v;return this;
        }
        public Builder pageSize(int v){
            pageSize=v;return this;
        }
        public TaskQuery build(){
            return new TaskQuery(this);
        }
    }
}
