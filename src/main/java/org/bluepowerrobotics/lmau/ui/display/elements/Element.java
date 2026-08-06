package org.bluepowerrobotics.lmau.ui.display.elements;

public interface Element {
    public int getMinX();
    public int getMinY();
    public int setXGetY(int x);
    public int setYGetX(int y);
    public boolean setXY(int x,int y);
    public int[] getXY();
    public Text getContent(int x,int y);
}
