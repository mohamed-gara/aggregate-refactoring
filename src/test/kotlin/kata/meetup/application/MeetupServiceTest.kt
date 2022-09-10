package kata.meetup.application

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import kata.meetup.domain.*
import kata.meetup.infra.MeetupRepository
import kata.meetup.infra.MongodbEventStore
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.bson.Document
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

class MeetupServiceTest {
  val eventStore = MongodbEventStore()
  val repository = MeetupRepository(eventStore)
  val now: Instant = LocalDateTime.of(2019, 6, 15, 20, 0).toInstant(UTC)

  val sut = MeetupService(repository, Clock.fixed(now, UTC))

  companion object {
    val mongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:4.4.16"))

    @BeforeAll@JvmStatic fun beforeClass() {
      mongoDBContainer.withCreateContainerCmdModifier{
        cmd -> cmd.withHostConfig(
          HostConfig().withPortBindings(PortBinding(Ports.Binding.bindPort(27017), ExposedPort(27017)))
        )
      }
      mongoDBContainer.start()
    }

    @AfterAll@JvmStatic fun afterClass() {
      mongoDBContainer.stop()
    }
  }

  @BeforeEach fun beforeEach() {
    val client = KMongo.createClient()
    val database = client.getDatabase("test")
    val stateCollection = database.getCollection<MeetupState>("meetups")
    val eventsCollection = database.getCollection<MeetupEvent>("eventLog")
    stateCollection.deleteMany(Document())
    eventsCollection.deleteMany(Document())
  }

  @Test fun should_be_able_to_give_state_of_meetup_event() {
    val startTime = LocalDateTime.of(2019, 6, 15, 20, 0, 0, 0)

    val meetupId = sut.registerMeetup("Coding dojo session 1", 50, startTime)

    val meetupStatus = sut.getMeetupStatus(meetupId)
    assertThat(meetupStatus.meetupId).isEqualTo(meetupId)
    assertThat(meetupStatus.eventCapacity).isEqualTo(50)
    assertThat(meetupStatus.eventName).isEqualTo("Coding dojo session 1")
    assertThat(meetupStatus.startTime).isEqualTo(startTime)
    assertThat(meetupStatus.participants).isEmpty()
    assertThat(meetupStatus.waitingList).isEmpty()

    assertThatEventStore()
      .containsExactly(
        MeetupRegistered(meetupId, "Coding dojo session 1", 50, startTime)
      )
  }

  @Test fun should_add_subscription_to_participants() {
    val meetupId = registerAMeetupWithCapacity(50)

    sut.subscribeUserToMeetup("Alice", meetupId)
    sut.subscribeUserToMeetup("Bob", meetupId)
    sut.subscribeUserToMeetup("Charles", meetupId)

    val meetupStatus = sut.getMeetupStatus(meetupId)
    assertThat(meetupStatus.participants).containsExactly("Alice", "Bob", "Charles")
    assertThat(meetupStatus.waitingList).isEmpty()

    assertThatEventStore()
      .containsExactly(
        MeetupRegistered(meetupId, "Coding dojo session 1", 50, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetup(meetupId, "Alice", now),
        UserSubscribedToMeetup(meetupId, "Bob", now),
        UserSubscribedToMeetup(meetupId, "Charles", now),
      )
  }

  @Test fun should_reject_subscription_with_already_subscribed_user() {
    val meetupId = registerAMeetupWithCapacity(50)
    sut.subscribeUserToMeetup("Alice", meetupId)

    assertThatThrownBy { sut.subscribeUserToMeetup("Alice", meetupId) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("User Alice already has a subscription")
  }

  @Test fun should_add_subscription_to_waiting_list_when_event_is_at_capacity() {
    val meetupId = registerAMeetupWithCapacity(2)

    sut.subscribeUserToMeetup("Alice", meetupId)
    sut.subscribeUserToMeetup("Bob", meetupId)
    sut.subscribeUserToMeetup("Charles", meetupId)
    sut.subscribeUserToMeetup("David", meetupId)

    val meetupStatus = sut.getMeetupStatus(meetupId)
    assertThat(meetupStatus.participants).containsExactly("Alice", "Bob")
    assertThat(meetupStatus.waitingList).containsExactly("Charles", "David")

    assertThatEventStore()
      .containsExactly(
        MeetupRegistered(meetupId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetup(meetupId, "Alice", now),
        UserSubscribedToMeetup(meetupId, "Bob", now),
        UserAddedToMeetupWaitingList(meetupId, "Charles", now),
        UserAddedToMeetupWaitingList(meetupId, "David", now),
      )
  }

  @Test fun should_put_first_user_of_waiting_list_into_participants_when_a_participant_cancels() {
    val meetupId = registerAMeetupWithCapacity(2)
    sut.subscribeUserToMeetup("Alice", meetupId)
    sut.subscribeUserToMeetup("Bob", meetupId)
    sut.subscribeUserToMeetup("Charles", meetupId)
    sut.subscribeUserToMeetup("David", meetupId)

    sut.cancelUserSubscription("Alice", meetupId)

    val meetupStatus = sut.getMeetupStatus(meetupId)
    assertThat(meetupStatus.participants).containsExactly("Bob", "Charles")
    assertThat(meetupStatus.waitingList).containsExactly("David")

    assertThatEventStore()
      .containsExactly(
        MeetupRegistered(meetupId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetup(meetupId, "Alice", now),
        UserSubscribedToMeetup(meetupId, "Bob", now),
        UserAddedToMeetupWaitingList(meetupId, "Charles", now),
        UserAddedToMeetupWaitingList(meetupId, "David", now),
        UserCancelledMeetupSubscription(meetupId, "Alice"),
        UserMovedFromWaitingListToParticipants(
          meetupId,
          "Charles",
          UserCancelledMeetupSubscription(meetupId, "Alice")
        ),
      )
  }

  @Test fun should_not_change_participants_list_when_a_user_in_waiting_list_cancels() {
    val meetupId = registerAMeetupWithCapacity(2)
    sut.subscribeUserToMeetup("Alice", meetupId)
    sut.subscribeUserToMeetup("Bob", meetupId)
    sut.subscribeUserToMeetup("Charles", meetupId)
    sut.subscribeUserToMeetup("David", meetupId)

    sut.cancelUserSubscription("Charles", meetupId)

    val meetupStatus = sut.getMeetupStatus(meetupId)
    assertThat(meetupStatus.participants).containsExactly("Alice", "Bob")
    assertThat(meetupStatus.waitingList).containsExactly("David")

    assertThatEventStore()
      .containsExactly(
        MeetupRegistered(meetupId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetup(meetupId, "Alice", now),
        UserSubscribedToMeetup(meetupId, "Bob", now),
        UserAddedToMeetupWaitingList(meetupId, "Charles", now),
        UserAddedToMeetupWaitingList(meetupId, "David", now),
        UserCancelledMeetupSubscription(meetupId, "Charles"),
      )
  }

  @Test fun should_add_participants_from_waiting_list_when_capacity_is_increased() {
    val meetupId = registerAMeetupWithCapacity(2)
    sut.subscribeUserToMeetup("Alice", meetupId)
    sut.subscribeUserToMeetup("Bob", meetupId)
    sut.subscribeUserToMeetup("Charles", meetupId)
    sut.subscribeUserToMeetup("David", meetupId)
    sut.subscribeUserToMeetup("Emily", meetupId)

    val meetupStatusBeforeCapacityChange = sut.getMeetupStatus(meetupId)
    assertThat(meetupStatusBeforeCapacityChange.eventCapacity).isEqualTo(2)
    assertThat(meetupStatusBeforeCapacityChange.participants).containsExactly("Alice", "Bob")
    assertThat(meetupStatusBeforeCapacityChange.waitingList).containsExactly("Charles", "David", "Emily")

    assertThatEventStore()
      .containsExactly(
        MeetupRegistered(meetupId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetup(meetupId, "Alice", now),
        UserSubscribedToMeetup(meetupId, "Bob", now),
        UserAddedToMeetupWaitingList(meetupId, "Charles", now),
        UserAddedToMeetupWaitingList(meetupId, "David", now),
        UserAddedToMeetupWaitingList(meetupId, "Emily", now),
      )

    val newCapacity = 4
    sut.increaseCapacity(meetupId, newCapacity)

    val meetupStatus = sut.getMeetupStatus(meetupId)
    assertThat(meetupStatus.eventCapacity).isEqualTo(4)
    assertThat(meetupStatus.participants).containsExactly("Alice", "Bob", "Charles", "David")
    assertThat(meetupStatus.waitingList).containsExactly("Emily")

    assertThatEventStore()
      .containsExactly(
        MeetupRegistered(meetupId, "Coding dojo session 1", 2, LocalDateTime.of(2019, 6, 15, 20, 0)),
        UserSubscribedToMeetup(meetupId, "Alice", now),
        UserSubscribedToMeetup(meetupId, "Bob", now),
        UserAddedToMeetupWaitingList(meetupId, "Charles", now),
        UserAddedToMeetupWaitingList(meetupId, "David", now),
        UserAddedToMeetupWaitingList(meetupId, "Emily", now),
        MeetupCapacityIncreased(meetupId, 4),
        UsersMovedFromWaitingListToParticipants(
          meetupId,
          listOf("Charles", "David"),
          MeetupCapacityIncreased(meetupId, 4)
        ),
      )
  }

  @Test fun find_meetup_list() {
    val startTime1 = LocalDateTime.of(2019, 6, 15, 20, 0)
    val startTime2 = LocalDateTime.of(2020, 6, 15, 20, 0)

    val meetupId1 = sut.registerMeetup("Coding dojo session 1", 50, startTime1)
    val meetupId2 = sut.registerMeetup("Coding dojo session 2", 100, startTime2)

    assertThatMeetups().containsExactly(
      MeetupState(meetupId1, 50, "Coding dojo session 1", startTime1, Subscriptions(), 0),
      MeetupState(meetupId2, 100, "Coding dojo session 2", startTime2, Subscriptions(), 0),
    )
  }

  private fun assertThatEventStore() = assertThat(eventStore.events)
    .usingRecursiveFieldByFieldElementComparator(
      RecursiveComparisonConfiguration.builder()
        .withStrictTypeChecking(true)
        .build()
    )

  private fun assertThatMeetups() = assertThat(repository.findAll())
    .usingRecursiveFieldByFieldElementComparator(
      RecursiveComparisonConfiguration.builder()
        .withStrictTypeChecking(true)
        .build()
    )

  fun registerAMeetupWithCapacity(eventCapacity: Int): Long {
    val startTime = LocalDateTime.of(2019, 6, 15, 20, 0)
    return sut.registerMeetup("Coding dojo session 1", eventCapacity, startTime)
  }
}
