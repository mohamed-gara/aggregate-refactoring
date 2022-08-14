package kata

import kata.persistence.Event
import java.time.LocalDateTime


sealed class MeetupBaseEvent(
  override val id: Long,
): Event

data class MeetupEventRegistered(
  override val id: Long,
  val eventName: String,
  val eventCapacity: Int,
  val startTime: LocalDateTime
) : MeetupBaseEvent(id)

data class UserSubscribedToMeetupEvent(
  override val id: Long,
  val userId: String,
) : MeetupBaseEvent(id)

data class UserAddedToMeetupEventWaitingList(
  override val id: Long,
  val userId: String,
) : MeetupBaseEvent(id)

data class UserCancelledMeetupSubscription(
  override val id: Long,
  val userId: String,
) : MeetupBaseEvent(id)

data class MeetupEventCapacityIncreased(
  override val id: Long,
  val newCapacity: Int
) : MeetupBaseEvent(id)

data class UserMovedFromWaitingListToParticipants(
  override val id: Long,
  val userId: String,
  val cause: Event
) : MeetupBaseEvent(id)

data class UsersMovedFromWaitingListToParticipants(
  override val id: Long,
  val userIdList: List<String>,
  val cause: Event
) : MeetupBaseEvent(id)