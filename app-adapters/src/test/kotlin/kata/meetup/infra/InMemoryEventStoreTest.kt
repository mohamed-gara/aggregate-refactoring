package kata.meetup.infra

import kata.meetup.domain.MeetupCapacityIncreased
import kata.meetup.domain.MeetupEvent
import kata.infra.memory.InMemoryEventStore
import org.assertj.core.api.Assertions
import org.assertj.core.api.ListAssert
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.Test


internal class InMemoryEventStoreTest {

  val eventStore = InMemoryEventStore()

  @Test fun append_events() {

    eventStore.append(MeetupCapacityIncreased(100, 20))
    eventStore.append(MeetupCapacityIncreased(200, 20))
    eventStore.append(MeetupCapacityIncreased(300, 20))
    eventStore.append(MeetupCapacityIncreased(100, 20))

    assertThatEventStore()
      .containsExactly(
        MeetupCapacityIncreased(100, 20),
        MeetupCapacityIncreased(200, 20),
        MeetupCapacityIncreased(300, 20),
        MeetupCapacityIncreased(100, 20),
      )
  }

  @Test fun fetch_stream_events() {

    eventStore.append(MeetupCapacityIncreased(100, 20))
    eventStore.append(MeetupCapacityIncreased(200, 20))
    eventStore.append(MeetupCapacityIncreased(300, 20))
    eventStore.append(MeetupCapacityIncreased(100, 20))

    assertThat(eventStore.readStream(100))
      .containsExactly(
        MeetupCapacityIncreased(100, 20),
        MeetupCapacityIncreased(100, 20),
      )
  }

  private fun assertThat(list: List<MeetupEvent>) = Assertions.assertThat(list)
    .usingRecursiveFieldByFieldElementComparator(
      RecursiveComparisonConfiguration.builder()
        .withStrictTypeChecking(true)
        .build()
    )

  private fun assertThatEventStore(): ListAssert<MeetupEvent> = Assertions.assertThat(eventStore.events)
    .usingRecursiveFieldByFieldElementComparator(
      RecursiveComparisonConfiguration.builder()
        .withStrictTypeChecking(true)
        .build()
    )
}
