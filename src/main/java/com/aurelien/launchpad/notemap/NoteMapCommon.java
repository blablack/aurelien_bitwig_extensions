package com.aurelien.launchpad.notemap;

import static com.aurelien.launchpad.LaunchpadConstants.*;

import com.aurelien.basic.NovationColor;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SettableRangedValue;

import java.util.Arrays;

public class NoteMapCommon
{
    protected int rootKey;

    private boolean[] noteOn;

    private MidiOut midiOutPort;

    private SettableRangedValue settingVelocity;

    public NoteMapCommon(MidiOut midiOutPort, SettableRangedValue settingVelocity)
    {
        this.midiOutPort = midiOutPort;
        this.settingVelocity = settingVelocity;
        rootKey = 36;
        noteOn = new boolean[128];
        Arrays.fill(noteOn, false);
    }

    public void setCellLED(int column, int row, NovationColor colour)
    {
        int key = row * 8 + column;

        int p_column = key & 0x7;
        int p_row = key >> 3;
        midiOutPort.sendMidi(144, p_row * 16 + p_column, colour.code());
    }

    public void onMidi(final ShortMidiMessage msg)
    {
        switch (msg.getData1())
        {
            case BUTTON_ARM:
                velocityChangeButton(15);
                break;
            case BUTTON_SOLO:
                velocityChangeButton(31);
                break;
            case BUTTON_TRKON:
                velocityChangeButton(47);
                break;
            case BUTTON_STOP:
                velocityChangeButton(63);
                break;
            case BUTTON_SNDB:
                velocityChangeButton(79);
                break;
            case BUTTON_SNDA:
                velocityChangeButton(95);
                break;
            case BUTTON_PAN:
                velocityChangeButton(111);
                break;
            case BUTTON_VOL:
                velocityChangeButton(127);
                break;
            default:
                break;
        }
    }

    private void velocityChangeButton(int Value)
    {
        settingVelocity.setImmediately((double) Value / 127);
    }

    public NovationColor getVelocityColor(int Vel_plus, int Vel)
    {
        if (settingVelocity.get() * 127 >= Vel_plus)
            return NovationColor.AMBER_FULL;
        else if (settingVelocity.get() * 127 >= Vel)
            return NovationColor.AMBER_LOW;
        else
            return NovationColor.OFF;
    }


}
