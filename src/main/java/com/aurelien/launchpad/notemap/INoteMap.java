package com.aurelien.launchpad.notemap;

import com.aurelien.basic.NovationColor;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;


public interface INoteMap {
    public void onMidi(final ShortMidiMessage msg);
    
    public int cellToKey(int x, int y);
    
    public void drawKeys();

    public void setCellLED(int column, int row, NovationColor colour);

    public NovationColor getVelocityColor(int Vel_plus, int Vel);

    public boolean canScrollUp();
    public void scrollUp();

    public boolean canScrollDown();
    public void scrollDown();

    public boolean canScrollLeft();
    public void scrollLeft() throws CannotScrollException;

    public boolean canScrollRight();
    public void scrollRight() throws CannotScrollException;

    public void turnOnNote(int Note);
    public void turnOffNote(int Note);
}
