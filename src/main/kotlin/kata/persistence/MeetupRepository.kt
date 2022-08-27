package kata.persistence

import kata.Meetup
import kata.MeetupState
import java.lang.Integer.max
import java.util.concurrent.atomic.AtomicLong

class MeetupRepository(
  private val eventStore: EventStore,
) {

  private val counter = AtomicLong(0)
  private val meetups = mutableMapOf<Long, MeetupState>()

  fun generateId(): Long {
    return counter.incrementAndGet()
  }

  fun findById(meetupId: Long): Meetup {
    val meetups = eventStore.readStream(meetupId)
    return Meetup(meetups, meetups.size)
  }

  fun save(meetup: Meetup) {
    meetup.events
      .drop(max(meetup.version, 0))
      .forEach(eventStore::append)

    meetups[meetup.state.id] = meetup.state
  }

  fun findAll(): List<MeetupState> {
    return meetups.values.toList()
  }
}
