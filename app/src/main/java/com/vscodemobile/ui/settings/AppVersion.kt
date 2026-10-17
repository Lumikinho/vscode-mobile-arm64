package com.vscodemobile.ui.settings

/**
 * Comparação de versões de app.
 *
 * Fica separada da checagem de rede de propósito: o erro clássico aqui é
 * comparar por string (`"1.10.0" < "1.9.0"`), que ordena ao contrário e faz o
 * app oferecer downgrade como se fosse atualização.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int = when {
        major != other.major -> major.compareTo(other.major)
        minor != other.minor -> minor.compareTo(other.minor)
        else -> patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /**
         * Aceita o que o GitHub devolve como tag: `v1.2.3`, `1.2.3`,
         * `v1.2.3-rc1`. Sufixo de pré-lançamento é ignorado na comparação —
         * o app só oferece versão estável.
         *
         * Devolve null para o que não é versão (uma tag tipo `nightly`), e o
         * chamador trata isso como "não sei", nunca como "sem novidade".
         */
        fun parse(raw: String?): AppVersion? {
            val text = raw?.trim()?.removePrefix("v")?.removePrefix("V") ?: return null
            val core = text.substringBefore('-').substringBefore('+')
            val parts = core.split('.')
            if (parts.size < 2) return null
            val nums = parts.take(3).map { it.toIntOrNull() ?: return null }
            return AppVersion(
                major = nums[0],
                minor = nums.getOrElse(1) { 0 },
                patch = nums.getOrElse(2) { 0 }
            )
        }

        /** null quando não dá para dizer. */
        fun isNewer(candidate: String?, current: AppVersion?): Boolean? {
            val c = parse(candidate) ?: return null
            val cur = current ?: return null
            return c > cur
        }
    }
}
