package com.vscodemobile.ui.workbench

/** Espelha MenuId de src/vs/platform/actions/common/actions.ts (subconjunto mobile). */
enum class MenuId { CommandPalette, ViewTitle, ExplorerContext, TerminalTabs, EditorTitle }

/**
 * Espelha Action2/ICommandService: um comando com id real do VS Code
 * (ex.: workbench.action.files.save), título e handler.
 */
data class WorkbenchCommand(
    val id: String,
    val title: String,
    val category: String? = null,
    val icon: String? = null,
    val menus: Set<MenuId> = setOf(MenuId.CommandPalette),
    val run: () -> Unit = {}
) {
    val paletteTitle: String get() = if (category != null) "$category: $title" else title
}

/** Espelha CommandsRegistry + MenuRegistry (só palette, sem context keys). */
object CommandRegistry {
    private val commands = LinkedHashMap<String, WorkbenchCommand>()

    fun register(cmd: WorkbenchCommand) {
        commands.putIfAbsent(cmd.id, cmd)
    }

    fun paletteCommands(): List<WorkbenchCommand> =
        commands.values.filter { MenuId.CommandPalette in it.menus }

    fun execute(id: String): Boolean {
        val cmd = commands[id] ?: return false
        cmd.run()
        return true
    }

    fun clear() = commands.clear()
}
