/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/UnlegitMC/FDPClient/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.client.Target.animalValue
import net.ccbluex.liquidbounce.features.module.modules.client.Target.deadValue
import net.ccbluex.liquidbounce.features.module.modules.client.Target.invisibleValue
import net.ccbluex.liquidbounce.features.module.modules.client.Target.mobValue
import net.ccbluex.liquidbounce.features.module.modules.client.Target.playerValue
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityGolem
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3


object EntityUtils : MinecraftInstance() {

    fun rayTraceCustom(blockReachDistance: Double, yaw: Float, pitch: Float): MovingObjectPosition? {
        val vec3: Vec3 = mc.thePlayer.getPositionEyes(1.0f)
        val vec31: Vec3 = getLookCustom(yaw, pitch)
        val vec32: Vec3 = vec3.addVector(
            vec31.xCoord * blockReachDistance,
            vec31.yCoord * blockReachDistance,
            vec31.zCoord * blockReachDistance
        )
        return mc.thePlayer.worldObj.rayTraceBlocks(vec3, vec32, false, false, true)
    }
    fun getLookCustom(yaw: Float, pitch: Float): Vec3 {
        return mc.thePlayer.getVectorForRotation(pitch, yaw)
    }
    fun isSelected(entity: Entity, canAttackCheck: Boolean): Boolean {
        if (entity is EntityLivingBase && (deadValue.get() || entity.isEntityAlive()) && entity !== mc.thePlayer) {
            if (invisibleValue.get() || !entity.isInvisible()) {
                if (playerValue.get() && entity is EntityPlayer) {
                    if (canAttackCheck) {
                        if (AntiBot.isBot(entity)) {
                            return false
                        }

                        if (isFriend(entity)) {
                            return false
                        }

                        if (entity.isSpectator) {
                            return false
                        }

                        if (entity.isPlayerSleeping) {
                            return false
                        }

                        if (!LiquidBounce.combatManager.isFocusEntity(entity)) {
                            return false
                        }

                        val teams = LiquidBounce.moduleManager.getModule(Teams::class.java)
                        return !teams!!.state || !teams.isInYourTeam(entity)
                    }

                    return true
                }
                return mobValue.get() && isMob(entity) || animalValue.get() && isAnimal(entity)
            }
        }
        return false
    }
    fun canRayCast(entity: Entity): Boolean {
        if (entity is EntityLivingBase) {
            if (entity is EntityPlayer) {
                val teams = LiquidBounce.moduleManager.getModule(Teams::class.java)
                return !teams!!.state || !teams.isInYourTeam(entity)
            } else {
                return mobValue.get() && isMob(entity) || animalValue.get() && isAnimal(entity)
            }
        }
        return false
    }
    fun isRendered(entityToCheck: Entity?): Boolean {
        return mc.theWorld != null && mc.theWorld.getLoadedEntityList().contains(entityToCheck)
    }

    fun getName(networkPlayerInfoIn: NetworkPlayerInfo): String? {
        return if (networkPlayerInfoIn.displayName != null) networkPlayerInfoIn.displayName.formattedText else ScorePlayerTeam.formatPlayerName(
            networkPlayerInfoIn.playerTeam,
            networkPlayerInfoIn.gameProfile.name
        )
    }

    fun isFriend(entity: Entity): Boolean {
        return entity is EntityPlayer && entity.getName() != null && LiquidBounce.fileManager.friendsConfig.isFriend(stripColor(entity.getName()))
    }

    fun isFriend(entity: String): Boolean {
        return LiquidBounce.fileManager.friendsConfig.isFriend(entity)
    }

    fun isAnimal(entity: Entity): Boolean {
        return entity is EntityAnimal || entity is EntitySquid || entity is EntityGolem || entity is EntityVillager || entity is EntityBat
    }

    fun isMob(entity: Entity): Boolean {
        return entity is EntityMob || entity is EntitySlime || entity is EntityGhast || entity is EntityDragon
    }
}