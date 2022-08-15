package kata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class HydrateMeetupEventStateTest {

  @Test fun empty_event_list() {
    val state = projectStateFrom(listOf())

    assertThat(state)
      .usingRecursiveComparison()
      .isEqualTo(MeetupEventState(
        0, 0, "", LocalDateTime.MIN
      ))
  }

  @Test fun meetup_registered_event() {
    val event = MeetupEventRegistered(
      1, "Coding Dojo", 30, LocalDateTime.of(2022, 8, 15, 2, 5)
    )

    val state = projectStateFrom(listOf(event))

    assertThat(state)
      .usingRecursiveComparison()
      .isEqualTo(MeetupEventState(
        1, 30, "Coding Dojo", LocalDateTime.of(2022, 8, 15, 2, 5)
      ))
  }
}