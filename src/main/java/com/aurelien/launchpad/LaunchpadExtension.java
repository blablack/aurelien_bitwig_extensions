package com.aurelien.launchpad;

import static com.aurelien.launchpad.LaunchpadConstants.*;

import com.aurelien.basic.NovationButton;
import com.aurelien.basic.NovationColor;
import com.aurelien.launchpad.notemap.INoteMap;
import com.aurelien.launchpad.notemap.NoteMapDiatonic;
import com.aurelien.launchpad.notemap.NoteMapDrums;
import com.aurelien.launchpad.notemap.NoteMapPiano;
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
   private ControllerHost host;
   private HardwareSurface hardwareSurface;
   private MidiOut midiOutPort;
   private NoteInput noteInput;

   private static final String[] MODE =
   {"Piano", "Diatonic", "Drums", "Clip"};
   private static final int DEFAULT_MODE = 0;
   private SettableEnumValue settingMode;
   private int activeMode;

   private static final int DEFAULT_ROOT = 0;

   private static final int DEFAULT_SCALE = 0;

   private INoteMap[] noteMaps;
   private Grid grid;

   private int[] noteCache;
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
      host = this.getHost();

      activeMode = 0;

      final MidiIn p_midiInPort = host.getMidiInPort(0);
      midiOutPort = host.getMidiOutPort(0);

      hardwareSurface = host.createHardwareSurface();

      noteInput = createNoteInput(p_midiInPort);
      noteInput.setShouldConsumeEvents(true);

      noteCache = new int[128];
      Arrays.fill(this.noteCache, NOTE_OFF);

      TrackBank p_trackBank = host.createMainTrackBank(8, 0, 8);
      grid = new Grid(p_trackBank, midiOutPort);

      CursorTrack p_cursorTrack = host.createCursorTrack(0, 1);
      p_trackBank.followCursorTrack(p_cursorTrack);
      p_trackBank.canScrollBackwards().markInterested();
      p_trackBank.canScrollForwards().markInterested();
      p_trackBank.sceneBank().canScrollBackwards().markInterested();
      p_trackBank.sceneBank().canScrollForwards().markInterested();

      p_cursorTrack.playingNotes().markInterested();
      p_cursorTrack.playingNotes().addValueObserver(this::playingNotes);

      p_midiInPort.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi(msg));

      final DocumentState documentState = host.getDocumentState();

      settingMode = documentState.getEnumSetting("Mode", "General", MODE, MODE[DEFAULT_MODE]);
      settingMode.addValueObserver(value -> {
         changeSettingMode(value);
      });

      SettableRangedValue p_settingVelocity = documentState.getNumberSetting("Velocity", "General", 0, 127, 1, "", 127);
      p_settingVelocity.addValueObserver(128, value -> {
         velocityChanged(value);
      });
      p_settingVelocity.subscribe();

      SettableEnumValue p_settingScale = documentState.getEnumSetting("Mode", "Diatonic", NoteMapDiatonic.getAllModes(),
            NoteMapDiatonic.getAllModes()[DEFAULT_SCALE]);
      p_settingScale.addValueObserver(value -> {
         changeDiatonicMode(value);
      });

      SettableEnumValue p_settingRoot = documentState.getEnumSetting("Root", "Diatonic", NoteMapDiatonic.getAllRoot(),
            NoteMapDiatonic.getAllRoot()[DEFAULT_ROOT]);
      p_settingRoot.addValueObserver(value -> {
         changeDiatonicRoot(value);
      });

      noteMaps = new INoteMap[3];
      noteMaps[0] = new NoteMapPiano(midiOutPort, p_settingVelocity);
      noteMaps[1] = new NoteMapDiatonic(midiOutPort, p_settingVelocity, p_settingScale, p_settingRoot);
      noteMaps[2] = new NoteMapDrums(midiOutPort, p_settingVelocity);

      reset();

      setMixerButton(p_midiInPort);
      setRightButtons(p_midiInPort);
      setTopButtons(p_midiInPort);

      host.showPopupNotification("Launchpad S Initialized");
   }

   private void onMidi(final ShortMidiMessage msg)
   {
      if (msg.getData2() != 127)
      {
         return;
      }

      if (msg.getStatusByte() == CHANNEL_ROOT_BUTTONS_TOP)
      {
         if (msg.getData1() == BUTTON_MIXER)
         {
            settingMode.set(MODE[nextInSetting(MODE, settingMode.get())]);
         }
         else if (activeMode != 3)
         {
            handleScrollButtons(msg);
         }
         else
         {
            grid.onMidi(msg);
         }
      }
      else
      {
         if (activeMode != 3)
         {
            noteMaps[activeMode].onMidi(msg);
         }
         else
         {
            grid.onMidi(msg);
         }
      }
   }

   private void handleScrollButtons(final ShortMidiMessage msg)
   {
      switch (msg.getData1())
      {
         case BUTTON_UP:
            if (noteMaps[activeMode].canScrollUp())
            {
               noteMaps[activeMode].scrollUp();
               updateNoteTranslationTable(noteMaps[activeMode]);
            }
            break;

         case BUTTON_DOWN:
            if (noteMaps[activeMode].canScrollDown())
            {
               noteMaps[activeMode].scrollDown();
               updateNoteTranslationTable(noteMaps[activeMode]);
            }
            break;
         default:
            if (activeMode == 1)
            {
               ((NoteMapDiatonic) noteMaps[activeMode]).onMidiDiatonic(msg);
            }
      }
   }

   private void playingNotes(PlayingNote[] PlayingNotes)
   {
      if (activeMode == 3)
         return;

      synchronized (this.noteCache)
      {
         // Send the new notes
         for (final PlayingNote note : PlayingNotes)
         {
            final int pitch = note.pitch();
            this.noteCache[pitch] = NOTE_ON_NEW;
            noteMaps[activeMode].turnOnNote(pitch);
         }
         // Send note offs
         for (int i = 0; i < this.noteCache.length; i++)
         {
            if (this.noteCache[i] == NOTE_ON_NEW)
            {
               this.noteCache[i] = NOTE_ON;
            }
            else if (this.noteCache[i] == NOTE_ON)
            {
               this.noteCache[i] = NOTE_OFF;
               noteMaps[activeMode].turnOffNote(i);
            }
         }
      }
   }

   private void velocityChanged(int Value)
   {
      Integer newVel[] = new Integer[128];
      Arrays.fill(newVel, Value);
      noteInput.setVelocityTranslationTable(newVel);
      hardwareSurface.invalidateHardwareOutputState();
   }

   private void setMixerButton(MidiIn midiInPort)
   {
      final NovationButton mixerButton = new TopButton(hardwareSurface, "MIXER_BUTTON", midiInPort, BUTTON_MIXER);
      mixerButton.setColor(hardwareSurface, () -> {
         switch (settingMode.get())
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
      }, midiOutPort);
   }

   private void setRightButtons(MidiIn midiInPort)
   {
      int p_vel = 0;
      for (int i = BUTTON_ARM; i >= BUTTON_VOL; i -= 16)
      {
         final int p_tempVel = p_vel == 0 ? 1 : p_vel;
         final int p_tempVel_plus = p_vel + 8;
         final int p_index = i / 16;

         final NovationButton p_velButton = new NovationButton(hardwareSurface, "VEL_BUTTON" + i,
               NovationButton.NovationButtonType.OFF, midiInPort, CHANNEL_ROOT_BUTTONS, 0, i);
         p_velButton.setColor(hardwareSurface, () -> {
            if (activeMode != 3)
            {
               return noteMaps[activeMode].getVelocityColor(p_tempVel_plus, p_tempVel);
            }
            else
            {
               return grid.getTrackColor(p_index);
            }
         }, midiOutPort);

         p_vel += 16;
      }
   }

   private void setTopButtons(MidiIn midiInPort)
   {
      final NovationButton p_leftButton = new TopButton(hardwareSurface, "LEFT_BUTTON", midiInPort, BUTTON_LEFT);
      p_leftButton.setColor(hardwareSurface, () -> {
         if (activeMode != 3)
         {
            return colorLeftRightScroll(noteMaps[activeMode].canScrollLeft());
         }
         else
         {
            return colorGridScroll(grid.canScrollLeft());
         }
      }, midiOutPort);

      final NovationButton p_rightButton = new TopButton(hardwareSurface, "RIGHT_BUTTON", midiInPort, BUTTON_RIGHT);
      p_rightButton.setColor(hardwareSurface, () -> {
         if (activeMode != 3)
         {
            return colorLeftRightScroll(noteMaps[activeMode].canScrollRight());
         }
         else
         {
            return colorGridScroll(grid.canScrollRight());
         }
      }, midiOutPort);

      final NovationButton p_upButton = new TopButton(hardwareSurface, "UP_BUTTON", midiInPort, BUTTON_UP);
      p_upButton.setColor(hardwareSurface, () -> {
         if (activeMode != 3)
         {
            return colorUpDownScroll(noteMaps[activeMode].canScrollUp());
         }
         else
         {
            return colorGridScroll(grid.canScrollUp());
         }
      }, midiOutPort);

      final NovationButton p_downButton = new TopButton(hardwareSurface, "DOWN_BUTTON", midiInPort, BUTTON_DOWN);
      p_downButton.setColor(hardwareSurface, () -> {
         if (activeMode != 3)
         {
            return colorUpDownScroll(noteMaps[activeMode].canScrollDown());
         }
         else
         {
            return colorGridScroll(grid.canScrollDown());
         }
      }, midiOutPort);
   }

   private NovationColor colorLeftRightScroll(boolean Possible)
   {
      if (activeMode == 1)
      {
         if (Possible)
         {
            return NovationColor.GREEN_FULL;
         }
         else
         {
            return NovationColor.GREEN_LOW;
         }
      }
      else
      {
         return NovationColor.OFF;
      }
   }

   private NovationColor colorUpDownScroll(boolean Possible)
   {
      if (Possible)
      {
         if (activeMode == 1)
         {
            return NovationColor.GREEN_FULL;
         }
         else
         {
            return NovationColor.RED_FULL;
         }
      }
      else
      {
         if (activeMode == 1)
         {
            return NovationColor.GREEN_LOW;
         }
         else
         {
            return NovationColor.RED_LOW;
         }
      }
   }

   private NovationColor colorGridScroll(boolean Possible)
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

   private void changeSettingMode(final String Value)
   {
      switch (Value)
      {
         case "Piano":
            activeMode = 0;
            break;
         case "Diatonic":
            activeMode = 1;
            break;
         case "Drums":
            activeMode = 2;
            break;
         case "Clip":
            activeMode = 3;
            break;
         default:
            break;
      }
      if (activeMode != 3)
      {
         host.showPopupNotification(noteMaps[activeMode].toString());
         updateNoteTranslationTable(noteMaps[activeMode]);
      }
      else
      {
         Integer[] table = new Integer[128];
         Arrays.fill(table, -1);
         noteInput.setKeyTranslationTable(table);
         grid.colorKeysForClip();
         host.showPopupNotification("Clip");
      }
      hardwareSurface.invalidateHardwareOutputState();
   }

   private void changeDiatonicMode(final String Value)
   {
      if (activeMode == 1)
      {
         NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) noteMaps[activeMode];
         changeDiatonic(Value, p_tmpNoteMap.getDiatonicKey());
      }
   }

   private void changeDiatonicRoot(final String Value)
   {
      if (activeMode == 1)
      {
         NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) noteMaps[activeMode];
         changeDiatonic(p_tmpNoteMap.getMode(), Value);
      }
   }

   private void changeDiatonic(final String Mode, final String Root)
   {
      NoteMapDiatonic p_tmpNoteMap = (NoteMapDiatonic) noteMaps[activeMode];
      p_tmpNoteMap.setDiatonicKey(Root);
      p_tmpNoteMap.setMode(Mode);
      host.showPopupNotification(p_tmpNoteMap.toString());
      updateNoteTranslationTable(p_tmpNoteMap);
      hardwareSurface.invalidateHardwareOutputState();
      noteMaps[activeMode] = p_tmpNoteMap;
   }

   public void updateNoteTranslationTable(INoteMap activeNoteMap)
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

      noteInput.setKeyTranslationTable(table);

      activeNoteMap.drawKeys();
   }

   @Override
   public void exit()
   {
      reset();
      host.showPopupNotification("Launchpad S Exited");
   }

   @Override
   public void flush()
   {
      if (activeMode == 3)
         grid.colorKeysForClip();
      if (this.hardwareSurface != null)
         this.hardwareSurface.updateHardware();
   }

   private void reset()
   {
      // Turn off all leds
      midiOutPort.sendMidi(176, 0, 0);

      // set grid mapping mode
      midiOutPort.sendMidi(176, 0, 1);

      // set flashing mode
      midiOutPort.sendMidi(176, 0, 40);
   }

   private NoteInput createNoteInput(final MidiIn midiInPort)
   {
      return midiInPort.createNoteInput("Launchpad S", "8000??", "9000??", "8001??", "9001??", "8002??", "9002??", "8003??",
            "9003??", "8004??", "9004??", "8005??", "9005??", "8006??", "9006??", "8007??", "9007??", "8010??", "9010??",
            "8011??", "9011??", "8012??", "9012??", "8013??", "9013??", "8014??", "9014??", "8015??", "9015??", "8016??",
            "9016??", "8017??", "9017??", "8020??", "9020??", "8021??", "9021??", "8022??", "9022??", "8023??", "9023??",
            "8024??", "9024??", "8025??", "9025??", "8026??", "9026??", "8027??", "9027??", "8030??", "9030??", "8031??",
            "9031??", "8032??", "9032??", "8033??", "9033??", "8034??", "9034??", "8035??", "9035??", "8036??", "9036??",
            "8037??", "9037??", "8040??", "9040??", "8041??", "9041??", "8042??", "9042??", "8043??", "9043??", "8044??",
            "9044??", "8045??", "9045??", "8046??", "9046??", "8047??", "9047??", "8050??", "9050??", "8051??", "9051??",
            "8052??", "9052??", "8053??", "9053??", "8054??", "9054??", "8055??", "9055??", "8056??", "9056??", "8057??",
            "9057??", "8060??", "9060??", "8061??", "9061??", "8062??", "9062??", "8063??", "9063??", "8064??", "9064??",
            "8065??", "9065??", "8066??", "9066??", "8067??", "9067??", "8070??", "9070??", "8071??", "9071??", "8072??",
            "9072??", "8073??", "9073??", "8074??", "9074??", "8075??", "9075??", "8076??", "9076??", "8077??", "9077??");
   }

   public static int nextInSetting(String arr[], String t)
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
