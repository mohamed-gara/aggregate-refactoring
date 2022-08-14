package kata.persistence

interface Event {
  val id: Long
}

interface EventStore {
  fun append(event: Event)
  fun readStream(id: Long): List<Event>
}

class InMemoryEventStore : EventStore {
  private val eventList = mutableListOf<Event>()

  val events: List<Event>
    get() = eventList

  override fun append(event: Event) {
    eventList.add(event)
  }

  override fun readStream(id: Long): List<Event> {
    return eventList.filter { it.id == id }
  }
}

