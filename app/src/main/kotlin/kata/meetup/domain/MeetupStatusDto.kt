package kata.meetup.domain

import java.time.LocalDateTime

class MeetupStatusDto(
  val meetupId: Long,
  val eventName: String,
  val eventCapacity: Int,
  val startTime: LocalDateTime,
  val participants: List<String>,
  val waitingList: List<String>,
) {
}
