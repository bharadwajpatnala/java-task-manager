package app.event;

import java.util.*;

public class AuditTrailListener
        implements TaskEventListener {

    private final Map<String, Integer> eventCounts =
            new HashMap<>();

    @Override
    public void onEvent(TaskEvent event) {

        eventCounts.merge(
                event.getEventType(), 1, Integer::sum);
    }

    public Map<String, Integer> getEventCounts() {
        return Collections.unmodifiableMap(eventCounts);
    }
}