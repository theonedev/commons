package io.onedev.commons.jsymbol.util;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import io.onedev.commons.utils.PlanarRange;

public class Utils {
	
	public static int[] getOrdinals(Enum<?> array[]) {
		int[] intArray = new int[array.length];
		for (int i=0; i<array.length; i++)
			intArray[i] = array[i].ordinal();
		return intArray;
	}
	
	public static PlanarRange getTextRange(Token token) {
		return new PlanarRange(token.getLine()-1, token.getCharPositionInLine(), token.getLine()-1, 
				token.getCharPositionInLine() + token.getText().length());
	}
	
	public static PlanarRange getTextRange(Token from, Token to) {
		return new PlanarRange(from.getLine()-1, from.getCharPositionInLine(), to.getLine()-1, 
				to.getCharPositionInLine() + to.getText().length());
	}

	public static PlanarRange getTextRange(ParserRuleContext context) {
		return getTextRange(context.start, context.stop);
	}
	
}
