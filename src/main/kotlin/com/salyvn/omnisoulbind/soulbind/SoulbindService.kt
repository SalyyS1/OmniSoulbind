package com.salyvn.omnisoulbind.soulbind

import com.salyvn.omnisoulbind.config.SoulbindConfig
import com.salyvn.omnisoulbind.util.TextUtil
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * Central read/write of soulbinding data. The binding is stored entirely in the
 * item's [org.bukkit.persistence.PersistentDataContainer] under the
 * `omnisoulbind` namespace, so nothing needs an external database (KISS).
 *
 * Keys:
 *  - owner_uuid : String  (player UUID)
 *  - owner_name : String  (last-known player name, for lore/display)
 */
class SoulbindService(plugin: Plugin, private val config: SoulbindConfig) {

    private val ownerUuidKey = NamespacedKey(plugin, "owner_uuid")
    private val ownerNameKey = NamespacedKey(plugin, "owner_name")

    // MMOItems stores its tier in PDC under the "mmoitems:tier" key as a String.
    // Read defensively so we never hard-depend on MMOItems being present.
    private val mmoTierKey = NamespacedKey("mmoitems", "tier")

    fun isSoulbound(item: ItemStack?): Boolean = ownerUuidOf(item) != null

    fun ownerUuidOf(item: ItemStack?): UUID? {
        if (item == null || item.type.isAir || !item.hasItemMeta()) return null
        val raw = item.itemMeta?.persistentDataContainer
            ?.get(ownerUuidKey, PersistentDataType.STRING) ?: return null
        return runCatching { UUID.fromString(raw) }.getOrNull()
    }

    fun ownerNameOf(item: ItemStack?): String? {
        if (item == null || item.type.isAir || !item.hasItemMeta()) return null
        return item.itemMeta?.persistentDataContainer?.get(ownerNameKey, PersistentDataType.STRING)
    }

    /** True when [player] is the bound owner of [item]. */
    fun isOwner(item: ItemStack?, player: Player): Boolean = ownerUuidOf(item) == player.uniqueId

    /**
     * Bind [item] to [owner]. Writes PDC keys and appends the lore line.
     * Returns false if the item is null/air or already soulbound.
     */
    fun bind(item: ItemStack?, owner: Player): Boolean {
        if (item == null || item.type.isAir) return false
        if (isSoulbound(item)) return false
        val meta = item.itemMeta ?: return false
        val pdc = meta.persistentDataContainer
        pdc.set(ownerUuidKey, PersistentDataType.STRING, owner.uniqueId.toString())
        pdc.set(ownerNameKey, PersistentDataType.STRING, owner.name)
        item.itemMeta = meta
        applyLore(item, owner.name)
        return true
    }

    /**
     * Strip all soulbinding from [item] (PDC keys + lore line).
     * Returns false if the item was not soulbound.
     */
    fun unbind(item: ItemStack?): Boolean {
        if (item == null || item.type.isAir || !item.hasItemMeta()) return false
        if (!isSoulbound(item)) return false
        val meta = item.itemMeta ?: return false
        val pdc = meta.persistentDataContainer
        pdc.remove(ownerUuidKey)
        pdc.remove(ownerNameKey)
        item.itemMeta = meta
        removeLore(item)
        return true
    }

    /** MMOItems tier stored in the item PDC, or null when absent. */
    fun mmoTierOf(item: ItemStack?): String? {
        if (item == null || item.type.isAir || !item.hasItemMeta()) return null
        return runCatching {
            item.itemMeta?.persistentDataContainer?.get(mmoTierKey, PersistentDataType.STRING)
        }.getOrNull()?.uppercase()
    }

    /** Decide whether [item] should auto-bind for [player] under current config. */
    fun shouldAutoBind(item: ItemStack?): Boolean {
        if (!config.autoBindEnabled || item == null || item.type.isAir) return false
        if (isSoulbound(item)) return false
        val tier = mmoTierOf(item)
        if (tier != null && tier in config.autoBindTiers) return true
        return item.type in config.autoBindMaterials
    }

    /** Rendered owner lore [Component] for a given owner name. */
    private fun ownerLoreComponent(ownerName: String): Component =
        TextUtil.colorize(config.loreLine.replace("%owner%", ownerName))

    /** Plain form used to detect/remove our own lore line without duplicating it. */
    private fun ownerLorePlain(ownerName: String): String =
        TextUtil.strip(config.loreLine.replace("%owner%", ownerName))

    /**
     * Append the soulbound lore line without destroying existing lore and without
     * duplicating on re-scan. Matches on the plain (stripped) text so formatting
     * changes across reloads still de-duplicate.
     */
    fun applyLore(item: ItemStack, ownerName: String) {
        val meta = item.itemMeta ?: return
        val target = ownerLorePlain(ownerName)
        val existing = meta.lore()?.toMutableList() ?: mutableListOf()
        val alreadyPresent = existing.any { TextUtil.plainOf(it) == target }
        if (!alreadyPresent) {
            existing.add(ownerLoreComponent(ownerName))
            meta.lore(existing)
            item.itemMeta = meta
        }
    }

    /** Remove the soulbound lore line (identified by plain owner text) if present. */
    private fun removeLore(item: ItemStack) {
        val meta = item.itemMeta ?: return
        val name = ownerNameOf(item) // may be null after PDC cleared; fall back to marker match
        val existing = meta.lore()?.toMutableList() ?: return
        val markerPrefix = TextUtil.strip(config.loreLine.substringBefore("%owner%")).trim()
        val removed = existing.removeAll { line ->
            val plain = TextUtil.plainOf(line)
            if (name != null) plain == ownerLorePlain(name)
            else markerPrefix.isNotEmpty() && plain.startsWith(markerPrefix)
        }
        if (removed) {
            meta.lore(if (existing.isEmpty()) null else existing)
            item.itemMeta = meta
        }
    }
}
