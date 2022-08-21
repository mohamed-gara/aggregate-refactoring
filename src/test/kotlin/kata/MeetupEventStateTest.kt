package kata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

class MeetupEventStateTest {

  @Test fun `empty event list`() {
    val state = projectStateFrom(listOf())

    assertThat(state)
      .usingRecursiveComparison()
      .isEqualTo(MeetupEventState(0, 0, "", LocalDateTime.MIN, lastAppliedEventIndex = -1))
  }

  @Test fun `meetup registered event`() {
    val startTime = LocalDateTime.of(2022, 8, 15, 2, 5)
    val event = MeetupEventRegistered(1, "Coding Dojo", 30, startTime)

    val state = projectStateFrom(listOf(event))

    assertThat(state)
      .usingRecursiveComparison()
      .isEqualTo(MeetupEventState(1, 30, "Coding Dojo", startTime, lastAppliedEventIndex = 0))
  }

  @Test fun `user subscribed to meetup event`() {
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
        )),
        1,
      ))
  }

  @Test fun `user added to meetup waiting list event`() {
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
        )),
        2,
      ))
  }

  @Test fun `user cancelled meetup event`() {
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
        Subscriptions(listOf()),
        2,
      ))
  }

  @Test fun `user moved from waiting list to participants event`() {
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
        )),
        2,
      ))
  }

  @Test fun `users moved from waiting list to participants event`() {
    val userRegistrationTime = LocalDateTime.of(2022, 8, 16, 2, 5).toInstant(UTC)
    val meetupRegistrationTime = LocalDateTime.of(2022, 8, 15, 2, 5)

    val state = projectStateFrom(listOf(
      MeetupEventRegistered(1, "Coding Dojo", 1, meetupRegistrationTime),
      UserAddedToMeetupEventWaitingList(1, "user1", userRegistrationTime),
      UserAddedToMeetupEventWaitingList(1, "user2", userRegistrationTime),
      UsersMovedFromWaitingListToParticipants(1, listOf("user1", "user2"), MeetupEventCapacityIncreased(1, 10)),
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
          Subscription("user2", userRegistrationTime, false),
        )),
        3,
      ))
  }

  @Nested inner class Snapshot {
    val userRegistrationTime: Instant = LocalDateTime.of(2022, 8, 16, 2, 5).toInstant(UTC)
    val meetupRegistrationTime: LocalDateTime = LocalDateTime.of(2022, 8, 15, 2, 5)
    val events = listOf(
      MeetupEventRegistered(1, "Coding Dojo", 1, meetupRegistrationTime),
      UserAddedToMeetupEventWaitingList(1, "user1", userRegistrationTime),
      UserAddedToMeetupEventWaitingList(1, "user2", userRegistrationTime),
      UsersMovedFromWaitingListToParticipants(1, listOf("user1", "user2"), MeetupEventCapacityIncreased(1, 10)),
    )
    val expectedState = MeetupEventState(
      1,
      1,
      "Coding Dojo",
      meetupRegistrationTime,
      Subscriptions(listOf(
        Subscription("user1", userRegistrationTime, false),
        Subscription("user2", userRegistrationTime, false),
      )),
      3,
    )

    @Test fun empty() {

      val state = projectStateFrom(events)

      assertThat(state)
        .usingRecursiveComparison()
        .isEqualTo(expectedState)
    }

    @Test fun `state after 1st event`() {
      val firstEventState = MeetupEventState(
        1,
        1,
        "Coding Dojo",
        meetupRegistrationTime,
        lastAppliedEventIndex = 0,
      )

      val state = projectStateFrom(events, firstEventState)

      assertThat(state)
        .usingRecursiveComparison()
        .isEqualTo(expectedState)
    }

    @Test fun `state after 2nd event`() {
      val secondEventState = MeetupEventState(
        1,
        1,
        "Coding Dojo",
        meetupRegistrationTime,
        Subscriptions(listOf(
          Subscription("user1", userRegistrationTime, true),
        )),
        lastAppliedEventIndex = 1,
      )

      val state = projectStateFrom(events, secondEventState)

      assertThat(state)
        .usingRecursiveComparison()
        .isEqualTo(expectedState)
    }

    @Test fun `state after 3rd event`() {
      val thirdEventState = MeetupEventState(
        1,
        1,
        "Coding Dojo",
        meetupRegistrationTime,
        Subscriptions(listOf(
          Subscription("user1", userRegistrationTime, true),
          Subscription("user2", userRegistrationTime, true),
        )),
        lastAppliedEventIndex = 2,
      )

      val state = projectStateFrom(events, thirdEventState)

      assertThat(state)
        .usingRecursiveComparison()
        .isEqualTo(expectedState)
    }

    @Test fun `state after 4th event`() {
      val forthEventState = MeetupEventState(
        1,
        1,
        "Coding Dojo",
        meetupRegistrationTime,
        Subscriptions(listOf(
          Subscription("user1", userRegistrationTime, false),
          Subscription("user2", userRegistrationTime, false),
        )),
        lastAppliedEventIndex = 3,
      )

      val state = projectStateFrom(events, forthEventState)

      assertThat(state)
        .usingRecursiveComparison()
        .isEqualTo(expectedState)
    }
  }

  @Nested inner class Apply {
    @Test fun update_events_and_state() {
      val userRegistrationTime = LocalDateTime.of(2022, 8, 16, 2, 5).toInstant(UTC)
      val meetupRegistrationTime = LocalDateTime.of(2022, 8, 15, 2, 5)

      val meetup = MeetupEvent(
        events = listOf(MeetupEventRegistered(1, "Coding Dojo", 30, meetupRegistrationTime)),
        version = 19
      )
      assertThat(meetup.state.lastAppliedEventIndex).isEqualTo(0)
      assertThat(meetup.version).isEqualTo(19)

      val updatedMeetup = meetup.subscribe("user1", userRegistrationTime)

      assertThat(updatedMeetup)
        .usingRecursiveComparison()
        .isEqualTo(
          MeetupEvent(
            version = 19,
            events = listOf(
              MeetupEventRegistered(1, "Coding Dojo", 30, meetupRegistrationTime),
              UserSubscribedToMeetupEvent(1, "user1", userRegistrationTime),
            ),
            state = MeetupEventState(
              1,
              30,
              "Coding Dojo",
              meetupRegistrationTime,
              Subscriptions(listOf(
                Subscription("user1", userRegistrationTime, false)
              )),
              1,
            )
          )
        )
    }
  }
}