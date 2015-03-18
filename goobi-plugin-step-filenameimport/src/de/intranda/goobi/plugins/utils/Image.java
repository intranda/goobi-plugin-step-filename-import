package de.intranda.goobi.plugins.utils;

public class Image {

    private String logicalNo;
    private String physicalNo;
    private String docstruct;

    public Image(String logicalNo, String physicalNo, String docstruct) {

        this.physicalNo = physicalNo;

        // X = ohne Pagnierung
        if (logicalNo.startsWith("X")) {
            logicalNo = "uncounted";
        }
        // A = arabische Ziffer
        else if (logicalNo.startsWith("A")) {
            this.logicalNo = String.valueOf(new Integer(logicalNo = logicalNo.replace("A", "")));
        }
        // R = römische Zahl in Großbuchstaben
        else if (logicalNo.startsWith("R")) {
            this.logicalNo = integerToRomanNumeral(new Integer(logicalNo = logicalNo.replace("R", "")));
        }
        // r = römische Zahl in Kleinbuchstaben
        else if (logicalNo.startsWith("r")) {
            this.logicalNo = integerToRomanNumeral(new Integer(logicalNo = logicalNo.replace("r", ""))).toLowerCase();
        } else {
            // fallback, should never happen
            this.logicalNo = logicalNo;
        }
        switch (docstruct) {
            case "volume":
                this.docstruct = "Volume";
                break;
            case "text":
                // TODO change to text after ruleset changes
                this.docstruct = "Chapter";
                break;
            case "image":
                this.docstruct = "Illustration";
                break;
            case "cover_front":
                this.docstruct = "CoverFront";
                break;
            case "cover_back":
                this.docstruct = "CoverBack";
                break;
            case "title_page":
                this.docstruct = "TitlePage";
                break;
            case "contents":
                this.docstruct = "Contents";
                break;
            case "preface":
                this.docstruct = "Preface";
                break;
            case "index":
                this.docstruct = "Index";
                break;
            case "additional":
                this.docstruct = "Additional";
                break;

            default:
                this.docstruct = "Text";
                break;
        }

    }

    public String getDocstruct() {
        return docstruct;
    }

    public String getLogicalNo() {
        return logicalNo;
    }

    public String getPhysicalNo() {
        return physicalNo;
    }

    private String integerToRomanNumeral(int input) {
        String s = "";
        while (input >= 1000) {
            s += "M";
            input -= 1000;
        }
        while (input >= 900) {
            s += "CM";
            input -= 900;
        }
        while (input >= 500) {
            s += "D";
            input -= 500;
        }
        while (input >= 400) {
            s += "CD";
            input -= 400;
        }
        while (input >= 100) {
            s += "C";
            input -= 100;
        }
        while (input >= 90) {
            s += "XC";
            input -= 90;
        }
        while (input >= 50) {
            s += "L";
            input -= 50;
        }
        while (input >= 40) {
            s += "XL";
            input -= 40;
        }
        while (input >= 10) {
            s += "X";
            input -= 10;
        }
        while (input >= 9) {
            s += "IX";
            input -= 9;
        }
        while (input >= 5) {
            s += "V";
            input -= 5;
        }
        while (input >= 4) {
            s += "IV";
            input -= 4;
        }
        while (input >= 1) {
            s += "I";
            input -= 1;
        }
        return s;
    }
}
