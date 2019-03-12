package io.onedev.commons.utils.command;

import io.onedev.commons.utils.StringUtils;

public class Argument {

    private String[] values;

    public void setValue(String value) {
        values = new String[] {value};
    }
    
    public void setLine(String line) {
        values = StringUtils.parseQuoteTokens(line);
    }
    
    public String[] getParts() {
        return values;
    }

}