package kata

import java.time.LocalDateTime
import java.time.Instant
import java.util.*


data class MeetupEventState(
  val id: Long,
  val capacity: Int,
  val eventName: String,
  val startTime: LocalDateTime,
  val subscriptions: Subscriptions = Subscriptions(listOf())
) {
  val users: List<String>
    get() = subscriptions.users

  val waitingList: List<Subscription>
    get() = subscriptions.waitingList

  val participants: List<Subscription>
    get() = subscriptions.participants

  val isFull: Boolean
    get() = participants.size == capacity

  fun hasSubscriptionFor(userId: String): Boolean {
    return subscriptions.findBy(userId) != null
  }
}

fun projectStateFrom(events: List<MeetupBaseEvent>): MeetupEventState =
  events.fold(MeetupEventState(0, 0, "", LocalDateTime.MIN)) {
    state, event -> when(event) {
      is MeetupEventRegistered -> MeetupEventState(event.id, event.eventCapacity, event.eventName, event.startTime)
      is MeetupEventCapacityIncreased -> state.copy(capacity = event.newCapacity)
      is UserSubscribedToMeetupEvent -> {
        val subscription = Subscription(event.userId, event.registrationTime, false)
        val newSubscriptions = state.subscriptions.add(subscription)
        return@fold state.copy(subscriptions = newSubscriptions)
      }
      is UserAddedToMeetupEventWaitingList -> {
        val subscription = Subscription(event.userId, event.registrationTime, true)
        val newSubscriptions = state.subscriptions.add(subscription)
        return@fold state.copy(subscriptions = newSubscriptions)
      }
      is UserCancelledMeetupSubscription -> {
        val (newSubscriptions) = state.subscriptions.removeBy(event.userId)
        return@fold state.copy(subscriptions = newSubscriptions)
      }
      is UserMovedFromWaitingListToParticipants -> {
        val userSubscription = state.subscriptions.findBy(event.userId)
        val newSubscriptions = if(userSubscription != null) state.subscriptions.confirm(userSubscription) else state.subscriptions
        return@fold state.copy(subscriptions = newSubscriptions)
      }
      is UsersMovedFromWaitingListToParticipants -> {
        val subscriptions = event.userIdList.mapNotNull { state.subscriptions.findBy(it) }
        val newSubscriptions = state.subscriptions.confirm(subscriptions)
        return@fold state.copy(subscriptions = newSubscriptions)
      }
    }
  }

data class MeetupEvent(
  val state: MeetupEventState,
  val events: List<MeetupBaseEvent>,
) {

  constructor(state: MeetupEventState) : this(state, listOf())

  fun subscribe(userId: String, registrationTime: Instant): MeetupEvent {
    val subscription = Subscription(userId, registrationTime, state.isFull)
    val newSubscriptions = state.subscriptions.add(subscription)

    return copy(state = state.copy(subscriptions = newSubscriptions))
  }

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

  private fun confirm(toConfirm: List<Subscription>): MeetupEvent {
    val newSubscriptions = state.subscriptions.confirm(toConfirm)
    return copy(state = state.copy(subscriptions = newSubscriptions))
  }

  private fun remove(userId: String): Pair<MeetupEvent, Optional<Subscription>> {
    val (newSubscriptions, removedSubscription) = state.subscriptions.removeBy(userId)

    val updatedEvent = copy(state = state.copy(subscriptions = newSubscriptions))
    return Pair(updatedEvent, removedSubscription)
  }

  fun updateCapacityTo(newCapacity: Int): MeetupEvent {
    val newSlots = newCapacity - state.capacity
    val toConfirm = state.subscriptions.firstInWaitingList(newSlots)

    return withCapacity(newCapacity).confirm(toConfirm)
  }

  private fun withCapacity(capacity: Int): MeetupEvent {
    return copy(state = state.copy(capacity = capacity))
  }
}
