package kata.dbtestutil

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import java.nio.file.Files
import java.nio.file.Paths

class MemoryDbTestContext private constructor(
  val jdbi: Jdbi,
  private val handle: Handle
) {
  fun close() {
    handle.close()
  }

  companion object {
    private fun loadSetupSql(file: String): String {
      val sqlPath = Paths.get(MemoryDbTestContext::class.java.getResource(file).toURI())
      return String(Files.readAllBytes(sqlPath))
    }

    fun openWithSql(resourcePath: String): MemoryDbTestContext {
      val sqlScript = loadSetupSql(resourcePath)
      val jdbi = Jdbi.create("jdbc:h2:mem:test")
      val handle = jdbi.open()
      handle.createScript(sqlScript).execute()
      return MemoryDbTestContext(jdbi, handle)
    }
  }
}
