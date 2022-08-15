package kata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

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

  @Test fun user_subscribed_to_meetup_event() {
    val userRegistrationTime = LocalDateTime.of(2022, 8, 16, 2, 5).toInstant(UTC)
    val meetupRegistrationTime = LocalDateTime.of(2022, 8, 15, 2, 5)

    val state = projectStateFrom(listOf(
      MeetupEventRegistered(1, "Coding Dojo", 30, meetupRegistrationTime),
      UserSubscribedToMeetupEvent(1, "user1", userRegistrationTime)
    ))

    assertThat(state)
      .usingRecursiveComparison()
      .isEqualTo(MeetupEventState(
        1,
        30,
        "Coding Dojo",
        meetupRegistrationTime,
        Subscriptions(listOf(
          Subscription("user1", userRegistrationTime, false)
        ))
      ))
  }

  @Test fun user_added_to_meetup_waiting_list_event() {
    val userRegistrationTime = LocalDateTime.of(2022, 8, 16, 2, 5).toInstant(UTC)
    val meetupRegistrationTime = LocalDateTime.of(2022, 8, 15, 2, 5)

    val state = projectStateFrom(listOf(
      MeetupEventRegistered(1, "Coding Dojo", 1, meetupRegistrationTime),
      UserSubscribedToMeetupEvent(1, "user1", userRegistrationTime),
      UserAddedToMeetupEventWaitingList(1, "user2", userRegistrationTime)
    ))

    assertThat(state)
      .usingRecursiveComparison()
      .isEqualTo(MeetupEventState(
        1,
        1,
        "Coding Dojo",
        meetupRegistrationTime,
        Subscriptions(listOf(
          Subscription("user1", userRegistrationTime, false),
          Subscription("user2", userRegistrationTime, true),
        ))
      ))
  }

  @Test fun user_cancelled_meetup_event() {
    val userRegistrationTime = LocalDateTime.of(2022, 8, 16, 2, 5).toInstant(UTC)
    val meetupRegistrationTime = LocalDateTime.of(2022, 8, 15, 2, 5)

    val state = projectStateFrom(listOf(
      MeetupEventRegistered(1, "Coding Dojo", 1, meetupRegistrationTime),
      UserSubscribedToMeetupEvent(1, "user1", userRegistrationTime),
      UserCancelledMeetupSubscription(1, "user1")
    ))

    assertThat(state)
      .usingRecursiveComparison()
      .isEqualTo(MeetupEventState(
        1,
        1,
        "Coding Dojo",
        meetupRegistrationTime,
        Subscriptions(listOf())
      ))
  }

  @Test fun user_moved_from_waiting_list_to_participants_event() {
    val userRegistrationTime = LocalDateTime.of(2022, 8, 16, 2, 5).toInstant(UTC)
    val meetupRegistrationTime = LocalDateTime.of(2022, 8, 15, 2, 5)

    val state = projectStateFrom(listOf(
      MeetupEventRegistered(1, "Coding Dojo", 1, meetupRegistrationTime),
      UserAddedToMeetupEventWaitingList(1, "user2", userRegistrationTime),
      UserMovedFromWaitingListToParticipants(1, "user2", UserCancelledMeetupSubscription(1, "user1")),
    ))

    assertThat(state)
      .usingRecursiveComparison()
      .isEqualTo(MeetupEventState(
        1,
        1,
        "Coding Dojo",
        meetupRegistrationTime,
        Subscriptions(listOf(
          Subscription("user2", userRegistrationTime, false),
        ))
      ))
  }
}