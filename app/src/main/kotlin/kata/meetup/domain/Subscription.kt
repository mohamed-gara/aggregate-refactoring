package kata.meetup.domain

import java.time.Instant

data class Subscription(
  val userId: String,
  val registrationTime: Instant,
  var isInWaitingList: Boolean,
) {
  fun confirm(): Subscription {
    return copy(isInWaitingList = false)
  }
}
