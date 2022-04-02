package kata;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Comparator.comparing;

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
        return subscriptions == null ? List.of() : subscriptions;
    }

    public MeetupEvent withCapacity(int capacity) {
        return new MeetupEvent(id, capacity, eventName, startTime, subscriptions);
    }

    public Subscription getSubscription(String userId) {
        return getSubscriptions().stream()
                .filter(it -> it.getUserId().equals(userId))
                .findAny()
                .orElse(null);
    }

    public boolean isInWaitingList(String userId) {
        return getSubscription(userId).isInWaitingList();
    }

    public List<Subscription> getWaitingList() {
        return getSubscriptions().stream()
                .filter(it -> it.isInWaitingList())
                .toList();
    }

    public List<Subscription> getParticipants() {
        return getSubscriptions().stream()
                .filter(it -> it.isInWaitingList() == false)
                .sorted(comparing(Subscription::getRegistrationTime))
                .toList();
    }

    public void remove(String userId) {
        getSubscriptions().stream()
                .filter(it -> it.getUserId().equals(userId))
                .findFirst()
                .ifPresent(it -> subscriptions.remove(it));
    }

    public List<String> getUsers() {
        return getSubscriptions().stream()
                .map(Subscription::getUserId)
                .toList();
    }

    public void changeFromWaitingListToParticipant(String userId) {
        getSubscriptions().stream()
                .filter(it -> it.getUserId().equals(userId))
                .findFirst()
                .ifPresent(it -> it.confirm());
    }
}
