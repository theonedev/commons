package io.onedev.commons.jsyntax;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import io.onedev.commons.utils.HtmlUtils;

public class TokenUtils {
	
	private static long MASK_BEGIN_POS = 	0xffff000000000000L;
	
	private static long MASK_END_POS = 		0x0000ffff00000000L;
	
	private static long MASK_TYPE = 		0x00000000ffffffffL;
	
	public static int getTypeId(long token) {
		return (int)(token & MASK_TYPE);
	}
	
	public static int getBeginPos(long token) {
		return (int)((token & MASK_BEGIN_POS) >>> 48);
	}
	
	public static int getEndPos(long token) {
		return (int)((token & MASK_END_POS) >>> 32);
	}
	
	public static boolean isType(long token, int type) {
		return (getTypeId(token) & type) != 0;
	}
	
	public static long getToken(int beginPos, int endPos, int typeId) {
		return (((long) beginPos) << 48) | (((long) endPos) << 32) | (long)typeId;
	}
	
	public static long getToken(int beginPos, int endPos, String typeNames) {
		return getToken(beginPos, endPos, TokenTypes.getId(typeNames));
	}
	
	public static String getText(String line, long token) {
		int beginPos = getBeginPos(token);
		int endPos = getEndPos(token);
		return line.substring(beginPos, endPos);
	}
	
	public static boolean isWhitespace(String line, long token) {
		if (getTypeId(token) != 0) {
			return false;
		} else {
			int beginPos = getBeginPos(token);
			int endPos = getEndPos(token);
			for (int i=beginPos; i<endPos; i++) {
				char ch = line.charAt(i);
				if (ch != ' ' && ch != '\n' && ch != '\r' && ch != '\t')
					return false;
			}
			return true;
		}
	}
	
	/**
	 * CodeMirror returns continuous spaces/tabs as a single token and it makes diff not ideal, 
	 * for instance, if we have three tabs as below:
	 * \t\t\t
	 * And if we add another tab:
	 * \t\t\t\t
	 * The diff will be a deletion of token "\t\t\t" and addition of token "\t\t\t\t", which is 
	 * not optimal. So we split a whitespace token containing multiple characters as multiple 
	 * single-character tokens to make the diff able to figure out that a single "\t" token is 
	 * inserted
	 * 
	 * @return
	 */
	@Nullable
	public static List<Long> splitWhitespace(String line, long token) {
		if (isWhitespace(line, token)) {
			List<Long> splitted = new ArrayList<>();
			int typeId = getTypeId(token);
			int beginPos = getBeginPos(token);
			int endPos = getEndPos(token);
			for (int i=beginPos; i<endPos; i++) {
				splitted.add(getToken(i, i+1, typeId));
			}
			return splitted;
		} else {
			return null;
		}
	}
	
	public static String toHtml(String line, long token, @Nullable String cssClasses, @Nullable String style) {
		return toHtml(new TextToken(getTypeId(token), getText(line, token)), cssClasses, style);
	}
	
	public static String toHtml(TextToken token, @Nullable String cssClasses, @Nullable String style) {
		String escapedText;
		if (token.getText().equals("\r"))
			escapedText = " ";
		else
			escapedText = HtmlUtils.escape(token.getText());
		
		StringBuilder cssClassesBuilder = new StringBuilder();
		cssClassesBuilder.append(TokenTypes.getNames(token.getType(), "cm-"));
		if (cssClasses != null) {
			if (cssClassesBuilder.length() != 0)
				cssClassesBuilder.append(" ");
			cssClassesBuilder.append(cssClasses);
		}
		
		if (cssClassesBuilder.length() != 0 || style != null) {
			StringBuilder htmlBuilder = new StringBuilder("<span ");
			if (cssClassesBuilder.length() != 0)
				htmlBuilder.append("class='").append(cssClassesBuilder).append("'");
			if (style != null)
				htmlBuilder.append("style='").append(style).append("'");
			htmlBuilder.append(">").append(escapedText).append("</span>");
			return htmlBuilder.toString();
		} else {
			return escapedText;
		}
	}
	
	public static String toString(String line, long token) {
		return toString(new TextToken(getTypeId(token), getText(line, token)));
	}

	public static String toString(TextToken token) {
		String typeNames = TokenTypes.getNames(token.getType(), null);
		if (StringUtils.isNotBlank(typeNames))
			return token.getText() + "[" + typeNames + "]";
		else
			return token.getText();
	}
	
	public static long[] toArray(List<Long> tokenList) {
		long[] tokenArray = new long[tokenList.size()];
		for (int i=0; i<tokenList.size(); i++)
			tokenArray[i] = tokenList.get(i);
		return tokenArray;
	}
	
	public static List<TextToken> getTextTokens(Tokenized tokenized) {
		List<TextToken> textTokens = new ArrayList<>();
		for (long token: tokenized.getTokens()) {
			textTokens.add(new TextToken(TokenUtils.getTypeId(token), 
					TokenUtils.getText(tokenized.getText(), token)));
		}
		return textTokens;
	}
	
}
