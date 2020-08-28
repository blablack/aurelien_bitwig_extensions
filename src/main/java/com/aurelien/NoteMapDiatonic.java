package com.aurelien;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.SettableEnumValue;

public class NoteMapDiatonic extends NoteMap
{
    final private Integer[][] ModernModes =
    {
            {0, 2, 4, 5, 7, 9, 11, 12}, // Ionian
            {0, 2, 3, 5, 7, 9, 10, 12}, // Dorian
            {0, 1, 3, 5, 7, 8, 10, 12}, // Phrygian
            {0, 2, 4, 6, 7, 9, 11, 12}, // Lydian
            {0, 2, 4, 5, 7, 9, 10, 12}, // Mixolydian
            {0, 2, 3, 5, 7, 8, 10, 12}, // Aeolian
            {0, 1, 3, 5, 6, 8, 10, 12} // Locrian
    };

    final static private String[] ModernModesNames =
    {"Ionian", "Dorian", "Phrygian", "Lydian", "Mixolydian", "Aeolian", "Locrian"};

    final static private String[] DiatonicKeys =
    {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    final private int defaultRoot = 36; // Default C

    private int mode;
    protected int diatonicRootKey;
    private int diatonicOctave;

    private SettableEnumValue m_settingRoot;
    private SettableEnumValue m_settingScale;

    final private NovationColor white;
    final private NovationColor black;
    final private NovationColor off;
    final private NovationColor on;

    public NoteMapDiatonic(MidiOut midiOutPort, SettableRangedValue settingVelocity, SettableEnumValue settingScale, SettableEnumValue settingRoot)
    {
        super(midiOutPort, settingVelocity);
        m_settingScale = settingScale;
        m_settingRoot = settingRoot;
        mode = 0;
        diatonicRootKey = 0;
        diatonicOctave = -2;

        this.m_rootKey = defaultRoot + diatonicRootKey + 12 * diatonicOctave;

        white = NovationColor.AMBER_LOW;
        black = NovationColor.RED_LOW;
        off = NovationColor.OFF;
        on = NovationColor.GREEN_FULL;
    }

    @Override
    public void DrawKeys()
    {
        for (var y = 0; y < 8; y++)
        {
            for (var x = 0; x < 8; x++)
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
        for (var y = 0; y < 8; y++)
        {
            for (var x = 0; x < 8; x++)
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
        for (var y = 0; y < 8; y++)
        {
            for (var x = 0; x < 8; x++)
            {
                var key = this.cellToKey(x, y);
                if (Note == key)
                {
                    setCellLED(x, y, this.isKeyBlack(key) ? black : white);
                }
            }
        }
    }

    public int cellToKey(int x, int y)
    {
        var octave = 7 - y;
        var key = this.m_rootKey + octave * 12 + ModernModes[this.mode][x];

        if (key >= 0 && key < 128)
            return key;

        return -1;
    }

    public Boolean isKeyBlack(int key)
    {
        var k = key % 12;

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

    public static String[] GetAllModes()
    {
        return ModernModesNames;
    }

    public static String[] GetAllRoot()
    {
        return DiatonicKeys;
    }

    public String getMode()
    {
        return ModernModesNames[mode];
    }

    public void SetMode(final String Mode)
    {
        for (int i = 0; i < 7; i++)
        {
            if (ModernModesNames[i].equals(Mode))
            {
                this.mode = i;
                break;
            }
        }

        this.m_rootKey = defaultRoot + diatonicRootKey + 12 * diatonicOctave;
    }

    public String getDiatonicKey()
    {
        return DiatonicKeys[this.diatonicRootKey];
    }

    public void SetDiatonicKey(final String Key)
    {
        for (int i = 0; i < 12; i++)
        {
            if (DiatonicKeys[i].equals(Key))
            {
                this.diatonicRootKey = i;
                break;
            }
        }

        this.m_rootKey = defaultRoot + diatonicRootKey + 12 * diatonicOctave;
    }

    public String NextInKey()
    {
        int p_tmpDiatonicKey = this.diatonicRootKey;
        if (p_tmpDiatonicKey < 11)
            p_tmpDiatonicKey++;
        else
            p_tmpDiatonicKey = 0;

        return DiatonicKeys[p_tmpDiatonicKey];
    }

    @Override
    public Boolean canScrollUp()
    {
        return diatonicOctave < 0;
    }

    @Override
    public void ScrollUp()
    {
        diatonicOctave++;
        SetRootKey();
    }

    @Override
    public Boolean canScrollDown()
    {
        return this.diatonicOctave > -3;
    }

    @Override
    public void ScrollDown()
    {
        diatonicOctave--;
        SetRootKey();
    }

    private void SetRootKey()
    {
        this.m_rootKey = defaultRoot + diatonicRootKey + 12 * diatonicOctave;
    }

    @Override
    public Boolean canScrollLeft()
    {
        return this.mode > 0;
    }

    @Override
    public void ScrollLeft()
    {
        this.mode = Math.max(0, this.mode - 1);
    }

    public String PreviousMode()
    {
        int tmpMode = Math.max(0, this.mode - 1);
        return ModernModesNames[tmpMode];
    }

    @Override
    public Boolean canScrollRight()
    {
        return this.mode < ModernModes.length - 1;
    }

    @Override
    public void ScrollRight()
    {
        this.mode = Math.min(ModernModes.length - 1, this.mode + 1);
    }

    public String NextMode()
    {
        int tmpMode = Math.min(ModernModes.length - 1, this.mode + 1);
        return ModernModesNames[tmpMode];
    }

    public void onMidiDiatonic(final ShortMidiMessage msg)
    {
        switch (msg.getData1())
        {
           case LaunchpadConstants.BUTTON_LEFT:
              m_settingScale.set(PreviousMode());
              break;

           case LaunchpadConstants.BUTTON_RIGHT:
              m_settingScale.set(NextMode());
              break;

           case LaunchpadConstants.BUTTON_SESSION:
              m_settingRoot.set(NextInKey());
              break;
        }
    }


    @Override
    public String toString()
    {
        return "Diatonic - " + ModernModesNames[mode] + " (" + getDiatonicKey() + ")";
    }
}
