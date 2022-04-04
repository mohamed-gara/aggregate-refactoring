package kata

import java.time.LocalDateTime
import kata.MeetupEvent
import java.time.Instant
import java.util.*
import java.util.Comparator.comparing

data class MeetupEvent(
  val id: Long,
  val capacity: Int,
  val eventName: String,
  val startTime: LocalDateTime,
  val subscriptions: MutableList<Subscription> = mutableListOf()
) {

  private fun withCapacity(capacity: Int): MeetupEvent {
    return copy(capacity=capacity)
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

  fun subscribe(userId: String) {
    val subscription = Subscription(userId, Instant.now(), isFull)
    subscriptions.add(subscription)
  }

  private val isFull: Boolean
    private get() = participants.size == capacity

  fun cancelSubscription(userId: String) {
    val inWaitingList = isInWaitingList(userId)
    remove(userId)

    if (!inWaitingList) {
      firstInWaitingList.ifPresent { it.confirm() }
    }
  }

  private val firstInWaitingList: Optional<Subscription>
    private get() = if (waitingList.isEmpty()) Optional.empty() else Optional.of(
      waitingList[0]
    )

  private fun isInWaitingList(userId: String): Boolean {
    return getSubscription(userId)!!.isInWaitingList
  }

  private fun remove(userId: String) {
    subscriptions.stream()
      .filter { it.userId == userId }
      .findFirst()
      .ifPresent { subscriptions.remove(it) }
  }

  fun updateCapacityTo(newCapacity: Int): MeetupEvent {
    val oldCapacity = capacity
    val updatedMeetupEvent = withCapacity(newCapacity)
    val newSlots = newCapacity - oldCapacity
    waitingList
      .stream()
      .limit(newSlots.toLong())
      .forEach { it.confirm() }
    return updatedMeetupEvent
  }
}
