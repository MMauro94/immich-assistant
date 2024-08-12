package dev.mmauro.immichassistant.common.task

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.mordant.animation.coroutines.CoroutineProgressAnimator
import com.github.ajalt.mordant.animation.progress.ProgressTask
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.widgets.progress.ProgressBarDefinition
import com.github.ajalt.mordant.widgets.progress.frameCount
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.text
import dev.mmauro.immichassistant.common.CommonCommand
import kotlinx.coroutines.*

interface TaskRunnerScope {
    val task: ProgressTask<TaskContext>
    fun started()
    fun update(message: String)
}

enum class TaskState {
    WAITING, RUNNING, COMPLETED, ERROR
}

data class TaskContext(
    val state: TaskState = TaskState.WAITING,
    val message: String? = null,
)

context(CommonCommand, CoroutineScope)
fun <T> CoroutineProgressAnimator.addDeferredTask(
    name: String,
    stopOnException: Boolean = true,
    taskRunner: suspend TaskRunnerScope.() -> T,
): Deferred<T> {
    class TaskRunnerScopeImpl(override val task: ProgressTask<TaskContext>) : TaskRunnerScope {
        override fun started() {
            task.update {
                context = context.copy(state = TaskState.RUNNING)
            }
        }

        override fun update(message: String) {
            task.update {
                context = context.copy(message = message)
            }
        }
    }

    val task = addTask(definition = taskLayout(name), context = TaskContext(), total = 1, completed = 0)
    val taskScope = TaskRunnerScopeImpl(task)
    return async {
        withContext(Dispatchers.IO) {
            val result = runCatching { taskRunner(taskScope) }
            task.update {
                val newState = if (result.isSuccess) {
                    TaskState.COMPLETED
                } else {
                    TaskState.ERROR
                }
                context = context.copy(state = newState)
                completed = 1
            }
            result.getOrElse { exception ->
                if (stopOnException) {
                    // Leave mordant time to render the last frame
                    delay(100)
                    stop()

                    throw PrintMessage(
                        message = buildString {
                            append(red("Error: "))
                            append("Task \"$name\" failed")
                            val details = exception.message?.ifBlank { null } ?: exception::class.simpleName
                            if (details != null) {
                                append(": ")
                                append(details)
                            }
                            if (commonOptions.debug) {
                                appendLine()
                                append(exception.stackTraceToString())
                            }
                        },
                        statusCode = 1
                    )
                }
                throw exception
            }
        }
    }
}

private fun taskLayout(name: String): ProgressBarDefinition<TaskContext> {
    return progressBarContextLayout {
        val fps = 8
        cell(fps = fps) {
            when (context.state) {
                TaskState.WAITING -> Text(" ")
                TaskState.RUNNING -> Spinner.Lines().also {
                    it.tick = frameCount(fps)
                }

                TaskState.COMPLETED -> Text("✔️")
                TaskState.ERROR -> Text("❌")
            }
        }
        text(fps = animationFps, align = TextAlign.LEFT) { name + context.message?.let { " $it" }.orEmpty() }
    }
}