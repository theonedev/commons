package io.onedev.commons.jsymbol.ruby;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.ruby.symbols.ImportedSymbol;
import io.onedev.commons.jsymbol.ruby.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.ruby.symbols.RubySymbol;
import io.onedev.commons.jsymbol.ruby.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.ruby.symbols.VariableSymbol;
import io.onedev.commons.utils.PlanarRange;

public class RubyExtractor extends AbstractSymbolExtractor<RubySymbol> {

	@Override
	public List<RubySymbol> extract(String fileName, String fileContent) {
		State state = new State(tokenize(fileContent));
		state.parse();
		return state.symbols;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "rb", "rake", "gemspec") || "Gemfile".equals(fileName)
				|| "Rakefile".equals(fileName);
	}

	@Override
	public int getVersion() {
		return 1;
	}

	private static List<Token> tokenize(String source) {
		List<Token> tokens = new ArrayList<>();
		int row = 0;
		int column = 0;
		for (int i=0; i<source.length();) {
			char ch = source.charAt(i);
			if (ch == ' ' || ch == '\t' || ch == '\f') {
				i++;
				column++;
			} else if (ch == '\r' || ch == '\n') {
				int startColumn = column;
				if (ch == '\r' && i+1 < source.length() && source.charAt(i+1) == '\n')
					i += 2;
				else
					i++;
				tokens.add(new Token(Type.NL, "\n", row, startColumn, row, startColumn+1));
				row++;
				column = 0;
			} else if (ch == '#') {
				while (i < source.length() && source.charAt(i) != '\r' && source.charAt(i) != '\n') {
					i++;
					column++;
				}
			} else if (ch == '\'' || ch == '"') {
				int start = i;
				int startRow = row;
				int startColumn = column;
				char quote = ch;
				i++;
				column++;
				while (i < source.length()) {
					ch = source.charAt(i++);
					if (ch == '\r' || ch == '\n') {
						if (ch == '\r' && i < source.length() && source.charAt(i) == '\n')
							i++;
						row++;
						column = 0;
					} else {
						column++;
					}
					if (ch == '\\') {
						if (i < source.length()) {
							ch = source.charAt(i++);
							if (ch == '\r' || ch == '\n') {
								if (ch == '\r' && i < source.length() && source.charAt(i) == '\n')
									i++;
								row++;
								column = 0;
							} else {
								column++;
							}
						}
					} else if (ch == quote) {
						break;
					}
				}
				tokens.add(new Token(Type.STRING, source.substring(start, Math.min(i, source.length())),
						startRow, startColumn, row, column));
			} else if (ch == ':' && i+1 < source.length() && isIdentifierStart(source.charAt(i+1))) {
				int startColumn = column;
				i++;
				column++;
				int start = i;
				i++;
				column++;
				while (i < source.length() && isIdentifierPart(source.charAt(i))) {
					i++;
					column++;
				}
				tokens.add(new Token(Type.SYMBOL, source.substring(start, i), row, startColumn, row, column));
			} else if (ch == '@' || ch == '$') {
				int start = i;
				int startColumn = column;
				i++;
				column++;
				if (ch == '@' && i < source.length() && source.charAt(i) == '@') {
					i++;
					column++;
				}
				if (i < source.length() && isIdentifierStart(source.charAt(i))) {
					i++;
					column++;
					while (i < source.length() && isIdentifierPart(source.charAt(i))) {
						i++;
						column++;
					}
					tokens.add(new Token(Type.IDENT, source.substring(start, i), row, startColumn, row, column));
				} else {
					tokens.add(new Token(Type.OTHER, source.substring(start, i), row, startColumn, row, column));
				}
			} else if (isIdentifierStart(ch)) {
				int start = i;
				int startColumn = column;
				i++;
				column++;
				while (i < source.length() && isIdentifierPart(source.charAt(i))) {
					i++;
					column++;
				}
				if ((source.charAt(i-1) == '?' || source.charAt(i-1) == '!')
						&& i < source.length() && source.charAt(i) == '=')
					i--;
				String text = source.substring(start, i);
				tokens.add(new Token(keywordType(text), text, row, startColumn, row, column));
			} else {
				int startColumn = column;
				String text;
				if (i+1 < source.length() && (source.startsWith("::", i) || source.startsWith("=>", i)
						|| source.startsWith("==", i) || source.startsWith("!=", i)
						|| source.startsWith("<=", i) || source.startsWith(">=", i))) {
					text = source.substring(i, i+2);
					i += 2;
					column += 2;
				} else {
					text = String.valueOf(ch);
					i++;
					column++;
				}
				Type type;
				if ("(".equals(text))
					type = Type.PAREN_L;
				else if (")".equals(text))
					type = Type.PAREN_R;
				else if ("[".equals(text))
					type = Type.BRACKET_L;
				else if ("]".equals(text))
					type = Type.BRACKET_R;
				else if ("{".equals(text))
					type = Type.BRACE_L;
				else if ("}".equals(text))
					type = Type.BRACE_R;
				else if (";".equals(text))
					type = Type.NL;
				else if ("=".equals(text) || "+=".equals(text) || "-=".equals(text) || "||=".equals(text)
						|| "&&=".equals(text))
					type = Type.ASSIGN;
				else if ("::".equals(text))
					type = Type.SCOPE;
				else
					type = Type.OTHER;
				tokens.add(new Token(type, text, row, startColumn, row, column));
			}
		}
		return tokens;
	}

	private static boolean isIdentifierStart(char ch) {
		return Character.isLetter(ch) || ch == '_';
	}

	private static boolean isIdentifierPart(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_' || ch == '?' || ch == '!';
	}

	private static Type keywordType(String text) {
		if ("class".equals(text))
			return Type.CLASS;
		else if ("module".equals(text))
			return Type.MODULE;
		else if ("def".equals(text))
			return Type.DEF;
		else if ("end".equals(text))
			return Type.END;
		else if ("do".equals(text))
			return Type.DO;
		else if ("begin".equals(text))
			return Type.BEGIN;
		else if ("if".equals(text) || "unless".equals(text) || "case".equals(text) || "while".equals(text)
				|| "until".equals(text) || "for".equals(text))
			return Type.BLOCK_START;
		else
			return Type.IDENT;
	}

	private static boolean isConstant(String name) {
		return name.length() != 0 && Character.isUpperCase(name.charAt(0));
	}

	private static PlanarRange range(Token token) {
		return new PlanarRange(token.fromRow, token.fromColumn, token.toRow, token.toColumn);
	}

	private static class State {

		private final List<Token> tokens;

		private final List<RubySymbol> symbols = new ArrayList<>();

		private final List<Frame> stack = new ArrayList<>();

		private int index;

		private int lineStart;

		private State(List<Token> tokens) {
			this.tokens = tokens;
		}

		private void parse() {
			lineStart = 0;
			while (index < tokens.size()) {
				Token token = tokens.get(index);
				boolean statementStart = isStatementStart(index);
				if (statementStart) {
					if (token.type == Type.CLASS || token.type == Type.MODULE)
						parseType(token.type == Type.CLASS? "class": "module");
					else if (token.type == Type.DEF)
						parseMethod();
					else if (token.type == Type.END)
						closeFrame();
					else if (token.type == Type.IDENT)
						parseStatementStart();
				}
				if (token.type == Type.DO || token.type == Type.BEGIN
						|| token.type == Type.BLOCK_START && (statementStart || previousSignificantIsAssign()))
					stack.add(new Frame(null, false));
				if (token.type == Type.NL)
					lineStart = index + 1;
				index++;
			}
		}

		private void parseType(String kind) {
			int nameIndex = nextNonNewline(index+1);
			if ("class".equals(kind) && isSingletonClassStart(nameIndex)) {
				stack.add(new Frame(null, true));
				index = nameIndex + 2;
				return;
			}
			if (nameIndex == -1 || tokens.get(nameIndex).type != Type.IDENT)
				return;
			int endIndex = nameEnd(nameIndex);
			Token nameToken = tokens.get(endIndex);
			String name = qualifiedName(nameIndex, endIndex);
			TypeSymbol symbol = new TypeSymbol(currentSymbol(), name, kind, range(nameToken), null);
			symbols.add(symbol);
			stack.add(new Frame(symbol, false));
			index = endIndex;
		}

		private void parseMethod() {
			int nameIndex = nextNonNewline(index+1);
			if (nameIndex == -1)
				return;
			int endIndex = methodNameEnd(nameIndex);
			if (endIndex == -1)
				return;
			String name = qualifiedName(nameIndex, endIndex);
			boolean singleton = name.contains(".") || name.startsWith("self.") || inSingletonContext();
			if (name.startsWith("self."))
				name = name.substring("self.".length());
			String parameters = parameters(endIndex+1);
			MethodSymbol symbol = new MethodSymbol(currentSymbol(), name, parameters, range(tokens.get(endIndex)), null,
					singleton);
			symbols.add(symbol);
			stack.add(new Frame(symbol, false));
			index = endIndex;
		}

		private void parseStatementStart() {
			Token token = tokens.get(index);
			if ("require".equals(token.text) || "require_relative".equals(token.text))
				parseRequire();
			else if ("autoload".equals(token.text))
				parseAutoload();
			else if ("attr_reader".equals(token.text) || "attr_writer".equals(token.text)
					|| "attr_accessor".equals(token.text))
				parseAttributes(token.text);
			else if ("alias_method".equals(token.text))
				parseAliasMethod();
			else if (isConstant(token.text))
				parseConstant();
		}

		private void parseRequire() {
			int valueIndex = nextNonNewline(index+1);
			if (valueIndex != -1 && tokens.get(valueIndex).type == Type.STRING) {
				Token value = tokens.get(valueIndex);
				String imported = value.text;
				if (imported.length() >= 2)
					imported = imported.substring(1, imported.length()-1);
				symbols.add(new ImportedSymbol(currentSymbol(), imported, range(value), range(value)));
				index = valueIndex;
			}
		}

		private void parseAutoload() {
			int nameIndex = nextNonNewline(index+1);
			if (nameIndex != -1 && (tokens.get(nameIndex).type == Type.SYMBOL
					|| tokens.get(nameIndex).type == Type.IDENT)) {
				Token token = tokens.get(nameIndex);
				symbols.add(new VariableSymbol(currentSymbol(), token.text, "autoload", range(token), null, false));
				index = nameIndex;
			}
		}

		private void parseAttributes(String kind) {
			int i = index + 1;
			while (i < tokens.size() && tokens.get(i).type != Type.NL) {
				Token token = tokens.get(i);
				if (token.type == Type.SYMBOL || token.type == Type.IDENT || token.type == Type.STRING) {
					String name = token.text;
					if (token.type == Type.STRING && name.length() >= 2)
						name = name.substring(1, name.length()-1);
					if (name.length() != 0) {
						boolean local = kind.equals("attr_writer");
						symbols.add(new VariableSymbol(currentSymbol(), name, kind, range(token), null, local));
					}
				}
				i++;
			}
			index = i - 1;
		}

		private void parseAliasMethod() {
			int i = nextNonNewline(index+1);
			if (i != -1 && tokens.get(i).type == Type.SYMBOL) {
				Token token = tokens.get(i);
				symbols.add(new MethodSymbol(currentSymbol(), token.text, "", range(token), null, false));
				index = i;
			}
		}

		private void parseConstant() {
			int assignIndex = nextNonNewline(index+1);
			if (assignIndex != -1 && tokens.get(assignIndex).type == Type.ASSIGN) {
				Token token = tokens.get(index);
				symbols.add(new VariableSymbol(currentSymbol(), token.text, "constant", range(token), null, false));
			}
		}

		private void closeFrame() {
			if (!stack.isEmpty())
				stack.remove(stack.size()-1);
		}

		@Nullable
		private RubySymbol currentSymbol() {
			for (int i=stack.size()-1; i>=0; i--) {
				if (stack.get(i).symbol != null)
					return stack.get(i).symbol;
			}
			return null;
		}

		private boolean isStatementStart(int tokenIndex) {
			for (int i=lineStart; i<tokenIndex; i++) {
				Type type = tokens.get(i).type;
				if (type != Type.NL)
					return false;
			}
			return true;
		}

		private int nextNonNewline(int start) {
			for (int i=start; i<tokens.size(); i++) {
				if (tokens.get(i).type != Type.NL)
					return i;
			}
			return -1;
		}

		private boolean previousSignificantIsAssign() {
			for (int i=index-1; i>=lineStart; i--) {
				Type type = tokens.get(i).type;
				if (type != Type.NL)
					return type == Type.ASSIGN;
			}
			return false;
		}

		private int nameEnd(int start) {
			int end = start;
			for (int i=start+1; i+1<tokens.size(); i+=2) {
				if (tokens.get(i).type == Type.SCOPE && tokens.get(i+1).type == Type.IDENT)
					end = i+1;
				else
					break;
			}
			return end;
		}

		private int methodNameEnd(int start) {
			int end = methodNameTailEnd(start);
			if (end == -1)
				return -1;
			if (start+2 < tokens.size() && ".".equals(tokens.get(start+1).text)
					&& methodNameTailEnd(start+2) != -1)
				end = methodNameTailEnd(start+2);
			for (int i=end+1; i+1<tokens.size(); i+=2) {
				if ((tokens.get(i).type == Type.SCOPE || ".".equals(tokens.get(i).text))
						&& methodNameTailEnd(i+1) != -1)
					end = methodNameTailEnd(i+1);
				else
					break;
			}
			if (end+1 < tokens.size() && "=".equals(tokens.get(end+1).text))
				end++;
			return end;
		}

		private int methodNameTailEnd(int start) {
			Token token = tokens.get(start);
			if (token.type == Type.IDENT)
				return start;
			else if (token.type == Type.BRACKET_L && start+1 < tokens.size()
					&& tokens.get(start+1).type == Type.BRACKET_R) {
				if (start+2 < tokens.size() && tokens.get(start+2).type == Type.ASSIGN)
					return start + 2;
				else
					return start + 1;
			} else if (isOperatorPart(token)) {
				int end = start;
				while (end+1 < tokens.size() && isOperatorPart(tokens.get(end+1)))
					end++;
				return end;
			} else {
				return -1;
			}
		}

		private boolean isOperatorPart(Token token) {
			return token.type == Type.ASSIGN || token.type == Type.OTHER && !token.text.equals(".")
					&& !token.text.equals(",") && !token.text.equals("{") && !token.text.equals("}");
		}

		private boolean isSingletonClassStart(int nameIndex) {
			return nameIndex != -1 && nameIndex+2 < tokens.size() && "<".equals(tokens.get(nameIndex).text)
					&& "<".equals(tokens.get(nameIndex+1).text) && "self".equals(tokens.get(nameIndex+2).text);
		}

		private boolean inSingletonContext() {
			for (int i=stack.size()-1; i>=0; i--) {
				Frame frame = stack.get(i);
				if (frame.singletonContext)
					return true;
				if (frame.symbol != null)
					return false;
			}
			return false;
		}

		private String qualifiedName(int start, int end) {
			StringBuilder builder = new StringBuilder();
			for (int i=start; i<=end; i++)
				builder.append(tokens.get(i).text);
			return builder.toString();
		}

		private String parameters(int start) {
			int i = start;
			if (i >= tokens.size() || tokens.get(i).type == Type.NL)
				return "";
			StringBuilder builder = new StringBuilder();
			if (tokens.get(i).type == Type.PAREN_L) {
				int depth = 0;
				for (; i<tokens.size(); i++) {
					Token token = tokens.get(i);
					builder.append(token.text);
					if (token.type == Type.PAREN_L)
						depth++;
					else if (token.type == Type.PAREN_R) {
						depth--;
						if (depth == 0)
							break;
					} else if (token.type == Type.NL) {
						break;
					}
				}
			} else {
				for (; i<tokens.size(); i++) {
					Token token = tokens.get(i);
					if (token.type == Type.NL)
						break;
					builder.append(token.text);
				}
			}
			return builder.toString().replaceAll("\\s+", " ").trim();
		}

	}

	private static class Frame {

		@Nullable
		private final RubySymbol symbol;

		private final boolean singletonContext;

		private Frame(@Nullable RubySymbol symbol, boolean singletonContext) {
			this.symbol = symbol;
			this.singletonContext = singletonContext;
		}

	}

	private enum Type {
		CLASS, MODULE, DEF, END, DO, BEGIN, BLOCK_START, IDENT, SYMBOL, STRING, ASSIGN, SCOPE, PAREN_L, PAREN_R,
		BRACKET_L, BRACKET_R, BRACE_L, BRACE_R, NL, OTHER
	}

	private static class Token {

		private final Type type;

		private final String text;

		private final int fromRow;

		private final int fromColumn;

		private final int toRow;

		private final int toColumn;

		private Token(Type type, String text, int fromRow, int fromColumn, int toRow, int toColumn) {
			this.type = type;
			this.text = text;
			this.fromRow = fromRow;
			this.fromColumn = fromColumn;
			this.toRow = toRow;
			this.toColumn = toColumn;
		}

	}

}
