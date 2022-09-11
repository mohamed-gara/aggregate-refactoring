package kata.meetup.domain

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant
import java.time.LocalDateTime


@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
sealed class MeetupEvent() {
  abstract val id: Long
}

data class MeetupRegistered(
  override val id: Long,
  val eventName: String,
  val eventCapacity: Int,
  val startTime: LocalDateTime
) : MeetupEvent()

data class UserSubscribedToMeetup(
  override val id: Long,
  val userId: String,
  val registrationTime: Instant,
) : MeetupEvent()

data class UserAddedToMeetupWaitingList(
  override val id: Long,
  val userId: String,
  val registrationTime: Instant,
  ) : MeetupEvent()

data class UserCancelledMeetupSubscription(
  override val id: Long,
  val userId: String,
) : MeetupEvent()

data class MeetupCapacityIncreased(
  override val id: Long,
  val newCapacity: Int
) : MeetupEvent()

data class UserMovedFromWaitingListToParticipants(
  override val id: Long,
  val userId: String,
  val cause: UserCancelledMeetupSubscription
) : MeetupEvent()

data class UsersMovedFromWaitingListToParticipants(
  override val id: Long,
  val userIdList: List<String>,
  val cause: MeetupCapacityIncreased
) : MeetupEvent()