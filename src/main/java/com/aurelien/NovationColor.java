package com.aurelien;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

final class NovationColor extends InternalHardwareLightState
{
    public static final NovationColor OFF = new NovationColor(12);
    public static final NovationColor RED_LOW = new NovationColor(13);
    public static final NovationColor RED_FULL = new NovationColor(15);
    public static final NovationColor AMBER_LOW = new NovationColor(29);
    public static final NovationColor AMBER_FULL = new NovationColor(63);
    public static final NovationColor YELLOW_FULL = new NovationColor(62);
    public static final NovationColor YELLOW_LOW = new NovationColor(0x2D);
    public static final NovationColor ORANGE = new NovationColor(39);
    public static final NovationColor LIME = new NovationColor(0x3D);
    public static final NovationColor GREEN_LOW = new NovationColor(28);
    public static final NovationColor GREEN_FULL = new NovationColor(60);

    public static final NovationColor RED_FLASHING = new NovationColor(11);
    public static final NovationColor AMBER_FLASHING = new NovationColor(59);
    public static final NovationColor YELLOW_FLASHING = new NovationColor(58);
    public static final NovationColor GREEN_FLASHING = new NovationColor(56);

    private final int m_colorCode;

    public NovationColor(final int ColorCode)
    {
        m_colorCode = ColorCode;
    }

    NovationColor(int Green, int Red, boolean Flashing)
    {
        int p_flashing = 12;
        if (Flashing)
            p_flashing = 8;

        m_colorCode = 16 * Green + Red + p_flashing;
    }

    @Override
    public HardwareLightVisualState getVisualState()
    {
        return HardwareLightVisualState.createForColor(Color.fromRGB(64, 0, 0));
    }

    @Override
    public boolean equals(final Object obj)
    {
        return obj instanceof NovationColor && equals((NovationColor) obj);
    }

    public boolean equals(final NovationColor obj)
    {
        if (obj == this)
            return true;

        return m_colorCode == obj.m_colorCode;
    }

    public int Code()
    {
        return m_colorCode;
    }
}
