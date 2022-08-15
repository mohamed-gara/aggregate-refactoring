package kata.persistence

import kata.*
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.HandleConsumer
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant
import java.util.function.Consumer

class MeetupEventRepository(
  private val jdbi: Jdbi,
  private val eventStore: EventStore,
) {
  fun generateId(): Long {
    return jdbi.withHandle<Long, RuntimeException> { handle: Handle ->
      handle
        .createQuery("SELECT NEXTVAL('MEETUP_EVENT_ID_SEQ')")
        .mapTo(Long::class.java)
        .one()
    }
  }

  fun findById(meetupEventId: Long): MeetupEvent {
    val meetupEvents = eventStore.readStream(meetupEventId)
    val stateFromEvents = projectStateFrom(meetupEvents as List<MeetupBaseEvent>)

    val subscriptions = findSubscriptionsOf(meetupEventId)

    return MeetupEvent(
      MeetupEventState(
        stateFromEvents.id,
        stateFromEvents.capacity,
        stateFromEvents.eventName,
        stateFromEvents.startTime,
        Subscriptions(subscriptions)
      )
    )
  }

  private fun findSubscriptionsOf(meetupEventId: Long): MutableList<Subscription> {
    val sql = "" +
        "SELECT * FROM USER_SUBSCRIPTION " +
        "WHERE meetup_event_id = :meetupEventId "
    return jdbi.withHandle<MutableList<Subscription>, RuntimeException> { handle: Handle ->
      handle.createQuery(sql)
        .bind("meetupEventId", meetupEventId)
        .map(SUBSCRIPTION_ROW_MAPPER)
        .list()
    }
  }

  fun save(meetupEvent: MeetupEvent) {
    jdbi.useTransaction<RuntimeException> { handle: Handle? ->
      val state = meetupEvent.state
      if (state.subscriptions != null) upsertSubscriptions(state.id, state.subscriptions.list).useHandle(
        handle
      )
      deleteMeetupSubscriptionsNotInUserIds(state.id, state.users).useHandle(handle)
    }
  }

  private fun upsertSubscriptions(
    meetupEventId: Long,
    subscriptions: Collection<Subscription>
  ): HandleConsumer<RuntimeException> {
    if (subscriptions.isEmpty()) return HandleConsumer { handle: Handle? -> }
    val sql = "" +
        "MERGE INTO USER_SUBSCRIPTION (user_id, meetup_event_id, registration_time, waiting_list) " +
        "KEY (meetup_event_id, user_id) " +
        "VALUES (:userId, :meetupEventId, :registrationTime, :waitingList)"
    return HandleConsumer { handle: Handle ->
      val preparedBatch = handle.prepareBatch(sql)
      subscriptions.forEach(Consumer { subscription: Subscription ->
        preparedBatch
          .bind("meetupEventId", meetupEventId)
          .bind("userId", subscription.userId)
          .bind("registrationTime", subscription.registrationTime)
          .bind("waitingList", subscription.isInWaitingList)
          .add()
      })
      preparedBatch.execute()
    }
  }

  private fun deleteMeetupSubscriptionsNotInUserIds(
    meetupEventId: Long,
    userIds: List<String?>
  ): HandleConsumer<RuntimeException> {
    if (userIds.isEmpty()) {
      val sql = "" +
          "DELETE FROM USER_SUBSCRIPTION " +
          "WHERE meetup_event_id = :meetupEventId "
      return HandleConsumer { handle: Handle ->
        handle.createUpdate(sql)
          .bind("meetupEventId", meetupEventId)
          .execute()
      }
    }
    val sql = "" +
        "DELETE FROM USER_SUBSCRIPTION " +
        "WHERE meetup_event_id = :meetupEventId " +
        "AND user_id NOT IN (<userIds>)"
    return HandleConsumer { handle: Handle ->
      handle.createUpdate(sql)
        .bind("meetupEventId", meetupEventId)
        .bindList("userIds", userIds)
        .execute()
    }
  }

  companion object {
    private val SUBSCRIPTION_ROW_MAPPER = RowMapper { rs: ResultSet, ctx: StatementContext? ->
      Subscription(
        rs.getString("user_id"),
        mapTo(rs, "registration_time", Instant::class.java, ctx!!),
        rs.getBoolean("waiting_list")
      )
    }
  }
}
