package io.onedev.commons.jsyntax;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class TextToken implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int type;
	
	private final String text;
	
	public TextToken(int type, String text) {
		this.type = type;
		this.text = text;
	}

	public int getType() {
		return type;
	}

	public String getText() {
		return text;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof TextToken)) 
			return false;
		if (this == other)
			return true;
		TextToken otherToken = (TextToken) other;
		return new EqualsBuilder()
			.append(type, otherToken.getType())
			.append(text, otherToken.getText())
			.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(getType())
			.append(getText())
			.toHashCode();
	}		
	
}
