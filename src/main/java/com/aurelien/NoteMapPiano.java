package com.aurelien;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;

public class NoteMapPiano extends NoteMap
{
    final private NovationColor white;
    final private NovationColor black;
    final private NovationColor off;
    final private NovationColor on;

    public NoteMapPiano(ControllerHost host, MidiOut midiOutPort)
    {
        super(host, midiOutPort);

        white = NovationColor.AMBER_LOW;
        black = NovationColor.RED_LOW;
        off = NovationColor.OFF;
        on = NovationColor.GREEN_FULL;
    }

    @Override
    public void DrawKeys()
    {
        for (int y = 0; y < 8; y++)
        {
            for (int x = 0; x < 8; x++)
            {
                int key = cellToKey(x, y);
                NovationColor colour = (key != -1) ? (this.isKeyBlack(key) ? black : white) : off;
                setCellLED(x, y, colour);
            }
        }
    }

    @Override
    public void TurnOnNote(int Note)
    {
        for (int y = 0; y < 8; y++)
        {
            for (int x = 0; x < 8; x++)
            {
                if (Note == this.cellToKey(x, y))
                {
                    setCellLED(x, y, on);
                }
            }
        }
    }

    @Override
    public void TurnOffNote(int Note)
    {
        for (int y = 0; y < 8; y++)
        {
            for (int x = 0; x < 8; x++)
            {
                int key = this.cellToKey(x, y);
                if (Note == key)
                {
                    setCellLED(x, y, this.isKeyBlack(key) ? black : white);
                }
            }
        }
    }

    @Override
    public int cellToKey(int x, int y)
    {
        int octave = (int) (3 - Math.floor(y / 2));

        int xx = 0;
        Boolean no_k = false;

        switch (x)
        {
            case 0:
                xx = 0;
                no_k = true;
                break;

            case 1:
                xx = 2;
                break;

            case 2:
                xx = 4;
                break;

            case 3:
                xx = 5;
                no_k = true;
                break;

            case 4:
                xx = 7;
                break;

            case 5:
                xx = 9;
                break;

            case 6:
                xx = 11;
                break;

            case 7:
                xx = 12;
                no_k = true;
                break;
        }

        Boolean white = (y & 1) != 0;

        if (!white && no_k)
            return -1;

        int key = this.rootKey + octave * 12 + xx;

        if (!white)
            key -= 1;

        return key;
    }

    public Boolean isKeyBlack(int key)
    {
        int k = key % 12;

        switch (k)
        {
            case 1:
            case 3:
            case 6:
            case 8:
            case 10:
                return true;
        }

        return false;
    }

    @Override
    public Boolean canScrollUp()
    {
        return this.rootKey < 72;
    }

    @Override
    public void ScrollUp()
    {
        this.rootKey = Math.min(this.rootKey + 12, 72);
    }

    @Override
    public Boolean canScrollDown()
    {
        return this.rootKey > 0;
    }

    @Override
    public void ScrollDown()
    {
        this.rootKey = Math.max(this.rootKey - 12, 0);
    }

    @Override
    public String toString()
    {
        return "Piano";
    }
}
