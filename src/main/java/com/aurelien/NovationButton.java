package com.aurelien;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.HardwareBindable;
import java.util.function.Supplier;

public class NovationButton
{
    public enum NovationButtonType
    {
        NoteOn, CC
    }

    final private String m_id;
    final private HardwareButton m_hwButton;

    private int m_midiMessageChannelRoot;

    private int m_note;
    private int m_channel;

    public NovationButton(HardwareSurface hardwareSurface, String ID, NovationButtonType Type, MidiIn midiInPort, int MidiMessageChannelRoot, int Channel, int Note)
    {
        m_id = ID;
        m_hwButton = hardwareSurface.createHardwareButton(m_id);

        m_note = Note;
        m_channel = Channel;
        m_midiMessageChannelRoot = MidiMessageChannelRoot;

        switch (Type)
        {
            case NoteOn:
                m_hwButton.pressedAction().setActionMatcher(midiInPort.createNoteOnActionMatcher(m_channel, m_note));
                break;
            case CC:
                m_hwButton.pressedAction().setActionMatcher(midiInPort.createCCActionMatcher(m_channel, m_note, 127));
                break;
        }
    }

    public void SetBinding(HardwareBindable hardwareBindable)
    {
        m_hwButton.pressedAction().setBinding(hardwareBindable);
    }

    public void AddBinding(HardwareBindable hardwareBindable)
    {
        m_hwButton.pressedAction().addBinding(hardwareBindable);
    }

    public void ClearBinding()
    {
        m_hwButton.pressedAction().clearBindings();
    }

    public void SetColor(HardwareSurface hardwareSurface, Supplier<? extends InternalHardwareLightState> valueSupplier, MidiOut midiOutPort)
    {
        MultiStateHardwareLight p_hwButtonLight = hardwareSurface.createMultiStateHardwareLight(m_id + "_LIGHT");
        m_hwButton.setBackgroundLight(p_hwButtonLight);

        p_hwButtonLight.state().setValueSupplier(valueSupplier);
        p_hwButtonLight.state().onUpdateHardware(isOn -> {
            midiOutPort.sendMidi(m_midiMessageChannelRoot + m_channel, m_note, ((NovationColor) isOn).Code());
        });
    }

    public void SetColor(MidiOut midiOutPort, NovationColor color)
    {
        midiOutPort.sendMidi(m_midiMessageChannelRoot + m_channel, m_note, color.Code());
    }
}
