package com.aurelien;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.ControllerExtension;

public class KBoardExtension extends ControllerExtension
{
   protected KBoardExtension(final KBoardExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = this.getHost();

      final MidiIn midiInPort = host.getMidiInPort(0);

      midiInPort.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi(msg));
      midiInPort.setSysexCallback((String data) -> onSysex(data));

      host.showPopupNotification("KBoard Initialized");
   }

   @Override
   public void exit()
   {
      getHost().showPopupNotification("KBoard Exited");
   }

   @Override
   public void flush()
   {
   }

   private void onMidi(ShortMidiMessage msg)
   {
      this.getHost().println("Midi: " + msg);
   }

   private void onSysex(final String data)
   {
      this.getHost().println("onSysex: " + data);
   }
}
