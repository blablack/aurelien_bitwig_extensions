package com.aurelien;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;

public class LaunchControlExtension extends ControllerExtension
{
    final int BUTTON_1 = 9;
    final int BUTTON_2 = 10;
    final int BUTTON_3 = 11;
    final int BUTTON_4 = 12;
    final int BUTTON_5 = 25;
    final int BUTTON_6 = 26;
    final int BUTTON_7 = 27;
    final int BUTTON_8 = 28;

    final int KNOB_1_UP = 21;
    final int KNOB_1_DOWN = 41;
    final int KNOB_2_UP = 22;
    final int KNOB_2_DOWN = 42;
    final int KNOB_3_UP = 23;
    final int KNOB_3_DOWN = 43;
    final int KNOB_4_UP = 24;
    final int KNOB_4_DOWN = 44;
    final int KNOB_5_UP = 25;
    final int KNOB_5_DOWN = 45;
    final int KNOB_6_UP = 26;
    final int KNOB_6_DOWN = 46;
    final int KNOB_7_UP = 27;
    final int KNOB_7_DOWN = 47;
    final int KNOB_8_UP = 28;
    final int KNOB_8_DOWN = 48;

    final int BUTTON_UP = 114;
    final int BUTTON_DOWN = 115;
    final int BUTTON_LEFT = 116;
    final int BUTTON_RIGHT = 117;

    final int CHANNEL_ROOT_BUTTONS_NUMBER = 144;
    final int CHANNEL_ROOT_BUTTONS_ARROW = 176;

    final String SYSEX_RESET = "F0002029020A7708F7";

    private HardwareSurface hardwareSurface;

    protected LaunchControlExtension(final LaunchControlExtensionDefinition definition, final ControllerHost host)
    {
        super(definition, host);
    }

    @Override
    public void init()
    {
        final ControllerHost host = this.getHost();

        final MidiIn midiInPort = host.getMidiInPort(0);
        final MidiOut midiOutPort = host.getMidiOutPort(0);
        final Transport transport = host.createTransport();

        Reset(midiOutPort);

        // Activate Flashing: 176+n, 0, 40
        // n (0-7) for the 8 user templates, and (8-15) for the 8 factory templates
        midiOutPort.sendMidi(184, 0, 40);

        hardwareSurface = host.createHardwareSurface();

        SetupTransportMode(transport, hardwareSurface, midiInPort, midiOutPort);
        SetupTracksMode(host, hardwareSurface, midiInPort, midiOutPort);

        midiInPort.setSysexCallback((String data) -> onSysex(data));

        host.showPopupNotification("Launch Control Initialized");
    }

    private void SetupTracksMode(ControllerHost host, HardwareSurface hardwareSurface, MidiIn midiInPort, MidiOut midiOutPort)
    {
        int p_channel = 9;

        TrackBank trackBank = host.createMainTrackBank(2, 0, 0);
        CursorTrack cursorTrack = host.createCursorTrack("CURSOR_TRACK", "Cursor Track", 0, 0, true);

        for (int i = 0; i < trackBank.getSizeOfBank(); i++)
        {
            Track track = trackBank.getItemAt(i);

            int p_hwID = -1;

            if (i == 0)
                p_hwID = KNOB_1_UP;
            else
                p_hwID = KNOB_5_UP;
            Parameter p_vol = track.volume();
            p_vol.markInterested();
            p_vol.setIndication(true);
            final AbsoluteHardwareKnob p_vol_knob = hardwareSurface.createAbsoluteHardwareKnob("TRACK_VOL_KNOB_" + i);
            p_vol_knob.setAdjustValueMatcher(midiInPort.createAbsoluteCCValueMatcher(p_channel, p_hwID));
            p_vol_knob.setBinding(trackBank.getItemAt(i).volume());

            if (i == 0)
                p_hwID = KNOB_1_DOWN;
            else
                p_hwID = KNOB_5_DOWN;
            Parameter p_pan = track.pan();
            p_pan.markInterested();
            p_pan.setIndication(true);
            final AbsoluteHardwareKnob p_pan_knob = hardwareSurface.createAbsoluteHardwareKnob("TRACK_PAN_KNOB_" + i);
            p_pan_knob.setAdjustValueMatcher(midiInPort.createAbsoluteCCValueMatcher(p_channel, p_hwID));
            p_pan_knob.setBinding(trackBank.getItemAt(i).pan());

            track.arm().markInterested();
            if (i == 0)
                p_hwID = BUTTON_1;
            else
                p_hwID = BUTTON_5;
            NovationButton recButton = new NovationButton(hardwareSurface, "TRACK_REC_BUTTON_" + p_hwID, NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, p_hwID);
            recButton.SetBinding(track.arm());
            recButton.SetColor(hardwareSurface, () -> track.arm().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW, midiOutPort);

            track.solo().markInterested();
            if (i == 0)
                p_hwID = BUTTON_2;
            else
                p_hwID = BUTTON_6;
            NovationButton soloButton = new NovationButton(hardwareSurface, "TRACK_SOLO_BUTTON_" + p_hwID, NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, p_hwID);
            soloButton.SetBinding(track.solo());
            soloButton.SetColor(hardwareSurface, () -> track.solo().get() ? NovationColor.YELLOW_FULL : NovationColor.YELLOW_LOW, midiOutPort);

            track.mute().markInterested();
            if (i == 0)
                p_hwID = BUTTON_3;
            else
                p_hwID = BUTTON_7;
            NovationButton muteButton = new NovationButton(hardwareSurface, "TRACK_MUTE_BUTTON_" + p_hwID, NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, p_hwID);
            muteButton.SetBinding(track.mute());
            muteButton.SetColor(hardwareSurface, () -> track.mute().get() ? NovationColor.AMBER_FULL : NovationColor.AMBER_LOW, midiOutPort);
        }

        trackBank.followCursorTrack(cursorTrack);
        trackBank.canScrollBackwards().markInterested();
        trackBank.canScrollForwards().markInterested();
        HardwareActionBindable scrollPageBackwardsAction = trackBank.scrollPageBackwardsAction();
        HardwareActionBindable scrollPageForwardsAction = trackBank.scrollPageForwardsAction();

        Application p_appl = host.createApplication();
        p_appl.panelLayout().markInterested();

        NovationButton upButton = new NovationButton(hardwareSurface, "PREVIOUS_TRACK_BUTTON_UP", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_UP);
        upButton.SetColor(hardwareSurface, () -> p_appl.panelLayout().get().equals("ARRANGE") && trackBank.canScrollBackwards().get() ? NovationColor.RED_FULL : NovationColor.OFF, midiOutPort);

        NovationButton downButton = new NovationButton(hardwareSurface, "NEXT_TRACK_BUTTON_DOWN", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_DOWN);
        downButton.SetColor(hardwareSurface, () -> p_appl.panelLayout().get().equals("ARRANGE") && trackBank.canScrollForwards().get() ? NovationColor.RED_FULL : NovationColor.OFF, midiOutPort);

        NovationButton leftButton = new NovationButton(hardwareSurface, "PREVIOUS_TRACK_BUTTON_LEFT", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_LEFT);
        leftButton.SetColor(hardwareSurface, () -> p_appl.panelLayout().get().equals("MIX") && trackBank.canScrollBackwards().get() ? NovationColor.RED_FULL : NovationColor.OFF, midiOutPort);

        NovationButton rightButton = new NovationButton(hardwareSurface, "NEXT_TRACK_BUTTON_RIGHT", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_RIGHT);
        rightButton.SetColor(hardwareSurface, () -> p_appl.panelLayout().get().equals("MIX") && trackBank.canScrollForwards().get() ? NovationColor.RED_FULL : NovationColor.OFF, midiOutPort);


        p_appl.panelLayout().addValueObserver(value -> {
            //this.getHost().println(value);
            switch (value)
            {
                case "ARRANGE":
                    upButton.SetBinding(scrollPageBackwardsAction);
                    downButton.SetBinding(scrollPageForwardsAction);

                    leftButton.ClearBinding();
                    rightButton.ClearBinding();
                    break;
                case "MIX":
                    leftButton.SetBinding(scrollPageBackwardsAction);
                    rightButton.SetBinding(scrollPageForwardsAction);

                    upButton.ClearBinding();
                    downButton.ClearBinding();
                    break;
                case "EDIT":
                    leftButton.ClearBinding();
                    rightButton.ClearBinding();
                    upButton.ClearBinding();
                    downButton.ClearBinding();
                    break;
            }
        });
    }

    private void SetupTransportMode(Transport transport, HardwareSurface hardwareSurface, MidiIn midiInPort, MidiOut midiOutPort)
    {
        int p_channel = 8;

        transport.isPlaying().markInterested();
        transport.isArrangerRecordEnabled().markInterested();
        transport.isMetronomeEnabled().markInterested();
        transport.playStartPositionInSeconds().markInterested();
        transport.playPositionInSeconds().markInterested();


        NovationButton playButton = new NovationButton(hardwareSurface, "PLAY_BUTTON", NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_1);
        playButton.SetBinding(transport.playAction());
        playButton.SetColor(hardwareSurface, () -> transport.isPlaying().get() ? NovationColor.GREEN_FLASHING : NovationColor.GREEN_LOW, midiOutPort);

        NovationButton stopButton = new NovationButton(hardwareSurface, "STOP_BUTTON", NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_2);
        stopButton.SetBinding(transport.stopAction());
        stopButton.SetColor(hardwareSurface, () -> transport.isPlaying().get() ? NovationColor.RED_FULL : transport.playStartPositionInSeconds().get() != transport.playPositionInSeconds().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW,
                midiOutPort);

        NovationButton recButton = new NovationButton(hardwareSurface, "TRANSPORT_REC_BUTTON", NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_3);
        recButton.SetBinding(transport.recordAction());
        recButton.SetColor(hardwareSurface, () -> transport.isArrangerRecordEnabled().get() ? transport.isPlaying().get() ? NovationColor.RED_FULL : NovationColor.RED_FLASHING : NovationColor.RED_LOW, midiOutPort);

        NovationButton clickButton = new NovationButton(hardwareSurface, "CLICK_BUTTON", NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_4);
        clickButton.SetBinding(transport.isMetronomeEnabled());
        clickButton.SetColor(hardwareSurface, () -> transport.isMetronomeEnabled().get() ? NovationColor.AMBER_FULL : NovationColor.AMBER_LOW, midiOutPort);
    }

    private void onSysex(final String data)
    {
        //this.getHost().println("onSysex: " + data);

        int p_page = Integer.parseInt(data.substring(15, 16), 16);
        switch (p_page)
        {
            case 0:
                getHost().showPopupNotification("Control Device");
                break;
            case 8:
                getHost().showPopupNotification("Transport Mode");
                break;
            case 9:
                getHost().showPopupNotification("Tracks Mode");
                break;
            default:
                getHost().showPopupNotification("Template not supported");
        }
    }

    @Override
    public void flush()
    {
        if (this.hardwareSurface != null)
            this.hardwareSurface.updateHardware();
    }

    private void Reset(MidiOut midiOutPort )
    {
        //midiOutPort.sendSysex(SYSEX_RESET);
    }

    @Override
    public void exit()
    {
        final ControllerHost host = this.getHost();
        final MidiOut midiOutPort = host.getMidiOutPort(0);
        
        Reset(midiOutPort);
        
        NovationColor p_off = NovationColor.OFF;
        for (int i = 0; i < 16; i++)
        {
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_1, p_off.Code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_2, p_off.Code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_3, p_off.Code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_4, p_off.Code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_5, p_off.Code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_6, p_off.Code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_7, p_off.Code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_8, p_off.Code());

            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_ARROW + i, BUTTON_UP, p_off.Code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_ARROW + i, BUTTON_DOWN, p_off.Code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_ARROW + i, BUTTON_LEFT, p_off.Code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_ARROW + i, BUTTON_RIGHT, p_off.Code());
        }

        getHost().showPopupNotification("Launchpad Exited");
    }
}