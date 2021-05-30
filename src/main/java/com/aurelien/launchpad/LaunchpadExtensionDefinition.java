package com.aurelien.launchpad;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchpadExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("aee088d2-8c6f-4604-b13d-14ecdcad01ce");

   public static final String MIDINAME = "Launchpad S MIDI 1";
   public static final String NAME = "Launchpad S";

   @Override
   public String getName()
   {
      return NAME;
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
      return NAME;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 13;
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
      {NAME}, new String[]
      {NAME});

      list.add(new String[]
      {MIDINAME}, new String[]
      {MIDINAME});
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
