package com.aurelien;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class KBoardExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("aee088d2-8c6f-4604-b13d-14ecdcad01cc");

   public KBoardExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "KBoard";
   }

   @Override
   public String getAuthor()
   {
      return "Aurelien";
   }

   @Override
   public String getVersion()
   {
      return "0.1";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Aurelien";
   }

   @Override
   public String getHardwareModel()
   {
      return "KBoard";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 11;
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 1;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      list.add(new String[]
      {"K-Board"}, new String[]
      {"K-Board"});

      if (platformType == PlatformType.WINDOWS)
      {
         list.add(new String[]
         {"K-Board MIDI 1"}, new String[]
         {"K-Board MIDI 1"});
      } else if (platformType == PlatformType.MAC)
      {
         list.add(new String[]
         {"K-Board MIDI 1"}, new String[]
         {"K-Board MIDI 1"});
      } else if (platformType == PlatformType.LINUX)
      {
         list.add(new String[]
         {"K-Board MIDI 1"}, new String[]
         {"K-Board MIDI 1"});
      }
   }

   @Override
   public KBoardExtension createInstance(final ControllerHost host)
   {
      return new KBoardExtension(this, host);
   }
}
