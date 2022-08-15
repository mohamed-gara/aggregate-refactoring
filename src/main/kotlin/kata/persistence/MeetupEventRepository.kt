package kata.persistence

import kata.*
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.HandleConsumer
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant
import java.util.function.Consumer

class MeetupEventRepository(
  private val jdbi: Jdbi,
  private val eventStore: EventStore,
) {
  fun generateId(): Long {
    return jdbi.withHandle<Long, RuntimeException> { handle: Handle ->
      handle
        .createQuery("SELECT NEXTVAL('MEETUP_EVENT_ID_SEQ')")
        .mapTo(Long::class.java)
        .one()
    }
  }

  fun findById(meetupEventId: Long): MeetupEvent {
    val meetupEvents = eventStore.readStream(meetupEventId)
    val stateFromEvents = projectStateFrom(meetupEvents as List<MeetupBaseEvent>)

    return MeetupEvent(
      MeetupEventState(
        stateFromEvents.id,
        stateFromEvents.capacity,
        stateFromEvents.eventName,
        stateFromEvents.startTime,
        stateFromEvents.subscriptions,
      )
    )
  }
}
