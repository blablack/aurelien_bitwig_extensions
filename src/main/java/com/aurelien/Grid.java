package com.aurelien;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.ClipLauncherSlot;

public class Grid
{
    private final MidiOut m_midiOutPort;
    private final TrackBank m_trackbank;


    public Grid(TrackBank trackbank, MidiOut midiOutPort)
    {
        m_trackbank = trackbank;
        m_midiOutPort = midiOutPort;

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
        if (msg.getStatusByte() == LaunchpadConstants.CHANNEL_ROOT_BUTTONS_TOP)
        {
            switch (msg.getData1())
            {
                case LaunchpadConstants.BUTTON_UP:
                    m_trackbank.scrollBackwards();
                    break;
                case LaunchpadConstants.BUTTON_DOWN:
                    m_trackbank.scrollForwards();
                    break;
                case LaunchpadConstants.BUTTON_LEFT:
                    m_trackbank.sceneBank().scrollBackwards();
                    break;
                case LaunchpadConstants.BUTTON_RIGHT:
                    m_trackbank.sceneBank().scrollForwards();
                    break;
            }
        }
        else
        {
            switch (msg.getData1())
            {
                case LaunchpadConstants.BUTTON_ARM:
                    m_trackbank.getItemAt(7).clipLauncherSlotBank().stop();
                    break;
                case LaunchpadConstants.BUTTON_SOLO:
                    m_trackbank.getItemAt(6).clipLauncherSlotBank().stop();
                    break;
                case LaunchpadConstants.BUTTON_TRKON:
                    m_trackbank.getItemAt(5).clipLauncherSlotBank().stop();
                    break;
                case LaunchpadConstants.BUTTON_STOP:
                    m_trackbank.getItemAt(4).clipLauncherSlotBank().stop();
                    break;
                case LaunchpadConstants.BUTTON_SNDB:
                    m_trackbank.getItemAt(3).clipLauncherSlotBank().stop();
                    break;
                case LaunchpadConstants.BUTTON_SNDA:
                    m_trackbank.getItemAt(2).clipLauncherSlotBank().stop();
                    break;
                case LaunchpadConstants.BUTTON_PAN:
                    m_trackbank.getItemAt(1).clipLauncherSlotBank().stop();
                    break;
                case LaunchpadConstants.BUTTON_VOL:
                    m_trackbank.getItemAt(0).clipLauncherSlotBank().stop();
                    break;
                default:
                    ClipLauncherSlot p_slot = m_trackbank.getItemAt((int) (msg.getData1() / 16)).clipLauncherSlotBank().getItemAt((int) (msg.getData1() % 16));
                    p_slot.launch();
            }
        }
    }

    public void ColorKeysForClip()
    {
        for (int y = 0; y < 8; y++)
        {
            for (int x = 0; x < 8; x++)
            {
                int key = x * 8 + y;

                int p_column = key & 0x7;
                int p_row = key >> 3;

                ClipLauncherSlot p_slot = m_trackbank.getItemAt(x).clipLauncherSlotBank().getItemAt(y);

                if (p_slot.exists().get() && p_slot.hasContent().get())
                {
                    if (p_slot.isPlaybackQueued().get())
                        m_midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.GREEN_FLASHING.Code());
                    else if (p_slot.isRecordingQueued().get())
                        m_midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.RED_FLASHING.Code());
                    else if (p_slot.isStopQueued().get())
                        m_midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.AMBER_FLASHING.Code());
                    else if (p_slot.isPlaying().get())
                        m_midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.GREEN_FULL.Code());
                    else if (p_slot.isRecording().get())
                        m_midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.RED_FULL.Code());
                    else
                        m_midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.GREEN_LOW.Code());
                }
                else
                {
                    m_midiOutPort.sendMidi(144, p_row * 16 + p_column, NovationColor.OFF.Code());
                }
            }
        }
    }

    public NovationColor GetTrackColor(int index)
    {
        Track p_track = m_trackbank.getItemAt(index);
               return !p_track.exists().get() || p_track.isStopped().get() ? NovationColor.OFF : p_track.isQueuedForStop().get() ? NovationColor.RED_FLASHING : NovationColor.GREEN_FULL;
    }

    public Boolean canScrollUp()
    {
        return m_trackbank.canScrollBackwards().get();
    }
    
    public Boolean canScrollDown()
    {
        return m_trackbank.canScrollForwards().get();
    }
    
    public Boolean canScrollLeft()
    {
        return m_trackbank.sceneBank().canScrollBackwards().get();
    }

    public Boolean canScrollRight()
    {
        return m_trackbank.sceneBank().canScrollForwards().get();
    }
}
