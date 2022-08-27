package kata.meetup.domain

import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream


data class MeetupState(
  val id: Long,
  val capacity: Int,
  val eventName: String,
  val startTime: LocalDateTime,
  val subscriptions: Subscriptions = Subscriptions(),
  val lastAppliedEventIndex: Int = -1,
) {

  val waitingList: List<Subscription>
    get() = subscriptions.waitingList

  val participants: List<Subscription>
    get() = subscriptions.participants

  val isFull: Boolean
    get() = participants.size == capacity

  fun hasSubscriptionFor(userId: String): Boolean {
    return subscriptions.findBy(userId) != null
  }

  fun applyEvent(event: MeetupEvent): MeetupState {
    val newState = when (event) {
      is MeetupRegistered -> MeetupState(event.id, event.eventCapacity, event.eventName, event.startTime)
      is MeetupCapacityIncreased -> apply(event)
      is UserSubscribedToMeetup -> apply(event)
      is UserAddedToMeetupWaitingList -> apply(event)
      is UserCancelledMeetupSubscription -> apply(event)
      is UserMovedFromWaitingListToParticipants -> apply(event)
      is UsersMovedFromWaitingListToParticipants -> apply(event)
    }
    return newState.incrementLastEventIndex()
  }

  private fun incrementLastEventIndex() =
    copy(lastAppliedEventIndex = lastAppliedEventIndex + 1)

  private fun apply(event: MeetupCapacityIncreased) =
    copy(capacity = event.newCapacity)

  private fun apply(event: UserSubscribedToMeetup): MeetupState {
    val subscription = Subscription(event.userId, event.registrationTime, false)
    val newSubscriptions = subscriptions.add(subscription)
    return copy(subscriptions = newSubscriptions)
  }

  private fun apply(event: UserAddedToMeetupWaitingList): MeetupState {
    val subscription = Subscription(event.userId, event.registrationTime, true)
    val newSubscriptions = subscriptions.add(subscription)
    return copy(subscriptions = newSubscriptions)
  }

  private fun apply(event: UserCancelledMeetupSubscription): MeetupState {
    val (newSubscriptions) = subscriptions.removeBy(event.userId)
    return copy(subscriptions = newSubscriptions)
  }

  private fun apply(event: UserMovedFromWaitingListToParticipants): MeetupState {
    val userSubscription = subscriptions.findBy(event.userId)
    val newSubscriptions =
      if (userSubscription != null) subscriptions.confirm(userSubscription) else subscriptions
    return copy(subscriptions = newSubscriptions)
  }

  private fun apply(event: UsersMovedFromWaitingListToParticipants): MeetupState {
    val subscriptions = event.userIdList.mapNotNull { subscriptions.findBy(it) }
    val newSubscriptions = this.subscriptions.confirm(subscriptions)
    return copy(subscriptions = newSubscriptions)
  }
}

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
