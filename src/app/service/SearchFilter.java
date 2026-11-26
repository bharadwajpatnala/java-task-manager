package app.service;
import app.model.Task;
public class SearchFilter implements TaskFilter {
    private final String search;
    public SearchFilter(String search){
        this.search=search==null?"":search.toLowerCase();
    }
    public boolean matches(Task t){
        return t.getTitle().toLowerCase().contains(search)||t.getDescription().toLowerCase().contains(search);
    }
}
