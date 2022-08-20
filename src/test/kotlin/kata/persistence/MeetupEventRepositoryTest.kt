package kata.persistence

import kata.*
import kata.dbtestutil.MemoryDbTestContext
import kata.dbtestutil.MemoryDbTestContext.Companion.openWithSql
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class MeetupEventRepositoryTest {

  lateinit var memoryDbTestContext: MemoryDbTestContext
  lateinit var sut: MeetupEventRepository
  val eventStore = InMemoryEventStore()

  @BeforeEach fun setUp() {
    memoryDbTestContext = openWithSql("/setup.sql")
    val jdbi = memoryDbTestContext.jdbi
    sut = MeetupEventRepository(jdbi, eventStore)
  }

  @AfterEach fun tearDown() {
    memoryDbTestContext.close()
  }

  @Test fun create_meetup_event() {
    eventStore.append(MeetupEventRegistered(1, "eventName", 50, LocalDateTime.of(2022, 1, 2, 6, 0)))
    val subscriptionTime = LocalDateTime.of(2022, 1, 1, 6, 0).toInstant(ZoneOffset.UTC)
    eventStore.append(UserSubscribedToMeetupEvent(1, "userId", subscriptionTime))

    val result = sut.findById(1L)

    Assertions.assertThat(result)
      .usingRecursiveComparison()
      .isEqualTo(meetup_event())
  }

  fun meetup_event(): MeetupEvent {
    val startTime = LocalDateTime.of(2022, 1, 2, 6, 0)
    val subscriptionTime = LocalDateTime.of(2022, 1, 1, 6, 0).toInstant(ZoneOffset.UTC)
    return MeetupEvent(listOf(
      MeetupEventRegistered(1L,"eventName", 50, startTime),
      UserSubscribedToMeetupEvent(1, "userId", subscriptionTime),
    ))
  }
}