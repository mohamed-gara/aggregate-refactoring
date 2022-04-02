package kata;

import kata.persistence.MeetupEventRepository;

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

        if (meetup.hasSubscriptionFor(userId)) {
            throw new RuntimeException(String.format("User %s already has a subscription", userId));
        }

        meetup.subscribe(userId);

        repository.save(meetup);
    }

    public void cancelUserSubscription(String userId, Long meetupEventId) {
        var meetup = repository.findById(meetupEventId);

        meetup.cancelSubscription(userId);

        repository.save(meetup);
    }

    public void increaseCapacity(Long meetupEventId, int newCapacity) {
        var meetupEvent = repository.findById(meetupEventId);

        var oldCapacity = meetupEvent.getCapacity();
        if (oldCapacity < newCapacity) {
            var updatedMeetupEvent = meetupEvent.updateCapacityTo(newCapacity);
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
