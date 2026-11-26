package app.event;

public class ConsoleLogListener
        implements TaskEventListener {

    @Override
    public void onEvent(TaskEvent event) {

        System.out.println(
                "Task Event: " + event.getEventType()
                        + " | Task ID: "
                        + event.getTask().getId());
    }
}
