package org.bluepowerrobotics.lmau.ui.display.elements;

import org.bluepowerrobotics.lmau.ui.display.Text;

public class Picture implements Element{
    private Box picDiscriptionBox;



    @Override
    public int getMinX() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int setXGetY(int x) {
        return 0;
    }

    @Override
    public int setYGetX(int y) {
        return 0;
    }

    @Override
    public boolean setXY(int x, int y) {
        return false;
    }

    @Override
    public int[] getXY() {
        return new int[0];
    }

    @Override
    public Text getContent(int x, int y) {
        return null;
    }

    @Override
    public boolean ifFocusable() {
        return false;
    }

    @Override
    public void onFocus(boolean ifFocused, int x, int y) {

    }

    @Override
    public void onFocus(boolean ifFocused) {

    }

    @Override
    public boolean ifSelectable() {
        return false;
    }

    @Override
    public void onSelect(boolean ifSelected, int x, int y) {

    }

    @Override
    public void onSelect(boolean ifSelected) {

    }

    @Override
    public boolean canInput() {
        return false;
    }

    @Override
    public void onInput(char[] input) {

    }
}
