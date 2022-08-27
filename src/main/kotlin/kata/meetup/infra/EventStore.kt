package kata.meetup.infra

import kata.meetup.domain.MeetupEvent


interface EventStore {
  fun append(event: MeetupEvent)
  fun readStream(id: Long): List<MeetupEvent>
}

class InMemoryEventStore : EventStore {
  private val eventList = mutableListOf<MeetupEvent>()

  val events: List<MeetupEvent>
    get() = eventList

  override fun append(event: MeetupEvent) {
    eventList.add(event)
  }

  override fun readStream(id: Long): List<MeetupEvent> {
    return eventList.filter { it.id == id }
  }
}

