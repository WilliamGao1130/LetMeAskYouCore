package org.bluepowerrobotics.lmau.ui.display.elements;

public class Box implements Element{
    boolean hasFrame=false;
    int headInterval=1,leftInterval=2,bottomInterval=1,rightInterval=2;

    public int getHeadInterval() {
        return headInterval;
    }

    public int getLeftInterval() {
        return leftInterval;
    }

    public int getBottomInterval() {
        return bottomInterval;
    }

    public int getRightInterval() {
        return rightInterval;
    }

    Element content;
    private int x=-1,y=-1;

    @Override
    public int getMinX() {
        if(content==null) return getLeftInterval()+getRightInterval();
        return content.getMinX()+getLeftInterval()+getRightInterval();
    }

    @Override
    public int getMinY() {
        if(content==null) return getHeadInterval()+getBottomInterval();
        return content.getMinY()+getHeadInterval()+getBottomInterval();
    }

    @Override
    public int setXGetY(int x) {
        if(x<getMinX()) return -1;
        this.x=x;
        int gy=content.setXGetY(x-getLeftInterval()-getRightInterval());
        if(gy!=-1) {
            this.y = gy + getHeadInterval() + getBottomInterval();
            return y;
        }else{
            return -1;
        }
    }

    @Override
    public int setYGetX(int y) {
        if(y<getMinY()) return -1;
        this.y=y;
        int gx=content.setYGetX(y-getHeadInterval()-getBottomInterval());
        if(gx!=-1) {
            this.x = gx + getLeftInterval() + getRightInterval();
            return x;
        }else{
            return -1;
        }
    }

    @Override
    public boolean setXY(int x, int y) {
        if(x<getMinX()) return false;
        if(y<getMinY()) return false;
        if(content.setXY(x-getLeftInterval()-getRightInterval(),
                y-getHeadInterval()-getBottomInterval())) {
            this.x=x;
            this.y=y;
            return true;
        }
        return false;
    }

    @Override
    public int[] getXY() {
        return new int[]{x, y};
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
}
