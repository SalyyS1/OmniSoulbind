package com.salyvn.omnisoulbind

import com.salyvn.omnisoulbind.command.SoulbindCommand
import com.salyvn.omnisoulbind.config.SoulbindConfig
import com.salyvn.omnisoulbind.listener.SoulbindListener
import com.salyvn.omnisoulbind.papi.SoulbindPlaceholders
import com.salyvn.omnisoulbind.soulbind.SoulbindService
import com.salyvn.omnisoulbind.util.TextUtil
import com.tcoded.folialib.FoliaLib
import org.bukkit.plugin.java.JavaPlugin

class OmniSoulbindPlugin : JavaPlugin() {

    lateinit var foliaLib: FoliaLib
        private set
    lateinit var soulbindConfig: SoulbindConfig
        private set
    lateinit var soulbindService: SoulbindService
        private set

    override fun onEnable() {
        instance = this
        foliaLib = FoliaLib(this)

        saveDefaultConfig()
        soulbindConfig = SoulbindConfig(this)
        soulbindConfig.load()

        soulbindService = SoulbindService(this, soulbindConfig)

        server.pluginManager.registerEvents(
            SoulbindListener(soulbindService, soulbindConfig, foliaLib),
            this
        )
        SoulbindCommand(this).register()

        // Register PlaceholderAPI expansion only when PAPI is present (soft-depend).
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            runCatching { SoulbindPlaceholders(this).register() }
                .onFailure { logger.warning("Failed to register PlaceholderAPI expansion: ${it.message}") }
        }

        logger.info("OmniSoulbind enabled. keep-on-death=${soulbindConfig.keepOnDeath}, auto-bind=${soulbindConfig.autoBindEnabled}.")
    }

    override fun onDisable() {
        logger.info("OmniSoulbind disabled.")
    }

    fun reload() {
        soulbindConfig.load()
        logger.info(TextUtil.strip("OmniSoulbind reloaded."))
    }

    companion object {
        @JvmStatic
        lateinit var instance: OmniSoulbindPlugin
            private set
    }
}
