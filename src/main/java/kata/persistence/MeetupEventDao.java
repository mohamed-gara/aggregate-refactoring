package kata.persistence;

import kata.MeetupEvent;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

import java.time.LocalDateTime;

import static kata.persistence.JdbiMapperHelper.mapTo;

public class MeetupEventDao {

    private final Jdbi jdbi;
    private final MeetupEventRepository repository;

    public MeetupEventDao(Jdbi jdbi, MeetupEventRepository repository) {
        this.jdbi = jdbi;
        this.repository = repository;
    }

    private static final RowMapper<MeetupEvent> MEETUP_EVENT_ROW_MAPPER = (rs, ctx) ->
            new MeetupEvent(
                    rs.getLong("id"),
                    rs.getInt("capacity"),
                    rs.getString("event_name"),
                    mapTo(rs, "start_time", LocalDateTime.class, ctx),
                    null
            );

    public void create(MeetupEvent meetupEvent) {
        repository.save(meetupEvent);
    }

    public void updateCapacity(Long meetupEventId, int newCapacity) {
        var meetupEvent = repository.findById(meetupEventId);
        var updatedMeetupEvent = meetupEvent.withCapacity(newCapacity);
        repository.save(updatedMeetupEvent);
    }

    public MeetupEvent findById(Long meetupEventId) {
        return repository.findById(meetupEventId);
    }

    public long generateId() {
        return jdbi.withHandle(handle -> handle
                .createQuery("SELECT NEXTVAL('MEETUP_EVENT_ID_SEQ')")
                .mapTo(Long.class)
                .one()
        );
    }
}
