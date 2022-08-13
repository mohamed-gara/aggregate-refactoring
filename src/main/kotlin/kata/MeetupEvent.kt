package kata

import java.time.LocalDateTime
import java.time.Instant
import java.util.*

data class MeetupEvent(
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

  val users: List<String>
    get() = subscriptions.users

  val isFull: Boolean
    get() = participants.size == capacity

  fun hasSubscriptionFor(userId: String): Boolean {
    return subscriptions.findBy(userId) != null
  }

  fun subscribe(userId: String): MeetupEvent {
    val subscription = Subscription(userId, Instant.now(), isFull)
    val newSubscriptions = subscriptions.add(subscription)

    return copy(subscriptions = newSubscriptions)
  }

  fun cancelSubscription(userId: String): MeetupEvent {
    val (updatedEvent, removedSub) = remove(userId)

    return removedSub.filter { !it.isInWaitingList }
      .flatMap { subscriptions.firstInWaitingList }
      .map { firstInWaitingList -> updatedEvent.confirm(firstInWaitingList) }
      .orElse(updatedEvent)
  }

  private fun confirm(subscription: Subscription): MeetupEvent {
    val newSubscriptions = subscriptions.confirm(subscription)
    return copy(subscriptions = newSubscriptions)
  }

  private fun confirm(toConfirm: List<Subscription>): MeetupEvent {
    val newSubscriptions = subscriptions.confirm(toConfirm)
    return copy(subscriptions = newSubscriptions)
  }

  private fun remove(userId: String): Pair<MeetupEvent, Optional<Subscription>> {
    val (newSubscriptions, removedSubscription) = subscriptions.removeBy(userId)

    val updatedEvent = copy(subscriptions = newSubscriptions)
    return Pair(updatedEvent, removedSubscription)
  }

  fun updateCapacityTo(newCapacity: Int): MeetupEvent {
    val newSlots = newCapacity - capacity
    val toConfirm = subscriptions.firstInWaitingList(newSlots)

    return withCapacity(newCapacity).confirm(toConfirm)
  }

  private fun withCapacity(capacity: Int): MeetupEvent {
    return copy(capacity = capacity)
  }
}
