package com.legendmohe.packetparser.compiler;

/**
 * Created by legendmohe on 2017/6/9.
 */
class Pattern {
    public String condition;
    public String attr;
    public String exp;
    public String opt;
    public int repeat;

    public Pattern(String condition, String attr, String exp, String opt, int repeat) {
        this.condition = condition;
        this.attr = attr;
        this.exp = exp;
        this.opt = opt;
        this.repeat = repeat;
    }

    public String getFormattedCondition() {
        if (condition == null) {
            return "";
        }
        String formattedCondition = condition;
        if (formattedCondition.contains("this.")) {
            formattedCondition = formattedCondition.replace("this.", "src.");
        }
        return formattedCondition;
    }

    public String getAttr() {
        return attr;
    }

    public String getFormattedExp() {
        if (exp == null) {
            return "";
        }
        String formattedExp = exp;
        if (formattedExp.contains("this.")) {
            formattedExp = formattedExp.replace("this.", "src.");
        }
        if (formattedExp.length() != 0 && (!formattedExp.startsWith("(") || !formattedExp.endsWith(")"))) {
            formattedExp = "(" + formattedExp + ")";
        }
        return formattedExp;
    }

    public String getOpt() {
        return opt;
    }

    public boolean containsIgnoreOpt() {
        return opt != null && opt.contains("~");
    }
}
