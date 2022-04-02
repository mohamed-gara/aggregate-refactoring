package kata;

import kata.persistence.MeetupEventDao;
import kata.persistence.MeetupEventRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MeetupSubscribe {

    private final MeetupEventDao meetupEventDao;
    private final MeetupEventRepository repository;

    public MeetupSubscribe(MeetupEventDao meetupEventDao, MeetupEventRepository repository) {
        this.meetupEventDao = meetupEventDao;
        this.repository = repository;
    }

    public Long registerMeetupEvent(String eventName, Integer eventCapacity, LocalDateTime startTime) {
        long id = meetupEventDao.generateId();
        MeetupEvent meetupEvent = new MeetupEvent(id, eventCapacity, eventName, startTime, null);
        meetupEventDao.create(meetupEvent);
        return id;
    }

    public void subscribeUserToMeetupEvent(String userId, Long meetupEventId) {
        var meetup1 = repository.findById(meetupEventId);
        var meetup = meetup1.getSubscription(userId);
        if (meetup != null) {
            throw new RuntimeException(String.format("User %s already has a subscription", userId));
        }

        var meetup2 = repository.findById(meetupEventId);
        List<Subscription> participants = meetup2.getParticipants();
        MeetupEvent meetupEvent = meetupEventDao.findById(meetupEventId);
        boolean addToWaitingList = participants.size() == meetupEvent.getCapacity();
        Subscription subscription = new Subscription(userId, Instant.now(), addToWaitingList);
        var meetup3 = repository.findById(meetupEventId);
        meetup3.getSubscriptions().add(subscription);
        repository.save(meetup3);
    }

    public void cancelUserSubscription(String userId, Long meetupEventId) {
        var meetup = repository.findById(meetupEventId);
        Boolean inWaitingList = meetup.isInWaitingList(userId);
        var meetup3 = repository.findById(meetupEventId);
        meetup3.remove(userId);
        repository.save(meetup3);

        if (!inWaitingList) {
            var meetup1 = repository.findById(meetupEventId);
            List<Subscription> waitingList = meetup1.getWaitingList();
            if (!waitingList.isEmpty()) {
                Subscription firstInWaitingList = waitingList.get(0);
                var meetup2 = repository.findById(meetupEventId);
                meetup2.changeFromWaitingListToParticipant(firstInWaitingList.getUserId());
                repository.save(meetup2);
            }
        }
    }

    public void increaseCapacity(Long meetupEventId, int newCapacity) {
        MeetupEvent meetupEvent = meetupEventDao.findById(meetupEventId);
        int oldCapacity = meetupEvent.getCapacity();

        if (oldCapacity < newCapacity) {
            meetupEventDao.updateCapacity(meetupEventId, newCapacity);
            int newSlots = newCapacity - oldCapacity;
            var meetup = repository.findById(meetupEventId);
            List<Subscription> waitingList = meetup.getWaitingList();
            waitingList.stream()
                    .limit(newSlots)
                    .forEach(subscription -> {
                        var meetup1 = repository.findById(meetupEventId);
                        meetup1.changeFromWaitingListToParticipant(subscription.getUserId());
                        repository.save(meetup1);
                    });
        }
    }

    public MeetupEventStatusDto getMeetupEventStatus(Long meetupEventId) {
        MeetupEvent meetupEvent = meetupEventDao.findById(meetupEventId);
        var meetup1 = repository.findById(meetupEventId);
        List<Subscription> participants = meetup1.getParticipants();
        var meetup = repository.findById(meetupEventId);
        List<Subscription> waitingList = meetup.getWaitingList();

        MeetupEventStatusDto meetupEventStatusDto = new MeetupEventStatusDto();
        meetupEventStatusDto.meetupId = meetupEvent.getId();
        meetupEventStatusDto.eventCapacity = meetupEvent.getCapacity();
        meetupEventStatusDto.startTime = meetupEvent.getStartTime();
        meetupEventStatusDto.eventName = meetupEvent.getEventName();
        meetupEventStatusDto.waitingList = waitingList.stream().map(Subscription::getUserId).collect(toList());
        meetupEventStatusDto.participants = participants.stream().map(Subscription::getUserId).collect(toList());
        return meetupEventStatusDto;
    }
}
