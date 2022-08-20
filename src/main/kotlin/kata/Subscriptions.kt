package kata

import java.util.*
import java.util.Comparator.comparing

data class Subscriptions(
  val list: List<Subscription> = listOf(),
) {

  val participants: List<Subscription>
    get() = list.stream()
      .filter { !it.isInWaitingList }
      .sorted(comparing { it.registrationTime })
      .toList()

  val waitingList: List<Subscription>
    get() = list.stream()
      .filter { it.isInWaitingList }
      .sorted(comparing { it.registrationTime })
      .toList()

  val firstInWaitingList: Optional<Subscription>
    get() =
      if (waitingList.isEmpty()) Optional.empty()
      else Optional.of(waitingList.first())

  fun findBy(userId: String): Subscription? = subscriptionOf(userId)
    .orElse(null)

  fun subscriptionOf(userId: String): Optional<Subscription> = list.stream()
      .filter { it.userId == userId }
      .findAny()

  fun removeBy(userId: String): Pair<Subscriptions, Optional<Subscription>> {
    val toDelete: Optional<Subscription> = list.stream()
      .filter { it.userId == userId }
      .findFirst()

    val newSubscriptions: List<Subscription> = toDelete
      .map { list - it }
      .orElse(list)

    val updatedSubscriptions = copy(list = newSubscriptions)
    return Pair(updatedSubscriptions, toDelete)
  }

  fun confirm(subscription: Subscription): Subscriptions {
    val newList = list.map {
      if (it.userId == subscription.userId) it.confirm() else it
    }
    return copy(list = newList)
  }

  fun confirm(subscriptions: List<Subscription>): Subscriptions {
    val newList = list.map {
      if (subscriptions.contains(it)) it.confirm() else it
    }
    return copy(list = newList)
  }

  fun firstInWaitingList(number: Int): List<Subscription> =
    waitingList.stream()
      .limit(number.toLong())
      .toList()

  fun add(subscription: Subscription) =
    copy(list = list + subscription)
}
