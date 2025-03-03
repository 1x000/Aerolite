/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/UnlegitMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import me.stars.utils.BlurUtilsOld
import me.stars.utils.Renderer
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.StrafeFix
import net.ccbluex.liquidbounce.injection.access.StaticStorage
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.block.PlaceInfo.Companion.get
import net.ccbluex.liquidbounce.utils.extensions.rayTraceWithServerSideRotation
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockAir
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.stats.StatList
import net.minecraft.util.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.security.SecureRandom
import kotlin.math.*

@ModuleInfo(name = "BlockFly", category = ModuleCategory.WORLD, keyBind = Keyboard.KEY_G)
class BlockFly : Module() {

    // Delay
    private val placeableDelayValue = ListValue("PlaceableDelay", arrayOf("Normal", "Smart", "OFF"), "Normal")
    private val placeDelayTower = BoolValue("TowerPlaceableDelay", true)
    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 0, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = minDelayValue.get()
            if (i > newValue) set(i)
        }
    }.displayable { !placeableDelayValue.equals("OFF") } as IntegerValue
    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 0, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = maxDelayValue.get()
            if (i < newValue) set(i)
        }
    }.displayable { !placeableDelayValue.equals("OFF") } as IntegerValue

    // AutoBlock
    private val autoBlockValue = ListValue("AutoBlock", arrayOf("Spoof", "LiteSpoof", "Switch", "OFF"), "LiteSpoof")

    // Basic stuff
    private val sprintValue = ListValue("Sprint", arrayOf("Always", "Dynamic", "OnGround", "OffGround", "Hypixel", "OFF"), "Always")
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val searchValue = BoolValue("Search", true)
    private val downValue = BoolValue("Down", true)
    private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post", "All"), "Pre")
    private val towerPlaceModeValue = ListValue("TowerPlaceTiming", arrayOf("Pre", "Post", "All"), "Post")

    // Eagle
    private val eagleValue = ListValue("Eagle", arrayOf("Silent", "Normal", "OFF"), "OFF")
    private val blocksToEagleValue = IntegerValue("BlocksToEagle", 0, 0, 10).displayable { !eagleValue.equals("OFF") }

    // Expand
    private val expandLengthValue = IntegerValue("ExpandLength", 1, 1, 6)

    // Rotations
    private val rotationsValue = ListValue("Rotations", arrayOf("None", "Vanilla", "AAC", "Test1", "Test2", "Custom", "Advanced", "NCP", "AAC4", "Backwards", "Snap", "BackSnap"), "AAC")
    private val towerrotationsValue = ListValue("TowerRotations", arrayOf("None", "Vanilla", "AAC", "Test1", "Test2", "Custom", "Advanced", "NCP", "AAC4", "Backwards"), "AAC")
    private val aacYawValue = IntegerValue("AACYawOffset", 0, 0, 90).displayable { rotationsValue.equals("AAC") }
    private val advancedYawModeValue = ListValue("AdvancedYawRotations", arrayOf("Offset", "Static", "RoundStatic", "Vanilla", "Round", "MoveDirection", "OffsetMove"), "MoveDirection").displayable { rotationsValue.equals("Advanced") }
    private val advancedPitchModeValue = ListValue("AdvancedPitchRotations", arrayOf("Offset", "Static", "Vanilla"), "Static").displayable { rotationsValue.equals("Advanced") }
    private val advancedYawOffsetValue = IntegerValue("AdvancedOffsetYaw", -15, -180, 180).displayable { rotationsValue.equals("Advanced") && advancedYawModeValue.equals("Offset") }
    private val advancedYawMoveOffsetValue = IntegerValue("AdvancedMoveOffsetYaw", -15, -180, 180).displayable { rotationsValue.equals("Advanced") && advancedYawModeValue.equals("Offset") }
    private val advancedYawStaticValue = IntegerValue("AdvancedStaticYaw", 145, -180, 180).displayable { rotationsValue.equals("Advanced") && (advancedYawModeValue.equals("Static") || advancedYawModeValue.equals("RoundStatic")) }
    private val advancedYawRoundValue = IntegerValue("AdvancedYawRoundValue", 45, 0, 180).displayable { rotationsValue.equals("Advanced") && (advancedYawModeValue.equals("Round") || advancedYawModeValue.equals("RoundStatic")) }
    private val advancedPitchOffsetValue = FloatValue("AdvancedOffsetPitch", -0.4f, -90f, 90f).displayable { rotationsValue.equals("Advanced") && advancedPitchModeValue.equals("Offset") }
    private val advancedPitchStaticValue = FloatValue("AdvancedStaticPitch", 82.4f, -90f, 90f).displayable { rotationsValue.equals("Advanced") && advancedPitchModeValue.equals("Static") }
    private val randomChangeValue = BoolValue("RandomChange", false)
    private val changeMaxPitchValue = IntegerValue("ChangeMaxPitch", 1, -89, 89).displayable { randomChangeValue.get() }
    private val changeMaxYawValue = IntegerValue("ChangeMaxYaw", 1, -179, 179).displayable { randomChangeValue.get() }
    private val customYawValue = IntegerValue("CustomYaw", -145, -180, 180).displayable { rotationsValue.equals("Custom") }
    private val customPitchValue = IntegerValue("CustomPitch", 79, -90, 90).displayable { rotationsValue.equals("Custom") }
    private val silentRotationValue = BoolValue("SilentRotation", true).displayable { !rotationsValue.equals("None") }
    private val minRotationSpeedValue: IntegerValue = object : IntegerValue("MinRotationSpeed", 180, 0, 180) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = maxRotationSpeedValue.get()
            if (v < newValue) set(v)
        }
    }.displayable { !rotationsValue.equals("None") } as IntegerValue
    private val maxRotationSpeedValue: IntegerValue = object : IntegerValue("MaxRotationSpeed", 180, 0, 180) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = minRotationSpeedValue.get()
            if (v > newValue) set(v)
        }
    }.displayable { !rotationsValue.equals("None") } as IntegerValue
    private val keepLengthValue = IntegerValue("KeepRotationTick", 0, 0, 20).displayable { !rotationsValue.equals("None") }

    // Zitter
    private val zitterModeValue = ListValue("ZitterMode", arrayOf("Teleport", "Smooth", "OFF"), "OFF")
    private val zitterSpeedValue = FloatValue("ZitterSpeed", 0.13f, 0.1f, 0.3f).displayable { !zitterModeValue.equals("OFF") }
    private val zitterStrengthValue = FloatValue("ZitterStrength", 0.072f, 0.05f, 0.2f).displayable { !zitterModeValue.equals("OFF") }

    // Game
    private val bypassValue = BoolValue("C03Spoof" ,true)
    private val timerModeValue = ListValue("TimerMode", arrayOf("Static", "Dynamic"), "Static")
    private val timerValue = FloatValue("StaticTimer", 1f, 0.1f, 5f).displayable {timerModeValue.equals("Static")}
    private val timerChangeDelayValue = IntegerValue("TimerChangeDelay", 1000, 50, 10000).displayable {timerModeValue.equals("Dynamic")}
    private val firstTimerValue = FloatValue("FirstTimer", 1.0f, 0.1f, 5f).displayable {timerModeValue.equals("Dynamic")}
    private val lastTimerValue = FloatValue("LastTimer", 1.0f, 0.1f, 5f).displayable {timerModeValue.equals("Dynamic")}
    private val motionSpeedEnabledValue = BoolValue("MotionSpeedSet", false)
    private val motionSpeedValue = FloatValue("MotionSpeed", 0.1f, 0.05f, 1f).displayable { motionSpeedEnabledValue.get() }
    private val speedModifierValue = FloatValue("SpeedModifier", 1f, 0f, 2f)
    private val boostValue = BoolValue("Boost", false)

    // Tower
    private val towerModeValue = ListValue(
        "TowerMode", arrayOf(
            "Jump",
            "Motion",
            "ConstantMotion",
            "PlusMotion",
            "StableMotion",
            "MotionTP",
            "MotionTP2",
            "Packet",
            "Teleport",
            "AAC3.3.9",
            "AAC3.6.4",
            "AAC4.4Constant",
            "AAC4Jump",
            "Verus",
            "NCP",
            "Matrix",
            "Universo"
        ), "Jump"
    )
    private val stopWhenBlockAboveValue = BoolValue("StopTowerWhenBlockAbove", true)
    private val towerFakeJumpValue = BoolValue("TowerFakeJump", true)
    private val towerActiveValue = ListValue("TowerActivation", arrayOf("Always", "PressSpace", "NoMove", "OFF"), "PressSpace")
    private val towerTimerValue = FloatValue("TowerTimer", 1f, 0.1f, 5f)

    // Safety
    private val sameYValue = ListValue("SameY", arrayOf("Simple", "AutoJump", "WhenSpeed", "OFF"), "WhenSpeed")
    private val safeWalkValue = ListValue("SafeWalk", arrayOf("Ground", "Air", "OFF"), "OFF")
    private val hitableCheckValue = ListValue("HitableCheck", arrayOf("Simple", "Strict", "OFF"), "Simple")
    private val moveFixValue = BoolValue("StrafeFix", false)

    // Extra click
    private val extraClickValue = ListValue("ExtraClick", arrayOf("EmptyC08", "AfterPlace", "RayTrace", "OFF"), "OFF")
    private val extraClickCountValue = IntegerValue("ExtraClickCount", 1, 1, 30).displayable { !extraClickValue.equals("OFF") }
    private val extraClickMaxDelayValue: IntegerValue = object : IntegerValue("ExtraClickMaxDelay", 100, 20, 300) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = extraClickMinDelayValue.get()
            if (i > newValue) set(i)
        }
    }.displayable { !extraClickValue.equals("OFF") } as IntegerValue
    private val extraClickMinDelayValue: IntegerValue = object : IntegerValue("ExtraClickMinDelay", 50, 20, 300) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = extraClickMaxDelayValue.get()
            if (i < newValue) set(i)
        }
    }.displayable { !extraClickValue.equals("OFF") } as IntegerValue

    // Jump mode
    private val jumpMotionValue = FloatValue("TowerJumpMotion", 0.42f, 0.3681289f, 0.79f).displayable { towerModeValue.equals("Jump") }
    private val jumpDelayValue = IntegerValue("TowerJumpDelay", 0, 0, 20).displayable { towerModeValue.equals("Jump") }

    // Stable/PlusMotion
    private val stableMotionValue = FloatValue("TowerStableMotion", 0.42f, 0.1f, 1f).displayable { towerModeValue.equals("StableMotion") }
    private val plusMotionValue = FloatValue("TowerPlusMotion", 0.1f, 0.01f, 0.2f).displayable { towerModeValue.equals("PlusMotion") }
    private val plusMaxMotionValue = FloatValue("TowerPlusMaxMotion", 0.8f, 0.1f, 2f).displayable { towerModeValue.equals("PlusMotion") }

    // ConstantMotion
    private val constantMotionValue = FloatValue("TowerConstantMotion", 0.42f, 0.1f, 1f).displayable { towerModeValue.equals("ConstantMotion") }
    private val constantMotionJumpGroundValue = FloatValue("TowerConstantMotionJumpGround", 0.79f, 0.76f, 1f).displayable { towerModeValue.equals("ConstantMotion") }

    // Teleport
    private val teleportHeightValue = FloatValue("TowerTeleportHeight", 1.15f, 0.1f, 5f).displayable { towerModeValue.equals("Teleport") }
    private val teleportDelayValue = IntegerValue("TowerTeleportDelay", 0, 0, 20).displayable { towerModeValue.equals("Teleport") }
    private val teleportGroundValue = BoolValue("TowerTeleportGround", true).displayable { towerModeValue.equals("Teleport") }
    private val teleportNoMotionValue = BoolValue("TowerTeleportNoMotion", false).displayable { towerModeValue.equals("Teleport") }

    // Visuals
    private val counterDisplayValue = BoolValue("Counter", true)
    private val counterModeValue = ListValue("CounterMode", arrayOf("LBP", "Drama", "Sigma", "Novoline", "Simple", "Advanced","Rounded"), "lbp").displayable { counterDisplayValue.get() }
    private val blurValue = BoolValue("Advanced Blur", false).displayable { counterDisplayValue.get() && counterModeValue.equals("Advanced") }
    private val blurStrength = FloatValue("Blur Strength", 1f, 0f, 30f).displayable { counterDisplayValue.get() && counterModeValue.equals("Advanced") }
    private val redValue = IntegerValue("Red", 255, 0, 255).displayable { counterDisplayValue.get() && counterModeValue.equals("Drama") }
    private val greenValue = IntegerValue("Green", 40, 0, 255).displayable { counterDisplayValue.get() && counterModeValue.equals("Drama") }
    private val blueValue = IntegerValue("Blue", 255, 0, 255).displayable { counterDisplayValue.get() && counterModeValue.equals("Drama") }
    private val alpha = IntegerValue("Alpha", 255, 0, 255).displayable { counterDisplayValue.get() && counterModeValue.equals("Drama") }
    private val markValue = BoolValue("Mark", false)
    private val markRainbowValue = BoolValue("MarkRainbow", false).displayable { markValue.get() }

    private var progress = 0f
    private var lastMS = 0L

    /**
     * MODULE
     */
    // Target block
    private var targetPlace: PlaceInfo? = null

    // Last OnGround position
    private var lastGroundY = 0

    // Rotation lock
    private var lockRotation: Rotation? = null

    // Auto block slot
    private var slot = 0

    // Zitter Smooth
    private var zitterDirection = false

    // Delay
    private val delayTimer = MSTimer()
    private val zitterTimer = MSTimer()
    private val clickTimer = MSTimer()
    private val towerTimer = TickTimer()
    private var delay: Long = 0
    private var clickDelay: Long = 0
    private var lastPlace = 0

    // Eagle
    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking = false

    // Down
    private var shouldGoDown = false
    private var jumpGround = 0.0
    private var towerStatus = false
    private var canSameY = false
    private var lastPlaceBlock: BlockPos? = null
    private var afterPlaceC08: C08PacketPlayerBlockPlacement? = null

    //Other
    private var doSpoof = false
    val changetimer = MSTimer()

    /**
     * Enable module
     */
    override fun onEnable() {
        progress = 0f
        lastMS = System.currentTimeMillis()
        slot = mc.thePlayer.inventory.currentItem
        doSpoof = false
        if (mc.thePlayer == null) return
        lastGroundY = mc.thePlayer.posY.toInt()
        lastPlace = 2
        clickDelay = TimeUtils.randomDelay(extraClickMinDelayValue.get(), extraClickMaxDelayValue.get())

        if (mc.thePlayer == null) return;
        if (sprintValue.get().equals("Hypixel", true)) {
            mc.thePlayer.isSprinting = false;
            mc.thePlayer.motionX *= 1.19;
            mc.thePlayer.motionZ *= 1.19;
        }
    }

    /**
     * Update event
     *
     * @param event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (getBlocksAmount2() <= 0) {
            this.state = false
            LiquidBounce.hud.addNotification(Notification("BlockFly", "No item in inventory!", NotifyType.WARNING))
            return
        }
        if (towerStatus && towerModeValue.get().lowercase() != "aac3.3.9" && towerModeValue.get().lowercase() != "aac4.4constant" && towerModeValue.get().lowercase() != "aac4jump") mc.timer.timerSpeed = towerTimerValue.get()
        if (!towerStatus) {
            when (timerModeValue.get().lowercase()) {
                "static" -> mc.timer.timerSpeed = timerValue.get()
                "dynamic" -> {
                    mc.timer.timerSpeed = firstTimerValue.get()
                    if (changetimer.hasTimePassed(timerChangeDelayValue.get().toLong())) {
                        mc.timer.timerSpeed = lastTimerValue.get()
                        changetimer.reset()
                    }
                }
            }
        }
        if (sprintValue.get().equals("Hypixel", true)) {
            if (mc.thePlayer.onGround) ;
            mc.thePlayer.setSprinting(false);
            mc.thePlayer.motionX *= 1.19;
            mc.thePlayer.motionZ *= 1.19;
        }
        if (towerStatus || mc.thePlayer.isCollidedHorizontally) {
            canSameY = false
            lastGroundY = mc.thePlayer.posY.toInt()
        } else {
            when (sameYValue.get().lowercase()) {
                "simple" -> {
                    canSameY = true
                }
                "autojump" -> {
                    canSameY = true
                    if (MovementUtils.isMoving() && mc.thePlayer.onGround) {
                        mc.thePlayer.jump()
                    }
                }
                "whenspeed" -> {
                    canSameY = LiquidBounce.moduleManager[Speed::class.java]!!.state
                }
                else -> {
                    canSameY = false
                }
            }
            if (mc.thePlayer.onGround) {
                lastGroundY = mc.thePlayer.posY.toInt()
            }
        }

        if (clickTimer.hasTimePassed(clickDelay)) {
            fun sendPacket(c08: C08PacketPlayerBlockPlacement) {
                if (clickDelay <35) {
                    PacketUtils.sendPacketNoEvent(c08)
                }
                if (clickDelay <50) {
                    PacketUtils.sendPacketNoEvent(c08)
                }
                PacketUtils.sendPacketNoEvent(c08)
            }
            repeat(extraClickCountValue.get()) {
                when (extraClickValue.get().lowercase()) {
                    "emptyc08" -> sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(slot)))
                    "afterplace" -> {
                        if (afterPlaceC08 != null) {
                            if (mc.thePlayer.getDistanceSqToCenter(lastPlaceBlock) < 10) {
                                sendPacket(afterPlaceC08!!)
                            } else {
                                afterPlaceC08 = null
                            }
                        }
                    }
                    "raytrace" -> {
                        val rayTraceInfo = mc.thePlayer.rayTraceWithServerSideRotation(5.0)
                        if (BlockUtils.getBlock(rayTraceInfo!!.blockPos) != Blocks.air) {
                            val blockPos = rayTraceInfo!!.blockPos
                            val hitVec = rayTraceInfo!!.hitVec
                            val directionVec = rayTraceInfo!!.sideHit.directionVec
                            val targetPos = rayTraceInfo!!.blockPos.add(directionVec.x, directionVec.y, directionVec.z)
                            if (mc.thePlayer.entityBoundingBox.intersectsWith(
                                    Blocks.stone.getSelectedBoundingBox(
                                        mc.theWorld,
                                        targetPos
                                    )
                                )
                            ) {
                                sendPacket(
                                    C08PacketPlayerBlockPlacement(
                                        blockPos,
                                        rayTraceInfo!!.sideHit.index,
                                        mc.thePlayer.inventory.getStackInSlot(slot),
                                        (hitVec.xCoord - blockPos.x.toDouble()).toFloat(),
                                        (hitVec.yCoord - blockPos.y.toDouble()).toFloat(),
                                        (hitVec.zCoord - blockPos.z.toDouble()).toFloat()
                                    )
                                )
                            } else {
                                sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(slot)))
                            }
                        }
                    }
                }
            }
            clickDelay = TimeUtils.randomDelay(extraClickMinDelayValue.get(), extraClickMaxDelayValue.get())
            clickTimer.reset()
        }

        mc.thePlayer.isSprinting = canSprint


        shouldGoDown = downValue.get() && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && blocksAmount > 1
        if (shouldGoDown) mc.gameSettings.keyBindSneak.pressed = false
        if (mc.thePlayer.onGround) {
            // Smooth Zitter
            if (zitterModeValue.equals("smooth")) {
                if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) mc.gameSettings.keyBindRight.pressed = false
                if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) mc.gameSettings.keyBindLeft.pressed = false
                if (zitterTimer.hasTimePassed(100)) {
                    zitterDirection = !zitterDirection
                    zitterTimer.reset()
                }
                if (zitterDirection) {
                    mc.gameSettings.keyBindRight.pressed = true
                    mc.gameSettings.keyBindLeft.pressed = false
                } else {
                    mc.gameSettings.keyBindRight.pressed = false
                    mc.gameSettings.keyBindLeft.pressed = true
                }
            }

            // Eagle
            if (!eagleValue.equals("off") && !shouldGoDown) {
                if (placedBlocksWithoutEagle >= blocksToEagleValue.get()) {
                    val shouldEagle = mc.theWorld.getBlockState(
                        BlockPos(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY - 1.0, mc.thePlayer.posZ
                        )
                    ).block === Blocks.air
                    if (eagleValue.equals("silent")) {
                        if (eagleSneaking != shouldEagle) {
                            mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, if (shouldEagle) C0BPacketEntityAction.Action.START_SNEAKING else C0BPacketEntityAction.Action.STOP_SNEAKING))
                        }
                        eagleSneaking = shouldEagle
                    } else mc.gameSettings.keyBindSneak.pressed = shouldEagle
                    placedBlocksWithoutEagle = 0
                } else placedBlocksWithoutEagle++
            }

            // Zitter
            if (zitterModeValue.equals("teleport")) {
                MovementUtils.strafe(zitterSpeedValue.get())
                val yaw = Math.toRadians(mc.thePlayer.rotationYaw + if (zitterDirection) 90.0 else -90.0)
                mc.thePlayer.motionX -= sin(yaw) * zitterStrengthValue.get()
                mc.thePlayer.motionZ += cos(yaw) * zitterStrengthValue.get()
                zitterDirection = !zitterDirection
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null) return
        val packet = event.packet

        //Verus
        if (packet is C03PacketPlayer && bypassValue.get()) {
            if (doSpoof) {
                packet.onGround = true
            }
        }

        // AutoBlock
        if (packet is C09PacketHeldItemChange) {
            if(packet.slotId == slot) {
                event.cancelEvent()
            } else {
                slot = packet.slotId
            }
        } else if (packet is C08PacketPlayerBlockPlacement) {
            // c08 item override to solve issues in scaffold and some other modules, maybe bypass some anticheat in future
            packet.stack = mc.thePlayer.inventory.mainInventory[slot]
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val eventState = event.eventState
        towerStatus = false
        if (boostValue.get()) MovementUtils.setMotion(0.172)
        // Tower
        if (motionSpeedEnabledValue.get()) MovementUtils.setMotion(motionSpeedValue.get().toDouble())
        towerStatus = (!stopWhenBlockAboveValue.get() || BlockUtils.getBlock(BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)) is BlockAir)
        if (towerStatus) {
            // further checks
            when (towerActiveValue.get().lowercase()) {
                "off" -> towerStatus = false
                "always" -> {
                    towerStatus = (mc.gameSettings.keyBindLeft.isKeyDown ||
                            mc.gameSettings.keyBindRight.isKeyDown || mc.gameSettings.keyBindForward.isKeyDown ||
                            mc.gameSettings.keyBindBack.isKeyDown)
                }
                "pressspace" -> {
                    towerStatus = mc.gameSettings.keyBindJump.isKeyDown
                }
                "nomove" -> {
                    towerStatus = !(mc.gameSettings.keyBindLeft.isKeyDown ||
                            mc.gameSettings.keyBindRight.isKeyDown || mc.gameSettings.keyBindForward.isKeyDown ||
                            mc.gameSettings.keyBindBack.isKeyDown) && mc.gameSettings.keyBindJump.isKeyDown
                }
            }
        }
        if (towerStatus) move()

        // Lock Rotation
        if (rotationsValue.get() != "None" && keepLengthValue.get()> 0 && lockRotation != null && silentRotationValue.get()) {
            val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation, rotationSpeed)
            RotationUtils.setTargetRotation(limitedRotation, keepLengthValue.get())
        }

        // Update and search for new block
        if (event.eventState == EventState.PRE) update()

        // Place block
        if (!towerStatus)
        {
            when (placeModeValue.get()) {
                "Pre" -> if (event.eventState == EventState.PRE) place()
                "Post" -> if (event.eventState == EventState.POST) place()
                "All" -> place()
            }
        }

        if (towerStatus) {
            when (towerPlaceModeValue.get()) {
                "Pre" -> if (event.eventState == EventState.PRE) place()
                "Post" -> if (event.eventState == EventState.POST) place()
                "All" -> place()
            }
        }

        // Reset placeable delay
        if (targetPlace == null && !placeableDelayValue.equals("OFF") && (!placeDelayTower.get() || !towerStatus)) {
            if (placeableDelayValue.equals("Smart")) {
                if (lastPlace == 0) {
                    delayTimer.reset()
                }
            } else {
                delayTimer.reset()
            }
        }

        LiquidBounce.moduleManager[StrafeFix::class.java]!!.applyForceStrafe(!rotationsValue.equals("None"), moveFixValue.get())
    }

    private fun fakeJump() {
        if (!towerFakeJumpValue.get()) {
            return
        }

        mc.thePlayer.isAirBorne = true
        mc.thePlayer.triggerAchievement(StatList.jumpStat)
    }

    fun getRandomHypixelValues(): Double {
        val secureRandom = SecureRandom()
        var value = secureRandom.nextDouble() * (1.0 / System.currentTimeMillis())
        for (i in 0 until RandomUtils.nextDouble(
            RandomUtils.nextDouble(4.0, 6.0),
            RandomUtils.nextDouble(8.0, 20.0)
        ).toInt()) value *= 1.0 / System.currentTimeMillis()
        return value
    }

    private fun move() {
        when (towerModeValue.get().lowercase()) {
            "none" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.42
                }
            }
            "vanilla" -> {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.motionY = 0.42
                }
            }
            "ncp"-> {
                if (mc.thePlayer.posY % 1 <= 0.00153598) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ)
                    mc.thePlayer.motionY = 0.41998f - getRandomHypixelValues() / 2
                } else if (mc.thePlayer.posY % 1 < 0.1 && !MovementUtils.isOnGround(0.0)) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ)
                }
            }
            "jump" -> {
                if (mc.thePlayer.onGround && towerTimer.hasTimePassed(jumpDelayValue.get())) {
                    fakeJump()
                    mc.thePlayer.motionY = jumpMotionValue.get().toDouble()
                    towerTimer.reset()
                }
            }
            "motion" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.42
                } else if (mc.thePlayer.motionY < 0.1) {
                    mc.thePlayer.motionY = -0.3
                }
            }
            "motiontp" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.42
                } else if (mc.thePlayer.motionY < 0.23) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, truncate(mc.thePlayer.posY), mc.thePlayer.posZ)
                }
            }
            "motiontp2" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.41999998688698
                } else if (mc.thePlayer.motionY < 0.23) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, truncate(mc.thePlayer.posY), mc.thePlayer.posZ)
                    mc.thePlayer.onGround = true
                    mc.thePlayer.motionY = 0.41999998688698
                }
            }
            "packet" -> {
                if (mc.thePlayer.onGround && towerTimer.hasTimePassed(2)) {
                    fakeJump()
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.42, mc.thePlayer.posZ, false))
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.753, mc.thePlayer.posZ, false))
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 1.0, mc.thePlayer.posZ)
                    towerTimer.reset()
                }
            }
            "teleport" -> {
                if (teleportNoMotionValue.get()) mc.thePlayer.motionY = 0.0
                if ((mc.thePlayer.onGround || !teleportGroundValue.get()) && towerTimer.hasTimePassed(teleportDelayValue.get())) {
                    fakeJump()
                    mc.thePlayer.setPositionAndUpdate(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY + teleportHeightValue.get(),
                        mc.thePlayer.posZ
                    )
                    towerTimer.reset()
                }
            }
            "watchdog" -> {
                fakeJump()
                if (!mc.gameSettings.keyBindJump.isKeyDown) return

                mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                mc.thePlayer.motionY = 0.42
            }
            "constantmotion" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    jumpGround = mc.thePlayer.posY
                    mc.thePlayer.motionY = constantMotionValue.get().toDouble()
                }
                if (mc.thePlayer.posY > jumpGround + constantMotionJumpGroundValue.get()) {
                    fakeJump()
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
                    mc.thePlayer.motionY = constantMotionValue.get().toDouble()
                    jumpGround = mc.thePlayer.posY
                }
            }
            "plusmotion" -> {
                mc.thePlayer.motionY += plusMotionValue.get()
                if (mc.thePlayer.motionY >= plusMaxMotionValue.get()) {
                    mc.thePlayer.motionY = plusMaxMotionValue.get().toDouble()
                }
                fakeJump()
            }
            "stablemotion" -> {
                mc.thePlayer.motionY = stableMotionValue.get().toDouble()
                fakeJump()
            }
            "aac3.3.9" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.4001
                }
                mc.timer.timerSpeed = 1f
                if (mc.thePlayer.motionY < 0) {
                    mc.thePlayer.motionY -= 0.00000945
                    mc.timer.timerSpeed = 1.6f
                }
            }
            "aac3.6.4" -> {
                if (mc.thePlayer.ticksExisted % 4 == 1) {
                    mc.thePlayer.motionY = 0.4195464
                    mc.thePlayer.setPosition(mc.thePlayer.posX - 0.035, mc.thePlayer.posY, mc.thePlayer.posZ)
                } else if (mc.thePlayer.ticksExisted % 4 == 0) {
                    mc.thePlayer.motionY = -0.5
                    mc.thePlayer.setPosition(mc.thePlayer.posX + 0.035, mc.thePlayer.posY, mc.thePlayer.posZ)
                }
            }
            "aac4.4constant" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    jumpGround = mc.thePlayer.posY
                    mc.thePlayer.motionY = 0.42
                }
                mc.thePlayer.motionX = 0.0
                mc.thePlayer.motionZ = -0.00000001
                mc.thePlayer.jumpMovementFactor = 0.000F
                mc.timer.timerSpeed = 0.60f
                if (mc.thePlayer.posY > jumpGround + 0.99) {
                    fakeJump()
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 0.001335979112146, mc.thePlayer.posZ)
                    mc.thePlayer.motionY = 0.42
                    jumpGround = mc.thePlayer.posY
                    mc.timer.timerSpeed = 0.75f
                }
            }
            "verus" -> {
                mc.thePlayer.setPosition(mc.thePlayer.posX, (mc.thePlayer.posY * 2).roundToInt().toDouble() / 2, mc.thePlayer.posZ)
                if (mc.thePlayer.ticksExisted % 2 == 0) {
                    mc.thePlayer.motionY = 0.5
                    mc.timer.timerSpeed = 0.8f
                    doSpoof = false
                }else{
                    mc.timer.timerSpeed = 1.33f
                    mc.thePlayer.motionY = 0.0
                    mc.thePlayer.onGround = true
                    doSpoof = true
                }
            }
            "aac4jump" -> {
                mc.timer.timerSpeed = 0.97f
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.387565
                    mc.timer.timerSpeed = 1.05f
                }
            }
            "universo" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.41999998688698
                } else if (mc.thePlayer.motionY < 0.19) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, truncate(mc.thePlayer.posY), mc.thePlayer.posZ)
                    mc.thePlayer.onGround = true
                    mc.thePlayer.motionY = 0.41999998688698
                }
            }
            "matrix" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.42
                } else if (mc.thePlayer.motionY < 0.19145141919180) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, truncate(mc.thePlayer.posY), mc.thePlayer.posZ)
                    mc.thePlayer.onGround = true
                    mc.thePlayer.motionY = 0.481145141919180
                }
            }
        }
    }

    private fun update() {
        if (if (!autoBlockValue.equals("off")) InventoryUtils.findAutoBlockBlock() == -1 else mc.thePlayer.heldItem == null ||
                    !(mc.thePlayer.heldItem.item is ItemBlock && !InventoryUtils.isBlockListBlock(mc.thePlayer.heldItem.item as ItemBlock))) {
            return
        }

        findBlock(expandLengthValue.get()> 1)
    }

    /**
     * Search for new target block
     */
    private fun findBlock(expand: Boolean) {
        val blockPosition = if (shouldGoDown) {
            if (mc.thePlayer.posY == mc.thePlayer.posY.toInt() + 0.5) {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.6, mc.thePlayer.posZ)
            } else {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.6, mc.thePlayer.posZ).down()
            }
        } else if (mc.thePlayer.posY == mc.thePlayer.posY.toInt() + 0.5 && !canSameY) {
            BlockPos(mc.thePlayer)
        } else if (canSameY && lastGroundY <= mc.thePlayer.posY) {
            BlockPos(mc.thePlayer.posX, lastGroundY - 1.0, mc.thePlayer.posZ)
        } else {
            BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()
        }
        if (!expand && (!BlockUtils.isReplaceable(blockPosition) || search(blockPosition, !shouldGoDown))) return
        if (expand) {
            for (i in 0 until expandLengthValue.get()) {
                if (search(blockPosition.add(if (mc.thePlayer.horizontalFacing == EnumFacing.WEST) -i else if (mc.thePlayer.horizontalFacing == EnumFacing.EAST) i else 0,
                        0, if (mc.thePlayer.horizontalFacing == EnumFacing.NORTH) -i else if (mc.thePlayer.horizontalFacing == EnumFacing.SOUTH) i else 0), false)) {
                    return
                }
            }
        } else if (searchValue.get()) {
            for (x in -1..1) {
                for (z in -1..1) {
                    if (search(blockPosition.add(x, 0, z), !shouldGoDown)) {
                        return
                    }
                }
            }
        }
    }

    /**
     * Place target block
     */
    private fun place() {
        if (targetPlace == null) {
            if (!placeableDelayValue.equals("OFF")) {
                if (lastPlace == 0 && placeableDelayValue.equals("Smart")) delayTimer.reset()
                if (placeableDelayValue.equals("Normal")) delayTimer.reset()
                if (lastPlace> 0) lastPlace--
            }
            return
        }
        if (!delayTimer.hasTimePassed(delay) || !towerStatus && canSameY && lastGroundY - 1 != targetPlace!!.vec3.yCoord.toInt()) {
            return
        }

        if (!rotationsValue.equals("None")) {
            val rayTraceInfo = mc.thePlayer.rayTraceWithServerSideRotation(5.0)
            when (hitableCheckValue.get().lowercase()) {
                "simple" -> {
                    if (!rayTraceInfo!!.blockPos.equals(targetPlace!!.blockPos)) {
                        return
                    }
                }
                "strict" -> {
                    if (!rayTraceInfo!!.blockPos.equals(targetPlace!!.blockPos) || rayTraceInfo!!.sideHit != targetPlace!!.enumFacing) {
                        return
                    }
                }
            }
        }

        val isDynamicSprint = sprintValue.equals("dynamic")
        var blockSlot = -1
        var itemStack = mc.thePlayer.heldItem
        if (mc.thePlayer.heldItem == null || !(mc.thePlayer.heldItem.item is ItemBlock && !InventoryUtils.isBlockListBlock(mc.thePlayer.heldItem.item as ItemBlock))) {
            if (autoBlockValue.equals("off")) return
            blockSlot = InventoryUtils.findAutoBlockBlock()
            if (blockSlot == -1) return
            if (autoBlockValue.equals("LiteSpoof") || autoBlockValue.equals("Spoof")) {
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(blockSlot - 36))
            } else {
                mc.thePlayer.inventory.currentItem = blockSlot - 36
            }
            itemStack = mc.thePlayer.inventoryContainer.getSlot(blockSlot).stack
        }
        if (isDynamicSprint) {
            mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
        }
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, itemStack, targetPlace!!.blockPos, targetPlace!!.enumFacing, targetPlace!!.vec3)) {
            // delayTimer.reset()
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
            if (mc.thePlayer.onGround) {
                val modifier = speedModifierValue.get()
                mc.thePlayer.motionX *= modifier.toDouble()
                mc.thePlayer.motionZ *= modifier.toDouble()
            }

            if (swingValue.equals("packet")) {
                mc.netHandler.addToSendQueue(C0APacketAnimation())
            } else if (swingValue.equals("normal")) {
                mc.thePlayer.swingItem()
            }
            lastPlace = 2
            lastPlaceBlock = targetPlace!!.blockPos.add(targetPlace!!.enumFacing.directionVec)
            when (extraClickValue.get().lowercase()) {
                "afterplace" -> {
                    // fake click
                    val blockPos = targetPlace!!.blockPos
                    val hitVec = targetPlace!!.vec3
                    afterPlaceC08 = C08PacketPlayerBlockPlacement(targetPlace!!.blockPos, targetPlace!!.enumFacing.index, itemStack, (hitVec.xCoord - blockPos.x.toDouble()).toFloat(), (hitVec.yCoord - blockPos.y.toDouble()).toFloat(), (hitVec.zCoord - blockPos.z.toDouble()).toFloat())
                }
            }
        }
        if (isDynamicSprint) {
            mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
        }

        if (autoBlockValue.equals("LiteSpoof") && blockSlot >= 0) {
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
        }

        // Reset
        targetPlace = null
    }

    /**
     * Disable scaffold module
     */
    override fun onDisable() {
        // tolleyStayTick=999
        if (mc.thePlayer == null) return
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
            if (eagleSneaking) mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING))
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) mc.gameSettings.keyBindRight.pressed = false
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) mc.gameSettings.keyBindLeft.pressed = false
        lockRotation = null
        mc.timer.timerSpeed = 1f
        shouldGoDown = false
        RotationUtils.reset()
        if (slot != mc.thePlayer.inventory.currentItem) mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
    }

    /**
     * Entity movement event
     *
     * @param event
     */
    @EventTarget
    fun onMove(event: MoveEvent) {
        if (safeWalkValue.equals("off") || shouldGoDown) return
        if (safeWalkValue.equals("air") || mc.thePlayer.onGround) event.isSafeWalk = true
    }

    /**
     * BlockFly visuals
     *
     * @param event
     */
    @JvmName("getBlocksAmount2")
    private fun getBlocksAmount2(): Int {
        var amount = 0
        for (i in 36..44) {
            val itemStack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (itemStack != null && itemStack.item is ItemBlock) {
                val block = (itemStack.item as ItemBlock).getBlock()
                if (!InventoryUtils.BLOCK_BLACKLIST.contains(block) && block.isFullCube) amount += itemStack.stackSize
            }
        }
        return amount
    }

    private val barrier = ItemStack(Item.getItemById(166), 0, 0)
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        progress = (System.currentTimeMillis() - lastMS).toFloat() / 100f
        if (progress >= 1) progress = 1f
        val scaledResolution = ScaledResolution(mc)
        val info = getBlocksAmount2().toString() + " blocks"
        val info2 = getBlocksAmount2().toString()
        val info3 = getBlocksAmount2().toString() + "Blocks Left"
        val infoWidth = Fonts.gs40.getStringWidth(info)
        val info3Width = Fonts.gs40.getStringWidth(info3)
        val height = event.scaledResolution.scaledHeight
        val width = event.scaledResolution.scaledWidth
        val slot = InventoryUtils.findAutoBlockBlock()
        var stack = barrier
        val w2=(mc.fontRendererObj.getStringWidth(info))
        if (counterDisplayValue.get()) {
            if (slot != -1) {
                if (mc.thePlayer.inventory.getCurrentItem() != null) {
                    val handItem = mc.thePlayer.inventory.getCurrentItem().item
                    if (handItem is ItemBlock && InventoryUtils.canPlaceBlock(handItem.block)) {
                        stack = mc.thePlayer.inventory.getCurrentItem()
                    }
                }
                if (stack == barrier) {
                    stack = mc.thePlayer.inventory.getStackInSlot(InventoryUtils.findAutoBlockBlock() - 36)
                    if (stack == null) {
                        stack = barrier
                    }
                }
            }
            when (counterModeValue.get().lowercase()) {
                "fdp" -> {
                    RenderUtils.drawRoundedCornerRect(
                        (width - w2 - 20) / 2f,
                        height * 0.8f - 24f,
                        (width + w2 + 18) / 2f,
                        height * 0.8f + 12f,
                        3f,
                        Color(43, 45, 48).rgb
                    )
                    Fonts.font40.drawCenteredString("▼",width / 2.0f + 2f, height * 0.8f+8f,Color(43,45,48).rgb)
                    RenderHelper.enableGUIStandardItemLighting()
                    mc.renderItem.renderItemIntoGUI(stack, width / 2 - 10, (height * 0.8 - 20).toInt())
                    RenderHelper.disableStandardItemLighting()
                    Fonts.font40.drawCenteredString(info, width / 2f, height * 0.8f, Color.WHITE.rgb, false)
                    GlStateManager.popMatrix()
                }
                "lbp" -> {
                    GlStateManager.translate(0f, -14f - progress * 4f, 0f)
                    //GL11.glPushMatrix();
                    GL11.glEnable(GL11.GL_BLEND)
                    GL11.glDisable(GL11.GL_TEXTURE_2D)
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                    GL11.glEnable(GL11.GL_LINE_SMOOTH)
                    GL11.glColor4f(0.25f, 0.25f, 0.25f, progress)
                    GL11.glBegin(GL11.GL_TRIANGLE_FAN)
                    GL11.glVertex2d(
                        (scaledResolution.scaledWidth / 2 - 3).toDouble(),
                        (scaledResolution.scaledHeight - 60).toDouble()
                    )
                    GL11.glVertex2d(
                        (scaledResolution.scaledWidth / 2).toDouble(),
                        (scaledResolution.scaledHeight - 57).toDouble()
                    )
                    GL11.glVertex2d(
                        (scaledResolution.scaledWidth / 2 + 3).toDouble(),
                        (scaledResolution.scaledHeight - 60).toDouble()
                    )
                    GL11.glEnd()
                    GL11.glEnable(GL11.GL_TEXTURE_2D)
                    GL11.glDisable(GL11.GL_BLEND)
                    GL11.glDisable(GL11.GL_LINE_SMOOTH)
                    //GL11.glPopMatrix();
                    RenderUtils.drawRoundedCornerRect(
                        (scaledResolution.scaledWidth / 2 - infoWidth / 2 - 4).toFloat(),
                        (scaledResolution.scaledHeight - 60).toFloat(),
                        (scaledResolution.scaledWidth / 2 + infoWidth / 2 + 4).toFloat(),
                        (scaledResolution.scaledHeight - 74).toFloat(),
                        2.5f,
                        Color(0.25f, 0.25f, 0.25f, progress).rgb
                    )
                    GlStateManager.resetColor()
                    Fonts.font35.drawCenteredString(
                        info, scaledResolution.scaledWidth / 2 + 0.1f,
                        (scaledResolution.scaledHeight - 70).toFloat(), Color(1f, 1f, 1f, 0.8f * progress).rgb, false
                    )
                    GlStateManager.translate(0f, 14f + progress * 4f, 0f)
                }
                "drama" -> {
                    RenderUtils.drawCircle(width /2f,height * 0.80f,18f,Color(40,40,40,170).rgb)
                    Renderer.drawCircleBorder(width /2f,height *0.80f,18f,Color(redValue.get(),greenValue.get(),blueValue.get(),alpha.get()).rgb, 4f)
                    Renderer.drawCircleBorder(width /2f,height *0.80f,21f,Color(redValue.get(),greenValue.get(),blueValue.get(),alpha.get()/2).rgb, 12f)
                    Renderer.enableGUIStandardItemLighting()
                    renderItemStack(stack, 0, 0)
                    Renderer.disableStandardItemLighting()
                    Fonts.font40.drawCenteredString(info2, width / 2.0f+3/2, height * 0.80f+5, Color(255,255,255,180).rgb, false)
                }
                "Rounded" -> {
                    RenderUtils.drawRoundedCornerRect(
                        (scaledResolution.scaledWidth / 2 - info3Width / 2 - 10).toFloat(),
                        (scaledResolution.scaledHeight - 57).toFloat(),
                        (scaledResolution.scaledWidth / 2 + info3Width / 2 + 10).toFloat(),
                        (scaledResolution.scaledHeight - 77).toFloat(),
                        2.5f,
                        Color(0.25f, 0.25f, 0.25f, progress).rgb)
                    renderItemStack(stack, (scaledResolution.scaledWidth / 2 + 0.1f).toInt(), scaledResolution.scaledHeight - 64)
                    Fonts.font35.drawCenteredString(
                        info3, scaledResolution.scaledWidth / 2 + 0.1f,
                        (scaledResolution.scaledHeight - 70).toFloat(), Color(1f, 1f, 1f, 0.8f * progress).rgb, false
                    )
                }
                "simple" -> {
                    Fonts.minecraftFont.drawString(getBlocksAmount2().toString() + "", (scaledResolution.scaledWidth / 2 - infoWidth / 2 - 1).toFloat(), (scaledResolution.scaledHeight / 2 - 36).toFloat(), -0x1000000, false)
                    Fonts.minecraftFont.drawString(getBlocksAmount2().toString() + "", (scaledResolution.scaledWidth / 2 - infoWidth / 2 + 1).toFloat(), (scaledResolution.scaledHeight / 2 - 36).toFloat(), -0x1000000, false)
                    Fonts.minecraftFont.drawString(getBlocksAmount2().toString() + "", (scaledResolution.scaledWidth / 2 - infoWidth / 2).toFloat(), (scaledResolution.scaledHeight / 2 - 35).toFloat(), -0x1000000, false)
                    Fonts.minecraftFont.drawString(getBlocksAmount2().toString() + "", (scaledResolution.scaledWidth / 2 - infoWidth / 2).toFloat(), (scaledResolution.scaledHeight / 2 - 37).toFloat(), -0x1000000, false)
                    Fonts.minecraftFont.drawString(getBlocksAmount2().toString() + "", (scaledResolution.scaledWidth / 2 - infoWidth / 2).toFloat(), (scaledResolution.scaledHeight / 2 - 36).toFloat(), -1, false)
                }
                "advanced" -> {
                    val canRenderStack =
                        slot >= 0 && slot < 9 && mc.thePlayer.inventory.mainInventory[slot] != null && mc.thePlayer.inventory.mainInventory[slot].item != null && mc.thePlayer.inventory.mainInventory[slot].item is ItemBlock
                    if (blurValue.get()) BlurUtilsOld.blurArea(
                        scaledResolution.scaledWidth / 2 - infoWidth / 2 - 4f,
                        scaledResolution.scaledHeight / 2 - 39f,
                        scaledResolution.scaledWidth / 2 + infoWidth / 2 + 4f,
                        scaledResolution.scaledHeight / 2 - if (canRenderStack) 5f else 26f,
                        blurStrength.get()
                    )

                    RenderUtils.drawRect(
                        (scaledResolution.scaledWidth / 2 - infoWidth / 2 - 4).toFloat(),
                        (scaledResolution.scaledHeight / 2 - 40).toFloat(),
                        (scaledResolution.scaledWidth / 2 + infoWidth / 2 + 4).toFloat(),
                        (scaledResolution.scaledHeight / 2 - 39).toFloat(),
                        if (getBlocksAmount2() > 1) -0x1 else -0xeff0
                    )
                    RenderUtils.drawRect(
                        (scaledResolution.scaledWidth / 2 - infoWidth / 2 - 4).toFloat(),
                        (scaledResolution.scaledHeight / 2 - 39).toFloat(),
                        (scaledResolution.scaledWidth / 2 + infoWidth / 2 + 4).toFloat(),
                        (scaledResolution.scaledHeight / 2 - 26).toFloat(),
                        -0x60000000
                    )

                    if (canRenderStack) {
                        RenderUtils.drawRect(
                            (scaledResolution.scaledWidth / 2 - infoWidth / 2 - 4).toFloat(),
                            (scaledResolution.scaledHeight / 2 - 26).toFloat(),
                            (scaledResolution.scaledWidth / 2 + infoWidth / 2 + 4).toFloat(),
                            (scaledResolution.scaledHeight / 2 - 5).toFloat(),
                            -0x60000000
                        )
                        GlStateManager.pushMatrix()
                        GlStateManager.translate(
                            (scaledResolution.scaledWidth / 2 - 8).toFloat(),
                            (scaledResolution.scaledHeight / 2 - 25).toFloat(),
                            (scaledResolution.scaledWidth / 2 - 8).toFloat()
                        )
                        renderItemStack(mc.thePlayer.inventory.mainInventory[slot], 0, 0)
                        GlStateManager.popMatrix()
                    }
                    GlStateManager.resetColor()

                    Fonts.gs40.drawCenteredString(
                        info,
                        (scaledResolution.scaledWidth / 2).toFloat(),
                        scaledResolution.scaledHeight / 2 - 36f,
                        -1)
                }
                "sigma" -> {
                    GlStateManager.translate(0f, -14f - progress * 4f, 0f)
                    //GL11.glPushMatrix();
                    //GL11.glPushMatrix();
                    GL11.glEnable(GL11.GL_BLEND)
                    GL11.glDisable(GL11.GL_TEXTURE_2D)
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                    GL11.glEnable(GL11.GL_LINE_SMOOTH)
                    GL11.glColor4f(0.15f, 0.15f, 0.15f, progress)
                    GL11.glBegin(GL11.GL_TRIANGLE_FAN)
                    GL11.glVertex2d(
                        (scaledResolution.scaledWidth / 2 - 3).toDouble(),
                        (scaledResolution.scaledHeight - 60).toDouble()
                    )
                    GL11.glVertex2d(
                        (scaledResolution.scaledWidth / 2).toDouble(),
                        (scaledResolution.scaledHeight - 57).toDouble()
                    )
                    GL11.glVertex2d(
                        (scaledResolution.scaledWidth / 2 + 3).toDouble(),
                        (scaledResolution.scaledHeight - 60).toDouble()
                    )
                    GL11.glEnd()
                    GL11.glEnable(GL11.GL_TEXTURE_2D)
                    GL11.glDisable(GL11.GL_BLEND)
                    GL11.glDisable(GL11.GL_LINE_SMOOTH)
                    //GL11.glPopMatrix();
                    //GL11.glPopMatrix();
                    RenderUtils.drawRoundedCornerRect(
                        scaledResolution.scaledWidth / 2 - infoWidth / 2 - 4f,
                        scaledResolution.scaledHeight - 60f,
                        scaledResolution.scaledWidth / 2 + infoWidth / 2 + 4f,
                        scaledResolution.scaledHeight - 74f,
                        2f,
                        Color(0.15f, 0.15f, 0.15f, progress).rgb
                    )
                    GlStateManager.resetColor()
                    Fonts.gs35.drawCenteredString(
                        info,
                        scaledResolution.scaledWidth / 2 + 0.1f,
                        scaledResolution.scaledHeight - 70f,
                        Color(1f, 1f, 1f, 0.8f * progress).rgb,
                        false
                    )
                    GlStateManager.translate(0f, 14f + progress * 4f, 0f)
                }
                "novoline" -> {
                    if (slot in 0..8 && mc.thePlayer.inventory.mainInventory[slot] != null && mc.thePlayer.inventory.mainInventory[slot].item != null && mc.thePlayer.inventory.mainInventory[slot].item is ItemBlock) {
                        //RenderUtils.drawRect(scaledResolution.getScaledWidth() / 2 - (infoWidth / 2) - 4, scaledResolution.getScaledHeight() / 2 - 26, scaledResolution.getScaledWidth() / 2 + (infoWidth / 2) + 4, scaledResolution.getScaledHeight() / 2 - 5, 0xA0000000);
                        GlStateManager.pushMatrix()
                        GlStateManager.translate(
                            (scaledResolution.scaledWidth / 2 - 22).toFloat(),
                            (scaledResolution.scaledHeight / 2 + 16).toFloat(),
                            (scaledResolution.scaledWidth / 2 - 22).toFloat()
                        )
                        renderItemStack(mc.thePlayer.inventory.mainInventory[slot], 0, 0)
                        GlStateManager.popMatrix()
                    }
                    GlStateManager.resetColor()

                    Fonts.minecraftFont.drawString(
                        getBlocksAmount2().toString() + " blocks",
                        (scaledResolution.scaledWidth / 2).toFloat(),
                        (scaledResolution.scaledHeight / 2 + 20).toFloat(),
                        -1,
                        true
                    )
                }
            }
        }
    }

    private fun renderItemStack(stack: ItemStack, x: Int, y: Int) {
        GlStateManager.pushMatrix()
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        RenderHelper.enableGUIStandardItemLighting()
        mc.renderItem.renderItemAndEffectIntoGUI(stack, x, y)
        mc.renderItem.renderItemOverlays(mc.fontRendererObj, stack, x, y)
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    /**
     * BlockFly visuals
     *
     * @param event
     */
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (markValue.get()) {
            for (i in 0 until (expandLengthValue.get() + 1)) {
                val blockPos = BlockPos(mc.thePlayer.posX + if (mc.thePlayer.horizontalFacing == EnumFacing.WEST) -i else if (mc.thePlayer.horizontalFacing == EnumFacing.EAST) i else 0, mc.thePlayer.posY - (if (mc.thePlayer.posY == mc.thePlayer.posY.toInt() + 0.5) { 0.0 } else { 1.0 }) - (if (shouldGoDown) { 1.0 } else { 0.0 }), mc.thePlayer.posZ + if (mc.thePlayer.horizontalFacing == EnumFacing.NORTH) -i else if (mc.thePlayer.horizontalFacing == EnumFacing.SOUTH) i else 0)
                val placeInfo = get(blockPos)
                if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                    val rainbow = markRainbowValue.get()
                    RenderUtils.drawBlockBox(blockPos, if (rainbow) net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow() else Color.BLUE, true, true, 0.8f)
                    break
                }
            }
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param checks        visible
     * @return
     */
    private fun search(blockPosition: BlockPos, checks: Boolean): Boolean {
        if (!BlockUtils.isReplaceable(blockPosition)) return false
        val eyesPos = Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
            mc.thePlayer.posZ
        )
        var placeRotation: PlaceRotation? = null
        for (side in StaticStorage.facings()) {
            val neighbor = blockPosition.offset(side)
            if (!BlockUtils.canBeClicked(neighbor)) continue
            val dirVec = Vec3(side.directionVec)
            var xSearch = 0.1
            while (xSearch < 0.9) {
                var ySearch = 0.1
                while (ySearch < 0.9) {
                    var zSearch = 0.1
                    while (zSearch < 0.9) {
                        val posVec = Vec3(blockPosition).addVector(xSearch, ySearch, zSearch)
                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec.add(Vec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))
                        if (checks && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(
                                posVec.add(dirVec)
                            ) || mc.theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null)
                        ) {
                            zSearch += 0.1
                            continue
                        }

                        // face block
                        val diffX = hitVec.xCoord - eyesPos.xCoord
                        val diffY = hitVec.yCoord - eyesPos.yCoord
                        val diffZ = hitVec.zCoord - eyesPos.zCoord
                        val diffXZ = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ).toDouble()
                        val rotation = Rotation(
                            MathHelper.wrapAngleTo180_float(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
                            MathHelper.wrapAngleTo180_float((-Math.toDegrees(atan2(diffY, diffXZ))).toFloat())
                        )
                        val rotationVector = RotationUtils.getVectorForRotation(rotation)
                        val vector = eyesPos.addVector(
                            rotationVector.xCoord * 4,
                            rotationVector.yCoord * 4,
                            rotationVector.zCoord * 4
                        )
                        val obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true)
                        if (!(obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && obj.blockPos == neighbor)) {
                            zSearch += 0.1
                            continue
                        }
                        if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(
                                placeRotation.rotation
                            )
                        ) placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)
                        zSearch += 0.1
                    }
                    ySearch += 0.1
                }
                xSearch += 0.1
            }
        }
        if (placeRotation == null) return false
        if (!towerrotationsValue.equals("None") && towerStatus) {
            lockRotation = when (towerrotationsValue.get().lowercase()) {
                "aac" -> {
                    Rotation(mc.thePlayer.rotationYaw + (if (mc.thePlayer.movementInput.moveForward < 0) 0 else 180) + aacYawValue.get(), placeRotation.rotation.pitch)
                }
                "vanilla" -> {
                    placeRotation.rotation
                }
                "test1" -> {
                    val caluyaw = ((placeRotation.rotation.yaw / 45).roundToInt() * 45).toFloat()
                    Rotation(caluyaw, placeRotation.rotation.pitch)
                }
                "ncp" -> {
                    val arrayOfFloat: FloatArray = getRotations(
                        placeRotation.placeInfo.blockPos,
                        placeRotation.placeInfo.enumFacing
                    )
                    val yaw = arrayOfFloat[0]
                    val pitch = arrayOfFloat[1]
                    Rotation(yaw, pitch)
                }
                "aac4" -> {
                    var diffYaw = 0
                    if (mc.thePlayer.movementInput.moveForward > 0) diffYaw = 180
                    if (mc.thePlayer.movementInput.moveForward < 0) diffYaw = 0
                    RotationUtils.setTargetRotation(
                        Rotation(mc.thePlayer.rotationYaw + diffYaw, 90f),
                        keepLengthValue.get()
                    )
                    Rotation(mc.thePlayer.rotationYaw + diffYaw, 90f)
                }
                "test2" -> {
                    Rotation(((MovementUtils.direction * 180f / Math.PI).toFloat() + 135), placeRotation.rotation.pitch)
                }
                "backwards" -> {
                    var calcyaw = ((MovementUtils.movingYaw - 180) / 45).roundToInt() * 45
                    var calcpitch = 0f
                    if (calcyaw % 90 == 0) {
                        calcpitch = 82f
                    } else {
                        calcpitch = 78f
                    }
                    Rotation(calcyaw.toFloat(), calcpitch)
                }
                "custom" -> {
                    Rotation(mc.thePlayer.rotationYaw + customYawValue.get(), customPitchValue.get().toFloat())
                }
                "advanced" -> {
                    var advancedYaw = 0f
                    var advancedPitch = 0f
                    advancedYaw = when (advancedYawModeValue.get().lowercase()) {
                        "offset" -> placeRotation.rotation.yaw + advancedYawOffsetValue.get()
                        "static" -> mc.thePlayer.rotationYaw + advancedYawStaticValue.get()
                        "vanilla" -> placeRotation.rotation.yaw
                        "round" -> ((placeRotation.rotation.yaw / advancedYawRoundValue.get()).roundToInt() * advancedYawRoundValue.get()).toFloat()
                        "roundstatic" -> (((mc.thePlayer.rotationYaw + advancedYawStaticValue.get()) / advancedYawRoundValue.get()).roundToInt() * advancedYawRoundValue.get()).toFloat()
                        "movedirection" -> MovementUtils.movingYaw - 180
                        "offsetmove" -> MovementUtils.movingYaw - 180 + advancedYawMoveOffsetValue.get()
                        else -> placeRotation.rotation.yaw
                    }
                    advancedPitch = when (advancedPitchModeValue.get().lowercase()) {
                        "offset" -> placeRotation.rotation.pitch + advancedPitchOffsetValue.get().toFloat()
                        "static" -> advancedPitchStaticValue.get().toFloat()
                        "vanilla" -> placeRotation.rotation.pitch
                        else -> placeRotation.rotation.pitch
                    }
                    Rotation(advancedYaw, advancedPitch)
                }
                else -> return false // this should not happen
            }
            if (silentRotationValue.get()) {
                val limitedRotation =
                    RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation!!, rotationSpeed)
                RotationUtils.setTargetRotation(limitedRotation, keepLengthValue.get())
            } else {
                mc.thePlayer.rotationYaw = lockRotation!!.yaw
                mc.thePlayer.rotationPitch = lockRotation!!.pitch
            }
        }
        if (!rotationsValue.equals("None") && !towerStatus) {
            val changeYaw = if (!randomChangeValue.get()) 0 else RandomUtils.nextInt(0, changeMaxYawValue.get())
            val changePitch = if (!randomChangeValue.get()) 0 else RandomUtils.nextInt(0, changeMaxPitchValue.get())
            lockRotation = when (rotationsValue.get().lowercase()) {
                "aac" -> {
                    Rotation(mc.thePlayer.rotationYaw + (if (mc.thePlayer.movementInput.moveForward < 0) 0 else 180) + aacYawValue.get(), placeRotation.rotation.pitch)
                }
                "vanilla", "snap", "backsnap" -> {
                    placeRotation.rotation
                }
                "ncp" -> {
                    val arrayOfFloat: FloatArray = getRotations(
                        placeRotation.placeInfo.blockPos,
                        placeRotation.placeInfo.enumFacing
                    )
                    val yaw = arrayOfFloat[0]
                    val pitch = arrayOfFloat[1]
                    Rotation(yaw, pitch)
                }
                "aac4" -> {
                    var diffYaw = 0
                    if (mc.thePlayer.movementInput.moveForward > 0) diffYaw = 180
                    if (mc.thePlayer.movementInput.moveForward < 0) diffYaw = 0
                    RotationUtils.setTargetRotation(
                        Rotation(mc.thePlayer.rotationYaw + diffYaw, 90f),
                        keepLengthValue.get()
                    )
                    Rotation(mc.thePlayer.rotationYaw + diffYaw, 90f)
                }
                "test1" -> {
                    val caluyaw = ((placeRotation.rotation.yaw / 45).roundToInt() * 45).toFloat()
                    Rotation(caluyaw + changeYaw, placeRotation.rotation.pitch + changePitch)
                }
                "test2" -> {
                    Rotation(((MovementUtils.direction * 180f / Math.PI).toFloat() + 135) + changeYaw, placeRotation.rotation.pitch + changePitch)
                }
                "backwards" -> {
                    var calcyaw = ((MovementUtils.movingYaw - 180) / 45).roundToInt() * 45
                    var calcpitch = 0f
                    if (calcyaw % 90 == 0) {
                        calcpitch = 82f
                    } else {
                        calcpitch = 78f
                    }
                    Rotation(calcyaw.toFloat(), calcpitch)
                }
                "custom" -> {
                    Rotation(mc.thePlayer.rotationYaw + customYawValue.get() + changeYaw, customPitchValue.get().toFloat() + changePitch)
                }
                "advanced" -> {
                    var advancedYaw = 0f
                    var advancedPitch = 0f
                    advancedYaw = when (advancedYawModeValue.get().lowercase()) {
                        "offset" -> placeRotation.rotation.yaw + advancedYawOffsetValue.get()
                        "static" -> mc.thePlayer.rotationYaw + advancedYawStaticValue.get()
                        "vanilla" -> placeRotation.rotation.yaw
                        "round" -> ((placeRotation.rotation.yaw / advancedYawRoundValue.get()).roundToInt() * advancedYawRoundValue.get()).toFloat()
                        "roundstatic" -> (((mc.thePlayer.rotationYaw + advancedYawStaticValue.get()) / advancedYawRoundValue.get()).roundToInt() * advancedYawRoundValue.get()).toFloat()
                        "movedirection" -> MovementUtils.movingYaw - 180
                        "offsetmove" -> MovementUtils.movingYaw - 180 + advancedYawMoveOffsetValue.get()
                        else -> placeRotation.rotation.yaw
                    }
                    advancedPitch = when (advancedPitchModeValue.get().lowercase()) {
                        "offset" -> placeRotation.rotation.pitch + advancedPitchOffsetValue.get().toFloat()
                        "static" -> advancedPitchStaticValue.get().toFloat()
                        "vanilla" -> placeRotation.rotation.pitch
                        else -> placeRotation.rotation.pitch
                    }
                    Rotation(advancedYaw, advancedPitch)
                }
                else -> return false // this should not happen
            }
            if (silentRotationValue.get()) {
                val limitedRotation =
                    RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation!!, rotationSpeed)
                if (rotationsValue.equals("Snap") || rotationsValue.equals("BackSnap")) {
                    RotationUtils.setTargetRotation(limitedRotation, 0)
                } else {
                    RotationUtils.setTargetRotation(limitedRotation, keepLengthValue.get())
                }
            } else {
                mc.thePlayer.rotationYaw = lockRotation!!.yaw
                mc.thePlayer.rotationPitch = lockRotation!!.pitch
            }
        }
        targetPlace = placeRotation.placeInfo
        return true
    }

    /**
     * @return hotbar blocks amount
     */
    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val itemStack = mc.thePlayer.inventoryContainer.getSlot(i).stack
                if (itemStack != null && itemStack.item is ItemBlock && InventoryUtils.canPlaceBlock((itemStack.item as ItemBlock).block)) {
                    amount += itemStack.stackSize
                }
            }
            return amount
        }

    private val rotationSpeed: Float
        get() = (Math.random() * (maxRotationSpeedValue.get() - minRotationSpeedValue.get()) + minRotationSpeedValue.get()).toFloat()

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (towerStatus) {
            event.cancelEvent()
        }
    }

    fun getRotations(paramBlockPos: BlockPos, paramEnumFacing: EnumFacing): FloatArray {
        val d1 = paramBlockPos.x + 0.5 - mc.thePlayer.posX + paramEnumFacing.frontOffsetX / 2.0
        val d2 = paramBlockPos.z + 0.5 - mc.thePlayer.posZ + paramEnumFacing.frontOffsetZ / 2.0
        val d3 = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - (paramBlockPos.y + 0.5)
        val d4 = MathHelper.sqrt_double(d1 * d1 + d2 * d2).toDouble()
        var f1 = (atan2(d2, d1) * 180.0 / 3.141592653589793).toFloat() - 90.0f
        val f2 = (atan2(d3, d4) * 180.0 / 3.141592653589793).toFloat()
        if (f1 < 0.0f) {
            f1 += 360.0f
        }
        return floatArrayOf(f1, f2)
    }

    val canSprint: Boolean
        get() = MovementUtils.isMoving() && when (sprintValue.get().lowercase()) {
            "always", "hypixel" -> true
            "dynamic" -> mc.gameSettings.keyBindSprint.isPressed
            "onground" -> mc.thePlayer.onGround
            "offground" -> !mc.thePlayer.onGround
            else -> false
        }

    override val tag: String
        get() = if (towerStatus) { "Towering" } else { "Moving" }
}