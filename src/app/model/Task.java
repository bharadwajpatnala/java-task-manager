package app.model;

public final class Task {
    private final int id;
    private final String title;
    private final String description;
    private final boolean completed;
    private final String dueDate;
    private final Priority priority;
    private final Integer parentId;
    private final long createdAt;

    private Task(Builder b) {
        this.id=b.id; this.title=b.title; this.description=b.description;
        this.completed=b.completed; this.dueDate=b.dueDate;
        this.priority=b.priority; this.parentId=b.parentId;
        this.createdAt=b.createdAt;
    }

    public int getId(){ return id; }
    public String getTitle(){ return title; }
    public String getDescription(){ return description; }
    public boolean isCompleted(){ return completed; }
    public boolean getCompleted(){ return completed; }
    public String getDueDate(){ return dueDate; }
    public Priority getPriority(){ return priority; }
    public Integer getParentId(){ return parentId; }
    public long getCreatedAt(){ return createdAt; }

    public static class Builder {
        private int id;
        private String title;
        private String description;
        private boolean completed=false;
        private String dueDate;
        private Priority priority=Priority.MEDIUM;
        private Integer parentId;
        private long createdAt=System.currentTimeMillis();

        public Builder id(int v){id=v;return this;}
        public Builder title(String v){title=v;return this;}
        public Builder description(String v){description=v;return this;}
        public Builder completed(boolean v){completed=v;return this;}
        public Builder dueDate(String v){dueDate=v;return this;}
        public Builder priority(Priority v){priority=v;return this;}
        public Builder parentId(Integer v){parentId=v;return this;}
        public Builder createdAt(long v){createdAt=v;return this;}

        public Task build() {
            if(title==null || title.isBlank())
                throw new IllegalArgumentException("Title is required");
            if(description==null || description.isBlank())
                throw new IllegalArgumentException("Description is required");
            if(priority==null)
                throw new IllegalArgumentException("Priority is required");
            return new Task(this);
        }
    }
}
