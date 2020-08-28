package com.aurelien;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SettableRangedValue;

public class NoteMapDrums extends NoteMap
{
    final private NovationColor even;
    final private NovationColor odd;
    final private NovationColor on;

    public NoteMapDrums(MidiOut midiOutPort, SettableRangedValue settingVelocity)
    {
        super(midiOutPort, settingVelocity);

        odd = NovationColor.AMBER_LOW;
        even = NovationColor.RED_LOW;
        on = NovationColor.GREEN_FULL;
    }

    @Override
    public void DrawKeys()
    {
        for (var x = 0; x < 4; x++)
        {
            for (var y = 0; y < 4; y++)
            {
                var cell_is_even = ((x + y) & 1) == 0;
                NovationColor colour = cell_is_even ? even : odd;

                setCellLED(x * 2, y * 2, colour);
                setCellLED(x * 2 + 1, y * 2, colour);
                setCellLED(x * 2, y * 2 + 1, colour);
                setCellLED(x * 2 + 1, y * 2 + 1, colour);
            }
        }
    }

    @Override
    public void TurnOnNote(int Note)
    {
        int x = ((this.m_rootKey - Note) % 4) * -2;
        int y = ((this.m_rootKey - Note) / 4 + 3) * 2;

        setCellLED(x, y, on);
        setCellLED(x + 1, y, on);
        setCellLED(x, y + 1, on);
        setCellLED(x + 1, y + 1, on);
    }

    @Override
    public void TurnOffNote(int Note)
    {
        int x = ((this.m_rootKey - Note) % 4) * -1;
        int y = ((this.m_rootKey - Note) / 4 + 3);

        var cell_is_even = ((x + y) & 1) == 0;
        NovationColor colour = cell_is_even ? even : odd;

        x *= 2;
        y *= 2;

        setCellLED(x, y, colour);
        setCellLED(x + 1, y, colour);
        setCellLED(x, y + 1, colour);
        setCellLED(x + 1, y + 1, colour);
    }

    @Override
    public int cellToKey(int x, int y)
    {
        int lx = x >> 1;
        int ly = y >> 1;
        return this.m_rootKey + (3 - ly) * 4 + lx;
    }

    @Override
    public Boolean canScrollUp()
    {
        return this.m_rootKey < 100;
    }

    @Override
    public void ScrollUp()
    {
        this.m_rootKey = Math.min(this.m_rootKey + 16, 100);
    }

    @Override
    public Boolean canScrollDown()
    {
        return this.m_rootKey > 4;
    }

    @Override
    public void ScrollDown()
    {
        this.m_rootKey = Math.max(this.m_rootKey - 16, 4);
    }

    @Override
    public String toString()
    {
        return "Drums";
    }
}
