package kata.meetup.domain

interface MeetupRepository {
  fun generateId(): Long
  fun findById(meetupId: Long): Meetup
  fun save(meetup: Meetup)
  fun findAll(): List<MeetupState>
}