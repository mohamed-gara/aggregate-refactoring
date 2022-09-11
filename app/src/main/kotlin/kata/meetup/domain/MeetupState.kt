package kata.meetup.domain

import java.time.LocalDateTime

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