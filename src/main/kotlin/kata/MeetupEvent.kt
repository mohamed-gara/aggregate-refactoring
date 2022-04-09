package kata

import java.time.LocalDateTime
import java.time.Instant
import java.util.*
import java.util.Comparator.comparing

data class MeetupEvent(
  val id: Long,
  val capacity: Int,
  val eventName: String,
  val startTime: LocalDateTime,
  val subscriptions: List<Subscription> = listOf()
) {

  private fun withCapacity(capacity: Int): MeetupEvent {
    return copy(capacity = capacity)
  }

  private fun getSubscription(userId: String): Subscription? {
    return subscriptions.stream()
      .filter { it.userId == userId }
      .findAny()
      .orElse(null)
  }

  val waitingList: List<Subscription>
    get() = subscriptions.stream()
      .filter { it.isInWaitingList }
      .sorted(comparing { it.registrationTime })
      .toList()

  val participants: List<Subscription>
    get() = subscriptions.stream()
      .filter { !it.isInWaitingList }
      .sorted(comparing { it.registrationTime })
      .toList()

  val users: List<String>
    get() = subscriptions.stream()
      .map { it.userId }
      .toList()

  fun hasSubscriptionFor(userId: String): Boolean {
    return getSubscription(userId) != null
  }

  fun subscribe(userId: String): MeetupEvent {
    val subscription = Subscription(userId, Instant.now(), isFull)
    val newSubscriptions = subscriptions + subscription
    return copy(subscriptions=newSubscriptions)
  }

  private val isFull: Boolean
    private get() = participants.size == capacity

  fun cancelSubscription(userId: String): MeetupEvent {
    val (updatedEvent, removedSub) = remove(userId)

    return removedSub.filter { !it.isInWaitingList }
      .flatMap { firstInWaitingList }
      .map { firstInWaitingList -> updatedEvent.confirm(firstInWaitingList) }
      .orElse(updatedEvent)
  }

  private fun confirm(subscription: Subscription): MeetupEvent {
    val newSubscriptions = subscriptions.map {
      if (it.userId == subscription.userId) it.confirm() else it
    }
    return copy(subscriptions=newSubscriptions.toMutableList())
  }

  private fun confirm(toConfirm: List<Subscription>): MeetupEvent {
    val newSubscriptions = subscriptions.map {
      if (toConfirm.contains(it)) it.confirm() else it
    }
    return copy(subscriptions=newSubscriptions.toMutableList())
  }

  private val firstInWaitingList: Optional<Subscription>
    private get() =
      if (waitingList.isEmpty()) Optional.empty()
      else Optional.of(waitingList.first())

  private fun remove(userId: String): Pair<MeetupEvent, Optional<Subscription>> {
    val toDelete: Optional<Subscription> = subscriptions.stream()
      .filter { it.userId == userId }
      .findFirst()

    val newSubscriptions: List<Subscription> = toDelete
      .map {subscriptions - it }
      .orElse(subscriptions)

    val updatedEvent = copy(subscriptions = newSubscriptions.toMutableList())
    return Pair(updatedEvent, toDelete)
  }

  fun updateCapacityTo(newCapacity: Int): MeetupEvent {
    val newSlots = newCapacity - capacity
    val toConfirm = waitingList
      .stream()
      .limit(newSlots.toLong())
      .toList()

    return withCapacity(newCapacity).confirm(toConfirm)
  }
}
