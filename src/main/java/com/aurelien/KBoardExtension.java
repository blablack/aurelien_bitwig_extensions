package com.aurelien;

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
      midiInPort.createNoteInput("K-Board");
      
      host.showPopupNotification("K-Board Initialized");
   }

   @Override
   public void exit()
   {
      getHost().showPopupNotification("K-Board Exited");
   }

   @Override
   public void flush()
   {
   }
}
