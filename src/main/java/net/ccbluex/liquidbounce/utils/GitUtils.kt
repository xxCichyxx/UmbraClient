/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.UmbraClient
import java.util.*

class GitUtils {
    companion object {
        @JvmField
        val gitInfo = Properties().also {
            val inputStream = UmbraClient::class.java.classLoader.getResourceAsStream("git.properties")
            if (inputStream != null) {
                it.load(inputStream)
            } else {
                it["git.branch"] = "master"
            }
        }

        @JvmField
        val gitBranch = (gitInfo["git.branch"] ?: "unknown")
    }
}