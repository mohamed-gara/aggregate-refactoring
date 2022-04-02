package kata.persistence;

import kata.MeetupEvent;
import kata.Subscription;
import kata.dbtestutil.MemoryDbTestContext;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeetupEventRepositoryTest {

  MemoryDbTestContext memoryDbTestContext;

  MeetupEventRepository sut;

  @BeforeEach
  void setUp() throws Exception {
    memoryDbTestContext = MemoryDbTestContext.openWithSql("/setup.sql");
    Jdbi jdbi = memoryDbTestContext.getJdbi();
    sut = new MeetupEventRepository(jdbi);
  }

  @AfterEach
  void tearDown() {
    memoryDbTestContext.close();
  }

  @Test void create_meetup_event() {
    sut.save(meetup_event());

    MeetupEvent result = sut.findById(1L);

    assertThat(result)
      .isEqualToComparingFieldByFieldRecursively(meetup_event());
  }

  MeetupEvent meetup_event() {
    var startTime = LocalDateTime.of(2022, 1, 2, 6, 0);
    var subscriptionTime = LocalDateTime.of(2022, 1, 1, 6, 0).toInstant(ZoneOffset.UTC);
    var subscription = new Subscription("userId", subscriptionTime, true);
    return new MeetupEvent(1L, 50, "eventName", startTime, List.of(subscription));
  }
}
