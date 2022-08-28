package kata.meetup.infra

import kata.meetup.domain.MeetupRepository
import kata.meetup.domain.Meetup
import kata.meetup.domain.MeetupState
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import java.lang.Integer.max
import java.util.concurrent.atomic.AtomicLong

class MeetupRepository(
  private val eventStore: EventStore,
) : MeetupRepository {

  private val counter = AtomicLong(0)
  private val client = KMongo.createClient()
  private val database = client.getDatabase("test")
  private val collection = database.getCollection<MeetupState>("meetups")

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

    collection.save(meetup.state)
  }

  override fun findAll(): List<MeetupState> {
    return collection.find().toList()
  }
}
