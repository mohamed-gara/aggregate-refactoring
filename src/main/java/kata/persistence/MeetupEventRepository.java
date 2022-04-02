package kata.persistence;

import kata.MeetupEvent;
import kata.Subscription;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static kata.persistence.JdbiMapperHelper.mapTo;

public class MeetupEventRepository {

    private final Jdbi jdbi;

    public MeetupEventRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public MeetupEvent findById(Long meetupEventId) {
        var subscriptions = findSubscriptionsOf(meetupEventId);
        String sql = "SELECT * FROM MEETUP_EVENT WHERE id = :id";
        return jdbi.withHandle(handle -> handle.createQuery(sql)
                .bind("id", meetupEventId)
                .map(mapper(subscriptions))
                .findOne()
                .orElse(null));
    }

    private RowMapper<MeetupEvent> mapper(List<Subscription> subscriptions) {
        return (rs, ctx) -> new MeetupEvent(
                        rs.getLong("id"),
                        rs.getInt("capacity"),
                        rs.getString("event_name"),
                        mapTo(rs, "start_time", LocalDateTime.class, ctx),
                        subscriptions
                );
    }

    private List<Subscription> findSubscriptionsOf(Long meetupEventId) {
        String sql = "" +
                "SELECT * FROM USER_SUBSCRIPTION " +
                "WHERE meetup_event_id = :meetupEventId ";

        return jdbi.withHandle(
            handle -> handle.createQuery(sql)
                .bind("meetupEventId", meetupEventId)
                .map(SUBSCRIPTION_ROW_MAPPER)
                .list()
        );
    }

    private static final RowMapper<Subscription> SUBSCRIPTION_ROW_MAPPER = (rs, ctx) ->
            new Subscription(
                    rs.getString("user_id"),
                    mapTo(rs, "registration_time", Instant.class, ctx),
                    rs.getBoolean("waiting_list")
            );

    public void save(MeetupEvent meetupEvent) {
        jdbi.useTransaction(handle -> {
            upsertMeetupEvent(meetupEvent).useHandle(handle);
            if (meetupEvent.getSubscriptions() != null) upsertSubscriptions(meetupEvent.getId(), meetupEvent.getSubscriptions()).useHandle(handle);
        });
    }

    private HandleConsumer<RuntimeException> upsertMeetupEvent(MeetupEvent meetupEvent) {
        String sql = "" +
                "MERGE INTO MEETUP_EVENT (id, event_name, start_time, capacity) " +
                "KEY (id) " +
                "VALUES (:id, :event_name, :start_time, :capacity)";

        return handle -> handle.createUpdate(sql)
                .bind("id", meetupEvent.getId())
                .bind("event_name", meetupEvent.getEventName())
                .bind("start_time", meetupEvent.getStartTime())
                .bind("capacity", meetupEvent.getCapacity())
                .execute();
    }

    private HandleConsumer<RuntimeException> upsertSubscriptions(long meetupEventId, Collection<Subscription> subscriptions) {
        String sql = "" +
                "MERGE INTO USER_SUBSCRIPTION (user_id, meetup_event_id, registration_time, waiting_list) " +
                "KEY (meetup_event_id, user_id) " +
                "VALUES (:userId, :meetupEventId, :registrationTime, :waitingList)";

        return handle -> {
            PreparedBatch preparedBatch = handle.prepareBatch(sql);
            subscriptions.forEach(subscription -> preparedBatch
                    .bind("meetupEventId", meetupEventId)
                    .bind("userId", subscription.getUserId())
                    .bind("registrationTime", subscription.getRegistrationTime())
                    .bind("waitingList", subscription.isInWaitingList())
                    .add());
            preparedBatch.execute();
        };
    }

    private HandleConsumer<RuntimeException> deleteMeetupSubscriptionsNotInUserIds(long meetupEventId, List<String> userIds) {
        String sql = "" +
                "DELETE FROM USER_SUBSCRIPTION " +
                "WHERE meetup_event_id = :meetupEventId " +
                "AND user_id NOT IN (<userIds>)";

        return handle -> handle.createUpdate(sql)
                .bind("meetupEventId", meetupEventId)
                .bindList("userIds", userIds)
                .execute();
    }
}
