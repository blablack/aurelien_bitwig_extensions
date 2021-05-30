package com.aurelien.basic;

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
        NOTEON, CC, OFF
    }

    private final String id;
    private final HardwareButton hwButton;

    private int midiMessageChannelRoot;

    private int note;
    private int channel;

    public NovationButton(HardwareSurface hardwareSurface, String ID, NovationButtonType Type, MidiIn midiInPort,
            int MidiMessageChannelRoot, int Channel, int Note)
    {
        id = ID;
        hwButton = hardwareSurface.createHardwareButton(id);

        note = Note;
        channel = Channel;
        midiMessageChannelRoot = MidiMessageChannelRoot;

        switch (Type)
        {
            case NOTEON:
                hwButton.pressedAction().setActionMatcher(midiInPort.createNoteOnActionMatcher(channel, note));
                break;
            case CC:
                hwButton.pressedAction().setActionMatcher(midiInPort.createCCActionMatcher(channel, note, 127));
                break;
            case OFF:
                break;
        }
    }

    public String getID()
    {
        return id;
    }

    public void setBinding(HardwareBindable hardwareBindable)
    {
        hwButton.pressedAction().setBinding(hardwareBindable);
    }

    public void clearBinding()
    {
        hwButton.pressedAction().clearBindings();
    }

    public void setColor(HardwareSurface hardwareSurface, Supplier<? extends InternalHardwareLightState> valueSupplier,
            MidiOut midiOutPort)
    {
        MultiStateHardwareLight p_hwButtonLight = hardwareSurface.createMultiStateHardwareLight(id + "_LIGHT");
        hwButton.setBackgroundLight(p_hwButtonLight);

        p_hwButtonLight.state().setValueSupplier(valueSupplier);
        p_hwButtonLight.state().onUpdateHardware(isOn -> {
            midiOutPort.sendMidi(midiMessageChannelRoot + channel, note, ((NovationColor) isOn).code());
        });
    }

    public void clearColor()
    {
        hwButton.setBackgroundLight(null);
    }
}
