package com.aurelien.launchcontrol;

import com.aurelien.basic.NovationButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;

public class SmallButton extends NovationButton
{
    public SmallButton(HardwareSurface hardwareSurface, String ID, MidiIn midiInPort, int Channel, int Note)
    {
        super(hardwareSurface, ID, NovationButton.NovationButtonType.CC, midiInPort,
                LaunchControlConstants.CHANNEL_ROOT_BUTTONS_ARROW, Channel, Note);
    }

}
