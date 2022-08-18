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
  val subscriptions: Subscriptions = Subscriptions(listOf())
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

  fun applyEvent(event: MeetupBaseEvent) = when (event) {
    is MeetupEventRegistered -> MeetupEventState(event.id, event.eventCapacity, event.eventName, event.startTime)
    is MeetupEventCapacityIncreased -> apply(event)
    is UserSubscribedToMeetupEvent -> apply(event)
    is UserAddedToMeetupEventWaitingList -> apply(event)
    is UserCancelledMeetupSubscription -> apply(event)
    is UserMovedFromWaitingListToParticipants -> apply(event)
    is UsersMovedFromWaitingListToParticipants -> apply(event)
  }

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

fun projectStateFrom(events: List<MeetupBaseEvent>): MeetupEventState =
  events.fold(MeetupEventState(0, 0, "", LocalDateTime.MIN)) {
    state, event -> state.applyEvent(event)
  }

data class MeetupEvent(
  val state: MeetupEventState,
  val events: List<MeetupBaseEvent>,
) {

  constructor(state: MeetupEventState) : this(state, listOf())

  fun subscribe(userId: String, registrationTime: Instant): MeetupBaseEvent {
    val event = if (state.isFull) {
      UserAddedToMeetupEventWaitingList(state.id, userId, registrationTime)
    } else {
      UserSubscribedToMeetupEvent(state.id, userId, registrationTime)
    }
    return event
  }

  fun unsubscribe(userId: String, meetupEventId: Long): List<MeetupBaseEvent> {

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

   return listOf(event1, event2)
  }

  fun increaseCapacityTo(newCapacity: Int): List<MeetupBaseEvent> {
    val meetupCapacityIncreased = MeetupEventCapacityIncreased(state.id, newCapacity)

    val movedUsers = UsersMovedFromWaitingListToParticipants(
      state.id,
      state.subscriptions.firstInWaitingList(newCapacity - state.capacity).map { it.userId },
      meetupCapacityIncreased
    )

    return listOf(meetupCapacityIncreased, movedUsers)
  }
}

private fun listOf(
  event1: Optional<UserCancelledMeetupSubscription>,
  event2: Optional<UserMovedFromWaitingListToParticipants>
): List<MeetupBaseEvent> = Stream.concat(event1.stream(), event2.stream())
  .toList()
