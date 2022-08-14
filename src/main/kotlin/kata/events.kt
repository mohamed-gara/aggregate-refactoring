package kata

import kata.persistence.Event
import java.time.LocalDateTime

data class MeetupEventRegistered(
  override val id: Long,
  val eventName: String,
  val eventCapacity: Int,
  val startTime: LocalDateTime
) : Event

data class UserSubscribedToMeetupEvent(
  override val id: Long,
  val userId: String,
) : Event

data class UserAddedToMeetupEventWaitingList(
  override val id: Long,
  val userId: String,
) : Event

data class UserCancelledMeetupSubscription(
  override val id: Long,
  val userId: String,
) : Event

data class MeetupEventCapacityIncreased(
  override val id: Long,
  val newCapacity: Int
) : Event

data class UserMovedFromWaitingListToParticipants(
  override val id: Long,
  val userId: String,
  val cause: Event
) : Event

data class UsersMovedFromWaitingListToParticipants(
  override val id: Long,
  val userIdList: List<String>,
  val cause: Event
) : Event