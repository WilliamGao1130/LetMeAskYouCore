package org.bluepowerrobotics.lmau.ui.display;

public interface TerminalDisplay {
    public int[] getMaxPx();
    public int[] calculateMaxChar();
    public int[] getPxPerChar();
    public void callForKeyBoard();
    public boolean pictureAvailable();
}
