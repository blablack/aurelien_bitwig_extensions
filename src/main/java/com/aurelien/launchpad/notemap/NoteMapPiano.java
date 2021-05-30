package com.aurelien.launchpad.notemap;

import com.aurelien.basic.NovationColor;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SettableRangedValue;

public class NoteMapPiano extends NoteMapCommon implements INoteMap
{
    private static final NovationColor WHITE = NovationColor.AMBER_LOW;
    private static final NovationColor BLACK = NovationColor.RED_LOW;
    private static final NovationColor OFF = NovationColor.OFF;
    private static final NovationColor ON = NovationColor.GREEN_FULL;

    public NoteMapPiano(MidiOut midiOutPort, SettableRangedValue settingVelocity)
    {
        super(midiOutPort, settingVelocity);
    }

    public void drawKeys()
    {
        for (int y = 0; y < 8; y++)
        {
            for (int x = 0; x < 8; x++)
            {
                int key = cellToKey(x, y);
                NovationColor colour = (key != -1) ? (this.isKeyBlack(key) ? BLACK : WHITE) : OFF;
                setCellLED(x, y, colour);
            }
        }
    }

    public void turnOnNote(int Note)
    {
        for (int y = 0; y < 8; y++)
        {
            for (int x = 0; x < 8; x++)
            {
                if (Note == this.cellToKey(x, y))
                {
                    setCellLED(x, y, ON);
                }
            }
        }
    }

    public void turnOffNote(int Note)
    {
        for (int y = 0; y < 8; y++)
        {
            for (int x = 0; x < 8; x++)
            {
                int key = this.cellToKey(x, y);
                if (Note == key)
                {
                    setCellLED(x, y, this.isKeyBlack(key) ? BLACK : WHITE);
                }
            }
        }
    }

    public int cellToKey(int x, int y)
    {
        int octave = (int) (3 - Math.floor(y / 2));

        int xx = 0;
        boolean no_k = false;

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

        boolean white = (y & 1) != 0;

        if (!white && no_k)
            return -1;

        int key = this.rootKey + octave * 12 + xx;

        if (!white)
            key -= 1;

        return key;
    }

    public boolean isKeyBlack(int key)
    {
        int k = key % 12;

        return k == 1 || k == 3 || k == 6 || k == 8 || k == 10;
    }

    public boolean canScrollUp()
    {
        return this.rootKey < 72;
    }

    public void scrollUp()
    {
        this.rootKey = Math.min(this.rootKey + 12, 72);
    }

    public boolean canScrollDown()
    {
        return this.rootKey > 0;
    }

    public void scrollDown()
    {
        this.rootKey = Math.max(this.rootKey - 12, 0);
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
        return "Piano";
    }
}
