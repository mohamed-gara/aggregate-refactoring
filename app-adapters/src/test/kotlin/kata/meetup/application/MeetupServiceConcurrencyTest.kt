package kata.meetup.application

import ch.qos.logback.classic.Level
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import kata.infra.mongodb.MeetupMongodbRepository
import kata.infra.mongodb.MongodbEventStore
import kata.meetup.domain.*
import kotlinx.coroutines.*
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.bson.Document
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import org.slf4j.LoggerFactory
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC


class MeetupServiceConcurrencyTest {
  val eventStore = MongodbEventStore()
  val repository = MeetupMongodbRepository(eventStore)
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
    val log = LoggerFactory.getLogger("org.mongodb.driver")
    (log as ch.qos.logback.classic.Logger).level= Level.ERROR

    val client = KMongo.createClient()
    val database = client.getDatabase("test")
    val stateCollection = database.getCollection<MeetupState>("meetups")
    val eventsCollection = database.getCollection<MeetupEvent>("eventLog")
    stateCollection.deleteMany(Document())
    eventsCollection.deleteMany(Document())
  }

  @Test fun users_subscribe_concurrently_to_meetup() {
    val startTime = LocalDateTime.of(2019, 6, 15, 20, 0)

    val meetupId = sut.registerMeetup("Coding dojo session 1", 50, startTime)

    subscribeOneHundredUsersConcurrentlyTo(meetupId)

    val state = repository.findById(meetupId).state

    assertSoftly { softly ->
      softly.assertThat(state.participants).hasSize(50)
      softly.assertThat(state.waitingList).hasSize(50)
      softly.assertThat(state.subscriptions.list).hasSize(100)
    }
  }

  private fun subscribeOneHundredUsersConcurrentlyTo(meetupId: Long) {
    runBlocking {
      val list = (1..100).map {
        withContext(Dispatchers.IO) {
          coroutineScope {
            GlobalScope.async {
              delay(50)
              sut.subscribeUserToMeetup("userId_$it", meetupId)
            }
          }
        }
      }

      list.awaitAll()
    }
  }
}
