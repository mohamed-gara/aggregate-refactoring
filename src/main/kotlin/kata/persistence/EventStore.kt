package kata.persistence

import kata.MeetupBaseEvent


interface EventStore {
  fun append(event: MeetupBaseEvent)
  fun readStream(id: Long): List<MeetupBaseEvent>
}

class InMemoryEventStore : EventStore {
  private val eventList = mutableListOf<MeetupBaseEvent>()

  val events: List<MeetupBaseEvent>
    get() = eventList

  override fun append(event: MeetupBaseEvent) {
    eventList.add(event)
  }

  override fun readStream(id: Long): List<MeetupBaseEvent> {
    return eventList.filter { it.id == id }
  }
}

