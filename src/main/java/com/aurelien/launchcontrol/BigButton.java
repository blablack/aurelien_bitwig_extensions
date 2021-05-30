package com.aurelien.launchcontrol;

import com.aurelien.basic.NovationButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;

public class BigButton extends NovationButton
{
    public BigButton(HardwareSurface hardwareSurface, String ID, MidiIn midiInPort, int Channel, int Note)
    {
        super(hardwareSurface, ID, NovationButton.NovationButtonType.NOTEON, midiInPort,
                LaunchControlConstants.CHANNEL_ROOT_BUTTONS_NUMBER, Channel, Note);
    }

}
