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
    final private String m_id;
    final private HardwareButton m_playButton;

    private int m_midiMessageChannelRoot;

    private int m_note;
    private int m_channel;

    public NovationButton(HardwareSurface hardwareSurface, String ID)
    {
        m_id = ID;
        m_playButton = hardwareSurface.createHardwareButton(m_id);
    }

    public void NoteOn(MidiIn midiInPort, int MidiMessageChannelRoot, int Channel, int Note, HardwareBindable hardwareBindable)
    {
        m_note = Note;
        m_channel = Channel;
        m_midiMessageChannelRoot = MidiMessageChannelRoot;

        m_playButton.pressedAction().setActionMatcher(midiInPort.createNoteOnActionMatcher(Channel, Note));
        m_playButton.pressedAction().setBinding(hardwareBindable);
    }

    public void CCAction(MidiIn midiInPort, int MidiMessageChannelRoot, int Channel, int Note, HardwareBindable hardwareBindable)
    {
        m_note = Note;
        m_channel = Channel;
        m_midiMessageChannelRoot = MidiMessageChannelRoot;

        m_playButton.pressedAction().setActionMatcher(midiInPort.createCCActionMatcher(Channel, Note, 127));
        m_playButton.pressedAction().setBinding(hardwareBindable);
    }

    public void SetColor(HardwareSurface hardwareSurface, Supplier<? extends InternalHardwareLightState> valueSupplier, MidiOut midiOutPort)
    {
        MultiStateHardwareLight m_playButtonLight = hardwareSurface.createMultiStateHardwareLight(m_id + "_LIGHT");
        m_playButton.setBackgroundLight(m_playButtonLight);

        m_playButtonLight.state().setValueSupplier(valueSupplier);
        m_playButtonLight.state().onUpdateHardware(isOn -> {
            midiOutPort.sendMidi(m_midiMessageChannelRoot + m_channel, m_note, ((NovationColor) isOn).Code());
        });
    }
}
