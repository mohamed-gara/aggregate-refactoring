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

    val registrationTime = Instant.now(clock)
    val updatedMeetup = meetup.subscribe(userId, registrationTime)

    if (meetup.state.isFull) {
      val userAddedToMeetupEventWaitingList = UserAddedToMeetupEventWaitingList(meetupEventId, userId, registrationTime)
      eventStore.append(userAddedToMeetupEventWaitingList)
    } else {
      val userSubscribedToMeetupEvent = UserSubscribedToMeetupEvent(meetupEventId, userId, registrationTime)
      eventStore.append(userSubscribedToMeetupEvent)
    }

    repository.save(updatedMeetup)
  }

  fun cancelUserSubscription(userId: String, meetupEventId: Long) {
    val meetup = repository.findById(meetupEventId)

    val updatedMeetupEvent = meetup.cancelSubscription(userId)

    repository.save(updatedMeetupEvent)

    val userCanceledSubscription = UserCancelledMeetupSubscription(meetupEventId, userId)
    eventStore.append(userCanceledSubscription)

    if (meetup.state.participants.any { it.userId == userId }) {
      val userMovedToParticipants = UserMovedFromWaitingListToParticipants(
        meetupEventId,
        updatedMeetupEvent.state.participants.last().userId,
        userCanceledSubscription
      )
      eventStore.append(userMovedToParticipants)
    }
  }

  fun increaseCapacity(meetupEventId: Long, newCapacity: Int) {
    val meetupEvent = repository.findById(meetupEventId)

    val oldCapacity = meetupEvent.state.capacity
    if (oldCapacity < newCapacity) {
      val updatedMeetupEvent = meetupEvent.updateCapacityTo(newCapacity)
      repository.save(updatedMeetupEvent)

      val meetupCapacityIncreased = MeetupEventCapacityIncreased(meetupEventId, newCapacity)
      eventStore.append(meetupCapacityIncreased)

      val movedUsers = UsersMovedFromWaitingListToParticipants(
        meetupEventId,
        meetupEvent.state.subscriptions.firstInWaitingList(newCapacity - oldCapacity).map { it.userId },
        meetupCapacityIncreased
      )
      eventStore.append(movedUsers)
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
