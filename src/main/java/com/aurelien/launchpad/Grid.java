package com.aurelien.launchpad;

import static com.aurelien.launchpad.LaunchpadConstants.*;

import com.aurelien.basic.NovationColor;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.ClipLauncherSlot;

public class Grid
{
    private final MidiOut midiOutPort;
    private final TrackBank trackbank;


    public Grid(TrackBank trackbank, MidiOut midiOutPort)
    {
        this.trackbank = trackbank;
        this.midiOutPort = midiOutPort;

        for (int i = 0; i < 8; i++)
        {
            Track p_track = trackbank.getItemAt(i);
            p_track.isStopped().markInterested();
            p_track.isQueuedForStop().markInterested();
            p_track.exists().markInterested();

            for (int j = 0; j < 8; j++)
            {
                ClipLauncherSlot p_slot = p_track.clipLauncherSlotBank().getItemAt(j);
                p_slot.exists().markInterested();
                p_slot.isPlaybackQueued().markInterested();
                p_slot.isRecordingQueued().markInterested();
                p_slot.isStopQueued().markInterested();
                p_slot.isPlaying().markInterested();
                p_slot.isRecording().markInterested();
                p_slot.hasContent().markInterested();
            }
        }
    }

    public void onMidi(final ShortMidiMessage msg)
    {
        if (msg.getStatusByte() == CHANNEL_ROOT_BUTTONS_TOP)
        {
            switch (msg.getData1())
            {
                case BUTTON_UP:
                    trackbank.scrollBackwards();
                    break;
                case BUTTON_DOWN:
                    trackbank.scrollForwards();
                    break;
                case BUTTON_LEFT:
                    trackbank.sceneBank().scrollBackwards();
                    break;
                case BUTTON_RIGHT:
                    trackbank.sceneBank().scrollForwards();
                    break;
                default:
                    break;
            }
        }
        else
        {
            switch (msg.getData1())
            {
                case BUTTON_ARM:
                    trackbank.getItemAt(7).clipLauncherSlotBank().stop();
                    break;
                case BUTTON_SOLO:
                    trackbank.getItemAt(6).clipLauncherSlotBank().stop();
                    break;
                case BUTTON_TRKON:
                    trackbank.getItemAt(5).clipLauncherSlotBank().stop();
                    break;
                case BUTTON_STOP:
                    trackbank.getItemAt(4).clipLauncherSlotBank().stop();
                    break;
                case BUTTON_SNDB:
                    trackbank.getItemAt(3).clipLauncherSlotBank().stop();
                    break;
                case BUTTON_SNDA:
                    trackbank.getItemAt(2).clipLauncherSlotBank().stop();
                    break;
                case BUTTON_PAN:
                    trackbank.getItemAt(1).clipLauncherSlotBank().stop();
                    break;
                case BUTTON_VOL:
                    trackbank.getItemAt(0).clipLauncherSlotBank().stop();
                    break;
                default:
                    ClipLauncherSlot p_slot =
                            trackbank.getItemAt((msg.getData1() / 16)).clipLauncherSlotBank().getItemAt((msg.getData1() % 16));
                    p_slot.launch();
            }
        }
    }

    public void colorKeysForClip()
    {
        for (int y = 0; y < 8; y++)
        {
            for (int x = 0; x < 8; x++)
            {
                int key = x * 8 + y;

                int p_column = key & 0x7;
                int p_row = key >> 3;

                ClipLauncherSlot p_slot = trackbank.getItemAt(x).clipLauncherSlotBank().getItemAt(y);

                if (p_slot.exists().get() && p_slot.hasContent().get())
                {
                    colorKeyForSlot(p_slot, p_column, p_row);
                }
                else
                {
                    midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.OFF.code());
                }
            }
        }
    }

    private void colorKeyForSlot(ClipLauncherSlot p_slot, int p_column, int p_row)
    {
        if (p_slot.isPlaybackQueued().get())
            midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.GREEN_FLASHING.code());
        else if (p_slot.isRecordingQueued().get())
            midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.RED_FLASHING.code());
        else if (p_slot.isStopQueued().get())
            midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.AMBER_FLASHING.code());
        else if (p_slot.isPlaying().get())
            midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.GREEN_FULL.code());
        else if (p_slot.isRecording().get())
            midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.RED_FULL.code());
        else
            midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.GREEN_LOW.code());
    }

    public NovationColor getTrackColor(int index)
    {
        Track p_track = trackbank.getItemAt(index);
        if (!p_track.exists().get() || p_track.isStopped().get())
        {
            return NovationColor.OFF;
        }
        else if (p_track.isQueuedForStop().get())
        {
            return NovationColor.RED_FLASHING;
        }
        else
        {
            return NovationColor.GREEN_FULL;
        }
    }

    public boolean canScrollUp()
    {
        return trackbank.canScrollBackwards().get();
    }

    public boolean canScrollDown()
    {
        return trackbank.canScrollForwards().get();
    }

    public boolean canScrollLeft()
    {
        return trackbank.sceneBank().canScrollBackwards().get();
    }

    public boolean canScrollRight()
    {
        return trackbank.sceneBank().canScrollForwards().get();
    }
}
