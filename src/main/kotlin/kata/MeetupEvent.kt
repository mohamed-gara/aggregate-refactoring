package kata

import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream


data class MeetupEventState(
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

  fun applyEvent(event: MeetupBaseEvent): MeetupEventState {
    val newState = when (event) {
      is MeetupEventRegistered -> MeetupEventState(event.id, event.eventCapacity, event.eventName, event.startTime)
      is MeetupEventCapacityIncreased -> apply(event)
      is UserSubscribedToMeetupEvent -> apply(event)
      is UserAddedToMeetupEventWaitingList -> apply(event)
      is UserCancelledMeetupSubscription -> apply(event)
      is UserMovedFromWaitingListToParticipants -> apply(event)
      is UsersMovedFromWaitingListToParticipants -> apply(event)
    }
    return newState.incrementLastEventIndex()
  }

  private fun incrementLastEventIndex() =
    copy(lastAppliedEventIndex = lastAppliedEventIndex + 1)

  private fun apply(event: MeetupEventCapacityIncreased) =
    copy(capacity = event.newCapacity)

  private fun apply(event: UserSubscribedToMeetupEvent): MeetupEventState {
    val subscription = Subscription(event.userId, event.registrationTime, false)
    val newSubscriptions = subscriptions.add(subscription)
    return copy(subscriptions = newSubscriptions)
  }

  private fun apply(event: UserAddedToMeetupEventWaitingList): MeetupEventState {
    val subscription = Subscription(event.userId, event.registrationTime, true)
    val newSubscriptions = subscriptions.add(subscription)
    return copy(subscriptions = newSubscriptions)
  }

  private fun apply(event: UserCancelledMeetupSubscription): MeetupEventState {
    val (newSubscriptions) = subscriptions.removeBy(event.userId)
    return copy(subscriptions = newSubscriptions)
  }

  private fun apply(event: UserMovedFromWaitingListToParticipants): MeetupEventState {
    val userSubscription = subscriptions.findBy(event.userId)
    val newSubscriptions =
      if (userSubscription != null) subscriptions.confirm(userSubscription) else subscriptions
    return copy(subscriptions = newSubscriptions)
  }

  private fun apply(event: UsersMovedFromWaitingListToParticipants): MeetupEventState {
    val subscriptions = event.userIdList.mapNotNull { subscriptions.findBy(it) }
    val newSubscriptions = this.subscriptions.confirm(subscriptions)
    return copy(subscriptions = newSubscriptions)
  }
}

fun projectStateFrom(
  events: List<MeetupBaseEvent>,
  snapshot: MeetupEventState = MeetupEventState(0, 0, "", LocalDateTime.MIN),
): MeetupEventState =
  events.drop(snapshot.lastAppliedEventIndex + 1)
    .fold(snapshot) { state, event ->
      state.applyEvent(event)
    }

fun newMeetup(
  id: Long,
  eventName: String,
  eventCapacity: Int,
  startTime: LocalDateTime
): MeetupEvent {
  val meetupEventRegistered = MeetupEventRegistered(id, eventName, eventCapacity, startTime)
  return MeetupEvent(listOf(meetupEventRegistered))
}

data class MeetupEvent(
  val state: MeetupEventState,
  val events: List<MeetupBaseEvent>,
  val version: Int = -1,
) {

  constructor(events: List<MeetupBaseEvent>) : this(projectStateFrom(events), events)
  constructor(events: List<MeetupBaseEvent>, version: Int) : this(projectStateFrom(events), events, version)

  fun subscribe(userId: String, registrationTime: Instant): MeetupEvent {
    val event = if (state.isFull) {
      UserAddedToMeetupEventWaitingList(state.id, userId, registrationTime)
    } else {
      UserSubscribedToMeetupEvent(state.id, userId, registrationTime)
    }
    return this.apply(event)
  }

  fun unsubscribe(userId: String, meetupEventId: Long): MeetupEvent {

    val event1 = state.subscriptions.subscriptionOf(userId)
      .map { UserCancelledMeetupSubscription(meetupEventId, userId) }

    val event2 = state.subscriptions.subscriptionOf(userId)
      .filter { !it.isInWaitingList }
      .flatMap { state.subscriptions.firstInWaitingList }
      .map { firstInWaitingList -> UserMovedFromWaitingListToParticipants(
        meetupEventId,
        firstInWaitingList.userId,
        event1.get()
      ) }

   return this.apply(listOf(event1, event2))
  }

  fun increaseCapacityTo(newCapacity: Int): MeetupEvent {
    val meetupCapacityIncreased = MeetupEventCapacityIncreased(state.id, newCapacity)

    val movedUsers = UsersMovedFromWaitingListToParticipants(
      state.id,
      state.subscriptions.firstInWaitingList(newCapacity - state.capacity).map { it.userId },
      meetupCapacityIncreased
    )

    return this.apply(listOf(meetupCapacityIncreased, movedUsers))
  }

  private fun apply(event: MeetupBaseEvent) = apply(listOf(event))

  private fun apply(newEvents: List<MeetupBaseEvent>) = MeetupEvent(
    events = this.events + newEvents,
    state = projectStateFrom(this.events + newEvents, state),
    version = version,
  )
}

private fun listOf(
  event1: Optional<UserCancelledMeetupSubscription>,
  event2: Optional<UserMovedFromWaitingListToParticipants>
): List<MeetupBaseEvent> = Stream.concat(event1.stream(), event2.stream())
  .toList()
