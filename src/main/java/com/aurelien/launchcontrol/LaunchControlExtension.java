package com.aurelien.launchcontrol;

import static com.aurelien.launchcontrol.LaunchControlConstants.*;

import com.aurelien.basic.NovationButton;
import com.aurelien.basic.NovationColor;

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
    private HardwareSurface hardwareSurface;
    private MidiOut midiOutPort;
    private ControllerHost host;

    static final String[] TEMPLATE_OPTIONS =
    {"User Template 1", "User Template 2", "User Template 3", "User Template 4", "User Template 5", "User Template 6",
            "User Template 7", "User Template 8", "Transport Mode", "Tracks Mode", "Single Device Mode", "2 Devices Mode"};
    static final int DEFAULT_TEMPLATE = 8;
    private SettableEnumValue settingTemplate;

    protected LaunchControlExtension(final LaunchControlExtensionDefinition definition, final ControllerHost host)
    {
        super(definition, host);
    }

    @Override
    public void init()
    {
        host = this.getHost();
        midiOutPort = host.getMidiOutPort(0);

        reset();

        // Activate Flashing: 176+n, 0, 40
        // n (0-7) for the 8 user templates, and (8-15) for the 8 factory templates
        midiOutPort.sendMidi(176 + 8, 0, 40);
        midiOutPort.sendMidi(176 + 9, 0, 40);

        final DocumentState documentState = host.getDocumentState();
        settingTemplate =
                documentState.getEnumSetting("Template", "General", TEMPLATE_OPTIONS, TEMPLATE_OPTIONS[DEFAULT_TEMPLATE]);
        settingTemplate.addValueObserver(this::changeCurrentTemplate);

        final MidiIn midiInPort = host.getMidiInPort(0);
        createNoteInput(midiInPort).setShouldConsumeEvents(true);
        final Transport transport = host.createTransport();

        hardwareSurface = host.createHardwareSurface();

        setupTransportMode(transport, midiInPort);
        final CursorTrack cursorTrack = setupTracksMode(host, midiInPort);
        setupDeviceMode(cursorTrack, midiInPort, true);
        setupDeviceMode(cursorTrack, midiInPort, false);

        midiInPort.setSysexCallback(this::onSysex);

        host.showPopupNotification("Launch Control Initialized");
    }

    private void setupTransportMode(final Transport transport, final MidiIn midiInPort)
    {
        final int p_channel = 8;

        transport.isPlaying().markInterested();
        transport.isArrangerRecordEnabled().markInterested();
        transport.isMetronomeEnabled().markInterested();
        transport.playStartPositionInSeconds().markInterested();
        transport.playPositionInSeconds().markInterested();

        final NovationButton playButton = new BigButton(hardwareSurface, "PLAY_BUTTON", midiInPort, p_channel, BUTTON_1);
        playButton.setBinding(transport.playAction());
        playButton.setColor(hardwareSurface,
                () -> transport.isPlaying().get() ? NovationColor.GREEN_FLASHING : NovationColor.GREEN_LOW, midiOutPort);

        final NovationButton stopButton = new BigButton(hardwareSurface, "STOP_BUTTON", midiInPort, p_channel, BUTTON_2);
        stopButton.setBinding(transport.stopAction());
        stopButton.setColor(hardwareSurface, () -> {
            if (transport.isPlaying().get())
            {
                return NovationColor.RED_FULL;
            }
            else if (transport.playStartPositionInSeconds().get() != transport.playPositionInSeconds().get())
            {
                return NovationColor.RED_FULL;
            }
            else
            {
                return NovationColor.RED_LOW;
            }
        }, midiOutPort);

        final NovationButton recButton =
                new BigButton(hardwareSurface, "TRANSPORT_REC_BUTTON", midiInPort, p_channel, BUTTON_3);
        recButton.setBinding(transport.recordAction());
        recButton.setColor(hardwareSurface, () -> {
            if (transport.isArrangerRecordEnabled().get())
            {
                if (transport.isPlaying().get())
                {
                    return NovationColor.RED_FULL;
                }
                else
                {
                    return NovationColor.RED_FLASHING;
                }
            }
            else
            {
                return NovationColor.RED_LOW;
            }
        }, midiOutPort);

        final NovationButton clickButton = new BigButton(hardwareSurface, "CLICK_BUTTON", midiInPort, p_channel, BUTTON_4);
        clickButton.setBinding(transport.isMetronomeEnabled());
        clickButton.setColor(hardwareSurface,
                () -> transport.isMetronomeEnabled().get() ? NovationColor.AMBER_FULL : NovationColor.AMBER_LOW, midiOutPort);
    }

    private CursorTrack setupTracksMode(final ControllerHost host, final MidiIn midiInPort)
    {
        final int p_channel = 9;

        final TrackBank trackBank = host.createMainTrackBank(2, 0, 0);
        final CursorTrack cursorTrack = host.createCursorTrack("CURSOR_TRACK", "Cursor Track", 0, 0, true);

        for (int i = 0; i < trackBank.getSizeOfBank(); i++)
        {
            final Track track = trackBank.getItemAt(i);
            setupOneTrack(track, i, trackBank, midiInPort, p_channel);
        }

        trackBank.followCursorTrack(cursorTrack);
        trackBank.canScrollBackwards().markInterested();
        trackBank.canScrollForwards().markInterested();
        final HardwareActionBindable scrollPageBackwardsAction = trackBank.scrollBackwardsAction();
        final HardwareActionBindable scrollPageForwardsAction = trackBank.scrollForwardsAction();

        final Application p_appl = host.createApplication();
        p_appl.panelLayout().markInterested();

        final NovationButton upButton =
                new SmallButton(hardwareSurface, "PREVIOUS_TRACK_BUTTON_UP", midiInPort, p_channel, BUTTON_UP);
        upButton.setColor(hardwareSurface, () -> tracksNavigationColor(p_appl, ARRANGE, trackBank.canScrollBackwards().get()),
                midiOutPort);

        final NovationButton downButton =
                new SmallButton(hardwareSurface, "NEXT_TRACK_BUTTON_DOWN", midiInPort, p_channel, BUTTON_DOWN);
        downButton.setColor(hardwareSurface, () -> tracksNavigationColor(p_appl, ARRANGE, trackBank.canScrollForwards().get()),
                midiOutPort);

        final NovationButton leftButton =
                new SmallButton(hardwareSurface, "PREVIOUS_TRACK_BUTTON_LEFT", midiInPort, p_channel, BUTTON_LEFT);
        leftButton.setColor(hardwareSurface, () -> tracksNavigationColor(p_appl, MIX, trackBank.canScrollBackwards().get()),
                midiOutPort);

        final NovationButton rightButton =
                new SmallButton(hardwareSurface, "NEXT_TRACK_BUTTON_RIGHT", midiInPort, p_channel, BUTTON_RIGHT);
        rightButton.setColor(hardwareSurface, () -> tracksNavigationColor(p_appl, MIX, trackBank.canScrollForwards().get()),
                midiOutPort);

        p_appl.panelLayout().addValueObserver(value -> {
            switch (value)
            {
                case ARRANGE:
                    upButton.setBinding(scrollPageBackwardsAction);
                    downButton.setBinding(scrollPageForwardsAction);

                    leftButton.clearBinding();
                    rightButton.clearBinding();
                    break;
                case MIX:
                    leftButton.setBinding(scrollPageBackwardsAction);
                    rightButton.setBinding(scrollPageForwardsAction);

                    upButton.clearBinding();
                    downButton.clearBinding();
                    break;
                case EDIT:
                    leftButton.clearBinding();
                    rightButton.clearBinding();
                    upButton.clearBinding();
                    downButton.clearBinding();
                    break;
                default:
                    break;
            }
        });

        return cursorTrack;
    }

    private NovationColor tracksNavigationColor(final Application p_appl, String Type, boolean Possible)
    {
        if (p_appl.panelLayout().get().equals(Type))
        {
            if (Possible)
            {
                return NovationColor.RED_FULL;
            }
            else
            {
                return NovationColor.RED_LOW;
            }
        }
        else
        {
            return NovationColor.OFF;
        }
    }

    private void setupOneTrack(final Track track, int i, final TrackBank trackBank, final MidiIn midiInPort,
            final int p_channel)
    {
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

        final NovationButton recButton =
                new BigButton(hardwareSurface, "TRACK_REC_BUTTON_" + p_hwID, midiInPort, p_channel, p_hwID);
        recButton.setBinding(track.arm());
        recButton.setColor(hardwareSurface,
                () -> tracksActionColor(track, track.arm().get(), NovationColor.RED_FULL, NovationColor.RED_LOW), midiOutPort);

        track.solo().markInterested();
        if (i == 0)
            p_hwID = BUTTON_2;
        else
            p_hwID = BUTTON_6;
        final NovationButton soloButton =
                new BigButton(hardwareSurface, "TRACK_SOLO_BUTTON_" + p_hwID, midiInPort, p_channel, p_hwID);
        soloButton.setBinding(track.solo());
        soloButton.setColor(hardwareSurface,
                () -> tracksActionColor(track, track.solo().get(), NovationColor.YELLOW_FLASHING, NovationColor.YELLOW_LOW),
                midiOutPort);

        track.mute().markInterested();
        if (i == 0)
            p_hwID = BUTTON_3;
        else
            p_hwID = BUTTON_7;
        final NovationButton muteButton =
                new BigButton(hardwareSurface, "TRACK_MUTE_BUTTON_" + p_hwID, midiInPort, p_channel, p_hwID);
        muteButton.setBinding(track.mute());
        muteButton.setColor(hardwareSurface,
                () -> tracksActionColor(track, track.mute().get(), NovationColor.ORANGE, NovationColor.AMBER_LOW), midiOutPort);
    }

    private NovationColor tracksActionColor(final Track track, final boolean ActionPossible, final NovationColor ColorOn,
            final NovationColor ColorOff)
    {
        if (track.exists().get())
        {
            if (ActionPossible)
            {
                return ColorOn;
            }
            else
            {
                return ColorOff;
            }
        }
        else
        {
            return NovationColor.OFF;
        }
    }

    private void setupDeviceMode(final CursorTrack cursorTrack, final MidiIn midiInPort, final boolean SingleDevice)
    {
        int p_channel;
        DeviceBank p_deviceBank;
        String p_name;
        if (SingleDevice)
        {
            p_name = "SINGLE_";
            p_channel = 10;
            p_deviceBank = cursorTrack.createDeviceBank(1);
        }
        else
        {
            p_name = "TWO_";
            p_channel = 11;
            p_deviceBank = cursorTrack.createDeviceBank(2);
        }

        PinnableCursorDevice p_cursor = cursorTrack.createCursorDevice(p_name + "CURSOR_DEVICE", "Cursor Device", 0,
                CursorDeviceFollowMode.FOLLOW_SELECTION);
        p_cursor.position().addValueObserver(p_deviceBank::scrollIntoView, 0);

        p_deviceBank.canScrollBackwards().markInterested();
        final NovationButton upDeviceButton =
                new SmallButton(hardwareSurface, p_name + "DEVICE_PREVIOUS_TRACK_BUTTON_UP", midiInPort, p_channel, BUTTON_UP);
        upDeviceButton.setBinding(p_deviceBank.scrollBackwardsAction());
        upDeviceButton.setColor(hardwareSurface,
                () -> p_deviceBank.canScrollBackwards().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW, midiOutPort);

        p_deviceBank.canScrollForwards().markInterested();
        final NovationButton downDeviceButton =
                new SmallButton(hardwareSurface, p_name + "DEVICE_NEXT_TRACK_BUTTON_DOWN", midiInPort, p_channel, BUTTON_DOWN);
        downDeviceButton.setBinding(p_deviceBank.scrollForwardsAction());
        downDeviceButton.setColor(hardwareSurface,
                () -> p_deviceBank.canScrollForwards().get() ? NovationColor.RED_FULL : NovationColor.RED_LOW, midiOutPort);

        /*
         * p_cursor.hasPrevious().markInterested(); final NovationButton leftButton = new
         * NovationButton(hardwareSurface, p_name + "DEVICE_BUTTON_LEFT",
         * NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel,
         * BUTTON_LEFT); leftButton.SetBinding(p_cursor.selectPreviousAction());
         * leftButton.SetColor(hardwareSurface, () -> p_cursor.hasPrevious().get() ? NovationColor.RED_FULL
         * : NovationColor.RED_LOW, midiOutPort);
         * 
         * p_cursor.hasNext().markInterested(); final NovationButton rightButton = new
         * NovationButton(hardwareSurface, p_name + "DEVICE_BUTTON_RIGHT",
         * NovationButton.NovationButtonType.CC, midiInPort, CHANNEL_ROOT_BUTTONS_ARROW, p_channel,
         * BUTTON_RIGHT); rightButton.SetBinding(p_cursor.selectNextAction());
         * rightButton.SetColor(hardwareSurface, () -> p_cursor.hasNext().get() ? NovationColor.RED_FULL :
         * NovationColor.RED_LOW, midiOutPort);
         */

        for (int i = 0; i < p_deviceBank.getSizeOfBank(); i++)
        {
            Device p_oneDevice = p_deviceBank.getDevice(i);
            setupOneDevice(p_oneDevice, p_name, i, p_channel, midiInPort);
        }
    }

    private void setupOneDevice(Device p_oneDevice, String p_name, int i, int p_channel, final MidiIn midiInPort)
    {
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

            final AbsoluteHardwareKnob p_vol_knob =
                    hardwareSurface.createAbsoluteHardwareKnob(p_name + "REMOTE_CTRL_KNOB_" + i + "_" + j);
            p_vol_knob.setAdjustValueMatcher(midiInPort.createAbsoluteCCValueMatcher(p_channel, p_knobID));
            p_remote.addBinding(p_vol_knob);
        }

        int p_buttonShift = 0;
        if (i == 1)
            p_buttonShift = 16;

        p_oneDevice.exists().markInterested();

        p_oneDevice.isEnabled().markInterested();
        final NovationButton muteButton = new BigButton(hardwareSurface, p_name + "DEVICE_MUTE_BUTTON_" + i, midiInPort,
                p_channel, BUTTON_1 + p_buttonShift);
        muteButton.setBinding(p_oneDevice.isEnabled().toggleAction());
        muteButton.setColor(hardwareSurface, () -> devicesActionColor(p_oneDevice, p_oneDevice.isEnabled().get(),
                NovationColor.GREEN_FULL, NovationColor.RED_FULL), midiOutPort);

        p_oneDevice.isRemoteControlsSectionVisible().markInterested();
        final NovationButton showRemote = new BigButton(hardwareSurface, p_name + "DEVICE_SHOW_REMOTE_BUTTON_" + i, midiInPort,
                p_channel, BUTTON_2 + p_buttonShift);
        showRemote.setBinding(p_oneDevice.isRemoteControlsSectionVisible().toggleAction());
        showRemote.setColor(hardwareSurface, () -> devicesActionColor(p_oneDevice,
                p_oneDevice.isRemoteControlsSectionVisible().get(), NovationColor.GREEN_FULL, NovationColor.RED_FULL),
                midiOutPort);

        remoteControlsBank.hasPrevious().markInterested();
        final NovationButton upButton = new BigButton(hardwareSurface, p_name + "DEVICE_BUTTON_REMOTE_UP_" + i, midiInPort,
                p_channel, BUTTON_3 + p_buttonShift);
        upButton.setBinding(remoteControlsBank.selectPreviousAction());
        upButton.setColor(hardwareSurface, () -> devicesActionColor(p_oneDevice, remoteControlsBank.hasPrevious().get(),
                NovationColor.RED_FULL, NovationColor.RED_LOW), midiOutPort);

        remoteControlsBank.hasNext().markInterested();
        final NovationButton downButton = new BigButton(hardwareSurface, p_name + "DEVICE_BUTTON_REMOTE_DOWN_" + i, midiInPort,
                p_channel, BUTTON_4 + p_buttonShift);
        downButton.setBinding(remoteControlsBank.selectNextAction());
        downButton.setColor(hardwareSurface, () -> devicesActionColor(p_oneDevice, remoteControlsBank.hasNext().get(),
                NovationColor.RED_FULL, NovationColor.RED_LOW), midiOutPort);
    }

    private NovationColor devicesActionColor(final Device p_oneDevice, final boolean ActionPossible,
            final NovationColor ColorOn, final NovationColor ColorOff)
    {
        if (p_oneDevice.exists().get())
        {
            if (ActionPossible)
            {
                return ColorOn;
            }
            else
            {
                return ColorOff;
            }
        }
        else
        {
            return NovationColor.OFF;
        }
    }

    private void onSysex(final String data)
    {
        final int p_page = Integer.parseInt(data.substring(15, 16), 16);

        if (p_page <= 11)
            settingTemplate.set(TEMPLATE_OPTIONS[p_page]);
        else
            changeCurrentTemplate(TEMPLATE_OPTIONS[DEFAULT_TEMPLATE]);
    }

    private void changeCurrentTemplate(final String Template)
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

    private void reset()
    {
        midiOutPort.sendSysex(SYSEX_RESET);

        final NovationColor p_off = NovationColor.OFF;
        for (int i = 0; i < 16; i++)
        {
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_1, p_off.code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_2, p_off.code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_3, p_off.code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_4, p_off.code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_5, p_off.code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_6, p_off.code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_7, p_off.code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_NUMBER + i, BUTTON_8, p_off.code());

            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_ARROW + i, BUTTON_UP, p_off.code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_ARROW + i, BUTTON_DOWN, p_off.code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_ARROW + i, BUTTON_LEFT, p_off.code());
            midiOutPort.sendMidi(CHANNEL_ROOT_BUTTONS_ARROW + i, BUTTON_RIGHT, p_off.code());
        }
    }

    private NoteInput createNoteInput(final MidiIn midiInPort)
    {
        return midiInPort.createNoteInput("Launch Control", "B0????", "B1????", "B2????", "B3????", "B4????", "B5????",
                "B6????", "B7????");
    }

    @Override
    public void exit()
    {
        reset();

        host.showPopupNotification("Launchpad Exited");
    }
}
