package kata

import kata.persistence.Event
import java.time.LocalDateTime

data class MeetupEventRegistered(
  val id: Long,
  val eventName: String,
  val eventCapacity: Int,
  val startTime: LocalDateTime
) : Event

data class UserSubscribedToMeetupEvent(
  val id: Long,
  val userId: String,
) : Event

data class UserAddedToMeetupEventWaitingList(
  val id: Long,
  val userId: String,
) : Event

data class UserCancelledMeetupSubscription(
  val id: Long,
  val userId: String,
) : Event
