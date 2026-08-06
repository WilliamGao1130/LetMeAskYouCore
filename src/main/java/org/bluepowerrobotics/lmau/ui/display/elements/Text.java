package org.bluepowerrobotics.lmau.ui.display.elements;

import org.bluepowerrobotics.lmau.ui.display.Color;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;

public class Text {
    private List<Text> textList=new ArrayList<Text>();
    Character content=null;
    Color color = new Color();
    int lengthTime = 1;
    public Text(String string){
        if(string.length()>1){
            for(char ch : string.toCharArray()){
                textList.add(new Text(String.valueOf(ch)));
            }
        }else{
            content = string.charAt(0);
        }
    }
    public Text(String string,Color color){
        if(string.length()>1){
            for(char ch : string.toCharArray()){
                textList.add(new Text(String.valueOf(ch),color,lengthTime));
            }
        }else{
            content=string.charAt(0);
            this.color =color;
        }
    }
    public Text(String string,Color color,int lengthTime){
        if(string.length()>1){
            for(char ch : string.toCharArray()){
                textList.add(new Text(String.valueOf(ch),color,lengthTime));
            }
        }else{
            content=string.charAt(0);
            this.color=color;
            this.lengthTime=lengthTime;
        }
    }
    public TEXT_TYPE getType(){
        if(textList!=null&&!textList.isEmpty()){
            return TEXT_TYPE.MULTI;
        }else{
            if (content==null) return TEXT_TYPE.NULL;
            int width=UCharacter.getIntPropertyValue(content, UProperty.EAST_ASIAN_WIDTH);
            boolean fullWidth=width==UCharacter.EastAsianWidth.FULLWIDTH
                    ||width==UCharacter.EastAsianWidth.WIDE
                    ||width==UCharacter.EastAsianWidth.AMBIGUOUS;
            return fullWidth?TEXT_TYPE.FULL_WIDTH:TEXT_TYPE.HALF_WIDTH;
        }
    }
    public char getChar(int index){
        if(textList!=null&&!textList.isEmpty()){
            return textList.get(index).content;
        }
        return content;
    }
    public TEXT_TYPE getType(int index){
        if(textList!=null&&!textList.isEmpty()){
            return textList.get(index).getType();
        }
        return getType();
    }
    enum TEXT_TYPE{MULTI,HALF_WIDTH,FULL_WIDTH,NULL}//NULL means the second place of a FULL_WIDTH character.
}
