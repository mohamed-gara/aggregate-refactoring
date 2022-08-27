package kata.meetup.infra

import kata.meetup.domain.MeetupRepository
import kata.meetup.domain.Meetup
import kata.meetup.domain.MeetupState
import java.lang.Integer.max
import java.util.concurrent.atomic.AtomicLong

class MeetupRepository(
  private val eventStore: EventStore,
) : MeetupRepository {

  private val counter = AtomicLong(0)
  private val meetups = mutableMapOf<Long, MeetupState>()

  override fun generateId(): Long {
    return counter.incrementAndGet()
  }

  override fun findById(meetupId: Long): Meetup {
    val meetups = eventStore.readStream(meetupId)
    return Meetup(meetups, meetups.size)
  }

  override fun save(meetup: Meetup) {
    meetup.events
      .drop(max(meetup.version, 0))
      .forEach(eventStore::append)

    meetups[meetup.state.id] = meetup.state
  }

  override fun findAll(): List<MeetupState> {
    return meetups.values.toList()
  }
}
