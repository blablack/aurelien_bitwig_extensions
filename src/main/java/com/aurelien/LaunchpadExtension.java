package com.aurelien;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.PlayingNote;

import java.util.Arrays;

public class LaunchpadExtension extends ControllerExtension
{
   final int BUTTON_UP = 104;
   final int BUTTON_DOWN = 105;
   final int BUTTON_LEFT = 106;
   final int BUTTON_RIGHT = 107;

   final int BUTTON_SESSION = 108;
   final int BUTTON_USER1 = 109;
   final int BUTTON_USER2 = 110;
   final int BUTTON_MIXER = 111;

   final int BUTTON_VOL = 8;
   final int BUTTON_PAN = 24;
   final int BUTTON_SNDA = 40;
   final int BUTTON_SNDB = 56;

   final int BUTTON_STOP = 72;
   final int BUTTON_TRKON = 88;
   final int BUTTON_SOLO = 104;
   final int BUTTON_ARM = 120;

   final int CHANNEL_ROOT_BUTTONS = 144;
   final int CHANNEL_ROOT_BUTTONS_TOP = 176;

   final int CHANNEL = 0;

   private HardwareSurface hardwareSurface;
   private MidiOut midiOutPort;
   private NoteInput noteInput;
   private ControllerHost host;

   final private String[] MODE =
   {"Piano", "Diatonic", "Drums"};
   final private int DEFAULT_MODE = 0;
   private SettableEnumValue m_settingMode;

   final private String[] ROOT =
   {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
   final int DEFAULT_ROOT = 0;
   private SettableEnumValue m_settingRoot;

   final private String[] SCALE =
   {"Ionian", "Dorian", "Phrygian", "Lydian", "Mixolydian", "Aeolian", "Locrian"};
   final private int DEFAULT_SCALE = 0;
   private SettableEnumValue m_settingScale;

   private SettableRangedValue m_settingVelocity;
   private int activeNoteMap;
   private NoteMap[] NoteMaps;

   protected static final int NOTE_OFF = 0;
   protected static final int NOTE_ON = 1;
   protected static final int NOTE_ON_NEW = 2;

   private int[] noteCache;

   protected LaunchpadExtension(final LaunchpadExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      host = this.getHost();

      activeNoteMap = 0;

      final MidiIn midiInPort = host.getMidiInPort(0);

      hardwareSurface = host.createHardwareSurface();

      noteInput = CreateNoteInput(midiInPort);
      noteInput.setShouldConsumeEvents(false);

      noteCache = new int[128];
      Arrays.fill(this.noteCache, NOTE_OFF);
      CursorTrack mCursorTrack = host.createCursorTrack(0, 1);
      // mCursorTrack.playingNotes().addValueObserver(notes -> mPlayingNotes = notes);
      mCursorTrack.playingNotes().markInterested();
      mCursorTrack.playingNotes().addValueObserver(this::PlayingNotes);

      midiOutPort = host.getMidiOutPort(0);

      NoteMaps = new NoteMap[3];
      NoteMaps[0] = new NoteMapPiano(host, midiOutPort);
      NoteMaps[1] = new NoteMapDiatonic(host, midiOutPort);
      NoteMaps[2] = new NoteMapDrums(host, midiOutPort);

      Reset();

      midiInPort.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi(msg));

      final DocumentState documentState = host.getDocumentState();

      m_settingMode = documentState.getEnumSetting("Mode", "General", MODE, MODE[DEFAULT_MODE]);
      m_settingMode.addValueObserver(value -> {
         ChangeSettingMode(value);
      });

      m_settingVelocity = documentState.getNumberSetting("Velocity", "General", 0, 127, 1, "", 127);
      m_settingVelocity.addValueObserver(128, value -> {
         VelocityChanged(value);
      });
      m_settingVelocity.subscribe();

      m_settingScale = documentState.getEnumSetting("Mode", "Diatonic", SCALE, SCALE[DEFAULT_SCALE]);
      m_settingScale.addValueObserver(value -> {
         ChangeDiatonicMode(value);
      });

      m_settingRoot = documentState.getEnumSetting("Root", "Diatonic", ROOT, ROOT[DEFAULT_ROOT]);
      m_settingRoot.addValueObserver(value -> {
         ChangeDiatonicRoot(value);
      });

      SetMixerButton(midiInPort);
      SetVelocityButton(midiInPort);
      SetTopButtons(midiInPort);

      host.showPopupNotification("Launchpad Initialized");
   }

   private void onMidi(final ShortMidiMessage msg)
   {
      // host.println("MidiIn: " + msg);
      if (msg.getData2() == 127)
      {
         if (msg.getStatusByte() == CHANNEL_ROOT_BUTTONS_TOP)
         {
            switch (msg.getData1())
            {
               case BUTTON_MIXER:
                  m_settingMode.set(MODE[NextInSetting(MODE, m_settingMode.get())]);
                  break;

               case BUTTON_UP:
                  if (NoteMaps[activeNoteMap].canScrollUp())
                  {
                     NoteMaps[activeNoteMap].ScrollUp();
                     updateNoteTranlationTable(NoteMaps[activeNoteMap]);
                  }
                  break;

               case BUTTON_DOWN:
                  if (NoteMaps[activeNoteMap].canScrollDown())
                  {
                     NoteMaps[activeNoteMap].ScrollDown();
                     updateNoteTranlationTable(NoteMaps[activeNoteMap]);
                  }
                  break;

               case BUTTON_LEFT:
                  if (NoteMaps[activeNoteMap] instanceof NoteMapDiatonic)
                  {
                     NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) NoteMaps[activeNoteMap];
                     m_settingScale.set(p_tmpNoteMap.PreviousMode());
                  }
                  break;

               case BUTTON_RIGHT:
                  if (NoteMaps[activeNoteMap] instanceof NoteMapDiatonic)
                  {
                     NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) NoteMaps[activeNoteMap];
                     m_settingScale.set(p_tmpNoteMap.NextMode());
                  }
                  break;

               case BUTTON_SESSION:
                  if (NoteMaps[activeNoteMap] instanceof NoteMapDiatonic)
                  {
                     NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) NoteMaps[activeNoteMap];
                     m_settingRoot.set(p_tmpNoteMap.NextInKey());
                  }
                  break;
            }
         } else
         {
            switch (msg.getData1())
            {
               case BUTTON_ARM:
                  VelocityChangeButton(15);
                  break;
               case BUTTON_SOLO:
                  VelocityChangeButton(31);
                  break;
               case BUTTON_TRKON:
                  VelocityChangeButton(47);
                  break;
               case BUTTON_STOP:
                  VelocityChangeButton(63);
                  break;
               case BUTTON_SNDB:
                  VelocityChangeButton(79);
                  break;
               case BUTTON_SNDA:
                  VelocityChangeButton(95);
                  break;
               case BUTTON_PAN:
                  VelocityChangeButton(111);
                  break;
               case BUTTON_VOL:
                  VelocityChangeButton(127);
                  break;
            }
         }
      }
   }

   private void PlayingNotes(PlayingNote[] PlayingNotes)
   {
      synchronized (this.noteCache)
      {
         // Send the new notes
         for (final PlayingNote note : PlayingNotes)
         {
            final int pitch = note.pitch();
            this.noteCache[pitch] = NOTE_ON_NEW;
            NoteMaps[activeNoteMap].TurnOnNote(pitch);
            // this.notifyNoteObservers (pitch, note.velocity ());
         }
         // Send note offs
         for (int i = 0; i < this.noteCache.length; i++)
         {
            if (this.noteCache[i] == NOTE_ON_NEW)
            {
               this.noteCache[i] = NOTE_ON;
            } else if (this.noteCache[i] == NOTE_ON)
            {
               this.noteCache[i] = NOTE_OFF;
               NoteMaps[activeNoteMap].TurnOffNote(i);
               // this.notifyNoteObservers (i, 0);
            }
         }
      }
   }

   private void VelocityChangeButton(int Value)
   {
      m_settingVelocity.setImmediately((double) Value / 127);
   }

   private void VelocityChanged(int Value)
   {
      Integer newVel[] = new Integer[128];
      Arrays.fill(newVel, Value);
      noteInput.setVelocityTranslationTable(newVel);
      hardwareSurface.invalidateHardwareOutputState();
   }

   private void SetVelocityButton(MidiIn midiInPort)
   {
      int p_vel = 0;
      for (int i = BUTTON_ARM; i >= BUTTON_VOL; i -= 16)
      {
         final int p_tempVel = p_vel == 0 ? 1 : p_vel;
         final int p_tempVel_plus = p_vel + 8;

         final NovationButton p_velButton = new NovationButton(hardwareSurface, "VEL_BUTTON" + i, NovationButton.NovationButtonType.OFF, midiInPort, CHANNEL_ROOT_BUTTONS, 0, i);
         p_velButton.SetColor(hardwareSurface, () -> m_settingVelocity.get() * 127 >= p_tempVel_plus ? NovationColor.AMBER_FULL : m_settingVelocity.get() * 127 >= p_tempVel ? NovationColor.AMBER_LOW : NovationColor.OFF, midiOutPort);

         p_vel += 16;
      }
   }

   private void SetMixerButton(MidiIn midiInPort)
   {
      final NovationButton mixerButton = new NovationButton(hardwareSurface, "MIXER_BUTTON", NovationButton.NovationButtonType.OFF, midiInPort, CHANNEL_ROOT_BUTTONS_TOP, 0, BUTTON_MIXER);
      mixerButton.SetColor(hardwareSurface, () -> {
         switch (m_settingMode.get())
         {
            case "Piano":
               return NovationColor.RED_FULL;
            case "Diatonic":
               return NovationColor.AMBER_FULL;
            case "Drums":
               return NovationColor.GREEN_FULL;
            default:
               return NovationColor.OFF;
         }
      }, midiOutPort);
   }

   private void SetTopButtons(MidiIn midiInPort)
   {
      final NovationButton p_leftButton = new NovationButton(hardwareSurface, "LEFT_BUTTON", NovationButton.NovationButtonType.OFF, midiInPort, CHANNEL_ROOT_BUTTONS_TOP, 0, BUTTON_LEFT);
      p_leftButton.SetColor(hardwareSurface, () -> NoteMaps[activeNoteMap].canScrollLeft() ? NovationColor.GREEN_FULL : NovationColor.OFF, midiOutPort);

      final NovationButton p_rightButton = new NovationButton(hardwareSurface, "RIGHT_BUTTON", NovationButton.NovationButtonType.OFF, midiInPort, CHANNEL_ROOT_BUTTONS_TOP, 0, BUTTON_RIGHT);
      p_rightButton.SetColor(hardwareSurface, () -> NoteMaps[activeNoteMap].canScrollRight() ? NovationColor.GREEN_FULL : NovationColor.OFF, midiOutPort);

      final NovationButton p_upButton = new NovationButton(hardwareSurface, "UP_BUTTON", NovationButton.NovationButtonType.OFF, midiInPort, CHANNEL_ROOT_BUTTONS_TOP, 0, BUTTON_UP);
      p_upButton.SetColor(hardwareSurface, () -> NoteMaps[activeNoteMap].canScrollUp() ? NovationColor.RED_FULL : NovationColor.OFF, midiOutPort);

      final NovationButton p_downButton = new NovationButton(hardwareSurface, "DOWN_BUTTON", NovationButton.NovationButtonType.OFF, midiInPort, CHANNEL_ROOT_BUTTONS_TOP, 0, BUTTON_DOWN);
      p_downButton.SetColor(hardwareSurface, () -> NoteMaps[activeNoteMap].canScrollDown() ? NovationColor.RED_FULL : NovationColor.OFF, midiOutPort);

      final NovationButton p_sessionButton = new NovationButton(hardwareSurface, "SESSION_BUTTON", NovationButton.NovationButtonType.OFF, midiInPort, CHANNEL_ROOT_BUTTONS_TOP, 0, BUTTON_SESSION);
      p_sessionButton.SetColor(hardwareSurface, () -> NoteMaps[activeNoteMap] instanceof NoteMapDiatonic ? NovationColor.LIME : NovationColor.OFF, midiOutPort);
   }

   private void ChangeSettingMode(final String Value)
   {
      switch (Value)
      {
         case "Piano":
            activeNoteMap = 0;
            break;
         case "Diatonic":
            activeNoteMap = 1;
            break;
         case "Drums":
            activeNoteMap = 2;
            break;
      }
      host.showPopupNotification(NoteMaps[activeNoteMap].toString());
      updateNoteTranlationTable(NoteMaps[activeNoteMap]);
      hardwareSurface.invalidateHardwareOutputState();
   }

   private void ChangeDiatonicMode(final String Value)
   {
      if (NoteMaps[activeNoteMap] instanceof NoteMapDiatonic)
      {
         NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) NoteMaps[activeNoteMap];
         ChangeDiatonic(Value, p_tmpNoteMap.getDiatonicKey());
      }
   }

   private void ChangeDiatonicRoot(final String Value)
   {
      if (NoteMaps[activeNoteMap] instanceof NoteMapDiatonic)
      {
         NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) NoteMaps[activeNoteMap];
         ChangeDiatonic(p_tmpNoteMap.getMode(), Value);
      }
   }

   private void ChangeDiatonic(final String Mode, final String Root)
   {
      NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) NoteMaps[activeNoteMap];
      p_tmpNoteMap.SetDiatonicKey(Root);
      p_tmpNoteMap.SetMode(Mode);
      host.showPopupNotification(p_tmpNoteMap.toString());
      updateNoteTranlationTable(p_tmpNoteMap);
      hardwareSurface.invalidateHardwareOutputState();
      NoteMaps[activeNoteMap] = p_tmpNoteMap;
   }

   public void updateNoteTranlationTable(NoteMap activeNoteMap)
   {
      Integer[] table = new Integer[128];
      Arrays.fill(table, -1);

      for (var i = 0; i < 128; i++)
      {
         var y = i >> 4;
         var x = i & 0xF;

         if (x < 8)
         {
            table[i] = activeNoteMap.cellToKey(x, y);
         }
      }

      noteInput.setKeyTranslationTable(table);

      activeNoteMap.DrawKeys();
   }

   @Override
   public void exit()
   {
      Reset();
      host.showPopupNotification("Launchpad Exited");
   }

   @Override
   public void flush()
   {
      if (this.hardwareSurface != null)
         this.hardwareSurface.updateHardware();
   }

   private void Reset()
   {
      // Turn off all leds
      midiOutPort.sendMidi(176, 0, 0);

      // set grid mapping mode
      midiOutPort.sendMidi(176, 0, 1);

      // set flashing mode
      midiOutPort.sendMidi(176, 0, 40);
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
