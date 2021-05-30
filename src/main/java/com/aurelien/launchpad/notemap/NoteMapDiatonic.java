package com.aurelien.launchpad.notemap;

import static com.aurelien.launchpad.LaunchpadConstants.*;

import com.aurelien.basic.NovationColor;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.SettableEnumValue;

public class NoteMapDiatonic extends NoteMapCommon implements INoteMap
{
    private static final Integer[][] ModernModes =
    {
            {0, 2, 4, 5, 7, 9, 11, 12}, // Ionian
            {0, 2, 3, 5, 7, 9, 10, 12}, // Dorian
            {0, 1, 3, 5, 7, 8, 10, 12}, // Phrygian
            {0, 2, 4, 6, 7, 9, 11, 12}, // Lydian
            {0, 2, 4, 5, 7, 9, 10, 12}, // Mixolydian
            {0, 2, 3, 5, 7, 8, 10, 12}, // Aeolian
            {0, 1, 3, 5, 6, 8, 10, 12} // Locrian
    };

    private static final String[] ModernModesNames =
    {"Ionian", "Dorian", "Phrygian", "Lydian", "Mixolydian", "Aeolian", "Locrian"};

    private static final String[] DiatonicKeys =
    {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    private static final int DEFAULTROOT = 36; // Default C

    private int mode;
    protected int diatonicRootKey;
    private static final int DIATONICOCTAVE = -2;

    private final SettableEnumValue settingRoot;
    private final SettableEnumValue settingScale;

    private static final NovationColor WHITE = NovationColor.AMBER_LOW;
    private static final NovationColor BLACK = NovationColor.RED_LOW;
    private static final NovationColor OFF = NovationColor.OFF;
    private static final NovationColor ON = NovationColor.GREEN_FULL;

    public NoteMapDiatonic(MidiOut midiOutPort, SettableRangedValue settingVelocity, SettableEnumValue settingScale,
            SettableEnumValue settingRoot)
    {
        super(midiOutPort, settingVelocity);
        this.settingScale = settingScale;
        this.settingRoot = settingRoot;
        mode = 0;
        diatonicRootKey = 0;

        setRootKey();
    }

    public void drawKeys()
    {
        for (var y = 0; y < 8; y++)
        {
            for (var x = 0; x < 8; x++)
            {
                int key = cellToKey(x, y);

                NovationColor colour;
                if (key != -1)
                {
                    colour = this.isKeyBlack(key) ? BLACK : WHITE;
                }
                else
                {
                    colour = OFF;
                }

                setCellLED(x, y, colour);
            }
        }
    }

    public void turnOnNote(int Note)
    {
        for (var y = 0; y < 8; y++)
        {
            for (var x = 0; x < 8; x++)
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
        for (var y = 0; y < 8; y++)
        {
            for (var x = 0; x < 8; x++)
            {
                var key = this.cellToKey(x, y);
                if (Note == key)
                {
                    setCellLED(x, y, this.isKeyBlack(key) ? BLACK : WHITE);
                }
            }
        }
    }

    public int cellToKey(int x, int y)
    {
        var octave = 7 - y;
        var key = this.rootKey + octave * 12 + ModernModes[this.mode][x];

        if (key >= 0 && key < 128)
            return key;

        return -1;
    }

    public boolean isKeyBlack(int key)
    {
        var k = key % 12;

        return (k == 1 || k == 3 || k == 6 || k == 8 || k == 10);
    }

    public static String[] getAllModes()
    {
        return ModernModesNames;
    }

    public static String[] getAllRoot()
    {
        return DiatonicKeys;
    }

    public String getMode()
    {
        return ModernModesNames[mode];
    }

    public void setMode(final String Mode)
    {
        for (int i = 0; i < 7; i++)
        {
            if (ModernModesNames[i].equals(Mode))
            {
                this.mode = i;
                break;
            }
        }

        setRootKey();
    }

    public String getDiatonicKey()
    {
        return DiatonicKeys[this.diatonicRootKey];
    }

    public void setDiatonicKey(final String Key)
    {
        for (int i = 0; i < 12; i++)
        {
            if (DiatonicKeys[i].equals(Key))
            {
                this.diatonicRootKey = i;
                break;
            }
        }

        setRootKey();
    }

    public boolean canScrollUp()
    {
        return this.diatonicRootKey < 11;
    }

    public void scrollUp()
    {
        diatonicRootKey++;
        settingRoot.set(DiatonicKeys[diatonicRootKey]);
        setRootKey();
    }

    public boolean canScrollDown()
    {
        return this.diatonicRootKey > 0;
    }

    public void scrollDown()
    {
        diatonicRootKey--;
        settingRoot.set(DiatonicKeys[diatonicRootKey]);
        setRootKey();
    }

    private void setRootKey()
    {
        this.rootKey = DEFAULTROOT + diatonicRootKey + 12 * DIATONICOCTAVE;
    }

    public boolean canScrollLeft()
    {
        return this.mode > 0;
    }

    public void scrollLeft()
    {
        this.mode = Math.max(0, this.mode - 1);
    }

    public String previousMode()
    {
        int tmpMode = Math.max(0, this.mode - 1);
        return ModernModesNames[tmpMode];
    }

    public boolean canScrollRight()
    {
        return this.mode < ModernModes.length - 1;
    }

    public void scrollRight()
    {
        this.mode = Math.min(ModernModes.length - 1, this.mode + 1);
    }

    public String sextMode()
    {
        int tmpMode = Math.min(ModernModes.length - 1, this.mode + 1);
        return ModernModesNames[tmpMode];
    }

    public void onMidiDiatonic(final ShortMidiMessage msg)
    {
        switch (msg.getData1())
        {
            case BUTTON_LEFT:
                settingScale.set(previousMode());
                break;

            case BUTTON_RIGHT:
                settingScale.set(sextMode());
                break;

            default:
                break;
        }
    }


    @Override
    public String toString()
    {
        return "Diatonic - " + ModernModesNames[mode] + " (" + getDiatonicKey() + ")";
    }
}
