package io.onedev.commons.jsymbol.rust;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.rust.symbols.BlockSymbol;
import io.onedev.commons.jsymbol.rust.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.rust.symbols.RustSymbol;
import io.onedev.commons.jsymbol.rust.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.rust.symbols.VariableSymbol;
import io.onedev.commons.utils.PlanarRange;

public class RustExtractor extends AbstractSymbolExtractor<RustSymbol> {

	@Override
	public List<RustSymbol> extract(String fileName, String fileContent) {
		return new Scanner(fileContent).scan();
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "rs");
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

		private final List<RustSymbol> symbols = new ArrayList<>();

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

		List<RustSymbol> scan() {
			scanDeclarations(0, code.length(), null, false);
			return symbols;
		}

		private void scanDeclarations(int start, int end, @Nullable RustSymbol parent, boolean publicContext) {
			int depth = 0;
			for (int index=start; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '{') {
					depth++;
				} else if (ch == '}') {
					depth = Math.max(0, depth-1);
				} else if (depth == 0 && ch == '#') {
					index = skipAttribute(index, end)-1;
				} else if (depth == 0 && isIdentifierStart(ch)) {
					ItemPrefix prefix = readItemPrefix(index, end);
					index = prefix.index;
					if (prefix.externBlock && index < end && code.charAt(index) == '{') {
						int bodyEnd = findMatching(index, end, '{', '}');
						if (bodyEnd != -1) {
							scanDeclarations(index+1, bodyEnd, parent, false);
							index = bodyEnd;
						}
						continue;
					}
					if (index >= end || !isIdentifierStart(code.charAt(index)))
						continue;
					int wordEnd = readIdentifierEnd(index);
					String word = code.substring(index, wordEnd);
					boolean exported = publicContext || prefix.publicItem;
					if ("mod".equals(word)) {
						index = scanMod(index, wordEnd, end, parent, exported)-1;
					} else if (isTypeKeyword(word)) {
						index = scanType(word, index, wordEnd, end, parent, exported)-1;
					} else if ("fn".equals(word)) {
						index = scanFunction(index, wordEnd, end, parent, exported, false)-1;
					} else if ("const".equals(word) || "static".equals(word)) {
						index = scanVariable(word, wordEnd, end, parent, exported)-1;
					} else if ("impl".equals(word)) {
						index = scanImpl(index, wordEnd, end, parent, exported)-1;
					} else if ("macro_rules".equals(word) && wordEnd < end && code.charAt(wordEnd) == '!') {
						index = scanMacro(wordEnd+1, end, parent, exported)-1;
					} else if ("macro".equals(word)) {
						index = scanMacro(wordEnd, end, parent, exported)-1;
					} else {
						index = wordEnd-1;
					}
				}
			}
		}

		private int scanMod(int keywordStart, int afterKeyword, int end, @Nullable RustSymbol parent, boolean exported) {
			int nameStart = skipWhitespace(afterKeyword, end);
			if (nameStart >= end || !isIdentifierStart(code.charAt(nameStart)))
				return afterKeyword;
			int nameEnd = readIdentifierEnd(nameStart);
			TypeSymbol symbol = new TypeSymbol(parent, code.substring(nameStart, nameEnd), "mod",
					position(nameStart, nameEnd), null, isLocal(parent, exported));
			symbols.add(symbol);
			int bodyStart = findNextTopLevel(nameEnd, end, '{', ';');
			if (bodyStart < end && code.charAt(bodyStart) == '{') {
				int bodyEnd = findMatching(bodyStart, end, '{', '}');
				if (bodyEnd != -1) {
					scanDeclarations(bodyStart+1, bodyEnd, symbol, false);
					return bodyEnd+1;
				}
			}
			return keywordStart + 1;
		}

		private int scanType(String kind, int keywordStart, int afterKeyword, int end, @Nullable RustSymbol parent,
				boolean exported) {
			int nameStart = skipWhitespace(afterKeyword, end);
			if (nameStart >= end || !isIdentifierStart(code.charAt(nameStart)))
				return afterKeyword;
			int nameEnd = readIdentifierEnd(nameStart);
			TypeSymbol symbol = new TypeSymbol(parent, code.substring(nameStart, nameEnd), kind,
					position(nameStart, nameEnd), null, isLocal(parent, exported));
			symbols.add(symbol);
			if ("type".equals(kind)) {
				int statementEnd = findStatementEnd(nameEnd, end);
				return statementEnd < end? statementEnd+1: nameEnd;
			}
			int bodyStart = findNextTopLevel(nameEnd, end, '{', ';');
			if (bodyStart < end && code.charAt(bodyStart) == '{') {
				int bodyEnd = findMatching(bodyStart, end, '{', '}');
				if (bodyEnd != -1) {
					if ("enum".equals(kind))
						scanEnumVariants(bodyStart+1, bodyEnd, symbol, exported);
					else if ("trait".equals(kind))
						scanDeclarations(bodyStart+1, bodyEnd, symbol, true);
					else
						scanFields(bodyStart+1, bodyEnd, symbol);
					return bodyEnd+1;
				}
			}
			return keywordStart + 1;
		}

		private int scanFunction(int keywordStart, int afterKeyword, int end, @Nullable RustSymbol parent,
				boolean exported, boolean traitMember) {
			int nameStart = skipWhitespace(afterKeyword, end);
			if (nameStart >= end || !isIdentifierStart(code.charAt(nameStart)))
				return afterKeyword;
			int nameEnd = readIdentifierEnd(nameStart);
			int paramsStart = findNextTopLevel(nameEnd, end, '(', ';');
			String params = "";
			if (paramsStart < end && code.charAt(paramsStart) == '(') {
				int paramsEnd = findMatching(paramsStart, end, '(', ')');
				if (paramsEnd != -1)
					params = source.substring(paramsStart, paramsEnd+1);
			}
			symbols.add(new FunctionSymbol(parent, code.substring(nameStart, nameEnd), params,
					position(nameStart, nameEnd), null, traitMember? false: !exported));
			int bodyStart = findNextTopLevel(nameEnd, end, '{', ';');
			if (bodyStart < end && code.charAt(bodyStart) == '{') {
				int bodyEnd = findMatching(bodyStart, end, '{', '}');
				if (bodyEnd != -1)
					return bodyEnd+1;
			}
			return bodyStart < end? bodyStart+1: nameEnd;
		}

		private int scanVariable(String kind, int afterKeyword, int end, @Nullable RustSymbol parent, boolean exported) {
			int nameStart = skipWhitespace(afterKeyword, end);
			if (nameStart < end && "mut".equals(readWord(nameStart))) {
				nameStart = skipWhitespace(readIdentifierEnd(nameStart), end);
			}
			if (nameStart >= end || !isIdentifierStart(code.charAt(nameStart)))
				return afterKeyword;
			int nameEnd = readIdentifierEnd(nameStart);
			symbols.add(new VariableSymbol(parent, code.substring(nameStart, nameEnd), kind, position(nameStart, nameEnd),
					null, !exported));
			int statementEnd = findStatementEnd(nameEnd, end);
			return statementEnd < end? statementEnd+1: nameEnd;
		}

		private int scanMacro(int afterKeyword, int end, @Nullable RustSymbol parent, boolean exported) {
			int nameStart = skipWhitespace(afterKeyword, end);
			if (nameStart >= end || !isIdentifierStart(code.charAt(nameStart)))
				return afterKeyword;
			int nameEnd = readIdentifierEnd(nameStart);
			symbols.add(new VariableSymbol(parent, code.substring(nameStart, nameEnd), "macro",
					position(nameStart, nameEnd), null, isLocal(parent, exported)));
			int bodyStart = findNextTopLevel(nameEnd, end, '{', ';');
			if (bodyStart < end && code.charAt(bodyStart) == '{') {
				int bodyEnd = findMatching(bodyStart, end, '{', '}');
				if (bodyEnd != -1)
					return bodyEnd+1;
			}
			return nameEnd;
		}

		private int scanImpl(int keywordStart, int afterKeyword, int end, @Nullable RustSymbol parent, boolean exported) {
			int bodyStart = findNextTopLevel(afterKeyword, end, '{', ';');
			if (bodyStart >= end || code.charAt(bodyStart) != '{')
				return afterKeyword;
			int bodyEnd = findMatching(bodyStart, end, '{', '}');
			if (bodyEnd == -1)
				return bodyStart+1;
			String name = StringUtils.normalizeSpace(source.substring(afterKeyword, bodyStart));
			if (name.length() == 0)
				name = "impl";
			BlockSymbol symbol = new BlockSymbol(parent, name, "impl", position(keywordStart, afterKeyword), null,
					isLocal(parent, exported));
			symbols.add(symbol);
			scanDeclarations(bodyStart+1, bodyEnd, symbol, false);
			return bodyEnd+1;
		}

		private void scanFields(int start, int end, RustSymbol parent) {
			int depth = 0;
			for (int index=start; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '{' || ch == '(' || ch == '[' || ch == '<') {
					depth++;
				} else if (ch == '}' || ch == ')' || ch == ']' || ch == '>') {
					depth = Math.max(0, depth-1);
				} else if (depth == 0 && ch == '#') {
					index = skipAttribute(index, end)-1;
				} else if (depth == 0 && isIdentifierStart(ch)) {
					ItemPrefix prefix = readItemPrefix(index, end);
					index = prefix.index;
					if (index >= end || !isIdentifierStart(code.charAt(index)))
						continue;
					int nameEnd = readIdentifierEnd(index);
					int afterName = skipWhitespace(nameEnd, end);
					if (afterName < end && code.charAt(afterName) == ':') {
						symbols.add(new VariableSymbol(parent, code.substring(index, nameEnd), "field",
								position(index, nameEnd), null, !prefix.publicItem));
					}
					index = skipMemberTail(index, end)-1;
				}
			}
		}

		private void scanEnumVariants(int start, int end, RustSymbol parent, boolean exported) {
			int depth = 0;
			for (int index=start; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '{' || ch == '(' || ch == '[' || ch == '<') {
					depth++;
				} else if (ch == '}' || ch == ')' || ch == ']' || ch == '>') {
					depth = Math.max(0, depth-1);
				} else if (depth == 0 && ch == '#') {
					index = skipAttribute(index, end)-1;
				} else if (depth == 0 && isIdentifierStart(ch)) {
					int nameEnd = readIdentifierEnd(index);
					symbols.add(new VariableSymbol(parent, code.substring(index, nameEnd), "variant",
							position(index, nameEnd), null, isLocal(parent, exported)));
					index = skipMemberTail(nameEnd, end)-1;
				}
			}
		}

		private ItemPrefix readItemPrefix(int index, int end) {
			boolean publicItem = false;
			while (true) {
				index = skipWhitespace(index, end);
				while (index < end && code.charAt(index) == '#') {
					index = skipAttribute(index, end);
					index = skipWhitespace(index, end);
				}
				String word = readWord(index);
				if ("pub".equals(word)) {
					publicItem = true;
					index = readIdentifierEnd(index);
					index = skipWhitespace(index, end);
					if (index < end && code.charAt(index) == '(') {
						int visibilityEnd = findMatching(index, end, '(', ')');
						if (visibilityEnd != -1)
							index = visibilityEnd+1;
					}
				} else if ("async".equals(word) || "unsafe".equals(word) || "extern".equals(word)
						|| "default".equals(word)) {
					index = readIdentifierEnd(index);
					if ("extern".equals(word)) {
						index = skipWhitespace(index, end);
						if (index < end && code.charAt(index) == '"') {
							int abiEnd = skipQuotedString(index, end);
							if (abiEnd != -1)
								index = abiEnd;
						}
						index = skipWhitespace(index, end);
						if (index < end && code.charAt(index) == '{')
							return new ItemPrefix(index, publicItem, true);
					}
				} else {
					return new ItemPrefix(index, publicItem, false);
				}
			}
		}

		private boolean isLocal(@Nullable RustSymbol parent, boolean exported) {
			return !exported;
		}

		private boolean isTypeKeyword(String word) {
			return "struct".equals(word) || "enum".equals(word) || "trait".equals(word)
					|| "union".equals(word) || "type".equals(word);
		}

		private int skipAttribute(int index, int end) {
			index++;
			if (index < end && code.charAt(index) == '!')
				index++;
			index = skipWhitespace(index, end);
			if (index < end && code.charAt(index) == '[') {
				int attrEnd = findMatching(index, end, '[', ']');
				if (attrEnd != -1)
					return attrEnd+1;
			}
			return index;
		}

		private int skipMemberTail(int index, int end) {
			int depth = 0;
			for (; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '{' || ch == '(' || ch == '[' || ch == '<')
					depth++;
				else if (ch == '}' || ch == ')' || ch == ']' || ch == '>')
					depth = Math.max(0, depth-1);
				else if (depth == 0 && ch == ',')
					return index+1;
			}
			return index;
		}

		private int findStatementEnd(int index, int end) {
			int depth = 0;
			for (; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '(' || ch == '[' || ch == '{' || ch == '<')
					depth++;
				else if (ch == ')' || ch == ']' || ch == '}' || ch == '>')
					depth = Math.max(0, depth-1);
				else if (depth == 0 && ch == ';')
					return index;
			}
			return end;
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

		private int skipQuotedString(int index, int end) {
			index++;
			while (index < end) {
				char ch = code.charAt(index++);
				if (ch == '\\' && index < end)
					index++;
				else if (ch == '"')
					return index;
			}
			return -1;
		}

		private String readWord(int index) {
			if (index >= code.length() || !isIdentifierStart(code.charAt(index)))
				return "";
			int end = readIdentifierEnd(index);
			return code.substring(index, end);
		}

		private int readIdentifierEnd(int index) {
			index++;
			while (index < code.length() && isIdentifierPart(code.charAt(index)))
				index++;
			return index;
		}

		private boolean isIdentifierStart(char ch) {
			return Character.isLetter(ch) || ch == '_';
		}

		private boolean isIdentifierPart(char ch) {
			return isIdentifierStart(ch) || Character.isDigit(ch);
		}

		private PlanarRange position(int start, int end) {
			return new PlanarRange(rows[start], columns[start], rows[end-1], columns[end-1]+1);
		}

		private static String maskCommentsAndStrings(String source) {
			StringBuilder builder = new StringBuilder(source);
			for (int index=0; index<builder.length(); index++) {
				char ch = builder.charAt(index);
				if (ch == 'r' && isRawStringStart(source, index)) {
					index = maskRawString(builder, source, index);
				} else if (ch == '/' && index+1 < builder.length() && builder.charAt(index+1) == '/') {
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
				} else if (ch == '"' || ch == '\'') {
					if (ch == '\'' && index+1 < builder.length()
							&& (Character.isLetter(builder.charAt(index+1)) || builder.charAt(index+1) == '_')) {
						continue;
					}
					char quote = ch;
					builder.setCharAt(index, ' ');
					while (index+1 < builder.length()) {
						char current = builder.charAt(++index);
						if (current == '\n')
							continue;
						builder.setCharAt(index, ' ');
						if (current == '\\' && index+1 < builder.length()) {
							if (builder.charAt(index+1) != '\n')
								builder.setCharAt(index+1, ' ');
							index++;
						} else if (current == quote) {
							break;
						}
					}
				}
			}
			return builder.toString();
		}

		private static boolean isRawStringStart(String source, int index) {
			int quote = index+1;
			while (quote < source.length() && source.charAt(quote) == '#')
				quote++;
			return quote < source.length() && source.charAt(quote) == '"';
		}

		private static int maskRawString(StringBuilder builder, String source, int index) {
			int quote = index+1;
			while (source.charAt(quote) == '#')
				quote++;
			int hashes = quote - index - 1;
			for (int i=index; i<=quote; i++)
				builder.setCharAt(i, ' ');
			for (int i=quote+1; i<source.length(); i++) {
				char current = source.charAt(i);
				if (current != '\n')
					builder.setCharAt(i, ' ');
				if (current == '"') {
					boolean closes = i + hashes < source.length();
					for (int j=0; closes && j<hashes; j++) {
						if (source.charAt(i+j+1) != '#')
							closes = false;
					}
					if (closes) {
						for (int j=1; j<=hashes; j++)
							builder.setCharAt(i+j, ' ');
						return i + hashes;
					}
				}
			}
			return source.length()-1;
		}

		private static class ItemPrefix {

			private final int index;

			private final boolean publicItem;

			private final boolean externBlock;

			private ItemPrefix(int index, boolean publicItem, boolean externBlock) {
				this.index = index;
				this.publicItem = publicItem;
				this.externBlock = externBlock;
			}

		}

	}

}
