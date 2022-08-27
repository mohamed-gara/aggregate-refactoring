package kata.meetup.application

import kata.meetup.infra.MeetupStatusDto
import kata.meetup.domain.Subscription
import kata.meetup.infra.MeetupRepository
import kata.meetup.domain.newMeetup
import java.time.LocalDateTime
import java.lang.RuntimeException
import java.time.Clock
import java.time.Instant
import java.util.stream.Collectors

class MeetupService(
  private val repository: MeetupRepository,
  private val clock: Clock,
) {

  fun registerMeetup(eventName: String, eventCapacity: Int, startTime: LocalDateTime): Long {
    val id = repository.generateId()
    val meetup = newMeetup(id, eventName, eventCapacity, startTime)
    repository.save(meetup)
    return id
  }

  fun subscribeUserToMeetup(userId: String, meetupId: Long) {
    val meetup = repository.findById(meetupId)

    if (meetup.state.hasSubscriptionFor(userId)) {
      throw RuntimeException(String.format("User %s already has a subscription", userId))
    }

    val updateMeetup = meetup.subscribe(userId, Instant.now(clock))
    repository.save(updateMeetup)
  }

  fun cancelUserSubscription(userId: String, meetupId: Long) {
    val meetup = repository.findById(meetupId)

    val updatedMeetup = meetup.unsubscribe(userId, meetupId)

    repository.save(updatedMeetup)
  }

  fun increaseCapacity(meetupId: Long, newCapacity: Int) {
    val meetup = repository.findById(meetupId)

    val oldCapacity = meetup.state.capacity
    if (oldCapacity < newCapacity) {
      val updatedMeetup = meetup.increaseCapacityTo(newCapacity)
      repository.save(updatedMeetup)
    }
  }

  fun getMeetupStatus(meetupId: Long): MeetupStatusDto {
    val meetup = repository.findById(meetupId).state

    return MeetupStatusDto(
      meetupId = meetup.id,
      eventCapacity = meetup.capacity,
      startTime = meetup.startTime,
      eventName = meetup.eventName,
      waitingList = meetup.waitingList.stream().map { obj: Subscription -> obj.userId }.collect(Collectors.toList()),
      participants = meetup.participants.stream().map { obj: Subscription -> obj.userId }.collect(Collectors.toList()),
    )
  }
}
