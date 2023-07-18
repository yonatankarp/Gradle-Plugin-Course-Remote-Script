import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


logger.quiet("=============================================================================================")
logger.quiet("                            Remote Files Plugin                                              ")
logger.quiet("=============================================================================================")


tasks.register("sortFiles") {
  group = "files"
  description = "Sorts files in a directory into build/files based on the sorting type (date/extension)"
  dependsOn("cleanFiles")

  doLast {
    logger.quiet("==== Sorting files ====")
    logger.newLine()

    val sortType = project.properties["tasks.files.sortType"].toString()

    file(project.properties["tasks.files.folder"].toString())
      .listFiles { file -> file.isFile && !file.isHidden }
      ?.forEach { file ->
        logger.quiet("📂 Filename: ${file.name}")

        // Get directory for the file based on the sorting type
        val fileMapper = getFileDirectoryMapper(sortType, logger)
        val directory = fileMapper.getDirectory(file)

        // Create new directory inside build folder
        val output = project
          .layout
          .buildDirectory
          .dir("files/$directory")
          .get()
          .asFile
          .also { it.mkdirs() }

        // Copy files to sorted directory inside build directory
        Files.copy(file.toPath(), File(output, file.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
      }
  }

  logger.newLine()
  logger.quiet("==== Files sorted successfully 🎉 ====")
}

tasks.register<Delete>("cleanFiles") {
  group = "files"
  description = "Clean build directory"

  delete {
    delete(project.layout.buildDirectory.dir("files").get())
  }
}

fun getFileDirectoryMapper(sortType: String, logger: Logger): FileDirectoryMapper {
  when (sortType) {
    "date" -> return FileDirectoryDateMapper()
    "extension" -> return FileDirectoryExtensionMapper()
    else -> {
      logger.quiet("⚠️ Property [tasks.files.sortType] isn't set or contains invalid value, default sorting will be done by creation date")
      return FileDirectoryDateMapper()
    }
  }
}


interface FileDirectoryMapper {
  fun getDirectory(file: File): String
}

class FileDirectoryDateMapper : FileDirectoryMapper {
  override fun getDirectory(file: File): String {
    val creationTime = Files.getAttribute(Paths.get(file.path), "creationTime") as FileTime
    val fileCreationDateFormat = DateTimeFormatter.ofPattern("MM-YYYY")
    return fileCreationDateFormat.format(
      Instant.ofEpochMilli(creationTime.toMillis()).atOffset(ZoneOffset.UTC).toLocalDate()
    )
  }
}

class FileDirectoryExtensionMapper : FileDirectoryMapper {
  override fun getDirectory(file: File): String = file.extension
}

fun Logger.newLine() = this.quiet("")
