package com.vscodemobile.ui.workbench

data class QuickPickItem(
    val label: String,
    val detail: String? = null,
    val id: String = label
)

/**
 * Espelha IQuickAccessProvider (platform/quickinput): cada provider responde
 * por um prefixo ('>' comandos, '' arquivos, '@' símbolos).
 */
interface QuickAccessProvider {
    val prefix: String
    val placeholder: String
    fun items(query: String): List<QuickPickItem>
    fun onPick(item: QuickPickItem)
}

/** Espelha IQuickAccessRegistry. */
object QuickAccessRegistry {
    private val providers = mutableListOf<QuickAccessProvider>()

    fun register(provider: QuickAccessProvider) {
        if (providers.none { it.prefix == provider.prefix }) providers.add(provider)
    }

    /** Provider do prefixo mais longo que casa; cai no prefixo '' (arquivos). */
    fun providerFor(query: String): QuickAccessProvider =
        providers
            .filter { it.prefix.isNotEmpty() && query.startsWith(it.prefix) }
            .maxByOrNull { it.prefix.length }
            ?: providers.first { it.prefix.isEmpty() }

    fun clear() = providers.clear()
}
