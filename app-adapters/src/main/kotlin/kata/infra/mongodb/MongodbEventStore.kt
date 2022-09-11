package kata.infra.mongodb

import kata.infra.EventStore
import kata.meetup.domain.MeetupEvent
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import org.litote.kmongo.save


class MongodbEventStore : EventStore {

  private val client = KMongo.createClient()
  private val database = client.getDatabase("test")
  private val collection = database.getCollection<MeetupEvent>("eventLog")

  val events: List<MeetupEvent>
    get() = collection.find().toList()

  override fun append(event: MeetupEvent) {
    collection.save(event)
  }

  override fun readStream(id: Long): List<MeetupEvent> {
    return collection.find().toList()
  }
}

