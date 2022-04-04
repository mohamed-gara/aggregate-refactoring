package kata

import kata.persistence.MeetupEventRepository
import java.time.LocalDateTime
import kata.MeetupEvent
import java.lang.RuntimeException
import kata.MeetupEventStatusDto
import java.util.List
import java.util.stream.Collectors

class MeetupSubscribe(
  private val repository: MeetupEventRepository,
) {

  fun registerMeetupEvent(eventName: String, eventCapacity: Int, startTime: LocalDateTime): Long {
    val id = repository.generateId()
    val meetupEvent = MeetupEvent(id, eventCapacity, eventName, startTime)

    repository.save(meetupEvent)
    return id
  }

  fun subscribeUserToMeetupEvent(userId: String, meetupEventId: Long) {
    val meetup = repository.findById(meetupEventId)

    if (meetup.hasSubscriptionFor(userId)) {
      throw RuntimeException(String.format("User %s already has a subscription", userId))
    }

    meetup.subscribe(userId)

    repository.save(meetup)
  }

  fun cancelUserSubscription(userId: String, meetupEventId: Long) {
    val meetup = repository.findById(meetupEventId)

    meetup.cancelSubscription(userId)

    repository.save(meetup)
  }

  fun increaseCapacity(meetupEventId: Long, newCapacity: Int) {
    val meetupEvent = repository.findById(meetupEventId)

    val oldCapacity = meetupEvent.capacity
    if (oldCapacity < newCapacity) {
      val updatedMeetupEvent = meetupEvent.updateCapacityTo(newCapacity)
      repository.save(updatedMeetupEvent)
    }
  }

  fun getMeetupEventStatus(meetupEventId: Long): MeetupEventStatusDto {
    val meetupEvent = repository.findById(meetupEventId)

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
