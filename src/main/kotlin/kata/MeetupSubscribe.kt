package kata

import kata.persistence.MeetupEventRepository
import java.time.LocalDateTime
import java.lang.RuntimeException
import kata.persistence.EventStore
import java.time.Clock
import java.time.Instant
import java.util.stream.Collectors

class MeetupSubscribe(
  private val repository: MeetupEventRepository,
  private val eventStore: EventStore,
  private val clock: Clock,
) {

  fun registerMeetupEvent(eventName: String, eventCapacity: Int, startTime: LocalDateTime): Long {
    val id = repository.generateId()
    val meetupEventRegistered = MeetupEventRegistered(id, eventName, eventCapacity, startTime)
    eventStore.append(meetupEventRegistered)

    return id
  }

  fun subscribeUserToMeetupEvent(userId: String, meetupEventId: Long) {
    val meetup = repository.findById(meetupEventId)

    if (meetup.state.hasSubscriptionFor(userId)) {
      throw RuntimeException(String.format("User %s already has a subscription", userId))
    }

    val event = meetup.subscribe(userId, Instant.now(clock))
    eventStore.append(event)
  }

  fun cancelUserSubscription(userId: String, meetupEventId: Long) {
    val meetup = repository.findById(meetupEventId)

    val eventList = meetup.unsubscribe(userId, meetupEventId)

    eventList.forEach(eventStore::append)
  }

  fun increaseCapacity(meetupEventId: Long, newCapacity: Int) {
    val meetupEvent = repository.findById(meetupEventId)

    val oldCapacity = meetupEvent.state.capacity
    if (oldCapacity < newCapacity) {
      val eventList = meetupEvent.increaseCapacityTo(newCapacity)
      eventList.forEach(eventStore::append)
    }
  }

  fun getMeetupEventStatus(meetupEventId: Long): MeetupEventStatusDto {
    val meetupEvent = repository.findById(meetupEventId).state

    return MeetupEventStatusDto(
      meetupId = meetupEvent.id,
      eventCapacity = meetupEvent.capacity,
      startTime = meetupEvent.startTime,
      eventName = meetupEvent.eventName,
      waitingList = meetupEvent.waitingList.stream().map { obj: Subscription -> obj.userId }.collect(Collectors.toList()),
      participants = meetupEvent.participants.stream().map { obj: Subscription -> obj.userId }.collect(Collectors.toList()),
    )
  }
}
