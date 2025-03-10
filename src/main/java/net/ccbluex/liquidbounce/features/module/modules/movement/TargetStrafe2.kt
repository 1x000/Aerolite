package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin


@ModuleInfo(name = "TargetStrafe2", category = ModuleCategory.MOVEMENT)
class TargetStrafe2 : Module() {
    private val renderModeValue = ListValue("RenderMode", arrayOf("Circle", "Polygon", "None"), "Polygon")
    private val lineWidthValue = FloatValue("LineWidth", 1f, 1f, 10f). displayable{!renderModeValue.equals("None")}
    private val radiusValue = FloatValue("StrafeRadius", 2.0f, 0.1f, 4.0f)
    private val forwardRadius = FloatValue("ForwardRadius", 2.0f, 0.1f, 4.0f)
    private val custom = BoolValue("customWard", false)
    private val forward = FloatValue("Forward", 0.0f, 0.0f, 1.0f)
    private val backward = FloatValue("Backward", 0.0f, 0.0f, 1.0f)

    private val holdSpaceValue = BoolValue("HoldSpace", false)
    private val onlySpeedValue = BoolValue("OnlySpeed", true)
    private val onlyflyValue = BoolValue("keyFly", false)
    private val onlyGroundValue = BoolValue("OnlyGround", false)
    private val renderValue = BoolValue("Render", true)
    private var direction = true
    private var yaw = 0f

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState === EventState.PRE) {
            if (mc.gameSettings.keyBindLeft.isKeyDown) {
                direction = true
            } else if (mc.gameSettings.keyBindRight.isKeyDown) {
                direction = false
            } else if (mc.thePlayer.isCollidedHorizontally) {
                direction = !direction
            }
        }
    }

    @EventTarget
    fun strafe(event: MoveEvent) {
        val target = LiquidBounce.combatManager.target
        val strafe = if(mc.thePlayer.getDistanceToEntity(target) <= forwardRadius.get()) if (direction) 1.0 else -1.0 else 0.0
        val testward = if (mc.thePlayer.getDistanceToEntity(target) <= radiusValue.get()) if(custom.get()) -backward.get().toDouble() else 0.0 else if(custom.get()) forward.get().toDouble() else 1.0

        if (canStrafe(target)) {
            if(mc.thePlayer.onGround || !onlyGroundValue.get()) {
                yaw = RotationUtils.getRotationsEntity(target).yaw
            }
            MovementUtils.setSpeed(event, MovementUtils.getSpeed().toDouble(), yaw, strafe, testward)
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val target = LiquidBounce.combatManager.target
        if (renderModeValue.get() != "None" && canStrafe(target)) {
            if (target == null) return
            val counter = intArrayOf(0)
            if (renderModeValue.get().equals("Circle", ignoreCase = true)) {
                GL11.glPushMatrix()
                GL11.glDisable(3553)
                GL11.glEnable(2848)
                GL11.glEnable(2881)
                GL11.glEnable(2832)
                GL11.glEnable(3042)
                GL11.glBlendFunc(770, 771)
                GL11.glHint(3154, 4354)
                GL11.glHint(3155, 4354)
                GL11.glHint(3153, 4354)
                GL11.glDisable(2929)
                GL11.glDepthMask(false)
                GL11.glLineWidth(lineWidthValue.get())
                GL11.glBegin(3)
                val x =
                    target.lastTickPosX + (target.posX - target.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
                val y =
                    target.lastTickPosY + (target.posY - target.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
                val z =
                    target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
                for (i in 0..359) {
                    val rainbow = Color(
                        Color.HSBtoRGB(
                            ((mc.thePlayer.ticksExisted / 70.0 + sin(i / 50.0 * 1.75)) % 1.0f).toFloat(),
                            0.7f,
                            1.0f
                        )
                    )
                    GL11.glColor3f(rainbow.red / 255.0f, rainbow.green / 255.0f, rainbow.blue / 255.0f)
                    GL11.glVertex3d(
                        x + radiusValue.get() * cos(i * 6.283185307179586 / 45.0),
                        y,
                        z + radiusValue.get() * sin(i * 6.283185307179586 / 45.0)
                    )
                }
                GL11.glEnd()
                GL11.glDepthMask(true)
                GL11.glEnable(2929)
                GL11.glDisable(2848)
                GL11.glDisable(2881)
                GL11.glEnable(2832)
                GL11.glEnable(3553)
                GL11.glPopMatrix()
            } else {
                val rad = radiusValue.get()
                GL11.glPushMatrix()
                GL11.glDisable(3553)
                startDrawing()
                GL11.glDisable(2929)
                GL11.glDepthMask(false)
                GL11.glLineWidth(lineWidthValue.get())
                GL11.glBegin(3)
                val x =
                    target.lastTickPosX + (target.posX - target.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
                val y =
                    target.lastTickPosY + (target.posY - target.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
                val z =
                    target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
                for (i in 0..10) {
                    counter[0] = counter[0] + 1
                    val rainbow = Color(ColorUtils.astolfoRainbow(counter[0] * 100, 5, 107))
                    //final Color rainbow = new Color(Color.HSBtoRGB((float) ((mc.thePlayer.ticksExisted / 70.0 + sin(i / 50.0 * 1.75)) % 1.0f), 0.7f, 1.0f));
                    GL11.glColor3f(rainbow.red / 255.0f, rainbow.green / 255.0f, rainbow.blue / 255.0f)
                    if (rad < 0.8 && rad > 0.0) GL11.glVertex3d(
                        x + rad * cos(i * 6.283185307179586 / 3.0),
                        y,
                        z + rad * sin(i * 6.283185307179586 / 3.0)
                    )
                    if (rad < 1.5 && rad > 0.7) {
                        counter[0] = counter[0] + 1
                        RenderUtils.glColor(ColorUtils.astolfoRainbow(counter[0] * 100, 5, 107))
                        GL11.glVertex3d(
                            x + rad * cos(i * 6.283185307179586 / 4.0),
                            y,
                            z + rad * sin(i * 6.283185307179586 / 4.0)
                        )
                    }
                    if (rad < 2.0 && rad > 1.4) {
                        counter[0] = counter[0] + 1
                        RenderUtils.glColor(ColorUtils.astolfoRainbow(counter[0] * 100, 5, 107))
                        GL11.glVertex3d(
                            x + rad * cos(i * 6.283185307179586 / 5.0),
                            y,
                            z + rad * sin(i * 6.283185307179586 / 5.0)
                        )
                    }
                    if (rad < 2.4 && rad > 1.9) {
                        counter[0] = counter[0] + 1
                        RenderUtils.glColor(ColorUtils.astolfoRainbow(counter[0] * 100, 5, 107))
                        GL11.glVertex3d(
                            x + rad * cos(i * 6.283185307179586 / 6.0),
                            y,
                            z + rad * sin(i * 6.283185307179586 / 6.0)
                        )
                    }
                    if (rad < 2.7 && rad > 2.3) {
                        counter[0] = counter[0] + 1
                        RenderUtils.glColor(ColorUtils.astolfoRainbow(counter[0] * 100, 5, 107))
                        GL11.glVertex3d(
                            x + rad * cos(i * 6.283185307179586 / 7.0),
                            y,
                            z + rad * sin(i * 6.283185307179586 / 7.0)
                        )
                    }
                    if (rad < 6.0 && rad > 2.6) {
                        counter[0] = counter[0] + 1
                        RenderUtils.glColor(ColorUtils.astolfoRainbow(counter[0] * 100, 5, 107))
                        GL11.glVertex3d(
                            x + rad * cos(i * 6.283185307179586 / 8.0),
                            y,
                            z + rad * sin(i * 6.283185307179586 / 8.0)
                        )
                    }
                    if (rad < 7.0 && rad > 5.9) {
                        counter[0] = counter[0] + 1
                        RenderUtils.glColor(ColorUtils.astolfoRainbow(counter[0] * 100, 5, 107))
                        GL11.glVertex3d(
                            x + rad * cos(i * 6.283185307179586 / 9.0),
                            y,
                            z + rad * sin(i * 6.283185307179586 / 9.0)
                        )
                    }
                    if (rad < 11.0) if (rad > 6.9) {
                        counter[0] = counter[0] + 1
                        RenderUtils.glColor(ColorUtils.astolfoRainbow(counter[0] * 100, 5, 107))
                        GL11.glVertex3d(
                            x + rad * cos(i * 6.283185307179586 / 10.0),
                            y,
                            z + rad * sin(i * 6.283185307179586 / 10.0)
                        )
                    }
                }
                GL11.glEnd()
                GL11.glDepthMask(true)
                GL11.glEnable(2929)
                stopDrawing()
                GL11.glEnable(3553)
                GL11.glPopMatrix()
            }
        }
    }

    private fun canStrafe(target: EntityLivingBase?): Boolean {
        return target != null && (!holdSpaceValue.get() || mc.thePlayer.movementInput.jump) && ((!onlySpeedValue.get() || LiquidBounce.moduleManager[Speed::class.java]!!.state) || (onlyflyValue.get() && LiquidBounce.moduleManager[Fly::class.java]!!.state))
    }

    fun startDrawing() {
        GL11.glEnable(3042)
        GL11.glEnable(3042)
        GL11.glBlendFunc(770, 771)
        GL11.glEnable(2848)
        GL11.glDisable(3553)
        GL11.glDisable(2929)
        Minecraft.getMinecraft().entityRenderer.setupCameraTransform(
            Minecraft.getMinecraft().timer.renderPartialTicks,
            0
        )
    }
    fun stopDrawing() {
        GL11.glDisable(3042)
        GL11.glEnable(3553)
        GL11.glDisable(2848)
        GL11.glDisable(3042)
        GL11.glEnable(2929)
    }
}