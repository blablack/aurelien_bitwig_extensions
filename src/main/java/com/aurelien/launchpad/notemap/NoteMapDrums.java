package com.aurelien.launchpad.notemap;

import com.aurelien.basic.NovationColor;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SettableRangedValue;

public class NoteMapDrums extends NoteMapCommon implements INoteMap
{
    private static final NovationColor EVEN = NovationColor.RED_LOW;
    private static final NovationColor ODD = NovationColor.AMBER_LOW;
    private static final NovationColor ON = NovationColor.GREEN_FULL;

    public NoteMapDrums(MidiOut midiOutPort, SettableRangedValue settingVelocity)
    {
        super(midiOutPort, settingVelocity);
    }

    public void drawKeys()
    {
        for (var x = 0; x < 4; x++)
        {
            for (var y = 0; y < 4; y++)
            {
                var cell_is_even = ((x + y) & 1) == 0;
                NovationColor colour = cell_is_even ? EVEN : ODD;

                setCellLED(x * 2, y * 2, colour);
                setCellLED(x * 2 + 1, y * 2, colour);
                setCellLED(x * 2, y * 2 + 1, colour);
                setCellLED(x * 2 + 1, y * 2 + 1, colour);
            }
        }
    }

    public void turnOnNote(final int Note)
    {
        int p_note = (Note - 4) % 16;
        int x = (p_note % 4) * 2;
        int y = (p_note / 4 - 3) * -2;

        setCellLED(x, y, ON);
        setCellLED(x + 1, y, ON);
        setCellLED(x, y + 1, ON);
        setCellLED(x + 1, y + 1, ON);
    }

    public void turnOffNote(final int Note)
    {
        int p_note = (Note - 4) % 16;
        int x = (p_note % 4);
        int y = (p_note / 4 - 3) * -1;

        var cell_is_even = ((x + y) & 1) == 0;
        NovationColor colour = cell_is_even ? EVEN : ODD;

        x *= 2;
        y *= 2;

        setCellLED(x, y, colour);
        setCellLED(x + 1, y, colour);
        setCellLED(x, y + 1, colour);
        setCellLED(x + 1, y + 1, colour);
    }

    public int cellToKey(int x, int y)
    {
        int lx = x >> 1;
        int ly = y >> 1;
        return this.rootKey + (3 - ly) * 4 + lx;
    }

    public boolean canScrollUp()
    {
        return this.rootKey < 100;
    }

    public void scrollUp()
    {
        this.rootKey = Math.min(this.rootKey + 16, 100);
    }

    public boolean canScrollDown()
    {
        return this.rootKey > 4;
    }

    public void scrollDown()
    {
        this.rootKey = Math.max(this.rootKey - 16, 4);
    }

    public boolean canScrollLeft()
    {
        return false;
    }

    public void scrollLeft() throws CannotScrollException
    {
        throw new CannotScrollException(this.toString() + " - cannot scroll left");
    }

    public boolean canScrollRight()
    {
        return false;
    }

    public void scrollRight() throws CannotScrollException
    {
        throw new CannotScrollException(this.toString() + " - cannot scroll right");
    }

    @Override
    public String toString()
    {
        return "Drums";
    }
}
