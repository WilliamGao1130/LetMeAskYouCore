package org.bluepowerrobotics.lmau.ui.input;

public interface Keyboard {
    public void CharReceivingRegister(KeyboardInputChar keyboardInputChar);
    public void CommandInputRegister(KeyboardCommandInput keyboardCommandInput);
    public void LongPressRegister(KeyboardLongPress keyboardLongPress);
    public static abstract class KeyboardInputChar{
        public abstract void onCharReceived(char[] chars);
        public abstract void onBackspaceReceived();
    }
    public static abstract class KeyboardCommandInput{
        public abstract void onCommandReceived(KEYBOARD_BUTTON[] commands);

    }
    public static abstract class KeyboardLongPress{
        public abstract void onLongPressStarted(KEYBOARD_BUTTON pressed);
        public abstract void onLongPressStopped(KEYBOARD_BUTTON pressed);
    }
    enum KEYBOARD_BUTTON{a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z
        ,zero,one,two,three,four,five,six,seven,eight,add,minus
        ,backtick,tilde,exclamation,at,hash,dollar,percent,caret,ampersand,asterisk
        ,leftParen,rightParen,underscore,equals
        ,leftBracket,rightBracket,backslash,leftBrace,rightBrace,pipe
        ,semicolon,colon,singleQuote,doubleQuote,comma,period,slash
        ,lessThan,greaterThan,questionMark
        ,control,option,alt,meta,win,command,esc,shift
        ,enter,backspace,delete,tab,space
        ,f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12
        ,pageUp,pageDown,home,end
        ,left,up,right,down
    }
}
