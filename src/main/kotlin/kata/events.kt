package kata

import java.time.Instant
import java.time.LocalDateTime


sealed class MeetupBaseEvent() {
  abstract val id: Long
}

data class MeetupEventRegistered(
  override val id: Long,
  val eventName: String,
  val eventCapacity: Int,
  val startTime: LocalDateTime
) : MeetupBaseEvent()

data class UserSubscribedToMeetupEvent(
  override val id: Long,
  val userId: String,
  val registrationTime: Instant,
) : MeetupBaseEvent()

data class UserAddedToMeetupEventWaitingList(
  override val id: Long,
  val userId: String,
  val registrationTime: Instant,
  ) : MeetupBaseEvent()

data class UserCancelledMeetupSubscription(
  override val id: Long,
  val userId: String,
) : MeetupBaseEvent()

data class MeetupEventCapacityIncreased(
  override val id: Long,
  val newCapacity: Int
) : MeetupBaseEvent()

data class UserMovedFromWaitingListToParticipants(
  override val id: Long,
  val userId: String,
  val cause: UserCancelledMeetupSubscription
) : MeetupBaseEvent()

data class UsersMovedFromWaitingListToParticipants(
  override val id: Long,
  val userIdList: List<String>,
  val cause: MeetupEventCapacityIncreased
) : MeetupBaseEvent()