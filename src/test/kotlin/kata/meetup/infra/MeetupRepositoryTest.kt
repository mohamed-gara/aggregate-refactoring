package kata.meetup.infra

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import kata.meetup.domain.Meetup
import kata.meetup.domain.MeetupRegistered
import kata.meetup.domain.MeetupState
import kata.meetup.domain.UserSubscribedToMeetup
import org.assertj.core.api.Assertions
import org.bson.Document
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class MeetupRepositoryTest {

  val eventStore = MongodbEventStore()
  val sut = MeetupRepository(eventStore)

  companion object {
    val mongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:4.4.16"))

    @BeforeAll
    @JvmStatic fun beforeClass() {
      mongoDBContainer.withCreateContainerCmdModifier{
          cmd -> cmd.withHostConfig(
        HostConfig().withPortBindings(PortBinding(Ports.Binding.bindPort(27017), ExposedPort(27017)))
      )
      }
      mongoDBContainer.start()
    }

    @AfterAll
    @JvmStatic fun afterClass() {
      mongoDBContainer.stop()
    }
  }

  @BeforeEach
  fun beforeEach() {
    val client = KMongo.createClient()
    val database = client.getDatabase("test")
    val collection = database.getCollection<MeetupState>("meetups")
    collection.deleteMany(Document())
  }

  @Test fun create_meetup() {
    eventStore.append(MeetupRegistered(1, "eventName", 50, LocalDateTime.of(2022, 1, 2, 6, 0)))
    val subscriptionTime = LocalDateTime.of(2022, 1, 1, 6, 0).toInstant(ZoneOffset.UTC)
    eventStore.append(UserSubscribedToMeetup(1, "userId", subscriptionTime))

    val result = sut.findById(1)

    Assertions.assertThat(result)
      .usingRecursiveComparison()
      .isEqualTo(meetup())
  }

  fun meetup(): Meetup {
    val startTime = LocalDateTime.of(2022, 1, 2, 6, 0)
    val subscriptionTime = LocalDateTime.of(2022, 1, 1, 6, 0).toInstant(ZoneOffset.UTC)
    return Meetup(
      listOf(
        MeetupRegistered(1L,"eventName", 50, startTime),
        UserSubscribedToMeetup(1, "userId", subscriptionTime),
      ),
    2,
    )
  }
}