package kata

import kata.persistence.MeetupEventRepository
import java.time.LocalDateTime
import java.lang.RuntimeException
import java.time.Clock
import java.time.Instant
import java.util.stream.Collectors

class MeetupSubscribe(
  private val repository: MeetupEventRepository,
  private val clock: Clock,
) {

  fun registerMeetupEvent(eventName: String, eventCapacity: Int, startTime: LocalDateTime): Long {
    val id = repository.generateId()
    val meetup = newMeetup(id, eventName, eventCapacity, startTime)
    repository.save(meetup)
    return id
  }

  fun subscribeUserToMeetupEvent(userId: String, meetupEventId: Long) {
    val meetup = repository.findById(meetupEventId)

    if (meetup.state.hasSubscriptionFor(userId)) {
      throw RuntimeException(String.format("User %s already has a subscription", userId))
    }

    val updateMeetup = meetup.subscribe(userId, Instant.now(clock))
    repository.save(updateMeetup)
  }

  fun cancelUserSubscription(userId: String, meetupEventId: Long) {
    val meetup = repository.findById(meetupEventId)

    val updatedMeetup = meetup.unsubscribe(userId, meetupEventId)

    repository.save(updatedMeetup)
  }

  fun increaseCapacity(meetupEventId: Long, newCapacity: Int) {
    val meetupEvent = repository.findById(meetupEventId)

    val oldCapacity = meetupEvent.state.capacity
    if (oldCapacity < newCapacity) {
      val updatedMeetup = meetupEvent.increaseCapacityTo(newCapacity)
      repository.save(updatedMeetup)
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
