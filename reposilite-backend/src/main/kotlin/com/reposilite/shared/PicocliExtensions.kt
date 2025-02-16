package com.reposilite.shared

import com.reposilite.VERSION
import panda.std.Result
import panda.std.asSuccess
import picocli.CommandLine
import java.util.TreeSet

abstract class Validator : Runnable {
    override fun run() { }
}

data class CommandConfiguration<VALUE>(
    val name: String,
    val configuration: VALUE
)

fun <CONFIGURATION : Runnable> loadCommandBasedConfiguration(configuration: CONFIGURATION, description: String): CommandConfiguration<CONFIGURATION> =
    description.split(" ", limit = 2)
        .let { CommandConfiguration(it[0], it.getOrElse(1, { "" })) }
        .also { CommandLine(configuration).execute(*splitArguments(it.configuration)) }
        .let { CommandConfiguration(it.name, configuration) }
        .also { it.configuration.run() }

private fun splitArguments(args: String): Array<String> =
    if (args.isEmpty()) arrayOf() else args.split(" ").toTypedArray()

fun createCommandHelp(commands: Map<String, CommandLine>, requestedCommand: String): Result<List<String>, String> {
    if (requestedCommand.isNotEmpty()) {
        return commands[requestedCommand]
            ?.let { listOf(it.usageMessage).asSuccess() }
            ?: error("Unknown command '$requestedCommand'")
    }

    val uniqueCommands: MutableSet<CommandLine> = TreeSet(Comparator.comparing { it.commandName })
    uniqueCommands.addAll(commands.values)

    val response = mutableListOf("Reposilite $VERSION Commands:")

    uniqueCommands
        .forEach { command ->
            val specification = command.commandSpec

            response.add("  " + command.commandName
                    + (if (specification.args().isEmpty()) "" else " ")
                    + specification.args().joinToString(separator = " ", transform = { it.paramLabel() })
                    + " - ${specification.usageMessage().description().joinToString(". ")}"
            )
        }

    return response.asSuccess()
}