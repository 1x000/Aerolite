/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 * 
 * This code belongs to WYSI-Foundation. Please give credits when using this in your repository.
 */
package net.ccbluex.liquidbounce.features.module.modules.render;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "AsianHat", category = ModuleCategory.RENDER)
public class AsianHat extends Module {

    private final ListValue colorModeValue = new ListValue("Color", new String[] {"Custom"}, "Custom");
	private final IntegerValue colorRedValue = new IntegerValue("Red", 255, 0, 255);
	private final IntegerValue colorGreenValue = new IntegerValue("Green", 255, 0, 255);
	private final IntegerValue colorBlueValue = new IntegerValue("Blue", 255, 0, 255);
	private final IntegerValue colorAlphaValue = new IntegerValue("Alpha", 255, 0, 255);
    private final IntegerValue colorEndAlphaValue = new IntegerValue("EndAlpha", 255, 0, 255);
	private final FloatValue saturationValue = new FloatValue("Saturation", 1F, 0F, 1F);
	private final FloatValue brightnessValue = new FloatValue("Brightness", 1F, 0F, 1F);
	private final IntegerValue mixerSecondsValue = new IntegerValue("Seconds", 2, 1, 10);
    private final IntegerValue spaceValue = new IntegerValue("Color-Space", 0, 0, 100);
    private final BoolValue noFirstPerson = new BoolValue("NoFirstPerson", true);
    private final BoolValue hatBorder = new BoolValue("HatBorder", true);
    //private final BoolValue hatRotation = new BoolValue("HatRotation", true);
    private final IntegerValue borderAlphaValue = new IntegerValue("BorderAlpha", 255, 0, 255);
    private final FloatValue borderWidthValue = new FloatValue("BorderWidth", 1F, 0.1F, 4F);

    private final List<double[]> positions = new ArrayList<>();
    private double lastRadius = 0;

    private void checkPosition(double radius) {
        if (radius != lastRadius) {
            // generate new positions
            positions.clear();
            for (int i = 0; i <= 360; i += 1)
                positions.add(new double[] {-Math.sin(i * Math.PI / 180) * radius, Math.cos(i * Math.PI / 180) * radius});
        }
        lastRadius = radius;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        EntityLivingBase entity = mc.thePlayer;
        if (entity == null || (noFirstPerson.get() && mc.gameSettings.thirdPersonView == 0)) return;

        final AxisAlignedBB bb = entity.getEntityBoundingBox();
        double radius = bb.maxX - bb.minX;
		double height = bb.maxY - bb.minY;
		double posX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks;
	    double posY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks;
	    double posZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks;

        Color colour = getColor(entity, 0);
        float r = colour.getRed() / 255.0F;
        float g = colour.getGreen() / 255.0F;
        float b = colour.getBlue() / 255.0F;
        float al = colorAlphaValue.get() / 255.0F;
        float Eal = colorEndAlphaValue.get() / 255.0F;

        float partialTicks = event.getPartialTicks();

        double viewX = -mc.getRenderManager().viewerPosX;
        double viewY = -mc.getRenderManager().viewerPosY;
        double viewZ = -mc.getRenderManager().viewerPosZ;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        checkPosition(radius);
        pre3D();
/*
        if (hatRotation.get() && RotationUtils.serverRotation != null) {
            //GlStateManager.translate(viewX, viewY, viewZ);
            GlStateManager.rotate(RotationUtils.serverRotation.getPitch(), 0, 1, 0);
            GlStateManager.rotate(RotationUtils.serverRotation.getYaw(), 0, 0, 1);
        }
*/
        worldrenderer.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION_COLOR);

        // main section
        worldrenderer.pos(viewX + posX, viewY + posY + height + 0.3, viewZ + posZ).color(r, g, b, al).endVertex();

        int i = 0;
        for (double[] smolPos : positions) {
			double posX2 = posX + smolPos[0];
			double posZ2 = posZ + smolPos[1];
            
            if (spaceValue.get() > 0 && !colorModeValue.get().equalsIgnoreCase("Custom")) {
                Color colour2 = getColor(entity, i * spaceValue.get());
                float r2 = colour2.getRed() / 255.0F;
                float g2 = colour2.getGreen() / 255.0F;
                float b2 = colour2.getBlue() / 255.0F;

                worldrenderer.pos(viewX + posX2, viewY + posY + height, viewZ + posZ2).color(r2, g2, b2, Eal).endVertex();
            } else {
                worldrenderer.pos(viewX + posX2, viewY + posY + height, viewZ + posZ2).color(r, g, b, Eal).endVertex();
            }

            i++;
		}

        worldrenderer.pos(viewX + posX, viewY + posY + height + 0.3, viewZ + posZ).color(r, g, b, al).endVertex();
        tessellator.draw();

        // border section
        if (hatBorder.get()) {
            float lineAlp = borderAlphaValue.get() / 255.0F;

            GL11.glLineWidth(borderWidthValue.get());

            worldrenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            
            i = 0;
            for (double[] smolPos : positions) {
			    double posX2 = posX + smolPos[0];
			    double posZ2 = posZ + smolPos[1];

                if (spaceValue.get() > 0 && !colorModeValue.get().equalsIgnoreCase("Custom")) {
                    Color colour2 = getColor(entity, i * spaceValue.get());
                    float r2 = colour2.getRed() / 255.0F;
                    float g2 = colour2.getGreen() / 255.0F;
                    float b2 = colour2.getBlue() / 255.0F;

                    worldrenderer.pos(viewX + posX2, viewY + posY + height, viewZ + posZ2).color(r2, g2, b2, lineAlp).endVertex();
                } else {
                    worldrenderer.pos(viewX + posX2, viewY + posY + height, viewZ + posZ2).color(r, g, b, lineAlp).endVertex();
                }

                i++;
		    }

            tessellator.draw();
        }
        
        post3D();
    }

	public final Color getColor(final Entity ent, final int index) {
		switch (colorModeValue.get()) {
			case "Custom":
				return new Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get());
			default:
                return new Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get());
		}
	}

    public static void pre3D() {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glDisable(2884);
    }

    public static void post3D() {
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glColor4f(1, 1, 1, 1);
    }

}