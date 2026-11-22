/*
 * Resolves Material You symbolic colors in the bundled Monet Black theme.
 * Standard .attheme files continue to use Telegram's original parser.
 */
package org.telegram.ui.ActionBar;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.ApplicationLoader;

final class MonetThemeColors {
    private MonetThemeColors() {}

    static boolean isReference(String raw) {
        String base = raw.trim().split("\\(", 2)[0].trim();
        return base.startsWith("n1_") || base.startsWith("n2_") ||
                base.startsWith("a1_") || base.startsWith("a2_") ||
                base.startsWith("a3_") || base.equals("mBlack") ||
                base.equals("mWhite") || base.equals("mRed500") ||
                base.equals("mRed200") || base.equals("mGreen500");
    }

    static int resolve(String raw) {
        String value = raw.trim();
        int paren = value.indexOf('(');
        String token = (paren >= 0 ? value.substring(0, paren) : value).trim();
        int color;
        switch (token) {
            case "mBlack": color = Color.BLACK; break;
            case "mWhite": color = Color.WHITE; break;
            case "mRed500": color = 0xfff44336; break;
            case "mRed200": color = 0xffef9a9a; break;
            case "mGreen500": color = 0xff4caf50; break;
            default:
                String[] parts = token.split("_", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Unknown Monet color: " + token);
                }
                String group;
                switch (parts[0]) {
                    case "n1": group = "neutral1"; break;
                    case "n2": group = "neutral2"; break;
                    case "a1": group = "accent1"; break;
                    case "a2": group = "accent2"; break;
                    case "a3": group = "accent3"; break;
                    default: throw new IllegalArgumentException("Unknown Monet palette: " + token);
                }
                int tone = Integer.parseInt(parts[1]);
                color = fallbackColor(parts[0], tone);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        Resources resources = ApplicationLoader.applicationContext.getResources();
                        int resourceId = resources.getIdentifier(
                                "system_" + group + "_" + tone, "color", "android");
                        if (resourceId != 0) {
                            color = resources.getColor(resourceId, null);
                        }
                    } catch (RuntimeException ignored) {
                        // Some OEM skins do not expose the complete dynamic palette.
                    }
                }
                break;
        }
        if (paren >= 0 && value.endsWith(")")) {
            String modifier = value.substring(paren + 1, value.length() - 1).trim();
            if (modifier.startsWith("l=")) {
                float[] hsl = new float[3];
                ColorUtils.colorToHSL(color, hsl);
                hsl[2] = clamp(Integer.parseInt(modifier.substring(2).trim()) / 100f);
                int alpha = Color.alpha(color);
                color = ColorUtils.setAlphaComponent(ColorUtils.HSLToColor(hsl), alpha);
            } else if (modifier.startsWith("a=")) {
                int alpha = Math.round(255f * clamp(Integer.parseInt(modifier.substring(2).trim()) / 100f));
                color = ColorUtils.setAlphaComponent(color, alpha);
            }
        }
        return color;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    // Stable dark fallback for pre-Android-12 devices and missing OEM tones.
    private static int fallbackColor(String palette, int tone) {
        int dark;
        int light;
        switch (palette) {
            case "n1":
                dark = 0xff15151a; light = 0xfff8f7fb; break;
            case "n2":
                dark = 0xff191b22; light = 0xfff4f2fa; break;
            case "a2":
                dark = 0xff202d49; light = 0xffe2e6f6; break;
            case "a3":
                dark = 0xff38223c; light = 0xffffd9f0; break;
            default:
                dark = 0xff16285d; light = 0xffd8e3ff; break;
        }
        return ColorUtils.blendARGB(light, dark, clamp(tone / 1000f));
    }
}
