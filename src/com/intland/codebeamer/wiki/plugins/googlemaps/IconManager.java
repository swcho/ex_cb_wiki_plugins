package com.intland.codebeamer.wiki.plugins.googlemaps;

import java.util.ArrayList;

public class IconManager {

    public static ArrayList icons = new ArrayList();

    public static void clear() {
        icons.clear();
    }

    public static String createIcon(String name) {
        if(icons.contains(name)) return "";
        icons.add(name);
        String icon = "icon"+name;
        String iconCode="";
        String iconURL = PropertyManager.getProperty("icon."+name);
        iconCode += "var "+icon+" = new GIcon();\n";
        iconCode += icon+".image = \""+iconURL+"\";\n";
        iconCode += icon+".shadow = \"http://labs.google.com/ridefinder/images/mm_20_shadow.png\";\n";
        iconCode += icon+".iconSize = new GSize(12, 20);\n";
        iconCode += icon+".shadowSize = new GSize(22, 20);\n";
        iconCode += icon+".iconAnchor = new GPoint(6, 20);\n";
        iconCode += icon+".infoWindowAnchor = new GPoint(5, 1);\n";
        return iconCode;
    }

}
