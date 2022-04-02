package kata.persistence;

import kata.Subscription;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

public class MeetupSubscriptionDao {

    private final MeetupEventRepository repository;

    public MeetupSubscriptionDao(MeetupEventRepository repository) {
        this.repository = repository;
    }

    public void addToSubscriptions(Subscription subscribtion, Long meetupEventId) {
        var meetup = repository.findById(meetupEventId);
        meetup.getSubscriptions().add(subscribtion);
        repository.save(meetup);
    }

    public void deleteSubscription(String userId, Long meetupEventId) {
        var meetup = repository.findById(meetupEventId);
        meetup.remove(userId);
        repository.save(meetup);
    }

    public void changeFromWaitingListToParticipants(String userId, Long meetupEventId) {
        var meetup = repository.findById(meetupEventId);
        meetup.changeFromWaitingListToParticipant(userId);
        repository.save(meetup);
    }

    public List<Subscription> findSubscriptionsParticipants(Long meetupEventId) {
        var meetup = repository.findById(meetupEventId);
        return meetup.getParticipants();
    }

    public List<Subscription> findSubscriptionsInWaitingList(Long meetupEventId) {
        var meetup = repository.findById(meetupEventId);
        return meetup.getWaitingList();
    }

    public Boolean isUserSubscriptionInWaitingList(String userId, Long meetupEventId) {
        var meetup = repository.findById(meetupEventId);
        return meetup.isInWaitingList(userId);
    }

    public Subscription findById(String userId, Long meetupEventId) {
        var meetup = repository.findById(meetupEventId);
        return meetup.getSubscription(userId);
    }
}
