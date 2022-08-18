package kata

import java.time.LocalDateTime
import java.util.*

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

  fun cancelSubscription(userId: String): MeetupEvent {
    val (updatedEvent, removedSub) = remove(userId)

    return removedSub.filter { !it.isInWaitingList }
      .flatMap { state.subscriptions.firstInWaitingList }
      .map { firstInWaitingList -> updatedEvent.confirm(firstInWaitingList) }
      .orElse(updatedEvent)
  }

  private fun confirm(subscription: Subscription): MeetupEvent {
    val newSubscriptions = state.subscriptions.confirm(subscription)
    return copy(state = state.copy(subscriptions = newSubscriptions))
  }

  private fun remove(userId: String): Pair<MeetupEvent, Optional<Subscription>> {
    val (newSubscriptions, removedSubscription) = state.subscriptions.removeBy(userId)

    val updatedEvent = copy(state = state.copy(subscriptions = newSubscriptions))
    return Pair(updatedEvent, removedSubscription)
  }
}
