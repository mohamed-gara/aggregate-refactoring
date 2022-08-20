package kata.persistence

import kata.MeetupEvent
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

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
    return MeetupEvent(meetupEvents)
  }
}
