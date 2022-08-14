package kata.persistence

import org.assertj.core.api.Assertions
import org.assertj.core.api.ListAssert
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.Test


internal class InMemoryEventStoreTest {

  val eventStore = InMemoryEventStore()

  @Test fun append_events() {

    eventStore.append(EventExample(100))
    eventStore.append(EventExample(200))
    eventStore.append(EventExample(300))
    eventStore.append(EventExample(100))

    assertThatEventStore()
      .containsExactly(
        EventExample(100),
        EventExample(200),
        EventExample(300),
        EventExample(100),
      )
  }

  @Test fun fetch_stream_events() {

    eventStore.append(EventExample(100))
    eventStore.append(EventExample(200))
    eventStore.append(EventExample(300))
    eventStore.append(EventExample(100))

    assertThat(eventStore.readStream(100))
      .containsExactly(
        EventExample(100),
        EventExample(100),
      )
  }

  private fun assertThat(list: List<Event>): ListAssert<Event> = Assertions.assertThat(list)
    .usingRecursiveFieldByFieldElementComparator(
      RecursiveComparisonConfiguration.builder()
        .withStrictTypeChecking(true)
        .build()
    )

  private fun assertThatEventStore(): ListAssert<Event> = Assertions.assertThat(eventStore.events)
    .usingRecursiveFieldByFieldElementComparator(
      RecursiveComparisonConfiguration.builder()
        .withStrictTypeChecking(true)
        .build()
    )
}

data class EventExample(
  override val id: Long,
) : Event