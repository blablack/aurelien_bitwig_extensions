package com.aurelien;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.PlayingNote;

import java.util.Arrays;

public class LaunchpadExtension extends ControllerExtension
{
   private ControllerHost m_host;
   private HardwareSurface m_hardwareSurface;
   private MidiOut m_midiOutPort;
   private NoteInput m_noteInput;

   final private String[] MODE =
   {"Piano", "Diatonic", "Drums", "Clip"};
   final private int DEFAULT_MODE = 0;
   private SettableEnumValue m_settingMode;
   private int m_activeMode;

   final int DEFAULT_ROOT = 0;

   final private int DEFAULT_SCALE = 0;

   private NoteMap[] m_noteMaps;
   private Grid m_grid;

   private int[] m_noteCache;
   protected static final int NOTE_OFF = 0;
   protected static final int NOTE_ON = 1;
   protected static final int NOTE_ON_NEW = 2;

   protected LaunchpadExtension(final LaunchpadExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      m_host = this.getHost();

      m_activeMode = 0;

      final MidiIn p_midiInPort = m_host.getMidiInPort(0);
      m_midiOutPort = m_host.getMidiOutPort(0);

      m_hardwareSurface = m_host.createHardwareSurface();

      m_noteInput = CreateNoteInput(p_midiInPort);
      m_noteInput.setShouldConsumeEvents(true);

      m_noteCache = new int[128];
      Arrays.fill(this.m_noteCache, NOTE_OFF);

      TrackBank p_trackBank = m_host.createMainTrackBank(8, 0, 8);
      m_grid = new Grid(p_trackBank, m_midiOutPort);

      CursorTrack p_cursorTrack = m_host.createCursorTrack(0, 1);
      p_trackBank.followCursorTrack(p_cursorTrack);
      p_trackBank.canScrollBackwards().markInterested();
      p_trackBank.canScrollForwards().markInterested();
      p_trackBank.sceneBank().canScrollBackwards().markInterested();
      p_trackBank.sceneBank().canScrollForwards().markInterested();

      p_cursorTrack.playingNotes().markInterested();
      p_cursorTrack.playingNotes().addValueObserver(this::PlayingNotes);

      p_midiInPort.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi(msg));

      final DocumentState documentState = m_host.getDocumentState();

      m_settingMode = documentState.getEnumSetting("Mode", "General", MODE, MODE[DEFAULT_MODE]);
      m_settingMode.addValueObserver(value -> {
         ChangeSettingMode(value);
      });

      SettableRangedValue p_settingVelocity = documentState.getNumberSetting("Velocity", "General", 0, 127, 1, "", 127);
      p_settingVelocity.addValueObserver(128, value -> {
         VelocityChanged(value);
      });
      p_settingVelocity.subscribe();

      SettableEnumValue p_settingScale = documentState.getEnumSetting("Mode", "Diatonic", NoteMapDiatonic.GetAllModes(), NoteMapDiatonic.GetAllModes()[DEFAULT_SCALE]);
      p_settingScale.addValueObserver(value -> {
         ChangeDiatonicMode(value);
      });

      SettableEnumValue p_settingRoot = documentState.getEnumSetting("Root", "Diatonic", NoteMapDiatonic.GetAllRoot(), NoteMapDiatonic.GetAllRoot()[DEFAULT_ROOT]);
      p_settingRoot.addValueObserver(value -> {
         ChangeDiatonicRoot(value);
      });

      m_noteMaps = new NoteMap[3];
      m_noteMaps[0] = new NoteMapPiano(m_midiOutPort, p_settingVelocity);
      m_noteMaps[1] = new NoteMapDiatonic(m_midiOutPort, p_settingVelocity, p_settingScale, p_settingRoot);
      m_noteMaps[2] = new NoteMapDrums(m_midiOutPort, p_settingVelocity);

      Reset();

      SetMixerButton(p_midiInPort);
      SetRightButtons(p_midiInPort);
      SetTopButtons(p_midiInPort);

      m_host.showPopupNotification("Launchpad Initialized");
   }

   private void onMidi(final ShortMidiMessage msg)
   {
      if (msg.getData2() == 127)
      {
         if (msg.getStatusByte() == LaunchpadConstants.CHANNEL_ROOT_BUTTONS_TOP)
         {
            if (msg.getData1() == LaunchpadConstants.BUTTON_MIXER)
            {
               m_settingMode.set(MODE[NextInSetting(MODE, m_settingMode.get())]);
            }
            else if (m_activeMode != 3)
            {
               switch (msg.getData1())
               {
                  case LaunchpadConstants.BUTTON_UP:
                     if (m_noteMaps[m_activeMode].canScrollUp())
                     {
                        m_noteMaps[m_activeMode].ScrollUp();
                        updateNoteTranslationTable(m_noteMaps[m_activeMode]);
                     }
                     break;

                  case LaunchpadConstants.BUTTON_DOWN:
                     if (m_noteMaps[m_activeMode].canScrollDown())
                     {
                        m_noteMaps[m_activeMode].ScrollDown();
                        updateNoteTranslationTable(m_noteMaps[m_activeMode]);
                     }
                     break;
                  default:
                     if (m_activeMode == 1)
                     {
                        ((NoteMapDiatonic) m_noteMaps[m_activeMode]).onMidiDiatonic(msg);
                     }
               }
            }
            else
            {
               m_grid.onMidi(msg);
            }
         }
         else
         {
            if (m_activeMode != 3)
            {
               m_noteMaps[m_activeMode].onMidi(msg);
            }
            else
            {
               m_grid.onMidi(msg);
            }
         }
      }
   }

   private void PlayingNotes(PlayingNote[] PlayingNotes)
   {
      if (m_activeMode == 3)
         return;

      synchronized (this.m_noteCache)
      {
         // Send the new notes
         for (final PlayingNote note : PlayingNotes)
         {
            final int pitch = note.pitch();
            this.m_noteCache[pitch] = NOTE_ON_NEW;
            m_noteMaps[m_activeMode].TurnOnNote(pitch);
         }
         // Send note offs
         for (int i = 0; i < this.m_noteCache.length; i++)
         {
            if (this.m_noteCache[i] == NOTE_ON_NEW)
            {
               this.m_noteCache[i] = NOTE_ON;
            }
            else if (this.m_noteCache[i] == NOTE_ON)
            {
               this.m_noteCache[i] = NOTE_OFF;
               m_noteMaps[m_activeMode].TurnOffNote(i);
            }
         }
      }
   }

   private void VelocityChanged(int Value)
   {
      Integer newVel[] = new Integer[128];
      Arrays.fill(newVel, Value);
      m_noteInput.setVelocityTranslationTable(newVel);
      m_hardwareSurface.invalidateHardwareOutputState();
   }

   private void SetMixerButton(MidiIn midiInPort)
   {
      final NovationButton mixerButton = new NovationButton(m_hardwareSurface, "MIXER_BUTTON", NovationButton.NovationButtonType.OFF, midiInPort, LaunchpadConstants.CHANNEL_ROOT_BUTTONS_TOP, 0, LaunchpadConstants.BUTTON_MIXER);
      mixerButton.SetColor(m_hardwareSurface, () -> {
         switch (m_settingMode.get())
         {
            case "Piano":
               return NovationColor.RED_FULL;
            case "Diatonic":
               return NovationColor.AMBER_FULL;
            case "Drums":
               return NovationColor.GREEN_FULL;
            case "Clip":
               return NovationColor.LIME;
            default:
               return NovationColor.OFF;
         }
      }, m_midiOutPort);
   }

   private void SetRightButtons(MidiIn midiInPort)
   {
      int p_vel = 0;
      for (int i = LaunchpadConstants.BUTTON_ARM; i >= LaunchpadConstants.BUTTON_VOL; i -= 16)
      {
         final int p_tempVel = p_vel == 0 ? 1 : p_vel;
         final int p_tempVel_plus = p_vel + 8;
         final int p_index = i / 16;

         final NovationButton p_velButton = new NovationButton(m_hardwareSurface, "VEL_BUTTON" + i, NovationButton.NovationButtonType.OFF, midiInPort, LaunchpadConstants.CHANNEL_ROOT_BUTTONS, 0, i);
         p_velButton.SetColor(m_hardwareSurface, () -> {
            if (m_activeMode != 3)
            {
               return m_noteMaps[m_activeMode].GetVelocityColor(p_tempVel_plus, p_tempVel);
            }
            else
            {
               return m_grid.GetTrackColor(p_index);
            }
         }, m_midiOutPort);

         p_vel += 16;
      }
   }

   private void SetTopButtons(MidiIn midiInPort)
   {
      final NovationButton p_leftButton = new NovationButton(m_hardwareSurface, "LEFT_BUTTON", NovationButton.NovationButtonType.OFF, midiInPort, LaunchpadConstants.CHANNEL_ROOT_BUTTONS_TOP, 0, LaunchpadConstants.BUTTON_LEFT);
      p_leftButton.SetColor(m_hardwareSurface, () -> {
         if (m_activeMode != 3)
         {
            return m_activeMode == 1 ? m_noteMaps[m_activeMode].canScrollLeft() ? NovationColor.GREEN_FULL : NovationColor.GREEN_LOW : NovationColor.OFF;
         }
         else
         {
            return m_grid.canScrollLeft() ? NovationColor.RED_FULL : NovationColor.RED_LOW;
         }
      }, m_midiOutPort);

      final NovationButton p_rightButton = new NovationButton(m_hardwareSurface, "RIGHT_BUTTON", NovationButton.NovationButtonType.OFF, midiInPort, LaunchpadConstants.CHANNEL_ROOT_BUTTONS_TOP, 0, LaunchpadConstants.BUTTON_RIGHT);
      p_rightButton.SetColor(m_hardwareSurface, () -> {
         if (m_activeMode != 3)
         {
            return m_activeMode == 1 ? m_noteMaps[m_activeMode].canScrollRight() ? NovationColor.GREEN_FULL : NovationColor.GREEN_LOW : NovationColor.OFF;
         }
         else
         {
            return m_grid.canScrollRight() ? NovationColor.RED_FULL : NovationColor.RED_LOW;
         }
      }, m_midiOutPort);

      final NovationButton p_upButton = new NovationButton(m_hardwareSurface, "UP_BUTTON", NovationButton.NovationButtonType.OFF, midiInPort, LaunchpadConstants.CHANNEL_ROOT_BUTTONS_TOP, 0, LaunchpadConstants.BUTTON_UP);
      p_upButton.SetColor(m_hardwareSurface, () -> {
         if (m_activeMode != 3)
         {
            return m_noteMaps[m_activeMode].canScrollUp() ? m_activeMode == 1 ? NovationColor.GREEN_FULL : NovationColor.RED_FULL : m_activeMode == 1 ? NovationColor.GREEN_LOW : NovationColor.RED_LOW;
         }
         else
         {
            return m_grid.canScrollUp() ? NovationColor.RED_FULL : NovationColor.RED_LOW;
         }
      }, m_midiOutPort);

      final NovationButton p_downButton = new NovationButton(m_hardwareSurface, "DOWN_BUTTON", NovationButton.NovationButtonType.OFF, midiInPort, LaunchpadConstants.CHANNEL_ROOT_BUTTONS_TOP, 0, LaunchpadConstants.BUTTON_DOWN);
      p_downButton.SetColor(m_hardwareSurface, () -> {
         if (m_activeMode != 3)
         {
            return m_noteMaps[m_activeMode].canScrollDown() ? m_activeMode == 1 ? NovationColor.GREEN_FULL : NovationColor.RED_FULL : m_activeMode == 1 ? NovationColor.GREEN_LOW : NovationColor.RED_LOW;
         }
         else
         {
            return m_grid.canScrollDown() ? NovationColor.RED_FULL : NovationColor.RED_LOW;
         }
      }, m_midiOutPort);

      /*
       * final NovationButton p_sessionButton = new NovationButton(m_hardwareSurface, "SESSION_BUTTON",
       * NovationButton.NovationButtonType.OFF, midiInPort, LaunchpadConstants.CHANNEL_ROOT_BUTTONS_TOP,
       * 0, LaunchpadConstants.BUTTON_SESSION); p_sessionButton.SetColor(m_hardwareSurface, () -> { if
       * (m_activeMode == 1) { return NovationColor.LIME; } else { return NovationColor.OFF; } },
       * m_midiOutPort);
       */
   }

   private void ChangeSettingMode(final String Value)
   {
      switch (Value)
      {
         case "Piano":
            m_activeMode = 0;
            break;
         case "Diatonic":
            m_activeMode = 1;
            break;
         case "Drums":
            m_activeMode = 2;
            break;
         case "Clip":
            m_activeMode = 3;
            break;
      }
      if (m_activeMode != 3)
      {
         m_host.showPopupNotification(m_noteMaps[m_activeMode].toString());
         updateNoteTranslationTable(m_noteMaps[m_activeMode]);
      }
      else
      {
         Integer[] table = new Integer[128];
         Arrays.fill(table, -1);
         m_noteInput.setKeyTranslationTable(table);
         m_grid.ColorKeysForClip();
         m_host.showPopupNotification("Clip");
      }
      m_hardwareSurface.invalidateHardwareOutputState();
   }

   private void ChangeDiatonicMode(final String Value)
   {
      if (m_activeMode == 1)
      {
         NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) m_noteMaps[m_activeMode];
         ChangeDiatonic(Value, p_tmpNoteMap.getDiatonicKey());
      }
   }

   private void ChangeDiatonicRoot(final String Value)
   {
      if (m_activeMode == 1)
      {
         NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) m_noteMaps[m_activeMode];
         ChangeDiatonic(p_tmpNoteMap.getMode(), Value);
      }
   }

   private void ChangeDiatonic(final String Mode, final String Root)
   {
      NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) m_noteMaps[m_activeMode];
      p_tmpNoteMap.SetDiatonicKey(Root);
      p_tmpNoteMap.SetMode(Mode);
      m_host.showPopupNotification(p_tmpNoteMap.toString());
      updateNoteTranslationTable(p_tmpNoteMap);
      m_hardwareSurface.invalidateHardwareOutputState();
      m_noteMaps[m_activeMode] = p_tmpNoteMap;
   }

   public void updateNoteTranslationTable(NoteMap activeNoteMap)
   {
      Integer[] table = new Integer[128];
      Arrays.fill(table, -1);

      for (int i = 0; i < 128; i++)
      {
         int y = i >> 4;
         int x = i & 0xF;

         if (x < 8)
         {
            table[i] = activeNoteMap.cellToKey(x, y);
         }
      }

      m_noteInput.setKeyTranslationTable(table);

      activeNoteMap.DrawKeys();
   }

   @Override
   public void exit()
   {
      Reset();
      m_host.showPopupNotification("Launchpad Exited");
   }

   @Override
   public void flush()
   {
      if (m_activeMode == 3)
         m_grid.ColorKeysForClip();
      if (this.m_hardwareSurface != null)
         this.m_hardwareSurface.updateHardware();
   }

   private void Reset()
   {
      // Turn off all leds
      m_midiOutPort.sendMidi(176, 0, 0);

      // set grid mapping mode
      m_midiOutPort.sendMidi(176, 0, 1);

      // set flashing mode
      m_midiOutPort.sendMidi(176, 0, 40);
   }

   private NoteInput CreateNoteInput(final MidiIn midiInPort)
   {
      return midiInPort.createNoteInput("Launchpad", "8000??", "9000??", "8001??", "9001??", "8002??", "9002??", "8003??", "9003??", "8004??", "9004??", "8005??", "9005??", "8006??", "9006??", "8007??", "9007??", "8010??", "9010??", "8011??", "9011??",
            "8012??", "9012??", "8013??", "9013??", "8014??", "9014??", "8015??", "9015??", "8016??", "9016??", "8017??", "9017??", "8020??", "9020??", "8021??", "9021??", "8022??", "9022??", "8023??", "9023??", "8024??", "9024??", "8025??", "9025??",
            "8026??", "9026??", "8027??", "9027??", "8030??", "9030??", "8031??", "9031??", "8032??", "9032??", "8033??", "9033??", "8034??", "9034??", "8035??", "9035??", "8036??", "9036??", "8037??", "9037??", "8040??", "9040??", "8041??", "9041??",
            "8042??", "9042??", "8043??", "9043??", "8044??", "9044??", "8045??", "9045??", "8046??", "9046??", "8047??", "9047??", "8050??", "9050??", "8051??", "9051??", "8052??", "9052??", "8053??", "9053??", "8054??", "9054??", "8055??", "9055??",
            "8056??", "9056??", "8057??", "9057??", "8060??", "9060??", "8061??", "9061??", "8062??", "9062??", "8063??", "9063??", "8064??", "9064??", "8065??", "9065??", "8066??", "9066??", "8067??", "9067??", "8070??", "9070??", "8071??", "9071??",
            "8072??", "9072??", "8073??", "9073??", "8074??", "9074??", "8075??", "9075??", "8076??", "9076??", "8077??", "9077??");
   }

   public static int NextInSetting(String arr[], String t)
   {
      int i = 0;
      for (String string : arr)
      {
         if (string.equals(t))
            break;
         i++;
      }
      i++;
      if (i >= arr.length)
         i = 0;

      return i;
   }
}
