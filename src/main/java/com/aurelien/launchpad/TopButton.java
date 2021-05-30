package com.aurelien.launchpad;

import com.aurelien.basic.NovationButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;

public class TopButton extends NovationButton
{
    public TopButton(HardwareSurface hardwareSurface, String ID, MidiIn midiInPort, int Note)
    {
        super(hardwareSurface, ID, NovationButton.NovationButtonType.OFF, midiInPort,
                LaunchpadConstants.CHANNEL_ROOT_BUTTONS_TOP, 0, Note);
    }

}
