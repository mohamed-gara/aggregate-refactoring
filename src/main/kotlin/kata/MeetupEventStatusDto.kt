package kata

import java.time.LocalDateTime

class MeetupEventStatusDto(
  val meetupId: Long,
  val eventName: String,
  val eventCapacity: Int,
  val startTime: LocalDateTime,
  val participants: List<String>,
  val waitingList: List<String>,
) {
}
