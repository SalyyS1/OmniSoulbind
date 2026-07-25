package com.salyvn.omnisoulbind.listener

import com.salyvn.omnisoulbind.config.SoulbindConfig
import com.salyvn.omnisoulbind.soulbind.SoulbindService
import com.salyvn.omnisoulbind.util.TextUtil
import com.tcoded.folialib.FoliaLib
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Enforces the three soulbind guarantees:
 *  1. Keep-on-death: a dying player's own soulbound items are pulled from the
 *     drop list, stashed, and returned on respawn (or immediately, per config).
 *  2. Anti-pickup: other players cannot pick up items they don't own.
 *  3. Anti-use: other players cannot click/interact with items bound to someone else.
 *
 * All item NBT reads happen on the event thread (region-correct on Folia); the
 * only deferred work is the respawn return, scheduled via FoliaLib to the owner's
 * entity scheduler.
 */
class SoulbindListener(
    private val service: SoulbindService,
    private val config: SoulbindConfig,
    private val foliaLib: FoliaLib
) : Listener {

    // Items stashed at death, keyed by player UUID, returned on respawn.
    private val stashed = ConcurrentHashMap<UUID, MutableList<ItemStack>>()

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPickup(event: EntityPickupItemEvent) {
        val stack = event.item.itemStack
        val entity = event.entity
        val owner = service.ownerUuidOf(stack)

        if (owner == null) {
            // Not yet soulbound: auto-bind to the picking-up player when configured.
            if (entity is Player && service.shouldAutoBind(stack)) {
                service.bind(stack, entity)
                event.item.itemStack = stack
            }
            return
        }

        if (!config.restrictPickup) return
        if (entity !is Player) {
            // Non-player entities never pick up soulbound items.
            event.isCancelled = true
            return
        }
        if (entity.uniqueId != owner) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDeath(event: PlayerDeathEvent) {
        if (!config.keepOnDeath) return
        val player = event.entity
        val kept = mutableListOf<ItemStack>()
        val drops = event.drops.iterator()
        while (drops.hasNext()) {
            val drop = drops.next()
            if (service.isOwner(drop, player)) {
                kept.add(drop.clone())
                drops.remove()
            }
        }
        if (kept.isEmpty()) return

        if (config.returnOnRespawn) {
            stashed[player.uniqueId] = kept
        } else {
            giveBack(player, kept)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val kept = stashed.remove(player.uniqueId) ?: return
        // Defer one tick so the respawn inventory is fully initialized.
        foliaLib.scheduler.runAtEntityLater(player, Runnable {
            giveBack(player, kept)
        }, 1L)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        if (!config.restrictUse) return
        val player = event.whoClicked as? Player ?: return
        // Block picking up / moving another player's soulbound item.
        val current = event.currentItem
        val owner = service.ownerUuidOf(current)
        if (owner != null && owner != player.uniqueId) {
            event.isCancelled = true
            notifyRestricted(player, service.ownerNameOf(current))
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (!config.restrictUse) return
        val player = event.player
        val item = event.item ?: return
        val owner = service.ownerUuidOf(item) ?: return
        if (owner != player.uniqueId) {
            event.isCancelled = true
            notifyRestricted(player, service.ownerNameOf(item))
        }
    }

    private fun notifyRestricted(player: Player, ownerName: String?) {
        val who = ownerName ?: "another player"
        player.sendMessage(TextUtil.colorize(config.messageUseDenied.replace("%owner%", who)))
    }

    /** Return kept items to the player's inventory; overflow drops at their feet. */
    private fun giveBack(player: Player, items: List<ItemStack>) {
        val overflow = player.inventory.addItem(*items.toTypedArray())
        for (leftover in overflow.values) {
            player.world.dropItemNaturally(player.location, leftover)
        }
    }
}
