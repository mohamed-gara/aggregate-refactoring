package kata;

import java.time.LocalDateTime;
import java.util.List;

public class MeetupEvent {

    private final Long id;
    private final int capacity;
    private final String eventName;
    private final LocalDateTime startTime;
    private final List<Subscription> subscriptions;

    public MeetupEvent(Long id, int capacity, String eventName, LocalDateTime startTime, List<Subscription> subscriptions) {
        this.id = id;
        this.capacity = capacity;
        this.eventName = eventName;
        this.startTime = startTime;
        this.subscriptions = subscriptions;
    }

    public Long getId() {
        return id;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getEventName() {
        return eventName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public MeetupEvent withCapacity(int capacity) {
        return new MeetupEvent(id, capacity, eventName, startTime, subscriptions);
    }
}
