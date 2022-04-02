package kata;

import kata.persistence.MeetupEventRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MeetupSubscribe {

    private final MeetupEventRepository repository;

    public MeetupSubscribe(MeetupEventRepository repository) {
        this.repository = repository;
    }

    public Long registerMeetupEvent(String eventName, Integer eventCapacity, LocalDateTime startTime) {
        var id = repository.generateId();
        var meetupEvent = new MeetupEvent(id, eventCapacity, eventName, startTime, List.of());

        repository.save(meetupEvent);

        return id;
    }

    public void subscribeUserToMeetupEvent(String userId, Long meetupEventId) {
        var meetup = repository.findById(meetupEventId);

        var userSubscription = meetup.getSubscription(userId);
        if (userSubscription != null) {
            throw new RuntimeException(String.format("User %s already has a subscription", userId));
        }

        var addToWaitingList = meetup.getParticipants().size() == meetup.getCapacity();
        var subscription = new Subscription(userId, Instant.now(), addToWaitingList);
        meetup.getSubscriptions().add(subscription);

        repository.save(meetup);
    }

    public void cancelUserSubscription(String userId, Long meetupEventId) {
        var meetup = repository.findById(meetupEventId);

        var inWaitingList = meetup.isInWaitingList(userId);
        meetup.remove(userId);

        if (!inWaitingList) {
            List<Subscription> waitingList = meetup.getWaitingList();
            if (!waitingList.isEmpty()) {
                Subscription firstInWaitingList = waitingList.get(0);
                meetup.changeFromWaitingListToParticipant(firstInWaitingList.getUserId());
            }
        }

        repository.save(meetup);
    }

    public void increaseCapacity(Long meetupEventId, int newCapacity) {
        var meetupEvent = repository.findById(meetupEventId);

        var oldCapacity = meetupEvent.getCapacity();
        if (oldCapacity < newCapacity) {
            var updatedMeetupEvent = meetupEvent.withCapacity(newCapacity);
            var newSlots = newCapacity - oldCapacity;
            meetupEvent.getWaitingList()
                    .stream()
                    .limit(newSlots)
                    .forEach(subscription -> meetupEvent.changeFromWaitingListToParticipant(subscription.getUserId()));

            repository.save(updatedMeetupEvent);
        }
    }

    public MeetupEventStatusDto getMeetupEventStatus(Long meetupEventId) {
        var meetupEvent = repository.findById(meetupEventId);

        var meetupEventStatusDto = new MeetupEventStatusDto();
        meetupEventStatusDto.meetupId = meetupEvent.getId();
        meetupEventStatusDto.eventCapacity = meetupEvent.getCapacity();
        meetupEventStatusDto.startTime = meetupEvent.getStartTime();
        meetupEventStatusDto.eventName = meetupEvent.getEventName();
        meetupEventStatusDto.waitingList = meetupEvent.getWaitingList().stream().map(Subscription::getUserId).collect(toList());
        meetupEventStatusDto.participants = meetupEvent.getParticipants().stream().map(Subscription::getUserId).collect(toList());
        return meetupEventStatusDto;
    }
}
