package org.bluepowerrobotics.lmau.core.config;

import org.bluepowerrobotics.lmau.util.Color;

public class DisplayConfig {
    public DisplayConfig(){}
    public enum Theme { LIGHT, DARK, SYSTEM }
    private ThemeUpdater themeUpdater =null;
    private boolean tightMode=false;
    private Theme theme = Theme.SYSTEM;
    private Theme usedTheme = Theme.valueOf(theme.name());
    public void setTightMode(boolean tightMode){this.tightMode=tightMode;}
    public boolean getTightMode(){return tightMode;}


    public Theme getSavedTheme() {
        return theme;
    }
    public Theme getUsedTheme(){
        if (themeUpdater != null) {
            themeUpdater.run(this);
        }else{
            usedTheme=theme;
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
                return Color.valueOf(Color.WHITE);
        }
    }
    public Color getDefaultBackGroundColor(){
        Theme theme1=getUsedTheme();
        switch (theme1){
            case LIGHT:
                return Color.valueOf(0x00FFFFFF);
            case DARK:
            default:
                return Color.valueOf(0x00000000);
        }
    }

    public void setThemeUpdater(ThemeUpdater themeUpdater) {
        this.themeUpdater = themeUpdater;
    }
    public static abstract class ThemeUpdater{
        protected void setUsedTheme(DisplayConfig displayConfig,Theme usedTheme){
            displayConfig.usedTheme = usedTheme;
        }
        public abstract void run(DisplayConfig displayConfig);
    }
}
