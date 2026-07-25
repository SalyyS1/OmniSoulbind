package com.salyvn.omnisoulbind.papi

import com.salyvn.omnisoulbind.OmniSoulbindPlugin
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

/**
 * PlaceholderAPI expansion — registered only when PAPI is installed.
 *
 *   %omnisoulbind_owner%  -> owner name of the player's held item, or "" if none.
 *   %omnisoulbind_bound%  -> "true"/"false" for whether the held item is soulbound.
 */
class SoulbindPlaceholders(private val plugin: OmniSoulbindPlugin) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "omnisoulbind"
    override fun getAuthor(): String = "SalyVn"
    override fun getVersion(): String = plugin.pluginMeta.version
    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return ""
        val held = player.inventory.itemInMainHand
        return when (params.lowercase()) {
            "owner" -> plugin.soulbindService.ownerNameOf(held) ?: ""
            "bound" -> plugin.soulbindService.isSoulbound(held).toString()
            else -> null
        }
    }
}
