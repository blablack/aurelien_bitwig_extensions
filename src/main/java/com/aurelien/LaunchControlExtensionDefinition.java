package com.aurelien;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchControlExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("aee088d2-8c6f-4604-b13d-14ecdcad01cd");

   public LaunchControlExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "Launch Control";
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
      return "Launch Control";
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
      if (platformType == PlatformType.WINDOWS)
      {
         list.add(new String[]
         {"Launch Control MIDI 1"}, new String[]
         {"Launch Control MIDI 1"});
      } else if (platformType == PlatformType.MAC)
      {
         list.add(new String[]
         {"Launch Control MIDI 1"}, new String[]
         {"Launch Control MIDI 1"});
      } else if (platformType == PlatformType.LINUX)
      {
         list.add(new String[]
         {"Launch Control MIDI 1"}, new String[]
         {"Launch Control MIDI 1"});
      }
   }

   @Override
   public String getHelpFilePath()
   {
      return "LaunchControl.html";
   }

   @Override
   public LaunchControlExtension createInstance(final ControllerHost host)
   {
      return new LaunchControlExtension(this, host);
   }
}
