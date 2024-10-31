package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CssServlet extends ClientServlet {

    public static enum DisplayColor {
        DEFAULT("default", "#990000"),
        BLUE("blue", "#002144"),
        GREEN("green", "#003300"),
        RED("red", "#660000"),
        PURPLE("purple", "#330033"),
        BLUE_GREEN("blue-green", "#003333"),
        MAROON("maroon", "#330000"),
        VIOLET_RED("violet-red", "#7D0541"),
        PLUM("plum", "#B93B8F"),
        CYAN("cyan", "#307D7E"),
        LIGHT_SLATE_BLUE("light-slate-blue", "#736AFF"),
        LIGHT_SKY_BLUE("light-sky-blue", "#566D7E"),
        PALE_TURQUOISE("pale-turquoise", "#5E7D7E"),
        DARK_SEA_GREAN("dark-sea-grean", "#617C58"),
        MEDIUM_AQUA_MARINE("medium-aqua-marine", "#348781"),
        DARK_GREEN("dark-green", "#254117"),
        SIENNA("sienna", "#8A4117"),
        PINK("pink", "#7D2252"),
        GOLDENROD("goldenrod", "#817339");

        private String label;
        private String color;

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }

        DisplayColor(String label, String color) {
            this.label = label;
            this.color = color;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final String DISPLAY_SIZE_SMALL = "small";
    public static final String DISPLAY_SIZE_LARGE = "large";

    public static final String[] DISPLAY_SIZE = { DISPLAY_SIZE_SMALL, DISPLAY_SIZE_LARGE };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PrintWriter out = new PrintWriter(resp.getOutputStream());
        String displaySize = "small";
        String displayColorLabel = "";

        if (req.getParameter("displaySize") != null) {
            displaySize = req.getParameter("displaySize");
        }

        if (req.getParameter("displayColor") != null) {
            displayColorLabel = req.getParameter("displayColor");
        }

        resp.setContentType("text/css");

        String backgroundColor = DisplayColor.DEFAULT.getColor();
        if (displayColorLabel.startsWith("#")) {
            backgroundColor = displayColorLabel;
        } else {
            for (DisplayColor displayColor : DisplayColor.values()) {
                if (displayColor.getLabel().equals(displayColorLabel)) {
                    backgroundColor = displayColor.getColor();
                    break;
                }
            }
        }

        out.println("body {font-family: Tahoma, Geneva, sans-serif; background:#FFFFFF}");
        out.println("p {width:650px; color:#2B3E42;}");
        if (displaySize.equals(DISPLAY_SIZE_LARGE)) {
            out.println(".menu {width:100%; padding:0px; background-color:" + backgroundColor
                    + "; color: #DDDDDD;margin: 0px; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        } else {
            out.println(".menu {width:720px; padding:0px; background-color:" + backgroundColor
                    + "; color: #DDDDDD; margin: 0px; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        }

        out.println(".main {width:720px; padding:0px; margin:0px; }");
        out.println("#projectNavigation {float: right; width: 270px; }");
        out.println("#projectInfo {width: 440px; }");
        if (displaySize.equals(DISPLAY_SIZE_LARGE)) {
            out.println("#takeAction {position: absolute; left: 740px; top: 55px; width: 700px;}");
        } else {
            out.println("#takeAction {width: 580px; margin-top: 10px; }");
        }
        out.println("#providerNavigationBox {position: absolute; left: 1340px; top: 0px;}");
        out.println("textarea {");
        out.println("        width: 100%;");
        out.println("        padding: 3px;");
        out.println("        box-sizing: border-box; /* Ensures padding is included in the width */");
        out.println("        resize: vertical; /* Allows vertical resizing but prevents horizontal resizing */");
        out.println("    }");

        out.println("/* Container for the three columns */");
        out.println("#three-column-container {");
        out.println("    display: flex;");
        out.println("    justify-content: space-between;");
        out.println("    width: 100%;");
        out.println("    padding: 0px; /* Adjust as needed */");
        out.println("    box-sizing: border-box;");
        out.println("}");
        out.println("");
        out.println("/* Common styling for each action section */");
        out.println("#actionNow, #actionLater, #actionNext {");
        out.println("    padding: 4px; /* Adjust for your preference */");
        out.println("    box-sizing: border-box;");
        out.println("}");
        out.println("");
        out.println("/* Specific width for each column */");
        out.println("#actionNow, #actionLater {");
        out.println("    width: 35%; /* Adjust as needed */");
        out.println("}");
        out.println("");
        out.println("#actionNext {");
        out.println("    width: 30%; /* Adjust as needed */");
        out.println("}");

        out.println("a:link.menuLink {padding:5px; color:#AAAAAA; text-decoration:none}");
        out.println("a:visited.menuLink {padding:5px; color:#AAAAAA; text-decoration:none}");
        out.println("a:hover.menuLink {padding:5px; color:#FFFFFF; text-decoration:none}");
        out.println("a:active.menuLink {padding:5px; color:#AAAAAA; text-decoration:none}");
        out.println(
                "a:link.menuLinkSelected {padding:5px; color:#444444; text-decoration:none; background: #FFFFFF; border-top-style: solid; border-width: 1px; border-left-style:solid; border-right-style:solid;}");
        out.println(
                "a:visited.menuLinkSelected {padding:5px; color:#444444; text-decoration:none; background: #FFFFFF; border-top-style: solid; border-width: 1px;border-left-style:solid; border-right-style:solid;}");
        out.println(
                "a:hover.menuLinkSelected {padding:5px; color:#000000; text-decoration:none; background: #FFFFFF; border-top-style: solid; border-width: 1px;border-left-style:solid; border-right-style:solid;}");
        out.println(
                "a:active.menuLinkSelected {padding:5px; color:#444444; text-decoration:none; background: #FFFFFF; border-top-style: solid; border-width: 1px;border-left-style:solid; border-right-style:solid;}");
        out.println(
                ".submenu {padding:5px; background-color:#F7F3E8; margin: 0px; border-color: #2B3E42; border-width: 1px; border-style: solid; border-width: 1px;border-left-style:solid; border-right-style:solid;}");
        out.println("th {background-color: #DDDDDD; text-align: left; vertical-align:top;}");
        out.println(
                "th.title {background-color: " + backgroundColor + "; color: #DDDDDD; padding-left: 5px;}");
        out.println("td {text-align: left; vertical-align:top;}");
        out.println("td.right {text-align: right;}");
        out.println("pre {background:#F7F3E8; padding:4px; margin: 2px; }");
        out.println(".scrollbox {width: 700px; overflow:auto;}");
        out.println(".pass {background:#77BED2; padding-left:5px;}");
        out.println(
                ".fail {background:#FFFF99; padding-left:5px; padding-right:5px; border-style: solid; border-width: 1px; border-color: #2B3E42}");
        out.println(".help {width:700px; padding: 10px;}");
        out.println(
                ".smallTitle {padding-right: 5px; padding-left: 5px; border-style:solid; border-width: 1px; border-color: #747E80;}");
        out.println(
                ".boxed {border-collapse: collapse; border-width: 1px; border-style: solid; padding-left:5px; padding-right:5px; border-color: #2B3E42;}");
        out.println(
                ".boxed-full {border-collapse: collapse; border-width: 1px; border-style: solid; padding-left:5px; padding-right:5px; border-color: #2B3E42; width: 100%;}");
        out.println(
                ".boxed-fill {border-collapse: collapse; border-width: 1px; border-style: solid; padding-left:5px; padding-right:5px; border-color: #2B3E42; width: 100%; }");
        out.println(
                ".boxed-submit {border-collapse: collapse; border-width: 1px; border-style: solid; padding-left:5px; padding-right:5px; border-color: #2B3E42; text-align: right;}");
        out.println(".right {float: right;}");
        out.println(".together {white-space:nowrap;}");
        out.println(
                "td.outside {border-collapse: collapse; border-width: 1px; border-style: solid; padding:0px;border-color: #2B3E42;}");
        out.println(
                "table.inside {border-collapse: collapse; border-style: none; padding-left:5px; padding-right:5px;  }");
        out.println("th.inside {padding-left:5px; padding-right:5px;  }");
        out.println("td.inside {padding-left:5px; padding-right:5px;  }");
        out.println("td.inside-highlight {padding-left:5px; padding-right:5px; background:#FFFF99; }");
        out.println(
                "td.boxed-highlight {border-collapse: collapse; border-width: 1px; border-style: solid; padding-left:5px; padding-right:5px; border-color: #2B3E42; background:#FFFF99; }");
        out.println(
                "td.boxed-lowlight {border-collapse: collapse; border-width: 1px; border-style: solid; padding-left:5px; padding-right:5px; border-color: #2B3E42; background:#EEEEEE; }");

        out.println(
                "a:link.button {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFFFF; border-color: #FFFFFF; border-width: 1px; border-style: solid}");
        out.println(
                "a:visited.button {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFFFF; border-color: #FFFFFF; border-width: 1px; border-style: solid}");
        out.println(
                "a:hover.button {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFF99; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        out.println(
                "a:active.button {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFFFF; border-color: #FFFFFF; border-width: 1px; border-style: solid}");

        out.println(
                "a:link.box {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFFFF; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        out.println(
                "a:visited.box {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFFFF; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        out.println(
                "a:hover.box {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFF99; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        out.println(
                "a:active.box {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFFFF; border-color: #2B3E42; border-width: 1px; border-style: solid}");

        out.println(
                ".optionBox {position:absolute; border-width: 1px; border-style: solid; padding:5px; border-color: #2B3E42; visibility: hidden; }");
        out.println(".editAction {display: none; }");

        out.println(
                "a:link.timerStopped {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFFFF; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        out.println(
                "a:visited.timerStopped {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFFFF; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        out.println(
                "a:hover.timerStopped {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFF99; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        out.println(
                "a:active.timerStopped {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFFFF; border-color: #2B3E42; border-width: 1px; border-style: solid}");

        out.println(
                "a:link.timerRunning {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFF99; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        out.println(
                "a:visited.timerRunning {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFF99; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        out.println(
                "a:hover.timerRunning {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFFFF; border-color: #2B3E42; border-width: 1px; border-style: solid}");
        out.println(
                "a:active.timerRunning {margin-top: 1px; margin-bottom: 1px; padding-left:3px; padding-right:3px; color:#000000; text-decoration:none; background-color:#FFFF99; border-color: #2B3E42; border-width: 1px; border-style: solid}");

        out.close();
    }
}
