/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/UnlegitMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.launch.data.legacyui.clickgui.style.styles.newdropdown.utils.normal.Main
import net.ccbluex.liquidbounce.launch.data.legacyui.clickgui.style.styles.newdropdown.utils.objects.Drag
import net.ccbluex.liquidbounce.launch.data.legacyui.clickgui.style.styles.newdropdown.utils.render.Scroll



enum class ModuleCategory(val displayName: String, val configName: String, val NotiModule: String) {
    COMBAT("%module.category.combat%", "Combat", "a"),
    PLAYER("%module.category.player%", "Player", "d"),
    MOVEMENT("%module.category.movement%", "Movement", "b"),
    RENDER("%module.category.render%", "Render", "s"),
    CLIENT("%module.category.client%", "Client", "q"),
    WORLD("%module.category.world%", "World",  "e"),
    MISC("%module.category.misc%", "Misc", "m"),
    EXPLOIT("%module.category.exploit%", "Exploit", "f"),
    ADDIT("%module.category.addit%", "Addit", "u");

    private var posX = 20
    private var posY = 20

    private var scroll = Scroll()

    
    open fun getScroll(): Scroll {
        return scroll
    }

    open fun getDrag(): Drag {
        posX = 20 + Main.categoryCount * 100
        return Drag(posX.toFloat(), posY.toFloat())
    }
    
    open fun getDragNew(moduleCategory: ModuleCategory): Drag {
        if (moduleCategory == COMBAT) return Drag(5f, posY.toFloat())
        else if (moduleCategory == PLAYER) return Drag(125f, posY.toFloat())
        else if (moduleCategory == MOVEMENT) return Drag(245f, posY.toFloat())
        else if (moduleCategory == RENDER) return Drag(365f, posY.toFloat())
        else if (moduleCategory == CLIENT) return Drag(485f, posY.toFloat())
        else if (moduleCategory == WORLD) return Drag(605f, posY.toFloat())
        else if (moduleCategory == MISC) return Drag(725f, posY.toFloat())
        else if (moduleCategory == EXPLOIT) return Drag(845f, posY.toFloat())
        else if (moduleCategory == ADDIT) return Drag(605f, posY.toFloat() + 320f)
        else return Drag(0f, 0f)
    }
}

enum class NotiInfo(val NotiModule: String){
    COMBAT( "a"),
    PLAYER( "d"),
    MOVEMENT( "b"),
    RENDER( "s"),
    CLIENT("q"),
    WORLD( "e"),
    MISC( "m"),
    EXPLOIT( "f"),
}


