package kata.persistence

import org.jdbi.v3.core.mapper.NoSuchMapperException
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

fun <T> mapTo(rs: ResultSet?, columnLabel: String?, type: Class<T>, ctx: StatementContext): T {
  val mapper = ctx.findColumnMapperFor(type).orElseThrow { NoSuchMapperException(type.toString()) }
  return mapper.map(rs, columnLabel, ctx)
}
