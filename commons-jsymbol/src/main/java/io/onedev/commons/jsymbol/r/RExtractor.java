package io.onedev.commons.jsymbol.r;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.r.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.r.symbols.RSymbol;
import io.onedev.commons.jsymbol.r.symbols.VariableSymbol;
import io.onedev.commons.utils.PlanarRange;

public class RExtractor extends AbstractSymbolExtractor<RSymbol> {

	@Override
	public List<RSymbol> extract(String fileName, String fileContent) {
		List<RSymbol> symbols = new ArrayList<>();
		List<Token> tokens = tokenize(fileContent);
		new Parser(tokens, symbols).parse(0, tokens.size(), null);
		return symbols;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "r", "R");
	}

	@Override
	public int getVersion() {
		return 1;
	}

	private static List<Token> tokenize(String source) {
		List<Token> tokens = new ArrayList<>();
		int row = 0;
		int column = 0;
		for (int i = 0; i < source.length();) {
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
			} else if ((ch == 'R' || ch == 'r') && i+1 < source.length() && source.charAt(i+1) == '"') {
				int start = i;
				int startRow = row;
				int startColumn = column;
				i += 2;
				column += 2;
				int delimiterStart = i;
				while (i < source.length() && "([{".indexOf(source.charAt(i)) == -1) {
					i++;
					column++;
				}
				if (i < source.length()) {
					String delimiter = source.substring(delimiterStart, i);
					char close = matchingRawStringClose(source.charAt(i));
					i++;
					column++;
					while (i < source.length()) {
						ch = source.charAt(i);
						if (ch == close && startsWith(source, i+1, delimiter)
								&& i+1+delimiter.length() < source.length()
								&& source.charAt(i+1+delimiter.length()) == '"') {
							i += 2 + delimiter.length();
							column += 2 + delimiter.length();
							break;
						} else {
							i++;
							if (ch == '\r' || ch == '\n') {
								if (ch == '\r' && i < source.length() && source.charAt(i) == '\n')
									i++;
								row++;
								column = 0;
							} else {
								column++;
							}
						}
					}
				}
				tokens.add(new Token(Type.STRING, source.substring(start, Math.min(i, source.length())),
						startRow, startColumn, row, column));
			} else if (ch == '"' || ch == '\'' || ch == '`') {
				int start = i;
				int startRow = row;
				int startColumn = column;
				char quote = ch;
				i++;
				column++;
				while (i < source.length()) {
					ch = source.charAt(i);
					if (ch == '\\') {
						i++;
						column++;
						if (i < source.length()) {
							if (source.charAt(i) == '\r' || source.charAt(i) == '\n') {
								if (source.charAt(i) == '\r' && i+1 < source.length() && source.charAt(i+1) == '\n')
									i++;
								row++;
								column = 0;
							} else {
								column++;
							}
							i++;
						}
					} else {
						i++;
						if (ch == '\r' || ch == '\n') {
							if (ch == '\r' && i < source.length() && source.charAt(i) == '\n')
								i++;
							row++;
							column = 0;
						} else {
							column++;
						}
						if (ch == quote)
							break;
					}
				}
				tokens.add(new Token(Type.STRING, source.substring(start, Math.min(i, source.length())),
						startRow, startColumn, row, column));
			} else if (isIdentifierStart(ch)) {
				int start = i;
				int startColumn = column;
				i++;
				column++;
				while (i < source.length() && isIdentifierPart(source.charAt(i))) {
					i++;
					column++;
				}
				String text = source.substring(start, i);
				tokens.add(new Token(keywordType(text), text, row, startColumn, row, column));
			} else if (Character.isDigit(ch) || ch == '.' && i+1 < source.length() && Character.isDigit(source.charAt(i+1))) {
				int start = i;
				int startColumn = column;
				i++;
				column++;
				boolean exponent = false;
				while (i < source.length()) {
					ch = source.charAt(i);
					if (Character.isDigit(ch) || ch == '.' || ch == 'L' || ch == 'l' || ch == 'i') {
						i++;
						column++;
						exponent = false;
					} else if (ch == 'e' || ch == 'E') {
						i++;
						column++;
						exponent = true;
					} else if ((ch == '+' || ch == '-') && exponent) {
						i++;
						column++;
						exponent = false;
					} else {
						break;
					}
				}
				tokens.add(new Token(Type.OTHER, source.substring(start, i), row, startColumn, row, column));
			} else {
				int startColumn = column;
				String text;
				if (startsWith(source, i, "<<-") || startsWith(source, i, "->>")) {
					text = source.substring(i, i+3);
					i += 3;
					column += 3;
				} else if (startsWith(source, i, "|>")) {
					text = source.substring(i, i+2);
					i += 2;
					column += 2;
				} else if (startsWith(source, i, "<-") || startsWith(source, i, "->")
						|| startsWith(source, i, ":=") || startsWith(source, i, "==")
						|| startsWith(source, i, "!=") || startsWith(source, i, ">=")
						|| startsWith(source, i, "<=") || startsWith(source, i, "&&")
						|| startsWith(source, i, "||") || startsWith(source, i, "::")
						|| startsWith(source, i, "[[") || startsWith(source, i, "]]")) {
					text = source.substring(i, i+2);
					i += 2;
					column += 2;
				} else {
					text = String.valueOf(ch);
					i++;
					column++;
				}
				Type type;
				if (";".equals(text))
					type = Type.SEMICOLON;
				else if ("(".equals(text))
					type = Type.PAREN_L;
				else if (")".equals(text))
					type = Type.PAREN_R;
				else if ("[".equals(text) || "[[".equals(text))
					type = Type.BRACKET_L;
				else if ("]".equals(text) || "]]".equals(text))
					type = Type.BRACKET_R;
				else if ("{".equals(text))
					type = Type.BRACE_L;
				else if ("}".equals(text))
					type = Type.BRACE_R;
				else if (isAssign(text))
					type = Type.ASSIGN;
				else if ("\\".equals(text))
					type = Type.FUNCTION;
				else
					type = Type.OTHER;
				tokens.add(new Token(type, text, row, startColumn, row, column));
			}
		}
		return tokens;
	}

	private static boolean startsWith(String source, int index, String value) {
		return source.regionMatches(index, value, 0, value.length());
	}

	private static char matchingRawStringClose(char open) {
		if (open == '[')
			return ']';
		else if (open == '{')
			return '}';
		else
			return ')';
	}

	private static boolean isIdentifierStart(char ch) {
		return Character.isLetter(ch) || ch == '.' || ch == '_';
	}

	private static boolean isIdentifierPart(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_' || ch == '.';
	}

	private static Type keywordType(String text) {
		if ("function".equals(text))
			return Type.FUNCTION;
		else if ("if".equals(text))
			return Type.IF;
		else if ("else".equals(text))
			return Type.ELSE;
		else if ("for".equals(text))
			return Type.FOR;
		else if ("while".equals(text))
			return Type.WHILE;
		else if ("repeat".equals(text))
			return Type.REPEAT;
		else
			return Type.ID;
	}

	private static boolean isAssign(String text) {
		return "<-".equals(text) || "<<-".equals(text) || "=".equals(text) || ":=".equals(text)
				|| "->".equals(text) || "->>".equals(text);
	}

	private enum Type {
		ID, STRING, FUNCTION, IF, ELSE, FOR, WHILE, REPEAT, ASSIGN, PAREN_L, PAREN_R, BRACKET_L,
		BRACKET_R, BRACE_L, BRACE_R, SEMICOLON, NL, OTHER
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

		private PlanarRange range() {
			return new PlanarRange(fromRow, fromColumn, toRow, toColumn);
		}

	}

	private static class Parser {

		private final List<Token> tokens;

		private final List<RSymbol> symbols;

		private Parser(List<Token> tokens, List<RSymbol> symbols) {
			this.tokens = tokens;
			this.symbols = symbols;
		}

		private void parse(int from, int to, @Nullable RSymbol parentSymbol) {
			int start = from;
			int parens = 0;
			int brackets = 0;
			int braces = 0;
			for (int i = from; i < to; i++) {
				Token token = tokens.get(i);
				if (token.type == Type.PAREN_L)
					parens++;
				else if (token.type == Type.PAREN_R)
					parens = Math.max(0, parens-1);
				else if (token.type == Type.BRACKET_L)
					brackets++;
				else if (token.type == Type.BRACKET_R)
					brackets = Math.max(0, brackets-1);
				else if (token.type == Type.BRACE_L)
					braces++;
				else if (token.type == Type.BRACE_R)
					braces = Math.max(0, braces-1);
				else if ((token.type == Type.SEMICOLON || token.type == Type.NL)
						&& parens == 0 && brackets == 0 && braces == 0) {
					if (token.type == Type.SEMICOLON || !isContinuedAtNewline(start, i, to)) {
						parseStatement(start, i, parentSymbol);
						start = i+1;
					}
				}
			}
			parseStatement(start, to, parentSymbol);
		}

		private boolean isContinuedAtNewline(int start, int newline, int to) {
			int previous = previousMeaningful(start, newline);
			int next = skipSeparators(newline+1, to);
			if (previous == -1 || next >= to)
				return false;

			String previousText = tokens.get(previous).text;
			Type nextType = tokens.get(next).type;
			return tokens.get(previous).type == Type.ASSIGN
					|| "(".equals(previousText)
					|| ",".equals(previousText)
					|| "+".equals(previousText)
					|| "-".equals(previousText)
					|| "*".equals(previousText)
					|| "/".equals(previousText)
					|| "^".equals(previousText)
					|| "~".equals(previousText)
					|| "|".equals(previousText)
					|| "||".equals(previousText)
					|| "&".equals(previousText)
					|| "&&".equals(previousText)
					|| "|>".equals(previousText)
					|| ":".equals(previousText)
					|| "==".equals(previousText)
					|| "!=".equals(previousText)
					|| ">".equals(previousText)
					|| ">=".equals(previousText)
					|| "<".equals(previousText)
					|| "<=".equals(previousText)
					|| nextType == Type.BRACE_L
					|| nextType == Type.ELSE;
		}

		private int previousMeaningful(int from, int to) {
			for (int i = to-1; i >= from; i--) {
				if (tokens.get(i).type != Type.NL && tokens.get(i).type != Type.SEMICOLON)
					return i;
			}
			return -1;
		}

		private void parseStatement(int from, int to, @Nullable RSymbol parentSymbol) {
			from = skipSeparators(from, to);
			to = trimSeparators(from, to);
			if (from >= to)
				return;

			int assignment = findTopLevelAssignment(from, to);
			if (assignment != -1) {
				parseAssignment(from, assignment, to, parentSymbol);
				return;
			}

			Token first = tokens.get(from);
			if (first.type == Type.BRACE_L) {
				int close = findMatching(from, to);
				if (close != -1)
					parse(from+1, close, parentSymbol);
			} else if (first.type == Type.PAREN_L) {
				int close = findMatching(from, to);
				if (close != -1)
					parseStatement(from+1, close, parentSymbol);
			} else if (first.type == Type.IF) {
				parseControlBody(afterCondition(from+1, to), to, parentSymbol);
				int elseIndex = findElseAtTopLevel(from, to);
				if (elseIndex != -1)
					parseControlBody(elseIndex+1, to, parentSymbol);
			} else if (first.type == Type.FOR || first.type == Type.WHILE) {
				parseControlBody(afterCondition(from+1, to), to, parentSymbol);
			} else if (first.type == Type.REPEAT) {
				parseControlBody(from+1, to, parentSymbol);
			}
		}

		private int skipSeparators(int from, int to) {
			while (from < to && (tokens.get(from).type == Type.NL || tokens.get(from).type == Type.SEMICOLON))
				from++;
			return from;
		}

		private int trimSeparators(int from, int to) {
			while (to > from && (tokens.get(to-1).type == Type.NL || tokens.get(to-1).type == Type.SEMICOLON))
				to--;
			return to;
		}

		private int findTopLevelAssignment(int from, int to) {
			int parens = 0;
			int brackets = 0;
			int braces = 0;
			for (int i = from; i < to; i++) {
				Token token = tokens.get(i);
				if (token.type == Type.PAREN_L)
					parens++;
				else if (token.type == Type.PAREN_R)
					parens = Math.max(0, parens-1);
				else if (token.type == Type.BRACKET_L)
					brackets++;
				else if (token.type == Type.BRACKET_R)
					brackets = Math.max(0, brackets-1);
				else if (token.type == Type.BRACE_L)
					braces++;
				else if (token.type == Type.BRACE_R)
					braces = Math.max(0, braces-1);
				else if (token.type == Type.ASSIGN && parens == 0 && brackets == 0 && braces == 0)
					return i;
			}
			return -1;
		}

		private void parseAssignment(int from, int assignment, int to, @Nullable RSymbol parentSymbol) {
			Token operator = tokens.get(assignment);
			int nameIndex;
			int valueFrom;
			int valueTo;
			if ("->".equals(operator.text) || "->>".equals(operator.text)) {
				nameIndex = soleIdIndex(assignment+1, to);
				valueFrom = from;
				valueTo = assignment;
			} else {
				nameIndex = soleIdIndex(from, assignment);
				valueFrom = assignment+1;
				valueTo = to;
			}
			if (nameIndex == -1)
				return;

			Token name = tokens.get(nameIndex);
			if (getChild(parentSymbol, name.text) != null)
				return;

			int functionIndex = skipSeparators(valueFrom, valueTo);
			if (functionIndex < valueTo && tokens.get(functionIndex).type == Type.FUNCTION) {
				FunctionInfo function = parseFunction(functionIndex, valueTo);
				FunctionSymbol symbol = new FunctionSymbol(parentSymbol, name.text, name.range(), function.scope,
						function.parameters, parentSymbol != null);
				symbols.add(symbol);
				if (function.bodyFrom != -1)
					parseStatement(function.bodyFrom, function.bodyTo, symbol);
			} else {
				symbols.add(new VariableSymbol(parentSymbol, name.text, name.range(), null, parentSymbol != null));
			}
		}

		private int soleIdIndex(int from, int to) {
			from = skipSeparators(from, to);
			to = trimSeparators(from, to);
			if (to == from+1 && tokens.get(from).type == Type.ID)
				return from;
			else
				return -1;
		}

		private FunctionInfo parseFunction(int from, int to) {
			int parenOpen = skipSeparators(from+1, to);
			String parameters = "()";
			int bodyFrom = -1;
			int bodyTo = -1;
			if (parenOpen < to && tokens.get(parenOpen).type == Type.PAREN_L) {
				int parenClose = findMatching(parenOpen, to);
				if (parenClose != -1) {
					parameters = "(" + concat(parenOpen+1, parenClose) + ")";
					bodyFrom = skipSeparators(parenClose+1, to);
					bodyTo = to;
				}
			}
			return new FunctionInfo(range(from, to), parameters, bodyFrom, bodyTo);
		}

		private String concat(int from, int to) {
			StringBuilder builder = new StringBuilder();
			for (int i = from; i < to; i++) {
				Token token = tokens.get(i);
				if (token.type != Type.NL)
					builder.append(token.text);
			}
			return builder.toString();
		}

		private PlanarRange range(int from, int to) {
			Token start = tokens.get(from);
			Token end = tokens.get(to-1);
			return new PlanarRange(start.fromRow, start.fromColumn, end.toRow, end.toColumn);
		}

		private int afterCondition(int from, int to) {
			from = skipSeparators(from, to);
			if (from < to && tokens.get(from).type == Type.PAREN_L) {
				int close = findMatching(from, to);
				if (close != -1)
					return close+1;
			}
			return from;
		}

		private void parseControlBody(int from, int to, @Nullable RSymbol parentSymbol) {
			from = skipSeparators(from, to);
			if (from < to)
				parseStatement(from, to, parentSymbol);
		}

		private int findElseAtTopLevel(int from, int to) {
			int parens = 0;
			int brackets = 0;
			int braces = 0;
			for (int i = from; i < to; i++) {
				Token token = tokens.get(i);
				if (token.type == Type.PAREN_L)
					parens++;
				else if (token.type == Type.PAREN_R)
					parens = Math.max(0, parens-1);
				else if (token.type == Type.BRACKET_L)
					brackets++;
				else if (token.type == Type.BRACKET_R)
					brackets = Math.max(0, brackets-1);
				else if (token.type == Type.BRACE_L)
					braces++;
				else if (token.type == Type.BRACE_R)
					braces = Math.max(0, braces-1);
				else if (token.type == Type.ELSE && parens == 0 && brackets == 0 && braces == 0)
					return i;
			}
			return -1;
		}

		private int findMatching(int open, int to) {
			Type openType = tokens.get(open).type;
			Type closeType;
			if (openType == Type.PAREN_L)
				closeType = Type.PAREN_R;
			else if (openType == Type.BRACKET_L)
				closeType = Type.BRACKET_R;
			else if (openType == Type.BRACE_L)
				closeType = Type.BRACE_R;
			else
				return -1;

			int depth = 0;
			for (int i = open; i < to; i++) {
				Token token = tokens.get(i);
				if (token.type == openType)
					depth++;
				else if (token.type == closeType) {
					depth--;
					if (depth == 0)
						return i;
				}
			}
			return -1;
		}

		@Nullable
		private RSymbol getChild(@Nullable RSymbol parentSymbol, String childName) {
			for (RSymbol symbol: symbols) {
				if (symbol.getParent() == parentSymbol && symbol.getName().equals(childName))
					return symbol;
			}
			return null;
		}

	}

	private static class FunctionInfo {

		private final PlanarRange scope;

		private final String parameters;

		private final int bodyFrom;

		private final int bodyTo;

		private FunctionInfo(PlanarRange scope, String parameters, int bodyFrom, int bodyTo) {
			this.scope = scope;
			this.parameters = parameters;
			this.bodyFrom = bodyFrom;
			this.bodyTo = bodyTo;
		}

	}

}
