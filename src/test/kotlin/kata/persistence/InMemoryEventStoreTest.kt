package kata.persistence

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


internal class InMemoryEventStoreTest {

  @Test fun append_event() {
    val eventStore = InMemoryEventStore()

    eventStore.append(EventExample("id_event"))

    Assertions.assertThat(eventStore.events)
      .usingRecursiveComparison()
      .isEqualTo(listOf(
        EventExample("id_event")
      ))
  }
}

data class EventExample(
  val id: String
) : Event