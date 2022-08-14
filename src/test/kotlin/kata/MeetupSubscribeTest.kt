package kata

import kata.dbtestutil.MemoryDbTestContext
import kata.persistence.InMemoryEventStore
import kata.persistence.MeetupEventRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MeetupSubscribeTest {
  lateinit var memoryDbTestContext: MemoryDbTestContext
  lateinit var sut: MeetupSubscribe
  lateinit var eventStore: InMemoryEventStore

  @BeforeEach fun setUp() {
    memoryDbTestContext = MemoryDbTestContext.openWithSql("/setup.sql")
    val jdbi = memoryDbTestContext.jdbi
    val repository = MeetupEventRepository(jdbi)
    eventStore = InMemoryEventStore()

    sut = MeetupSubscribe(repository, eventStore)
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

    assertThat(eventStore.events)
      .usingRecursiveComparison()
      .isEqualTo(listOf(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 50, startTime)
      ))
  }

  @Test fun should_add_subscription_to_participants() {
    val meetupEventId = registerAMeetupWithCapacity(50)

    sut.subscribeUserToMeetupEvent("Alice", meetupEventId)
    sut.subscribeUserToMeetupEvent("Bob", meetupEventId)
    sut.subscribeUserToMeetupEvent("Charles", meetupEventId)

    val meetupEventStatus = sut.getMeetupEventStatus(meetupEventId)
    assertThat(meetupEventStatus.participants).containsExactly("Alice", "Bob", "Charles")
    assertThat(meetupEventStatus.waitingList).isEmpty()

    assertThat(eventStore.events)
      .usingRecursiveComparison()
      .isEqualTo(listOf(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 50, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetupEvent(meetupEventId, "Alice"),
        UserSubscribedToMeetupEvent(meetupEventId, "Bob"),
        UserSubscribedToMeetupEvent(meetupEventId, "Charles"),
      ))
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

    assertThat(eventStore.events)
      .usingRecursiveComparison()
      .isEqualTo(listOf(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetupEvent(meetupEventId, "Alice"),
        UserSubscribedToMeetupEvent(meetupEventId, "Bob"),
        UserAddedToMeetupEventWaitingList(meetupEventId, "Charles"),
        UserAddedToMeetupEventWaitingList(meetupEventId, "David"),
      ))
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

    assertThat(eventStore.events)
      .usingRecursiveComparison()
      .isEqualTo(listOf(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetupEvent(meetupEventId, "Alice"),
        UserSubscribedToMeetupEvent(meetupEventId, "Bob"),
        UserAddedToMeetupEventWaitingList(meetupEventId, "Charles"),
        UserAddedToMeetupEventWaitingList(meetupEventId, "David"),
        UserCancelledMeetupSubscription(meetupEventId, "Alice"),
      ))
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

    assertThat(eventStore.events)
      .usingRecursiveComparison()
      .isEqualTo(listOf(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetupEvent(meetupEventId, "Alice"),
        UserSubscribedToMeetupEvent(meetupEventId, "Bob"),
        UserAddedToMeetupEventWaitingList(meetupEventId, "Charles"),
        UserAddedToMeetupEventWaitingList(meetupEventId, "David"),
        UserCancelledMeetupSubscription(meetupEventId, "Charles"),
      ))
  }

  @Test fun should_add_participants_from_waiting_list_when_capacity_is_increased() {
    val meetupEventId = registerAMeetupWithCapacity(2)
    sut.subscribeUserToMeetupEvent("Alice", meetupEventId)
    sut.subscribeUserToMeetupEvent("Bob", meetupEventId)
    sut.subscribeUserToMeetupEvent("Charles", meetupEventId)
    sut.subscribeUserToMeetupEvent("David", meetupEventId)
    sut.subscribeUserToMeetupEvent("Emily", meetupEventId)

    val newCapacity = 4
    sut.increaseCapacity(meetupEventId, newCapacity)

    val meetupEventStatus = sut.getMeetupEventStatus(meetupEventId)
    assertThat(meetupEventStatus.eventCapacity).isEqualTo(4)
    assertThat(meetupEventStatus.participants).containsExactly("Alice", "Bob", "Charles", "David")
    assertThat(meetupEventStatus.waitingList).containsExactly("Emily")

    assertThat(eventStore.events)
      .usingRecursiveComparison()
      .isEqualTo(listOf(
        MeetupEventRegistered(meetupEventId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetupEvent(meetupEventId, "Alice"),
        UserSubscribedToMeetupEvent(meetupEventId, "Bob"),
        UserSubscribedToMeetupEvent(meetupEventId, "Charles"),
        UserSubscribedToMeetupEvent(meetupEventId, "David"),
        UserAddedToMeetupEventWaitingList(meetupEventId, "Emily"),
        MeetupEventCapacityIncreased(meetupEventId, 4),
      ))
  }

  fun registerAMeetupWithCapacity(eventCapacity: Int): Long {
    val startTime = LocalDateTime.of(2019, 6, 15, 20, 0)
    return sut.registerMeetupEvent("Coding dojo session 1", eventCapacity, startTime)
  }
}
