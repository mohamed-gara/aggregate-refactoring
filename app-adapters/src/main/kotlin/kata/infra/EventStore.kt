package kata.infra

import kata.meetup.domain.MeetupEvent

interface EventStore {
  fun append(event: MeetupEvent)
  fun readStream(id: Long): List<MeetupEvent>
}