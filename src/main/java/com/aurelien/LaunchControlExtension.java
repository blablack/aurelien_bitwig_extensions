package com.aurelien;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;

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
    private MidiOut midiOutPort;
    private ControllerHost host;

    protected LaunchControlExtension(final LaunchControlExtensionDefinition definition, final ControllerHost host)
    {
        super(definition, host);
    }

    final String[] TEMPLATE_OPTIONS =
    {"User Template 1", "User Template 2", "User Template 3", "User Template 4", "User Template 5", "User Template 6", "User Template 7", "User Template 8", "Transport Mode", "Tracks Mode", "Device Mode"};
    final int DEFAULT_TEMPLATE = 8;
    SettableEnumValue m_settingTemplate;

    @Override
    public void init()
    {
        host = this.getHost();
        midiOutPort = host.getMidiOutPort(0);

        Reset();

        // Activate Flashing: 176+n, 0, 40
        // n (0-7) for the 8 user templates, and (8-15) for the 8 factory templates
        midiOutPort.sendMidi(176 + 8, 0, 40);
        midiOutPort.sendMidi(176 + 9, 0, 40);

        final DocumentState documentState = host.getDocumentState();
        m_settingTemplate = documentState.getEnumSetting("Template", "General", TEMPLATE_OPTIONS, TEMPLATE_OPTIONS[DEFAULT_TEMPLATE]);
        m_settingTemplate.addValueObserver(value -> {
            ChangeCurrentTemplate(value);
        });

        final MidiIn midiInPort = host.getMidiInPort(0);
        CreateNoteInput(midiInPort).setShouldConsumeEvents(true);
        final Transport transport = host.createTransport();

        hardwareSurface = host.createHardwareSurface();

        SetupTransportMode(transport, midiInPort);
        final CursorTrack cursorTrack = SetupTracksMode(host, midiInPort);
        SetupDeviceMode(host, cursorTrack, midiInPort);

        midiInPort.setSysexCallback((final String data) -> onSysex(data));

        host.showPopupNotification("Launch Control Initialized");
    }

    private void SetupTransportMode(final Transport transport, final MidiIn midiInPort)
    {
        final int p_channel = 8;

        transport.isPlaying().markInterested();
        transport.isArrangerRecordEnabled().markInterested();
        transport.isMetronomeEnabled().markInterested();
        transport.playStartPositionInSeconds().markInterested();
        transport.playPositionInSeconds().markInterested();

        final NovationButton playButton = new NovationButton(hardwareSurface, "PLAY_BUTTON", NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_1);
        playButton.SetBinding(transport.playAction());
        playButton.SetColor(hardwareSurface, () -> transport.isPlaying().get() ? NovationColor.GREEN_FLASHING : NovationColor.GREEN_LOW, midiOutPort);

        final NovationButton stopButton = new NovationButton(hardwareSurface, "STOP_BUTTON", NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_2);
        stopButton.SetBinding(transport.stopAction());
        stopButton.SetColor(hardwareSurface, () -> transport.isPlaying().get() ? NovationColor.RED_FULL : transport.playStartPositionInSeconds().get() != transport.playPositionInSeconds().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW,
                midiOutPort);

        final NovationButton recButton = new NovationButton(hardwareSurface, "TRANSPORT_REC_BUTTON", NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_3);
        recButton.SetBinding(transport.recordAction());
        recButton.SetColor(hardwareSurface, () -> transport.isArrangerRecordEnabled().get() ? transport.isPlaying().get() ? NovationColor.RED_FULL : NovationColor.RED_FLASHING : NovationColor.RED_LOW, midiOutPort);

        final NovationButton clickButton = new NovationButton(hardwareSurface, "CLICK_BUTTON", NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_4);
        clickButton.SetBinding(transport.isMetronomeEnabled());
        clickButton.SetColor(hardwareSurface, () -> transport.isMetronomeEnabled().get() ? NovationColor.AMBER_FULL : NovationColor.AMBER_LOW, midiOutPort);
    }

    private CursorTrack SetupTracksMode(final ControllerHost host, final MidiIn midiInPort)
    {
        final int p_channel = 9;

        final TrackBank trackBank = host.createMainTrackBank(2, 0, 0);
        final CursorTrack cursorTrack = host.createCursorTrack("CURSOR_TRACK", "Cursor Track", 0, 0, true);

        for (int i = 0; i < trackBank.getSizeOfBank(); i++)
        {
            final Track track = trackBank.getItemAt(i);

            int p_hwID = -1;

            if (i == 0)
                p_hwID = KNOB_1_UP;
            else
                p_hwID = KNOB_5_UP;
            final Parameter p_vol = track.volume();
            p_vol.markInterested();
            p_vol.setIndication(true);
            final AbsoluteHardwareKnob p_vol_knob = hardwareSurface.createAbsoluteHardwareKnob("TRACK_VOL_KNOB_" + i);
            p_vol_knob.setAdjustValueMatcher(midiInPort.createAbsoluteCCValueMatcher(p_channel, p_hwID));
            p_vol_knob.setBinding(trackBank.getItemAt(i).volume());

            if (i == 0)
                p_hwID = KNOB_1_DOWN;
            else
                p_hwID = KNOB_5_DOWN;
            final Parameter p_pan = track.pan();
            p_pan.markInterested();
            p_pan.setIndication(true);
            final AbsoluteHardwareKnob p_pan_knob = hardwareSurface.createAbsoluteHardwareKnob("TRACK_PAN_KNOB_" + i);
            p_pan_knob.setAdjustValueMatcher(midiInPort.createAbsoluteCCValueMatcher(p_channel, p_hwID));
            p_pan_knob.setBinding(trackBank.getItemAt(i).pan());

            track.exists().markInterested();

            track.arm().markInterested();
            if (i == 0)
                p_hwID = BUTTON_1;
            else
                p_hwID = BUTTON_5;

            final NovationButton recButton = new NovationButton(hardwareSurface, "TRACK_REC_BUTTON_" + p_hwID, NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, p_hwID);
            recButton.SetBinding(track.arm());
            recButton.SetColor(hardwareSurface, () -> track.exists().get() ? track.arm().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW : NovationColor.OFF, midiOutPort);

            track.solo().markInterested();
            if (i == 0)
                p_hwID = BUTTON_2;
            else
                p_hwID = BUTTON_6;
            final NovationButton soloButton = new NovationButton(hardwareSurface, "TRACK_SOLO_BUTTON_" + p_hwID, NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, p_hwID);
            soloButton.SetBinding(track.solo());
            soloButton.SetColor(hardwareSurface, () -> track.exists().get() ? track.solo().get() ? NovationColor.YELLOW_FLASHING : NovationColor.YELLOW_LOW : NovationColor.OFF, midiOutPort);

            track.mute().markInterested();
            if (i == 0)
                p_hwID = BUTTON_3;
            else
                p_hwID = BUTTON_7;
            final NovationButton muteButton = new NovationButton(hardwareSurface, "TRACK_MUTE_BUTTON_" + p_hwID, NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, p_hwID);
            muteButton.SetBinding(track.mute());
            muteButton.SetColor(hardwareSurface, () -> track.exists().get() ? track.mute().get() ? NovationColor.ORANGE : NovationColor.AMBER_LOW : NovationColor.OFF, midiOutPort);
        }

        trackBank.followCursorTrack(cursorTrack);
        trackBank.canScrollBackwards().markInterested();
        trackBank.canScrollForwards().markInterested();
        final HardwareActionBindable scrollPageBackwardsAction = trackBank.scrollBackwardsAction();
        final HardwareActionBindable scrollPageForwardsAction = trackBank.scrollForwardsAction();

        final Application p_appl = host.createApplication();
        p_appl.panelLayout().markInterested();

        final NovationButton upButton = new NovationButton(hardwareSurface, "PREVIOUS_TRACK_BUTTON_UP", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_UP);
        upButton.SetColor(hardwareSurface, () -> p_appl.panelLayout().get().equals("ARRANGE") ? trackBank.canScrollBackwards().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW: NovationColor.OFF, midiOutPort);

        final NovationButton downButton = new NovationButton(hardwareSurface, "NEXT_TRACK_BUTTON_DOWN", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_DOWN);
        downButton.SetColor(hardwareSurface, () -> p_appl.panelLayout().get().equals("ARRANGE") ? trackBank.canScrollForwards().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW: NovationColor.OFF, midiOutPort);

        final NovationButton leftButton = new NovationButton(hardwareSurface, "PREVIOUS_TRACK_BUTTON_LEFT", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_LEFT);
        leftButton.SetColor(hardwareSurface, () -> p_appl.panelLayout().get().equals("MIX") ? trackBank.canScrollBackwards().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW: NovationColor.OFF, midiOutPort);

        final NovationButton rightButton = new NovationButton(hardwareSurface, "NEXT_TRACK_BUTTON_RIGHT", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_RIGHT);
        rightButton.SetColor(hardwareSurface, () -> p_appl.panelLayout().get().equals("MIX") ? trackBank.canScrollForwards().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW: NovationColor.OFF, midiOutPort);

        p_appl.panelLayout().addValueObserver(value -> {
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

        return cursorTrack;
    }

    private void SetupDeviceMode(final ControllerHost host, final CursorTrack cursorTrack, final MidiIn midiInPort)
    {
        final int p_channel = 10;

        cursorTrack.hasPrevious().markInterested();
        final NovationButton upDeviceButton = new NovationButton(hardwareSurface, "DEVICE_PREVIOUS_TRACK_BUTTON_UP", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_UP);
        upDeviceButton.SetBinding(cursorTrack.selectPreviousAction());
        upDeviceButton.SetColor(hardwareSurface, () -> cursorTrack.hasPrevious().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW, midiOutPort);

        cursorTrack.hasNext().markInterested();
        final NovationButton downDeviceButton = new NovationButton(hardwareSurface, "DEVICE_NEXT_TRACK_BUTTON_DOWN", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_DOWN);
        downDeviceButton.SetBinding(cursorTrack.selectNextAction());
        downDeviceButton.SetColor(hardwareSurface, () -> cursorTrack.hasNext().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW, midiOutPort);

        DeviceBank p_deviceBank = cursorTrack.createDeviceBank(2);
        PinnableCursorDevice p_cursor = cursorTrack.createCursorDevice("CURSOR_DEVICE", "Cursor Device", 0, CursorDeviceFollowMode.FOLLOW_SELECTION);
        p_cursor.position().addValueObserver(value -> p_deviceBank.scrollIntoView(value), 0);

        p_cursor.hasPrevious().markInterested();
        final NovationButton leftButton = new NovationButton(hardwareSurface, "DEVICE_BUTTON_LEFT", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_LEFT);
        leftButton.SetBinding(p_cursor.selectPreviousAction());
        leftButton.SetColor(hardwareSurface, () -> p_cursor.hasPrevious().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW, midiOutPort);

        p_cursor.hasNext().markInterested();
        final NovationButton rightButton = new NovationButton(hardwareSurface, "DEVICE_BUTTON_RIGHT", NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel, BUTTON_RIGHT);
        rightButton.SetBinding(p_cursor.selectNextAction());
        rightButton.SetColor(hardwareSurface, () -> p_cursor.hasNext().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW, midiOutPort);

        for (int i = 0; i < p_deviceBank.getSizeOfBank(); i++)
        {
            Device p_oneDevice = p_deviceBank.getDevice(i);
            CursorRemoteControlsPage remoteControlsBank = p_oneDevice.createCursorRemoteControlsPage(8);
            for (int j = 0; j < remoteControlsBank.getParameterCount(); j++)
            {
                RemoteControl p_remote = remoteControlsBank.getParameter(j);
                p_remote.markInterested();
                p_remote.setIndication(true);

                int p_knobID = -1;

                if (i == 0)
                {
                    if (j < 4)
                        p_knobID = KNOB_1_UP + j;
                    else
                        p_knobID = KNOB_1_DOWN + j - 4;
                }
                else
                {
                    if (j < 4)
                        p_knobID = KNOB_5_UP + j;
                    else
                        p_knobID = KNOB_5_DOWN + j - 4;
                }

                final AbsoluteHardwareKnob p_vol_knob = hardwareSurface.createAbsoluteHardwareKnob("REMOTE_CTRL_KNOB_" + i + "_" + j);
                p_vol_knob.setAdjustValueMatcher(midiInPort.createAbsoluteCCValueMatcher(p_channel, p_knobID));
                p_remote.addBinding(p_vol_knob);
            }

            int p_buttonShift = 0;
            if (i == 1)
                p_buttonShift = 16;

            p_oneDevice.exists().markInterested();

            p_oneDevice.isEnabled().markInterested();
            final NovationButton muteButton = new NovationButton(hardwareSurface, "DEVICE_MUTE_BUTTON_" + i, NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_1 + p_buttonShift);
            muteButton.SetBinding(p_oneDevice.isEnabled().toggleAction());
            muteButton.SetColor(hardwareSurface, () -> p_oneDevice.exists().get() ? p_oneDevice.isEnabled().get() ? NovationColor.GREEN_FULL : NovationColor.RED_FULL : NovationColor.OFF, midiOutPort);

            p_oneDevice.isRemoteControlsSectionVisible().markInterested();
            final NovationButton showRemote = new NovationButton(hardwareSurface, "DEVICE_SHOW_REMOTE_BUTTON_" + i, NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_2 + p_buttonShift);
            showRemote.SetBinding(p_oneDevice.isRemoteControlsSectionVisible().toggleAction());
            showRemote.SetColor(hardwareSurface, () -> p_oneDevice.exists().get() ? p_oneDevice.isRemoteControlsSectionVisible().get() ? NovationColor.GREEN_FULL : NovationColor.RED_FULL : NovationColor.OFF, midiOutPort);

            remoteControlsBank.hasPrevious().markInterested();
            final NovationButton upButton = new NovationButton(hardwareSurface, "DEVICE_BUTTON_REMOTE_UP_" + i, NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_3 + p_buttonShift);
            upButton.SetBinding(remoteControlsBank.selectPreviousAction());
            upButton.SetColor(hardwareSurface, () -> p_oneDevice.exists().get() ? remoteControlsBank.hasPrevious().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW : NovationColor.OFF, midiOutPort);

            remoteControlsBank.hasNext().markInterested();
            final NovationButton downButton = new NovationButton(hardwareSurface, "DEVICE_BUTTON_REMOTE_DOWN_" + i, NovationButton.NovationButtonType.NoteOn, midiInPort, CHANNEL_ROOT_BUTTONS_NUMBER, p_channel, BUTTON_4 + p_buttonShift);
            downButton.SetBinding(remoteControlsBank.selectNextAction());
            downButton.SetColor(hardwareSurface, () -> p_oneDevice.exists().get() ? remoteControlsBank.hasNext().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW : NovationColor.OFF, midiOutPort);
        }
    }

    private void onSysex(final String data)
    {
        final int p_page = Integer.parseInt(data.substring(15, 16), 16);

        if (p_page <= 10)
            m_settingTemplate.set(TEMPLATE_OPTIONS[p_page]);
        else
            ChangeCurrentTemplate(TEMPLATE_OPTIONS[DEFAULT_TEMPLATE]);
    }

    private void ChangeCurrentTemplate(final String Template)
    {
        host.showPopupNotification(Template);

        int p_index = -1;
        for (final String p_tempTemplate : TEMPLATE_OPTIONS)
        {
            p_index++;
            if (Template.equals(p_tempTemplate))
                break;
        }

        midiOutPort.sendSysex("F0002029020A77" + String.format("%02X", p_index) + "F7");

        hardwareSurface.invalidateHardwareOutputState();
    }

    @Override
    public void flush()
    {
        if (this.hardwareSurface != null)
            this.hardwareSurface.updateHardware();
    }

    private void Reset()
    {
        midiOutPort.sendSysex(SYSEX_RESET);

        final NovationColor p_off = NovationColor.OFF;
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
    }

    private NoteInput CreateNoteInput(final MidiIn midiInPort)
   {
      return midiInPort.createNoteInput("Launch Control", "B0????", "B1????", "B2????", "B3????", "B4????", "B5????", "B6????", "B7????");
   }

    @Override
    public void exit()
    {
        Reset();

        host.showPopupNotification("Launchpad Exited");
    }
}
