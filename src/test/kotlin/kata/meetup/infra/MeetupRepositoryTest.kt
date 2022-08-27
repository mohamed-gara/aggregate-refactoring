package kata.meetup.infra

import kata.meetup.domain.Meetup
import kata.meetup.domain.MeetupRegistered
import kata.meetup.domain.UserSubscribedToMeetup
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class MeetupRepositoryTest {

  val eventStore = InMemoryEventStore()
  val sut = MeetupRepository(eventStore)

  @Test fun create_meetup() {
    eventStore.append(MeetupRegistered(1, "eventName", 50, LocalDateTime.of(2022, 1, 2, 6, 0)))
    val subscriptionTime = LocalDateTime.of(2022, 1, 1, 6, 0).toInstant(ZoneOffset.UTC)
    eventStore.append(UserSubscribedToMeetup(1, "userId", subscriptionTime))

    val result = sut.findById(1)

    Assertions.assertThat(result)
      .usingRecursiveComparison()
      .isEqualTo(meetup())
  }

  fun meetup(): Meetup {
    val startTime = LocalDateTime.of(2022, 1, 2, 6, 0)
    val subscriptionTime = LocalDateTime.of(2022, 1, 1, 6, 0).toInstant(ZoneOffset.UTC)
    return Meetup(
      listOf(
        MeetupRegistered(1L,"eventName", 50, startTime),
        UserSubscribedToMeetup(1, "userId", subscriptionTime),
      ),
    2,
    )
  }
}