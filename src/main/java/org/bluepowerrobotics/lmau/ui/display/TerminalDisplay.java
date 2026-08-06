package org.bluepowerrobotics.lmau.ui.display;

public interface TerminalDisplay {
    public int[] getMaxPx();
    public int[] calculateMaxChar();
    public void callForKeyBoard();
}
