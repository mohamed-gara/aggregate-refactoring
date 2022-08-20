package kata.persistence

import kata.MeetupBaseEvent
import kata.MeetupEventCapacityIncreased
import org.assertj.core.api.Assertions
import org.assertj.core.api.ListAssert
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.Test


internal class InMemoryEventStoreTest {

  val eventStore = InMemoryEventStore()

  @Test fun append_events() {

    eventStore.append(MeetupEventCapacityIncreased(100, 20))
    eventStore.append(MeetupEventCapacityIncreased(200, 20))
    eventStore.append(MeetupEventCapacityIncreased(300, 20))
    eventStore.append(MeetupEventCapacityIncreased(100, 20))

    assertThatEventStore()
      .containsExactly(
        MeetupEventCapacityIncreased(100, 20),
        MeetupEventCapacityIncreased(200, 20),
        MeetupEventCapacityIncreased(300, 20),
        MeetupEventCapacityIncreased(100, 20),
      )
  }

  @Test fun fetch_stream_events() {

    eventStore.append(MeetupEventCapacityIncreased(100, 20))
    eventStore.append(MeetupEventCapacityIncreased(200, 20))
    eventStore.append(MeetupEventCapacityIncreased(300, 20))
    eventStore.append(MeetupEventCapacityIncreased(100, 20))

    assertThat(eventStore.readStream(100))
      .containsExactly(
        MeetupEventCapacityIncreased(100, 20),
        MeetupEventCapacityIncreased(100, 20),
      )
  }

  private fun assertThat(list: List<MeetupBaseEvent>) = Assertions.assertThat(list)
    .usingRecursiveFieldByFieldElementComparator(
      RecursiveComparisonConfiguration.builder()
        .withStrictTypeChecking(true)
        .build()
    )

  private fun assertThatEventStore(): ListAssert<MeetupBaseEvent> = Assertions.assertThat(eventStore.events)
    .usingRecursiveFieldByFieldElementComparator(
      RecursiveComparisonConfiguration.builder()
        .withStrictTypeChecking(true)
        .build()
    )
}
