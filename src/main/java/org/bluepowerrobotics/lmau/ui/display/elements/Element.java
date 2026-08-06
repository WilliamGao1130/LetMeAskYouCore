package org.bluepowerrobotics.lmau.ui.display.elements;

public interface Element {
    public int getMinX();
    public int getMinY();
    public int setXGetY(int x);
    public int setYGetX(int y);
    public boolean setXY(int x,int y);
    public int[] getXY();
    public Text getContent(int x,int y);
    public boolean ifFocusable();
    public void onFocus(boolean ifFocused, int x, int y);//mouse
    public void onFocus(boolean ifFocused);//keyboard
    public boolean ifSelectable();
    public void onSelect(boolean ifSelected, int x, int y);//mouse
    public void onSelect(boolean ifSelected);//keyboard

}
