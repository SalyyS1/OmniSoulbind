package com.salyvn.omnisoulbind.config

import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

/**
 * Loads and holds all tunable behaviour from config.yml.
 *
 * Binding lives in the item NBT (PDC), so there is no data store — only this
 * config needs (re)loading. All fields fall back to sensible defaults.
 */
class SoulbindConfig(private val plugin: JavaPlugin) {

    // Death handling
    var keepOnDeath: Boolean = true
        private set

    /** true = return kept items on respawn, false = re-add immediately after death. */
    var returnOnRespawn: Boolean = true
        private set

    // Anti-others
    var restrictPickup: Boolean = true
        private set
    var restrictUse: Boolean = true
        private set

    // Auto-bind
    var autoBindEnabled: Boolean = false
        private set

    /** MMOItems tiers (upper-cased) that trigger auto-bind on pickup/equip. */
    var autoBindTiers: Set<String> = emptySet()
        private set

    /** Vanilla materials that trigger auto-bind on pickup/equip. */
    var autoBindMaterials: Set<Material> = emptySet()
        private set

    // Lore
    var loreLine: String = "&7Soulbound to &e%owner%"
        private set

    // Messages
    var messageBound: String = "&aBound this item to your soul."
        private set
    var messageAlreadyBound: String = "&eThis item is already soulbound to &e%owner%&e."
        private set
    var messageNoItem: String = "&cHold an item to soulbind it."
        private set
    var messageUnbound: String = "&aRemoved the soulbinding from this item."
        private set
    var messageNotBound: String = "&7This item is not soulbound."
        private set
    var messageReloaded: String = "&aOmniSoulbind reloaded."
        private set
    var messageInfo: String = "&7Soulbound to &e%owner%&7."
        private set
    var messageUseDenied: String = "&cThis item is soulbound to &e%owner%&c."
        private set

    fun load() {
        plugin.reloadConfig()
        val c = plugin.config

        keepOnDeath = c.getBoolean("keep-on-death", true)
        returnOnRespawn = c.getBoolean("return-on-respawn", true)
        restrictPickup = c.getBoolean("restrict-pickup", true)
        restrictUse = c.getBoolean("restrict-use", true)

        autoBindEnabled = c.getBoolean("auto-bind.enabled", false)
        autoBindTiers = c.getStringList("auto-bind.tiers").map { it.uppercase() }.toSet()
        autoBindMaterials = c.getStringList("auto-bind.materials")
            .mapNotNull { runCatching { Material.valueOf(it.uppercase()) }.getOrNull() }
            .toSet()

        loreLine = c.getString("lore-line", loreLine)!!

        val m = "messages."
        messageBound = c.getString(m + "bound", messageBound)!!
        messageAlreadyBound = c.getString(m + "already-bound", messageAlreadyBound)!!
        messageNoItem = c.getString(m + "no-item", messageNoItem)!!
        messageUnbound = c.getString(m + "unbound", messageUnbound)!!
        messageNotBound = c.getString(m + "not-bound", messageNotBound)!!
        messageReloaded = c.getString(m + "reloaded", messageReloaded)!!
        messageInfo = c.getString(m + "info", messageInfo)!!
        messageUseDenied = c.getString(m + "use-denied", messageUseDenied)!!
    }
}
