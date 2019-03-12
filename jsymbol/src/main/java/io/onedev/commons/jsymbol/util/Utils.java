package io.onedev.commons.jsymbol.util;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import io.onedev.commons.jsymbol.TokenPosition;

public class Utils {
	
	public static int[] getOrdinals(Enum<?> array[]) {
		int[] intArray = new int[array.length];
		for (int i=0; i<array.length; i++)
			intArray[i] = array[i].ordinal();
		return intArray;
	}
	
	public static TokenPosition getTokenPosition(Token token) {
		return new TokenPosition(token.getLine()-1, token.getCharPositionInLine(), token.getLine()-1, 
				token.getCharPositionInLine() + token.getText().length());
	}
	
	public static TokenPosition getTokenPosition(Token from, Token to) {
		return new TokenPosition(from.getLine()-1, from.getCharPositionInLine(), to.getLine()-1, 
				to.getCharPositionInLine() + to.getText().length());
	}

	public static TokenPosition getTokenPosition(ParserRuleContext context) {
		return getTokenPosition(context.start, context.stop);
	}
	
}
