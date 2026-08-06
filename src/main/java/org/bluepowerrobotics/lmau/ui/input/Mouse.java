package org.bluepowerrobotics.lmau.ui.input;

public interface Mouse {
    public void ClickRegister(MouseClickInput mouseClickInput);
    public void DragRegister(MouseDragInput mouseDragInput);
    public void LongPressRegister(MouseLongPress mouseLongPress);
    public void WheelRegister(MouseWheelInput mouseWheelInput);

    public static abstract class MouseClickInput{
        public abstract void onClick(int x,int y,MOUSE_BUTTON mouseButton);
    }
    public static abstract class MouseDragInput{
        public abstract void onDragStarted(int x,int y,MOUSE_BUTTON mouseButton);
        public abstract void onDragMoved(int x,int y);
        public abstract void onDragEnded(int x,int y);
        public abstract void onDragCanceled();
    }
    public static abstract class MouseLongPress{
        public abstract void onLongPressStarted(int x,int y,MOUSE_BUTTON mouseButton);
        public abstract void onLongPressStopped(int x,int y,MOUSE_BUTTON mouseButton);
    }
    public static abstract class MouseWheelInput{
        public abstract void onWheelScrolled(int x,int y,int delta);
    }
    enum MOUSE_BUTTON{left,middle,right}
}
