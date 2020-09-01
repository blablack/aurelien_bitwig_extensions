package com.aurelien;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchpadExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("aee088d2-8c6f-4604-b13d-14ecdcad01ce");

   public LaunchpadExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "Launchpad S";
   }

   @Override
   public String getAuthor()
   {
      return "Aurelien";
   }

   @Override
   public String getVersion()
   {
      return "0.2";
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
      return "Launchpad S";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 12;
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
      {"Launchpad S"}, new String[]
      {"Launchpad S"});

      if (platformType == PlatformType.WINDOWS)
      {
         list.add(new String[]
         {"Launchpad S MIDI 1"}, new String[]
         {"Launchpad S MIDI 1"});
      }
      else if (platformType == PlatformType.MAC)
      {
         list.add(new String[]
         {"Launchpad S MIDI 1"}, new String[]
         {"Launchpad S MIDI 1"});
      }
      else if (platformType == PlatformType.LINUX)
      {
         list.add(new String[]
         {"Launchpad S MIDI 1"}, new String[]
         {"Launchpad S MIDI 1"});
      }
   }

   @Override
   public String getHelpFilePath()
   {
      return "LaunchpadS.html";
   }

   @Override
   public LaunchpadExtension createInstance(final ControllerHost host)
   {
      return new LaunchpadExtension(this, host);
   }
}
