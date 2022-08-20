package kata

import kata.dbtestutil.MemoryDbTestContext
import kata.persistence.InMemoryEventStore
import kata.persistence.MeetupEventRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

class MeetupSubscribeTest {
  lateinit var memoryDbTestContext: MemoryDbTestContext
  lateinit var sut: MeetupSubscribe
  val eventStore = InMemoryEventStore()
  val now: Instant = Instant.now()

  @BeforeEach fun setUp() {
    memoryDbTestContext = MemoryDbTestContext.openWithSql("/setup.sql")
    val jdbi = memoryDbTestContext.jdbi
    val repository = MeetupEventRepository(jdbi, eventStore)

    sut = MeetupSubscribe(repository, eventStore, Clock.fixed(now, UTC))
  }

  @AfterEach fun tearDown() {
    memoryDbTestContext.close()
  }

  @Test fun should_be_able_to_give_state_of_meetup_event() {
    val startTime = LocalDateTime.of(2019, 6, 15, 20, 0)

    val meetupEventId = sut.registerMeetupEvent("Coding dojo session 1", 50, startTime)

    val meetupEventStatus = sut.getMeetupEventStatus(meetupEventId)
    assertThat(meetupEventStatus.meetupId).isEqualTo(meetupEventId)
    assertThat(meetupEventStatus.eventCapacity).isEqualTo(50)
    assertThat(meetupEventStatus.eventName).isEqualTo("Coding dojo session 1")
    assertThat(meetupEventStatus.startTime).isEqualTo(startTime)
    assertThat(meetupEventStatus.participants).isEmpty()
    assertThat(meetupEventStatus.waitingList).isEmpty()

    assertThatEventStore()
      .containsExactly(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 50, startTime)
      )
  }

  @Test fun should_add_subscription_to_participants() {
    val meetupEventId = registerAMeetupWithCapacity(50)

    sut.subscribeUserToMeetupEvent("Alice", meetupEventId)
    sut.subscribeUserToMeetupEvent("Bob", meetupEventId)
    sut.subscribeUserToMeetupEvent("Charles", meetupEventId)

    val meetupEventStatus = sut.getMeetupEventStatus(meetupEventId)
    assertThat(meetupEventStatus.participants).containsExactly("Alice", "Bob", "Charles")
    assertThat(meetupEventStatus.waitingList).isEmpty()

    assertThatEventStore()
      .containsExactly(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 50, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetupEvent(meetupEventId, "Alice", now),
        UserSubscribedToMeetupEvent(meetupEventId, "Bob", now),
        UserSubscribedToMeetupEvent(meetupEventId, "Charles", now),
      )
  }

  @Test fun should_reject_subscription_with_already_subscribed_user() {
    val meetupEventId = registerAMeetupWithCapacity(50)
    sut.subscribeUserToMeetupEvent("Alice", meetupEventId)

    assertThatThrownBy { sut.subscribeUserToMeetupEvent("Alice", meetupEventId) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("User Alice already has a subscription")
  }

  @Test fun should_add_subscription_to_waiting_list_when_event_is_at_capacity() {
    val meetupEventId = registerAMeetupWithCapacity(2)

    sut.subscribeUserToMeetupEvent("Alice", meetupEventId)
    sut.subscribeUserToMeetupEvent("Bob", meetupEventId)
    sut.subscribeUserToMeetupEvent("Charles", meetupEventId)
    sut.subscribeUserToMeetupEvent("David", meetupEventId)

    val meetupEventStatus = sut.getMeetupEventStatus(meetupEventId)
    assertThat(meetupEventStatus.participants).containsExactly("Alice", "Bob")
    assertThat(meetupEventStatus.waitingList).containsExactly("Charles", "David")

    assertThatEventStore()
      .containsExactly(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetupEvent(meetupEventId, "Alice", now),
        UserSubscribedToMeetupEvent(meetupEventId, "Bob", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "Charles", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "David", now),
      )
  }

  @Test fun should_put_first_user_of_waiting_list_into_participants_when_a_participant_cancels() {
    val meetupEventId = registerAMeetupWithCapacity(2)
    sut.subscribeUserToMeetupEvent("Alice", meetupEventId)
    sut.subscribeUserToMeetupEvent("Bob", meetupEventId)
    sut.subscribeUserToMeetupEvent("Charles", meetupEventId)
    sut.subscribeUserToMeetupEvent("David", meetupEventId)

    sut.cancelUserSubscription("Alice", meetupEventId)

    val meetupEventStatus = sut.getMeetupEventStatus(meetupEventId)
    assertThat(meetupEventStatus.participants).containsExactly("Bob", "Charles")
    assertThat(meetupEventStatus.waitingList).containsExactly("David")

    assertThatEventStore()
      .containsExactly(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetupEvent(meetupEventId, "Alice", now),
        UserSubscribedToMeetupEvent(meetupEventId, "Bob", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "Charles", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "David", now),
        UserCancelledMeetupSubscription(meetupEventId, "Alice"),
        UserMovedFromWaitingListToParticipants(
          meetupEventId,
          "Charles",
          UserCancelledMeetupSubscription(meetupEventId, "Alice")
        ),
      )
  }

  @Test fun should_not_change_participants_list_when_a_user_in_waiting_list_cancels() {
    val meetupEventId = registerAMeetupWithCapacity(2)
    sut.subscribeUserToMeetupEvent("Alice", meetupEventId)
    sut.subscribeUserToMeetupEvent("Bob", meetupEventId)
    sut.subscribeUserToMeetupEvent("Charles", meetupEventId)
    sut.subscribeUserToMeetupEvent("David", meetupEventId)

    sut.cancelUserSubscription("Charles", meetupEventId)

    val meetupEventStatus = sut.getMeetupEventStatus(meetupEventId)
    assertThat(meetupEventStatus.participants).containsExactly("Alice", "Bob")
    assertThat(meetupEventStatus.waitingList).containsExactly("David")

    assertThatEventStore()
      .containsExactly(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetupEvent(meetupEventId, "Alice", now),
        UserSubscribedToMeetupEvent(meetupEventId, "Bob", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "Charles", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "David", now),
        UserCancelledMeetupSubscription(meetupEventId, "Charles"),
      )
  }

  @Test fun should_add_participants_from_waiting_list_when_capacity_is_increased() {
    val meetupEventId = registerAMeetupWithCapacity(2)
    sut.subscribeUserToMeetupEvent("Alice", meetupEventId)
    sut.subscribeUserToMeetupEvent("Bob", meetupEventId)
    sut.subscribeUserToMeetupEvent("Charles", meetupEventId)
    sut.subscribeUserToMeetupEvent("David", meetupEventId)
    sut.subscribeUserToMeetupEvent("Emily", meetupEventId)

    val meetupEventStatusBeforeCapacityChange = sut.getMeetupEventStatus(meetupEventId)
    assertThat(meetupEventStatusBeforeCapacityChange.eventCapacity).isEqualTo(2)
    assertThat(meetupEventStatusBeforeCapacityChange.participants).containsExactly("Alice", "Bob")
    assertThat(meetupEventStatusBeforeCapacityChange.waitingList).containsExactly("Charles", "David", "Emily")

    assertThatEventStore()
      .containsExactly(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetupEvent(meetupEventId, "Alice", now),
        UserSubscribedToMeetupEvent(meetupEventId, "Bob", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "Charles", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "David", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "Emily", now),
      )

    val newCapacity = 4
    sut.increaseCapacity(meetupEventId, newCapacity)

    val meetupEventStatus = sut.getMeetupEventStatus(meetupEventId)
    assertThat(meetupEventStatus.eventCapacity).isEqualTo(4)
    assertThat(meetupEventStatus.participants).containsExactly("Alice", "Bob", "Charles", "David")
    assertThat(meetupEventStatus.waitingList).containsExactly("Emily")

    assertThatEventStore()
      .containsExactly(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetupEvent(meetupEventId, "Alice", now),
        UserSubscribedToMeetupEvent(meetupEventId, "Bob", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "Charles", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "David", now),
        UserAddedToMeetupEventWaitingList(meetupEventId, "Emily", now),
        MeetupEventCapacityIncreased(meetupEventId, 4),
        UsersMovedFromWaitingListToParticipants(
          meetupEventId,
          listOf("Charles", "David"),
          MeetupEventCapacityIncreased(meetupEventId, 4)
        ),
      )
  }

  private fun assertThatEventStore() = assertThat(eventStore.events)
    .usingRecursiveFieldByFieldElementComparator(
      RecursiveComparisonConfiguration.builder()
        .withStrictTypeChecking(true)
        .build()
    )

  fun registerAMeetupWithCapacity(eventCapacity: Int): Long {
    val startTime = LocalDateTime.of(2019, 6, 15, 20, 0)
    return sut.registerMeetupEvent("Coding dojo session 1", eventCapacity, startTime)
  }
}
