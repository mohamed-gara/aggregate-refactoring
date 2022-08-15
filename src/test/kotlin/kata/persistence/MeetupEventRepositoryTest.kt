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
    sut.save(meetup_event())
    eventStore.append(MeetupEventRegistered(1, "eventName", 50, LocalDateTime.of(2022, 1, 2, 6, 0)))

    val result = sut.findById(1L)

    Assertions.assertThat(result)
      .usingRecursiveComparison()
      .isEqualTo(meetup_event())
  }

  fun meetup_event(): MeetupEvent {
    val startTime = LocalDateTime.of(2022, 1, 2, 6, 0)
    val subscriptionTime = LocalDateTime.of(2022, 1, 1, 6, 0).toInstant(ZoneOffset.UTC)
    val subscription = Subscription("userId", subscriptionTime, true)
    return MeetupEvent(MeetupEventState(1L, 50, "eventName", startTime, Subscriptions(listOf(subscription))))
  }
}