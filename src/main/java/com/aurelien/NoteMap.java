package com.aurelien;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;

import java.util.Arrays;

public class NoteMap
{
    protected int rootKey;

    Boolean noteOn[];

    ControllerHost m_host;
    MidiOut m_midiOutPort;

    public NoteMap(ControllerHost host, MidiOut midiOutPort)
    {
        m_host = host;
        m_midiOutPort = midiOutPort;
        rootKey = 36;
        noteOn = new Boolean[128];
        Arrays.fill(noteOn, false);
    }

    public int cellToKey(int x, int y)
    {
        // -1 means no key (gap)
        return -1;
    }

    public void DrawKeys()
    {
        for (var y = 0; y < 8; y++)
        {
            for (var x = 0; x < 8; x++)
            {
                setCellLED(x, y, NovationColor.OFF);
            }
        }
    }

    public void TurnOnNote(int Note)
    {
    }
    
    public void TurnOffNote(int Note)
    {
    }

    public void setCellLED(int column, int row, NovationColor colour)
    {
        int key = row * 8 + column;

        var p_column = key & 0x7;
        var p_row = key >> 3;
        m_midiOutPort.sendMidi(144, p_row * 16 + p_column, colour.Code());
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
}
