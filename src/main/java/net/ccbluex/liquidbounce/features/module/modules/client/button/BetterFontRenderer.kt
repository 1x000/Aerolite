package net.ccbluex.liquidbounce.features.module.modules.client.button

import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import java.awt.Color

class BetterButtonRenderer(button: GuiButton) : AbstractButtonRenderer(button) {

    private var animation = 0.0
    private var lastUpdate = System.currentTimeMillis()
    override fun render(mouseX: Int, mouseY: Int, mc: Minecraft) {
        val time = System.currentTimeMillis()
        val pct = (time - lastUpdate) / 500.0
        if (button.visible) {
            GL11.glPushMatrix()
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
            button.hovered =
                mouseX >= button.xPosition && mouseY >= button.yPosition && mouseX < button.xPosition + button.width && mouseY < button.yPosition + button.height
            GlStateManager.enableBlend()
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
            GlStateManager.blendFunc(770, 771)
            var j = 14737632

            val percent = EaseUtils.easeInOutQuad(animation)
            val al = if (140 + ((percent * 50).toInt()) in 1..254) 140 + ((percent * 50).toInt()) else 140;
            RenderUtils.drawRoundedCornerRect(
                button.xPosition.toFloat(),
                button.yPosition.toFloat(),
                button.xPosition.toFloat() + button.width.toFloat(),
                button.yPosition.toFloat() + button.height.toFloat(),
                2F,
                Color(0, 0, 0, al).rgb
            )
            //ClientUtils.displayChatMessage(percent.toString())
            lastUpdate = time
            if (!button.enabled) {
                RenderUtils.drawRoundedCornerRect(
                    button.xPosition.toFloat(),
                    button.yPosition.toFloat(),
                    button.xPosition.toFloat() + button.width.toFloat(),
                    button.yPosition.toFloat() + button.height.toFloat(),
                    2F,
                    Color(100, 100, 100, 180).rgb
                )
                j = 10526880
            } else if (button.hovered) {

                if (animation < 1) {
                    animation += pct
                }
                if (animation > 1) {
                    animation = 1.0
                }
                //System.out.println(aFade);
                RenderUtils.drawGradientSidewaysV(
                    button.xPosition - 0.5, button.yPosition - 5.0, button.xPosition + button.width + 0.5,
                    button.yPosition.toDouble(), Color(20, 20, 20, 0).rgb,
                    Color(60, 60, 60, (20 * percent).toInt()).rgb,
                )
                RenderUtils.drawGradientSidewaysH(
                    button.xPosition - 5.0,
                    button.yPosition - 0.5,
                    button.xPosition.toDouble(),
                    button.yPosition + button.height + 0.5,
                    Color(20, 20, 20, 0).rgb,
                    Color(60, 60, 60, (20 * percent).toInt()).rgb,
                )
                RenderUtils.drawGradientSidewaysV(
                    button.xPosition - 0.5,
                    button.yPosition + button.height.toDouble(),
                    button.xPosition + button.width + 0.5,
                    button.yPosition + button.height + 5.0,
                    Color(60, 60, 60, (20 * percent).toInt()).rgb,
                    Color(20, 20, 20, 0).rgb
                )
                RenderUtils.drawGradientSidewaysH(
                    button.xPosition + button.width.toDouble(),
                    button.yPosition - 0.5,
                    button.xPosition + button.width + 5.0,
                    button.yPosition + button.height + 0.5,
                    Color(60, 60, 60, (20 * percent).toInt()).rgb,
                    Color(20, 20, 20, 0).rgb
                )
                //RenderUtil.drawRoundRect(button.xPosition, button.yPosition, (button.xPosition + button.width), (button.yPosition + button.height),2,new Color(255,255,255,180+((aFade-100))).getRGB());
                j = 16777120
            } else {

                if (animation > 0) {
                    animation -= pct
                }
                if (animation < 0) {
                    animation = 0.0
                }
                RenderUtils.drawGradientSidewaysV(
                    button.xPosition - 0.5, button.yPosition - 5.0, button.xPosition + button.width + 0.5,
                    button.yPosition.toDouble(), Color(20, 20, 20, 0).rgb, Color(60, 60, 60, (20 * percent).toInt()).rgb
                )
                RenderUtils.drawGradientSidewaysH(
                    button.xPosition - 5.0,
                    button.yPosition - 0.5,
                    button.xPosition.toDouble(),
                    button.yPosition + button.height + 0.5,
                    Color(20, 20, 20, 0).rgb,
                    Color(60, 60, 60, (20 * percent).toInt()).rgb
                )
                RenderUtils.drawGradientSidewaysV(
                    button.xPosition - 0.5,
                    button.yPosition + button.height.toDouble(),
                    button.xPosition + button.width + 0.5,
                    button.yPosition + button.height + 5.0,
                    Color(60, 60, 60, (20 * percent).toInt()).rgb,
                    Color(20, 20, 20, 0).rgb
                )
                RenderUtils.drawGradientSidewaysH(
                    button.xPosition + button.width.toDouble(),
                    button.yPosition - 0.5,
                    button.xPosition + button.width + 5.0,
                    button.yPosition + button.height + 0.5,
                    Color(60, 60, 60, (20 * percent).toInt()).rgb,
                    Color(20, 20, 20, 0).rgb
                )
            }
            GL11.glPopMatrix()
        }
    }
}