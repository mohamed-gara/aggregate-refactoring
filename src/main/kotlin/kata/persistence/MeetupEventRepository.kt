package kata.persistence

import kata.MeetupEvent
import kata.MeetupEventState
import java.lang.Integer.max
import java.util.concurrent.atomic.AtomicLong

class MeetupEventRepository(
  private val eventStore: EventStore,
) {

  private val counter = AtomicLong(0)
  private val meetups = mutableMapOf<Long, MeetupEventState>()

  fun generateId(): Long {
    return counter.incrementAndGet()
  }

  fun findById(meetupEventId: Long): MeetupEvent {
    val meetupEvents = eventStore.readStream(meetupEventId)
    return MeetupEvent(meetupEvents, meetupEvents.size)
  }

  fun save(meetup: MeetupEvent) {
    meetup.events
      .drop(max(meetup.version, 0))
      .forEach(eventStore::append)

    meetups[meetup.state.id] = meetup.state
  }

  fun findAll(): List<MeetupEventState> {
    return meetups.values.toList()
  }
}
