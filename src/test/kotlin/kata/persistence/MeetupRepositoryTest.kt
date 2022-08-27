package kata.persistence

import kata.Meetup
import kata.MeetupRegistered
import kata.UserSubscribedToMeetup
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class MeetupRepositoryTest {

  lateinit var sut: MeetupRepository
  val eventStore = InMemoryEventStore()

  @BeforeEach fun setUp() {
    sut = MeetupRepository(eventStore)
  }

  @Test fun create_meetup_event() {
    eventStore.append(MeetupRegistered(1, "eventName", 50, LocalDateTime.of(2022, 1, 2, 6, 0)))
    val subscriptionTime = LocalDateTime.of(2022, 1, 1, 6, 0).toInstant(ZoneOffset.UTC)
    eventStore.append(UserSubscribedToMeetup(1, "userId", subscriptionTime))

    val result = sut.findById(1L)

    Assertions.assertThat(result)
      .usingRecursiveComparison()
      .isEqualTo(meetup_event())
  }

  fun meetup_event(): Meetup {
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