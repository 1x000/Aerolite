/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 * 
 * This code belongs to WYSI-Foundation. Please give credits when using this in your repository.
 */
package net.ccbluex.liquidbounce.features.module.modules.render.LiquidBouncePlus;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.value.IntegerValue;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.Objects;

@ModuleInfo(name = "ColorMixer", category = ModuleCategory.RENDER, canEnable = false)
public class ColorMixer extends Module {

    private static float[] lastFraction = new float[]{};
    public static Color[] lastColors = new Color[]{};

    private static float[] lastFractionC = new float[]{};
    public static Color[] lastColorsC = new Color[]{};

    public final IntegerValue blendAmount = new IntegerValue("Mixer-Amount", 2, 2, 3) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            regenerateColors(!Objects.equals(oldValue, newValue));
        }
    };
    /*
    @Override
    public void onInitialize() {
        regenerateColors();
    }
    */
    public final ColorElement col1RedValue = new ColorElement(1, ColorElement.Material.RED);
    public final ColorElement col1GreenValue = new ColorElement(1, ColorElement.Material.GREEN);
    public final ColorElement col1BlueValue = new ColorElement(1, ColorElement.Material.BLUE);

    public final ColorElement col2RedValue = new ColorElement(2, ColorElement.Material.RED);
    public final ColorElement col2GreenValue = new ColorElement(2, ColorElement.Material.GREEN);
    public final ColorElement col2BlueValue = new ColorElement(2, ColorElement.Material.BLUE);

    public final ColorElement col3RedValue = new ColorElement(3, ColorElement.Material.RED, blendAmount);
    public final ColorElement col3GreenValue = new ColorElement(3, ColorElement.Material.GREEN, blendAmount);
    public final ColorElement col3BlueValue = new ColorElement(3, ColorElement.Material.BLUE, blendAmount);

    public static Color getMixedColor(int index, int seconds) {
        final ColorMixer colMixer = (ColorMixer) LiquidBounce.moduleManager.getModule(ColorMixer.class);
        if (colMixer == null) return Color.white;

        if (lastColors.length <= 0 || lastFraction.length <= 0) regenerateColors(true); // just to make sure it won't go white

        return BlendUtils.blendColors(lastFraction, lastColors, (System.currentTimeMillis() + index) % (seconds * 1000) / (float) (seconds * 1000));
    }

    public static Color getMixedColor(int r, int g, int b, int r2, int g2, int b2, int index, int seconds) {
        if (lastColorsC.length <= 0 || lastFractionC.length <= 0) regenerateColors2(r,g,b,r2,g2,b2,true); // just to make sure it won't go white

        return BlendUtils.blendColors(lastFractionC, lastColorsC, (System.currentTimeMillis() + index) % (seconds * 1000) / (float) (seconds * 1000));
    }

    public static void regenerateColors(boolean forceValue) {
        final ColorMixer colMixer = (ColorMixer) LiquidBounce.moduleManager.getModule(ColorMixer.class);

        if (colMixer == null) return;
            
        // color generation
        if (forceValue || lastColors.length <= 0 || lastColors.length != (colMixer.blendAmount.get() * 2) - 1) {
            Color[] generator = new Color[(colMixer.blendAmount.get() * 2) - 1];

            // reflection is cool
            for (int i = 1; i <= colMixer.blendAmount.get(); i++) {
                Color result = Color.white;
                try {                
                    Field red = ColorMixer.class.getField("col"+i+"RedValue");
                    Field green = ColorMixer.class.getField("col"+i+"GreenValue");
                    Field blue = ColorMixer.class.getField("col"+i+"BlueValue");

                    int r = ((ColorElement)red.get(colMixer)).get();
                    int g = ((ColorElement)green.get(colMixer)).get();
                    int b = ((ColorElement)blue.get(colMixer)).get();

                    result = new Color(Math.max(0, Math.min(r, 255)), Math.max(0, Math.min(g, 255)), Math.max(0, Math.min(b, 255)));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                generator[i - 1] = result;
            }

            int h = colMixer.blendAmount.get();
            for (int z = colMixer.blendAmount.get() - 2; z >= 0; z--) {
                generator[h] = generator[z];
                h++;
            }

            lastColors = generator;
        }

        // cache thingy
        if (forceValue || lastFraction.length <= 0 || lastFraction.length != (colMixer.blendAmount.get() * 2) - 1) {
            // color frac regenerate if necessary
            float[] colorFraction = new float[(colMixer.blendAmount.get() * 2) - 1];    

            for (int i = 0; i <= (colMixer.blendAmount.get() * 2) - 2; i++)
            {
                colorFraction[i] = (float)i / (float)((colMixer.blendAmount.get() * 2) - 2);
            }

            lastFraction = colorFraction;
        }
    }

    public static void regenerateColors2(int r, int g, int b, int r2, int g2, int b2,boolean forceValue) {
        // color generation
        if (forceValue || lastColorsC.length <= 0) {
            Color[] generator = new Color[3];

            // reflection is cool
            for (int i = 1; i <= 2; i++) {
                Color result = Color.white;
                try {
                    if (i == 1) result = new Color(Math.max(0, Math.min(r, 255)), Math.max(0, Math.min(g, 255)), Math.max(0, Math.min(b, 255)));
                    if (i == 2) result = new Color(Math.max(0, Math.min(r2, 255)), Math.max(0, Math.min(g2, 255)), Math.max(0, Math.min(b2, 255)));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                generator[i - 1] = result;
            }

            int h = 2;
            for (int z = 2 - 2; z >= 0; z--) {
                generator[h] = generator[z];
                h++;
            }

            lastColorsC = generator;
        }

        // cache thingy
        if (forceValue || lastFractionC.length <= 0) {
            // color frac regenerate if necessary
            float[] colorFraction = new float[3];

            for (int i = 0; i <= (2 * 2) - 2; i++)
            {
                colorFraction[i] = (float)i / (float)(2);
            }

            lastFractionC = colorFraction;
        }
    }
    
}