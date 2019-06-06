package io.onedev.commons.jsyntax;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Splitter;

public class TokenTypes {

	public static int META = 		0x00000001;
	
	public static int KEYWORD = 	0x00000002;
	
	public static int ATOM =		0x00000004;
	
	public static int NUMBER = 		0x00000008;
	
	public static int DEF =			0x00000010;
	
	public static int VARIABLE = 	0x00000020;
	
	public static int VARIABLE2 =	0x00000040;
	
	public static int VARIABLE3 =	0x00000080;
	
	public static int PROPERTY =	0x00000100;
	
	public static int OPERATOR =	0x00000200;
	
	public static int COMMENT =		0x00000400;
	
	public static int STRING =		0x00000800;
	
	public static int STRING2 =		0x00001000;
	
	public static int QUALIFIER = 	0x00002000;
	
	public static int BUILTIN =		0x00004000;
	
	public static int BRACKET =		0x00008000;
	
	public static int TAG = 		0x00010000;
	
	public static int ATTRIBUTE =	0x00020000;
	
	public static int LINK =		0x00040000;
	
	public static int ERROR =		0x00080000;
	
	private static Map<Integer, String> TYPE_TO_NAME = new HashMap<>();
	
	private static Map<String, Integer> NAME_TO_TYPE = new HashMap<>();
	
	static {
		TYPE_TO_NAME.put(META, "meta");
		TYPE_TO_NAME.put(KEYWORD, "keyword");
		TYPE_TO_NAME.put(ATOM, "atom");
		TYPE_TO_NAME.put(NUMBER, "number");
		TYPE_TO_NAME.put(DEF, "def");
		TYPE_TO_NAME.put(VARIABLE, "variable");
		TYPE_TO_NAME.put(VARIABLE2, "variable-2");
		TYPE_TO_NAME.put(VARIABLE3, "variable-3");
		TYPE_TO_NAME.put(PROPERTY, "property");
		TYPE_TO_NAME.put(OPERATOR, "operator");
		TYPE_TO_NAME.put(COMMENT, "comment");
		TYPE_TO_NAME.put(STRING, "string");
		TYPE_TO_NAME.put(STRING2, "string-2");
		TYPE_TO_NAME.put(QUALIFIER, "qualifier");
		TYPE_TO_NAME.put(BUILTIN, "built-in");
		TYPE_TO_NAME.put(BRACKET, "bracket");
		TYPE_TO_NAME.put(TAG, "tag");
		TYPE_TO_NAME.put(ATTRIBUTE, "attribute");
		TYPE_TO_NAME.put(LINK, "link");
		TYPE_TO_NAME.put(ERROR, "error");
		
		for (Map.Entry<Integer, String> entry: TYPE_TO_NAME.entrySet())
			NAME_TO_TYPE.put(entry.getValue(), entry.getKey());
	}
	
	public static int getId(String names) {
		int id = 0;
		for (String name: Splitter.on(" ").trimResults().omitEmptyStrings().split(names)) {
			Integer type = NAME_TO_TYPE.get(name);
			if (type != null)
				id |= type;
		}
		return id;
	}
	
	public static String getNames(int id, @Nullable String prefix) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<Integer, String> entry: TYPE_TO_NAME.entrySet()) {
			if ((entry.getKey() & id) != 0) {
				if (builder.length() != 0)
					builder.append(" ");
				if (prefix != null)
					builder.append(prefix);
				builder.append(entry.getValue());
			}
		}
		return builder.toString();
	}
	
}
