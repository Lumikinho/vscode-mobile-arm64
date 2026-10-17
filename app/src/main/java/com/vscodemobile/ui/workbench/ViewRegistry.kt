package com.vscodemobile.ui.workbench

/** Espelha ViewContainerLocation de src/vs/workbench/common/views.ts */
enum class ViewContainerLocation { Sidebar, Panel, AuxiliaryBar }

/** Espelha IViewContainerDescriptor (só campos usados no mobile). */
data class ViewContainerDescriptor(
    val id: String,
    val title: String,
    /** Código Codicon (ver Codicon.*). */
    val icon: String,
    val order: Int,
    val openCommandId: String
)

/** Espelha IViewDescriptor. */
data class ViewDescriptor(
    val id: String,
    val name: String,
    val containerId: String,
    val order: Int,
    val canToggleVisibility: Boolean = true
)

/** Espelha registerViewWelcomeContent. */
data class WelcomeContent(val viewId: String, val content: String, val order: Int)

/** Espelha ViewContainersRegistry + ViewsRegistry (versão mobile, sem DI). */
object ViewRegistry {
    private val containers = mutableListOf<ViewContainerDescriptor>()
    private val views = mutableListOf<ViewDescriptor>()
    private val welcome = mutableListOf<WelcomeContent>()

    fun registerContainer(desc: ViewContainerDescriptor) {
        if (containers.none { it.id == desc.id }) containers.add(desc)
    }

    fun registerViews(vararg v: ViewDescriptor) {
        v.forEach { vd -> if (views.none { it.id == vd.id }) views.add(vd) }
    }

    fun registerWelcome(vararg w: WelcomeContent) {
        welcome.addAll(w)
    }

    fun sidebarContainers(): List<ViewContainerDescriptor> =
        containers.sortedBy { it.order }

    fun viewsOf(containerId: String): List<ViewDescriptor> =
        views.filter { it.containerId == containerId }.sortedBy { it.order }

    fun welcomeOf(viewId: String): List<WelcomeContent> =
        welcome.filter { it.viewId == viewId }.sortedBy { it.order }

    fun clear() {
        containers.clear()
        views.clear()
        welcome.clear()
    }
}
