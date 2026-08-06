package org.bluepowerrobotics.lmau.core.config;

import org.bluepowerrobotics.lmau.ui.display.Color;

public class DisplayConfig {
    private Runnable updateTheme() {
        return null;//should be replaced when building
    }

    private boolean tightMode=false;
    public void setTightMode(boolean tightMode){this.tightMode=tightMode;}
    public boolean getTightMode(){return tightMode;}

    public enum Theme { LIGHT, DARK, SYSTEM }
    private Theme theme = Theme.SYSTEM;
    private Theme usedTheme = theme;
    public Theme getSavedTheme() {
        return theme;
    }
    public Theme getUsedTheme(){
        Runnable task = updateTheme();
        if (task != null) {
            task.run();
        }
        return usedTheme;
    }
    public void setTheme(Theme theme) {
        this.theme = theme;
    }
    public Color getDefaultColor(){
        Theme theme1=getUsedTheme();
        switch (theme1){
            case LIGHT:
                return Color.valueOf(Color.BLACK);
            case DARK:
                return Color.valueOf(Color.WHITE);
            default:
                return Color.valueOf(Color.GRAY);
        }
    }
}
