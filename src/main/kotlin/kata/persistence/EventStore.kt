package kata.persistence

interface Event {
}

interface EventStore {
  fun append(event: Event)
}

class InMemoryEventStore : EventStore {
  private val eventList = mutableListOf<Event>()

  val events: List<Event>
    get() = eventList

  override fun append(event: Event) {
    eventList.add(event)
  }
}

