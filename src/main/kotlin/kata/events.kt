package kata

import kata.persistence.Event
import java.time.LocalDateTime

data class MeetupEventRegistered(
  val id: Long,
  val eventName: String,
  val eventCapacity: Int,
  val startTime: LocalDateTime
) : Event

