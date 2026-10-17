package com.vscodemobile.ui.workbench

import com.vscodemobile.ui.theme.Codicon

/**
 * Ações da UI injetadas nos comandos (o handler executa na camada UI).
 * Espelha o ServicesAccessor: o plugin declara, o workbench resolve.
 */
data class WorkbenchActions(
    val save: () -> Unit = {},
    val saveAll: () -> Unit = {},
    val newFile: () -> Unit = {},
    val openFile: () -> Unit = {},
    val openFolder: () -> Unit = {},
    val closeEditor: () -> Unit = {},
    val showView: (String) -> Unit = {},
    val toggleDrawer: () -> Unit = {},
    val togglePanel: (Int) -> Unit = {},
    val showPalette: () -> Unit = {},
    val showThemes: () -> Unit = {},
    val openTermux: () -> Unit = {},
    val restartShell: () -> Unit = {},
    val showProblems: () -> Unit = {},
    val onLinuxSetup: () -> Unit = {},
    val onLinuxShell: () -> Unit = {},
    val managePlugins: () -> Unit = {}
)

/**
 * Plugins built-in: registram containers, views e comandos com os IDs reais
 * do VS Code (workbench.view.*, workbench.action.*), como os .contribution.ts.
 */
object BuiltinPlugins {

    fun registerAll(actions: WorkbenchActions) {
        registerContainers()
        registerCommands(actions)
        registerQuickAccess(actions)
    }

    private fun registerContainers() {
        ViewRegistry.registerContainer(
            ViewContainerDescriptor(
                "workbench.view.explorer", "Explorer", Codicon.Files, 0,
                "workbench.view.explorer"
            )
        )
        ViewRegistry.registerContainer(
            ViewContainerDescriptor(
                "workbench.view.search", "Search", Codicon.Search, 1,
                "workbench.view.search"
            )
        )
        ViewRegistry.registerContainer(
            ViewContainerDescriptor(
                "workbench.view.scm", "Source Control", Codicon.SourceControl, 2,
                "workbench.view.scm"
            )
        )
        ViewRegistry.registerContainer(
            ViewContainerDescriptor(
                "workbench.view.debug", "Run and Debug", Codicon.DebugAlt, 3,
                "workbench.view.debug"
            )
        )
        ViewRegistry.registerContainer(
            ViewContainerDescriptor(
                "workbench.view.extensions", "Extensions", Codicon.Extensions, 4,
                "workbench.view.extensions"
            )
        )
        ViewRegistry.registerViews(
            ViewDescriptor("workbench.explorer.fileView", "Folders", "workbench.view.explorer", 1),
            ViewDescriptor("workbench.explorer.openEditorsView", "Open Editors", "workbench.view.explorer", 0),
            ViewDescriptor("workbench.view.search", "Search", "workbench.view.search", 0),
            ViewDescriptor("workbench.scm", "Changes", "workbench.view.scm", 1),
            ViewDescriptor("workbench.debug.variablesView", "Variables", "workbench.view.debug", 10),
            ViewDescriptor("workbench.debug.watchView", "Watch", "workbench.view.debug", 20),
            ViewDescriptor("workbench.debug.callStackView", "Call Stack", "workbench.view.debug", 30),
            ViewDescriptor("workbench.debug.breakPointsView", "Breakpoints", "workbench.view.debug", 40),
            ViewDescriptor("workbench.panel.markers.view", "Problems", "workbench.panel.markers", 0),
            ViewDescriptor("terminal", "Terminal", "workbench.panel.terminal", 0)
        )
        ViewRegistry.registerWelcome(
            WelcomeContent("workbench.scm", "No source control providers registered.", 0),
            WelcomeContent("workbench.explorer.fileView", "No folder opened.", 0)
        )
    }

    private fun registerCommands(a: WorkbenchActions) {
        fun cmd(id: String, category: String, title: String, run: () -> Unit) {
            CommandRegistry.register(WorkbenchCommand(id, title, category, run = run))
        }
        cmd("workbench.action.files.save", "File", "Save", a.save)
        cmd("workbench.action.files.saveAll", "File", "Save All Files", a.saveAll)
        cmd("workbench.action.files.newUntitledFile", "File", "New Untitled File", a.newFile)
        cmd("workbench.action.files.openFile", "File", "Open…", a.openFile)
        cmd("workbench.action.files.openFolder", "File", "Open Folder…", a.openFolder)
        cmd("workbench.action.closeActiveEditor", "View", "Close Editor", a.closeEditor)
        cmd("workbench.view.explorer", "View", "Explorer", { a.showView("workbench.view.explorer") })
        cmd("workbench.view.search", "View", "Search", { a.showView("workbench.view.search") })
        cmd("workbench.view.scm", "View", "Source Control", { a.showView("workbench.view.scm") })
        cmd("workbench.view.debug", "View", "Run and Debug", { a.showView("workbench.view.debug") })
        cmd("workbench.view.extensions", "View", "Extensions", { a.showView("workbench.view.extensions") })
        cmd("workbench.action.toggleSidebarVisibility", "View", "Toggle Sidebar", a.toggleDrawer)
        cmd("workbench.actions.view.problems", "View", "Toggle Problems", a.showProblems)
        cmd("workbench.action.output.toggleOutput", "View", "Toggle Output", { a.togglePanel(1) })
        cmd("workbench.action.terminal.toggleTerminal", "Terminal", "Toggle Integrated Terminal", { a.togglePanel(2) })
        cmd("workbench.action.terminal.new", "Terminal", "Create New Terminal", a.restartShell)
        cmd("workbench.action.terminal.openTermux", "Terminal", "Open in Termux", a.openTermux)
        cmd("workbench.linux.setupAlpine", "Linux", "Setup Alpine", a.onLinuxSetup)
        cmd("workbench.linux.openShell", "Linux", "Open Shell", a.onLinuxShell)
        cmd("workbench.action.debug.start", "Run", "Start Debugging", a.openTermux)
        cmd("workbench.action.selectTheme", "Preferences", "Color Theme", a.showThemes)
        cmd("workbench.plugin.manage", "Plugins", "Manage WebView Plugins", a.managePlugins)
    }

    private fun registerQuickAccess(a: WorkbenchActions) {
        QuickAccessRegistry.register(object : QuickAccessProvider {
            override val prefix = ">"
            override val placeholder = "Type a command"
            override fun items(query: String): List<QuickPickItem> =
                CommandRegistry.paletteCommands().map {
                    QuickPickItem(it.paletteTitle, it.id, it.id)
                }
            override fun onPick(item: QuickPickItem) {
                CommandRegistry.execute(item.id)
            }
        })
    }
}
