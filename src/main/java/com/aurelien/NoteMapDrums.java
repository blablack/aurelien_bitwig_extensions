package com.aurelien;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;

public class NoteMapDrums extends NoteMap
{
    final private NovationColor even;
    final private NovationColor odd;
    final private NovationColor on;

    public NoteMapDrums(ControllerHost host, MidiOut midiOutPort)
    {
        super(host, midiOutPort);

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
        int x = ((this.rootKey - Note) % 4) * -2;
        int y = ((this.rootKey - Note) / 4 + 3) * 2;

        setCellLED(x, y, on);
        setCellLED(x + 1, y, on);
        setCellLED(x, y + 1, on);
        setCellLED(x + 1, y + 1, on);
    }

    @Override
    public void TurnOffNote(int Note)
    {
        int x = ((this.rootKey - Note) % 4) * -1;
        int y = ((this.rootKey - Note) / 4 + 3) ;

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
        return this.rootKey + (3 - ly) * 4 + lx;
    }

    @Override
    public Boolean canScrollUp()
    {
        return this.rootKey < 100;
    }

    @Override
    public void ScrollUp()
    {
        this.rootKey = Math.min(this.rootKey + 16, 100);
    }

    @Override
    public Boolean canScrollDown()
    {
        return this.rootKey > 4;
    }

    @Override
    public void ScrollDown()
    {
        this.rootKey = Math.max(this.rootKey - 16, 4);
    }

    @Override
    public String toString()
    {
        return "Drums";
    }
}
