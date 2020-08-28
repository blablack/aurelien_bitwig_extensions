package com.aurelien;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SettableRangedValue;

import java.util.Arrays;

public class NoteMap
{
    protected int m_rootKey;

    private Boolean m_noteOn[];

    private MidiOut m_midiOutPort;

    private SettableRangedValue m_settingVelocity;

    public NoteMap(MidiOut midiOutPort, SettableRangedValue settingVelocity)
    {
        m_midiOutPort = midiOutPort;
        m_settingVelocity = settingVelocity;
        m_rootKey = 36;
        m_noteOn = new Boolean[128];
        Arrays.fill(m_noteOn, false);
    }

    public int cellToKey(int x, int y)
    {
        // -1 means no key (gap)
        return -1;
    }

    public void DrawKeys()
    {
        for (int y = 0; y < 8; y++)
        {
            for (int x = 0; x < 8; x++)
            {
                setCellLED(x, y, NovationColor.OFF);
            }
        }
    }

    public void setCellLED(int column, int row, NovationColor colour)
    {
        int key = row * 8 + column;

        int p_column = key & 0x7;
        int p_row = key >> 3;
        m_midiOutPort.sendMidi(144, p_row * 16 + p_column, colour.Code());
    }

    public void onMidi(final ShortMidiMessage msg)
    {
        switch (msg.getData1())
        {
            case LaunchpadConstants.BUTTON_ARM:
                VelocityChangeButton(15);
                break;
            case LaunchpadConstants.BUTTON_SOLO:
                VelocityChangeButton(31);
                break;
            case LaunchpadConstants.BUTTON_TRKON:
                VelocityChangeButton(47);
                break;
            case LaunchpadConstants.BUTTON_STOP:
                VelocityChangeButton(63);
                break;
            case LaunchpadConstants.BUTTON_SNDB:
                VelocityChangeButton(79);
                break;
            case LaunchpadConstants.BUTTON_SNDA:
                VelocityChangeButton(95);
                break;
            case LaunchpadConstants.BUTTON_PAN:
                VelocityChangeButton(111);
                break;
            case LaunchpadConstants.BUTTON_VOL:
                VelocityChangeButton(127);
                break;
        }
    }

    private void VelocityChangeButton(int Value)
    {
        m_settingVelocity.setImmediately((double) Value / 127);
    }

    public NovationColor GetVelocityColor(int Vel_plus, int Vel)
    {
        return m_settingVelocity.get() * 127 >= Vel_plus ? NovationColor.AMBER_FULL : m_settingVelocity.get() * 127 >= Vel ? NovationColor.AMBER_LOW : NovationColor.OFF;
    }

    public Boolean canScrollUp()
    {
        return false;
    }

    public void ScrollUp()
    {
    }

    public Boolean canScrollDown()
    {
        return false;
    }

    public void ScrollDown()
    {
    }

    public Boolean canScrollLeft()
    {
        return false;
    }

    public void ScrollLeft()
    {
    }

    public Boolean canScrollRight()
    {
        return false;
    }

    public void ScrollRight()
    {
    }

    public void TurnOnNote(int Note)
    {
    }

    public void TurnOffNote(int Note)
    {
    }

}
