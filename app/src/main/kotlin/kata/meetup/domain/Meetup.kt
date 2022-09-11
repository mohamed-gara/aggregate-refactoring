package kata.meetup.domain

import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream


fun projectStateFrom(
  events: List<MeetupEvent>,
  snapshot: MeetupState = MeetupState(0, 0, "", LocalDateTime.MIN),
): MeetupState =
  events.drop(snapshot.lastAppliedEventIndex + 1)
    .fold(snapshot) { state, event ->
      state.applyEvent(event)
    }

fun newMeetup(
  id: Long,
  eventName: String,
  eventCapacity: Int,
  startTime: LocalDateTime
): Meetup {
  val meetupRegistered = MeetupRegistered(id, eventName, eventCapacity, startTime)
  return Meetup(listOf(meetupRegistered))
}

data class Meetup(
  val state: MeetupState,
  val events: List<MeetupEvent>,
  val version: Int = -1,
) {

  constructor(events: List<MeetupEvent>) : this(projectStateFrom(events), events)
  constructor(events: List<MeetupEvent>, version: Int) : this(projectStateFrom(events), events, version)

  fun subscribe(userId: String, registrationTime: Instant): Meetup {
    val event = if (state.isFull) {
      UserAddedToMeetupWaitingList(state.id, userId, registrationTime)
    } else {
      UserSubscribedToMeetup(state.id, userId, registrationTime)
    }
    return this.apply(event)
  }

  fun unsubscribe(userId: String, meetupId: Long): Meetup {

    val event1 = state.subscriptions.subscriptionOf(userId)
      .map { UserCancelledMeetupSubscription(meetupId, userId) }

    val event2 = state.subscriptions.subscriptionOf(userId)
      .filter { !it.isInWaitingList }
      .flatMap { state.subscriptions.firstInWaitingList }
      .map { firstInWaitingList -> UserMovedFromWaitingListToParticipants(
        meetupId,
        firstInWaitingList.userId,
        event1.get()
      ) }

   return this.apply(listOf(event1, event2))
  }

  fun increaseCapacityTo(newCapacity: Int): Meetup {
    val meetupCapacityIncreased = MeetupCapacityIncreased(state.id, newCapacity)

    val movedUsers = UsersMovedFromWaitingListToParticipants(
      state.id,
      state.subscriptions.firstInWaitingList(newCapacity - state.capacity).map { it.userId },
      meetupCapacityIncreased
    )

    return this.apply(listOf(meetupCapacityIncreased, movedUsers))
  }

  private fun apply(event: MeetupEvent) = apply(listOf(event))

  private fun apply(newEvents: List<MeetupEvent>) = Meetup(
    events = this.events + newEvents,
    state = projectStateFrom(this.events + newEvents, state),
    version = version,
  )
}

private fun listOf(
  event1: Optional<UserCancelledMeetupSubscription>,
  event2: Optional<UserMovedFromWaitingListToParticipants>
): List<MeetupEvent> = Stream.concat(event1.stream(), event2.stream())
  .toList()
