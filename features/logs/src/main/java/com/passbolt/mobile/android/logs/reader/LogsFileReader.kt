package net.svaroh.passly.logs.reader

import net.svaroh.passly.core.logger.LogFilesManager
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import kotlinx.coroutines.withContext
import java.io.File

internal class LogsFileReader(
    private val logFilesManager: LogFilesManager,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : LogsReader {
    override suspend fun getLogLines(): List<String> =
        withContext(coroutineLaunchContext.io) {
            val logFile = File(requireNotNull(logFilesManager.logFilePath()) { "Log file not initialized" })
            if (logFile.exists()) logFile.readLines() else emptyList()
        }

    override suspend fun getLogFilePath() = logFilesManager.logFilePath()
}
