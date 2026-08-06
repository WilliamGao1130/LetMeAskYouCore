package org.bluepowerrobotics.lmau.ui.display.elements;

public class Box implements Element{
    boolean hasFrame=false;
    int headInterval=1,leftInterval=2,bottomInterval=1,rightInterval=2;


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
}
