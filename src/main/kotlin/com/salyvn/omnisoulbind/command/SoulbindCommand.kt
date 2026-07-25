package com.salyvn.omnisoulbind.command

import com.salyvn.omnisoulbind.OmniSoulbindPlugin
import com.salyvn.omnisoulbind.util.TextUtil
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.entity.Player

/**
 * Registers the /soulbind command tree via the Paper Lifecycle + Brigadier API.
 *   /soulbind          -> bind the held item to the sender      (omnisoulbind.use)
 *   /soulbind info     -> show the owner of the held item        (omnisoulbind.use)
 *   /soulbind remove   -> strip binding from the held item       (omnisoulbind.admin)
 *   /soulbind reload   -> reload config                          (omnisoulbind.admin)
 */
@Suppress("UnstableApiUsage")
class SoulbindCommand(private val plugin: OmniSoulbindPlugin) {

    fun register() {
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val root = Commands.literal("soulbind")
                .requires { it.sender.hasPermission("omnisoulbind.use") }
                .executes { ctx ->
                    val player = ctx.source.sender as? Player
                    if (player == null) {
                        ctx.source.sender.sendMessage(TextUtil.colorize("&cPlayers only."))
                        return@executes 1
                    }
                    bindHeld(player)
                    1
                }
                .then(
                    Commands.literal("info").executes { ctx ->
                        val player = ctx.source.sender as? Player ?: run {
                            ctx.source.sender.sendMessage(TextUtil.colorize("&cPlayers only."))
                            return@executes 1
                        }
                        infoHeld(player)
                        1
                    }
                )
                .then(
                    Commands.literal("remove")
                        .requires { it.sender.hasPermission("omnisoulbind.admin") }
                        .executes { ctx ->
                            val player = ctx.source.sender as? Player ?: run {
                                ctx.source.sender.sendMessage(TextUtil.colorize("&cPlayers only."))
                                return@executes 1
                            }
                            removeHeld(player)
                            1
                        }
                )
                .then(
                    Commands.literal("reload")
                        .requires { it.sender.hasPermission("omnisoulbind.admin") }
                        .executes { ctx ->
                            plugin.reload()
                            ctx.source.sender.sendMessage(TextUtil.colorize(plugin.soulbindConfig.messageReloaded))
                            1
                        }
                )
                .build()

            event.registrar().register(root, "Soulbind held items to yourself", listOf("sb"))
        }
    }

    private fun bindHeld(player: Player) {
        val item = player.inventory.itemInMainHand
        if (item.type.isAir) {
            player.sendMessage(TextUtil.colorize(plugin.soulbindConfig.messageNoItem))
            return
        }
        val existingOwner = plugin.soulbindService.ownerNameOf(item)
        if (existingOwner != null) {
            player.sendMessage(TextUtil.colorize(plugin.soulbindConfig.messageAlreadyBound.replace("%owner%", existingOwner)))
            return
        }
        if (plugin.soulbindService.bind(item, player)) {
            player.sendMessage(TextUtil.colorize(plugin.soulbindConfig.messageBound))
        }
    }

    private fun infoHeld(player: Player) {
        val item = player.inventory.itemInMainHand
        if (item.type.isAir) {
            player.sendMessage(TextUtil.colorize(plugin.soulbindConfig.messageNoItem))
            return
        }
        val owner = plugin.soulbindService.ownerNameOf(item)
        if (owner == null) {
            player.sendMessage(TextUtil.colorize(plugin.soulbindConfig.messageNotBound))
        } else {
            player.sendMessage(TextUtil.colorize(plugin.soulbindConfig.messageInfo.replace("%owner%", owner)))
        }
    }

    private fun removeHeld(player: Player) {
        val item = player.inventory.itemInMainHand
        if (item.type.isAir) {
            player.sendMessage(TextUtil.colorize(plugin.soulbindConfig.messageNoItem))
            return
        }
        if (plugin.soulbindService.unbind(item)) {
            player.sendMessage(TextUtil.colorize(plugin.soulbindConfig.messageUnbound))
        } else {
            player.sendMessage(TextUtil.colorize(plugin.soulbindConfig.messageNotBound))
        }
    }
}
