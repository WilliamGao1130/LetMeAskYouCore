package org.bluepowerrobotics.lmau.ui;

import org.bluepowerrobotics.lmau.core.config.DisplayConfig;
import org.bluepowerrobotics.lmau.ui.display.TerminalDisplay;
import org.bluepowerrobotics.lmau.ui.globalization.Language;

public class Resource {
    private Resource(){

    }
    private static Resource instance=null;
    private static Resource setInstance(Language language, TerminalDisplay terminalDisplay, DisplayConfig  displayConfig){
        instance=new Resource();
        instance.language=language;
        instance.terminalDisplay = terminalDisplay;
        instance.displayConfig = displayConfig;
        return instance;
    }
    public static Resource getInstance(){
        if(instance!=null){
            return instance;
        }else{
            System.err.println("ui.Resource: getInstance should be called after setInstance!");
            return null;
        }
    }

    private Language language = null;
    private TerminalDisplay terminalDisplay = null;
    private DisplayConfig displayConfig = null;

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Language getLanguage(){
        return language;
    }

    public void setTerminalDisplay(TerminalDisplay terminalDisplay) {
        this.terminalDisplay = terminalDisplay;
    }

    public TerminalDisplay getTerminalDisplay() {
        return terminalDisplay;
    }

    public void setDisplayConfig(DisplayConfig displayConfig) {
        this.displayConfig = displayConfig;
    }

    public DisplayConfig getDisplayConfig() {
        return displayConfig;
    }
}
