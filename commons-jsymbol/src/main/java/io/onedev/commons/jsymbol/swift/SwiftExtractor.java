package io.onedev.commons.jsymbol.swift;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.swift.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.swift.symbols.SwiftSymbol;
import io.onedev.commons.jsymbol.swift.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.swift.symbols.VariableSymbol;
import io.onedev.commons.utils.PlanarRange;

public class SwiftExtractor extends AbstractSymbolExtractor<SwiftSymbol> {

	@Override
	public List<SwiftSymbol> extract(String fileName, String fileContent) {
		return new Scanner(fileContent).scan();
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "swift");
	}

	@Override
	public int getVersion() {
		return 1;
	}

	private static class Scanner {

		private final String source;

		private final String code;

		private final int[] rows;

		private final int[] columns;

		private final List<SwiftSymbol> symbols = new ArrayList<>();

		Scanner(String source) {
			this.source = source;
			code = maskCommentsAndStrings(source);
			rows = new int[source.length()+1];
			columns = new int[source.length()+1];
			int row = 0;
			int column = 0;
			for (int i=0; i<source.length(); i++) {
				rows[i] = row;
				columns[i] = column;
				if (source.charAt(i) == '\n') {
					row++;
					column = 0;
				} else {
					column++;
				}
			}
			rows[source.length()] = row;
			columns[source.length()] = column;
		}

		List<SwiftSymbol> scan() {
			scanDeclarations(0, code.length(), null);
			return symbols;
		}

		private void scanDeclarations(int start, int end, @Nullable SwiftSymbol parent) {
			int depth = 0;
			for (int index=start; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '{') {
					depth++;
				} else if (ch == '}') {
					depth = Math.max(0, depth-1);
				} else if (depth == 0 && ch == '@') {
					index = skipAttribute(index, end)-1;
				} else if (depth == 0 && isIdentifierStart(ch)) {
					DeclarationPrefix prefix = readDeclarationPrefix(index, end);
					index = prefix.index;
					if (index >= end || !isIdentifierStart(code.charAt(index)))
						continue;
					int wordEnd = readIdentifierEnd(index);
					String word = code.substring(index, wordEnd);
					if (isTypeKeyword(word)) {
						index = scanType(word, index, wordEnd, end, parent, prefix.local)-1;
					} else if ("extension".equals(word)) {
						index = scanExtension(index, wordEnd, end, parent, prefix.local)-1;
					} else if ("func".equals(word)) {
						index = scanFunction("func", index, wordEnd, end, parent, prefix.local)-1;
					} else if ("init".equals(word) || "deinit".equals(word) || "subscript".equals(word)) {
						index = scanSpecialFunction(word, index, wordEnd, end, parent, prefix.local)-1;
					} else if ("var".equals(word) || "let".equals(word)) {
						index = scanVariables(word, wordEnd, end, parent, prefix.local)-1;
					} else if ("case".equals(word) && parent != null) {
						index = scanEnumCases(wordEnd, end, parent)-1;
					} else if ("typealias".equals(word) || "associatedtype".equals(word)) {
						index = scanAlias(word, wordEnd, end, parent, prefix.local)-1;
					} else {
						index = wordEnd-1;
					}
				}
			}
		}

		private int scanType(String kind, int keywordStart, int afterKeyword, int end, @Nullable SwiftSymbol parent,
				boolean local) {
			int nameStart = skipWhitespace(afterKeyword, end);
			if (nameStart >= end || !isIdentifierStart(code.charAt(nameStart)))
				return afterKeyword;
			int nameEnd = readIdentifierEnd(nameStart);
			int bodyStart = findNextTopLevel(nameEnd, end, '{', '\0');
			int bodyEnd = -1;
			if (bodyStart < end && code.charAt(bodyStart) == '{')
				bodyEnd = findMatching(bodyStart, end, '{', '}');
			TypeSymbol symbol = new TypeSymbol(parent, code.substring(nameStart, nameEnd), kind,
					position(nameStart, nameEnd), bodyEnd != -1? range(bodyStart, bodyEnd): null, local);
			symbols.add(symbol);
			if (bodyEnd != -1) {
				scanDeclarations(bodyStart+1, bodyEnd, symbol);
				return bodyEnd+1;
			}
			return keywordStart+1;
		}

		private int scanExtension(int keywordStart, int afterKeyword, int end, @Nullable SwiftSymbol parent,
				boolean local) {
			int nameStart = skipWhitespace(afterKeyword, end);
			if (nameStart >= end)
				return afterKeyword;
			int nameEnd = readExtendedTypeEnd(nameStart, end);
			if (nameEnd <= nameStart)
				return afterKeyword;
			String name = normalizeName(source.substring(nameStart, nameEnd));
			int bodyStart = findNextTopLevel(nameEnd, end, '{', '\0');
			int bodyEnd = -1;
			if (bodyStart < end && code.charAt(bodyStart) == '{')
				bodyEnd = findMatching(bodyStart, end, '{', '}');
			TypeSymbol symbol = new TypeSymbol(parent, name, "extension", position(nameStart, nameEnd),
					bodyEnd != -1? range(bodyStart, bodyEnd): null, local);
			symbols.add(symbol);
			if (bodyEnd != -1) {
				scanDeclarations(bodyStart+1, bodyEnd, symbol);
				return bodyEnd+1;
			}
			return keywordStart+1;
		}

		private int scanFunction(String kind, int keywordStart, int afterKeyword, int end,
				@Nullable SwiftSymbol parent, boolean local) {
			int nameStart = skipWhitespace(afterKeyword, end);
			if (nameStart >= end || !isFunctionNameStart(code.charAt(nameStart)))
				return afterKeyword;
			int nameEnd = readFunctionNameEnd(nameStart);
			int paramsStart = findNextTopLevel(nameEnd, end, '(', '\n');
			String params = "";
			if (paramsStart < end && code.charAt(paramsStart) == '(') {
				int paramsEnd = findMatching(paramsStart, end, '(', ')');
				if (paramsEnd != -1)
					params = source.substring(paramsStart, paramsEnd+1);
			}
			int bodyStart = findNextTopLevel(nameEnd, end, '{', '\n');
			int bodyEnd = -1;
			if (bodyStart < end && code.charAt(bodyStart) == '{')
				bodyEnd = findMatching(bodyStart, end, '{', '}');
			symbols.add(new FunctionSymbol(parent, source.substring(nameStart, nameEnd), kind, params,
					position(nameStart, nameEnd), bodyEnd != -1? range(bodyStart, bodyEnd): null, local));
			if (bodyEnd != -1)
				return bodyEnd+1;
			return paramsStart < end? paramsStart+1: nameEnd;
		}

		private int scanSpecialFunction(String kind, int keywordStart, int afterKeyword, int end,
				@Nullable SwiftSymbol parent, boolean local) {
			int nameStart = keywordStart;
			int paramsStart = findNextTopLevel(afterKeyword, end, '(', '\n');
			String params = "";
			if (paramsStart < end && code.charAt(paramsStart) == '(') {
				int paramsEnd = findMatching(paramsStart, end, '(', ')');
				if (paramsEnd != -1)
					params = source.substring(paramsStart, paramsEnd+1);
			}
			int bodyStart = findNextTopLevel(afterKeyword, end, '{', '\n');
			int bodyEnd = -1;
			if (bodyStart < end && code.charAt(bodyStart) == '{')
				bodyEnd = findMatching(bodyStart, end, '{', '}');
			symbols.add(new FunctionSymbol(parent, kind, kind, params, position(nameStart, afterKeyword),
					bodyEnd != -1? range(bodyStart, bodyEnd): null, local));
			if (bodyEnd != -1)
				return bodyEnd+1;
			return paramsStart < end? paramsStart+1: afterKeyword;
		}

		private int scanVariables(String kind, int afterKeyword, int end, @Nullable SwiftSymbol parent, boolean local) {
			int index = afterKeyword;
			while (index < end) {
				index = skipWhitespace(index, end);
				while (index < end && code.charAt(index) == '(') {
					int patternEnd = findMatching(index, end, '(', ')');
					if (patternEnd == -1)
						return index;
					scanPatternVariables(kind, index+1, patternEnd, parent, local);
					index = skipWhitespace(patternEnd+1, end);
					if (index < end && code.charAt(index) == ',') {
						index++;
						continue;
					}
					return skipVariableTail(index, end);
				}
				if (index >= end || !isIdentifierStart(code.charAt(index)))
					return afterKeyword;
				int nameEnd = readIdentifierEnd(index);
				symbols.add(new VariableSymbol(parent, code.substring(index, nameEnd), kind, position(index, nameEnd),
						null, local));
				index = skipVariableTail(nameEnd, end);
				if (index >= end || code.charAt(index-1) != ',')
					return index;
			}
			return index;
		}

		private void scanPatternVariables(String kind, int start, int end, @Nullable SwiftSymbol parent, boolean local) {
			int depth = 0;
			for (int index=start; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '(' || ch == '[' || ch == '<')
					depth++;
				else if (ch == ')' || ch == ']' || ch == '>')
					depth = Math.max(0, depth-1);
				else if (depth == 0 && isIdentifierStart(ch)) {
					int nameEnd = readIdentifierEnd(index);
					symbols.add(new VariableSymbol(parent, code.substring(index, nameEnd), kind,
							position(index, nameEnd), null, local));
					index = nameEnd-1;
				}
			}
		}

		private int scanEnumCases(int afterKeyword, int end, SwiftSymbol parent) {
			int index = afterKeyword;
			while (index < end) {
				index = skipWhitespace(index, end);
				if (index >= end || !isIdentifierStart(code.charAt(index)))
					return afterKeyword;
				int nameEnd = readIdentifierEnd(index);
				symbols.add(new VariableSymbol(parent, code.substring(index, nameEnd), "case",
						position(index, nameEnd), null, false));
				index = skipVariableTail(nameEnd, end);
				if (index >= end || code.charAt(index-1) != ',')
					return index;
			}
			return index;
		}

		private int scanAlias(String kind, int afterKeyword, int end, @Nullable SwiftSymbol parent, boolean local) {
			int nameStart = skipWhitespace(afterKeyword, end);
			if (nameStart >= end || !isIdentifierStart(code.charAt(nameStart)))
				return afterKeyword;
			int nameEnd = readIdentifierEnd(nameStart);
			symbols.add(new TypeSymbol(parent, code.substring(nameStart, nameEnd), kind, position(nameStart, nameEnd),
					null, local));
			return skipDeclarationTail(nameEnd, end);
		}

		private DeclarationPrefix readDeclarationPrefix(int index, int end) {
			boolean local = false;
			while (true) {
				index = skipWhitespace(index, end);
				while (index < end && code.charAt(index) == '@') {
					index = skipAttribute(index, end);
					index = skipWhitespace(index, end);
				}
				String word = readWord(index);
				if (isAccessKeyword(word)) {
					index = readIdentifierEnd(index);
					index = skipWhitespace(index, end);
					if (index < end && code.charAt(index) == '(') {
						int detailEnd = findMatching(index, end, '(', ')');
						if (detailEnd != -1)
							index = detailEnd+1;
					} else {
						local |= "private".equals(word) || "fileprivate".equals(word);
					}
				} else if ("class".equals(word) && isMemberModifier(index, end)) {
					index = readIdentifierEnd(index);
				} else if (isDeclarationModifier(word)) {
					index = readIdentifierEnd(index);
				} else {
					return new DeclarationPrefix(index, local);
				}
			}
		}

		private int skipAttribute(int index, int end) {
			index++;
			if (index < end && isIdentifierStart(code.charAt(index)))
				index = readIdentifierEnd(index);
			index = skipWhitespace(index, end);
			if (index < end && code.charAt(index) == '(') {
				int attrEnd = findMatching(index, end, '(', ')');
				if (attrEnd != -1)
					index = attrEnd+1;
			}
			return index;
		}

		private int skipVariableTail(int index, int end) {
			int depth = 0;
			for (; index<end; index++) {
				char ch = code.charAt(index);
				if (depth == 0 && (ch == ',' || ch == '\n' || ch == ';'))
					return index+1;
				if (depth == 0 && ch == '{') {
					int bodyEnd = findMatching(index, end, '{', '}');
					return bodyEnd != -1? bodyEnd+1: index+1;
				}
				if (ch == '(' || ch == '[' || ch == '<')
					depth++;
				else if (ch == ')' || ch == ']' || ch == '>')
					depth = Math.max(0, depth-1);
			}
			return index;
		}

		private int skipDeclarationTail(int index, int end) {
			for (; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '\n' || ch == ';')
					return index+1;
			}
			return index;
		}

		private int readExtendedTypeEnd(int index, int end) {
			int depth = 0;
			boolean whereClause = false;
			for (; index<end; index++) {
				char ch = code.charAt(index);
				if (depth == 0 && (ch == '{' || ch == '\n'))
					return index;
				if (depth == 0 && ch == ':' && !whereClause)
					return index;
				if (depth == 0 && isIdentifierStart(ch)) {
					int wordEnd = readIdentifierEnd(index);
					if ("where".equals(code.substring(index, wordEnd)))
						whereClause = true;
					index = wordEnd-1;
					continue;
				}
				if (ch == '<' || ch == '(' || ch == '[')
					depth++;
				else if (ch == '>' || ch == ')' || ch == ']')
					depth = Math.max(0, depth-1);
			}
			return index;
		}

		private int findNextTopLevel(int index, int end, char first, char second) {
			int depth = 0;
			for (; index<end; index++) {
				char ch = code.charAt(index);
				if (depth == 0 && (ch == first || ch == second))
					return index;
				if (ch == '(' || ch == '[' || ch == '<')
					depth++;
				else if (ch == ')' || ch == ']' || ch == '>')
					depth = Math.max(0, depth-1);
			}
			return end;
		}

		private int findMatching(int open, int end, char openChar, char closeChar) {
			int depth = 0;
			for (int index=open; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == openChar)
					depth++;
				else if (ch == closeChar) {
					depth--;
					if (depth == 0)
						return index;
				}
			}
			return -1;
		}

		private int skipWhitespace(int index, int end) {
			while (index < end && Character.isWhitespace(code.charAt(index)))
				index++;
			return index;
		}

		private String readWord(int index) {
			if (index >= code.length() || !isIdentifierStart(code.charAt(index)))
				return "";
			return code.substring(index, readIdentifierEnd(index));
		}

		private int readIdentifierEnd(int index) {
			index++;
			while (index < code.length() && isIdentifierPart(code.charAt(index)))
				index++;
			return index;
		}

		private int readFunctionNameEnd(int index) {
			index++;
			while (index < code.length() && isFunctionNamePart(code.charAt(index)))
				index++;
			return index;
		}

		private boolean isTypeKeyword(String word) {
			return "class".equals(word) || "struct".equals(word) || "enum".equals(word)
					|| "protocol".equals(word) || "actor".equals(word);
		}

		private boolean isAccessKeyword(String word) {
			return "open".equals(word) || "public".equals(word) || "package".equals(word)
					|| "internal".equals(word) || "fileprivate".equals(word) || "private".equals(word);
		}

		private boolean isDeclarationModifier(String word) {
			return "static".equals(word) || "final".equals(word)
					|| "override".equals(word) || "mutating".equals(word) || "nonmutating".equals(word)
					|| "convenience".equals(word) || "required".equals(word) || "optional".equals(word)
					|| "indirect".equals(word) || "lazy".equals(word) || "weak".equals(word)
					|| "unowned".equals(word) || "async".equals(word) || "throws".equals(word)
					|| "rethrows".equals(word);
		}

		private boolean isMemberModifier(int index, int end) {
			index = skipWhitespace(readIdentifierEnd(index), end);
			String word = readWord(index);
			return "func".equals(word) || "var".equals(word) || "let".equals(word) || "subscript".equals(word);
		}

		private boolean isIdentifierStart(char ch) {
			return Character.isLetter(ch) || ch == '_' || ch == '`';
		}

		private boolean isIdentifierPart(char ch) {
			return isIdentifierStart(ch) || Character.isDigit(ch);
		}

		private boolean isFunctionNameStart(char ch) {
			return isIdentifierStart(ch) || "+-*/%=!<>&|^~?".indexOf(ch) != -1;
		}

		private boolean isFunctionNamePart(char ch) {
			return isIdentifierPart(ch) || "+-*/%=!<>&|^~?".indexOf(ch) != -1;
		}

		private String normalizeName(String name) {
			return name.trim().replaceAll("\\s+", " ");
		}

		private PlanarRange position(int start, int end) {
			return new PlanarRange(rows[start], columns[start], rows[end-1], columns[end-1]+1);
		}

		private PlanarRange range(int start, int end) {
			return new PlanarRange(rows[start], columns[start], rows[end], columns[end]+1);
		}

		private static String maskCommentsAndStrings(String source) {
			StringBuilder builder = new StringBuilder(source);
			for (int index=0; index<builder.length(); index++) {
				char ch = builder.charAt(index);
				if (ch == '/' && index+1 < builder.length() && builder.charAt(index+1) == '/') {
					builder.setCharAt(index++, ' ');
					builder.setCharAt(index, ' ');
					while (index+1 < builder.length() && builder.charAt(index+1) != '\n')
						builder.setCharAt(++index, ' ');
				} else if (ch == '/' && index+1 < builder.length() && builder.charAt(index+1) == '*') {
					builder.setCharAt(index++, ' ');
					builder.setCharAt(index, ' ');
					int depth = 1;
					while (index+1 < builder.length() && depth != 0) {
						char current = builder.charAt(++index);
						if (current != '\n')
							builder.setCharAt(index, ' ');
						if (current == '/' && index+1 < builder.length() && builder.charAt(index+1) == '*') {
							builder.setCharAt(++index, ' ');
							depth++;
						} else if (current == '*' && index+1 < builder.length() && builder.charAt(index+1) == '/') {
							builder.setCharAt(++index, ' ');
							depth--;
						}
					}
				} else if (ch == '#') {
					if (index+1 < builder.length() && builder.charAt(index+1) == '"')
						index = maskRawString(builder, index);
				} else if (ch == '"' || ch == '\'') {
					index = maskQuotedString(builder, index, ch);
				}
			}
			return builder.toString();
		}

		private static int maskRawString(StringBuilder builder, int index) {
			int hashes = 0;
			while (index + hashes < builder.length() && builder.charAt(index + hashes) == '#')
				hashes++;
			int quote = index + hashes;
			if (quote >= builder.length() || builder.charAt(quote) != '"')
				return index;
			for (int i=index; i<=quote; i++)
				builder.setCharAt(i, ' ');
			for (int i=quote+1; i<builder.length(); i++) {
				char current = builder.charAt(i);
				if (current != '\n')
					builder.setCharAt(i, ' ');
				if (current == '"') {
					boolean closes = i + hashes < builder.length();
					for (int j=0; closes && j<hashes; j++) {
						if (builder.charAt(i+j+1) != '#')
							closes = false;
					}
					if (closes) {
						for (int j=0; j<hashes; j++)
							builder.setCharAt(i+j+1, ' ');
						return i + hashes;
					}
				}
			}
			return builder.length()-1;
		}

		private static int maskQuotedString(StringBuilder builder, int index, char quote) {
			builder.setCharAt(index, ' ');
			boolean multiline = quote == '"' && index+2 < builder.length()
					&& builder.charAt(index+1) == '"' && builder.charAt(index+2) == '"';
			if (multiline) {
				builder.setCharAt(++index, ' ');
				builder.setCharAt(++index, ' ');
			}
			while (index+1 < builder.length()) {
				char current = builder.charAt(++index);
				if (current != '\n')
					builder.setCharAt(index, ' ');
				if (current == '\\' && index+1 < builder.length()) {
					if (builder.charAt(index+1) != '\n')
						builder.setCharAt(index+1, ' ');
					index++;
				} else if (current == quote) {
					if (!multiline)
						break;
					if (index+2 < builder.length() && builder.charAt(index+1) == '"'
							&& builder.charAt(index+2) == '"') {
						builder.setCharAt(++index, ' ');
						builder.setCharAt(++index, ' ');
						break;
					}
				}
			}
			return index;
		}

	}

	private static class DeclarationPrefix {

		private final int index;

		private final boolean local;

		private DeclarationPrefix(int index, boolean local) {
			this.index = index;
			this.local = local;
		}

	}

}
