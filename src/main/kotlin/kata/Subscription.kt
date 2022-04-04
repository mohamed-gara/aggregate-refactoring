package kata

import java.time.Instant

class Subscription(
  val userId: String,
  val registrationTime: Instant,
  var isInWaitingList: Boolean,
) {
  fun confirm(): Boolean {
    return false.also { isInWaitingList = it }
  }
}
