package io.onedev.commons.jsymbol.php;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.php.symbols.ConstantSymbol;
import io.onedev.commons.jsymbol.php.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.php.symbols.ImportedSymbol;
import io.onedev.commons.jsymbol.php.symbols.NamespaceSymbol;
import io.onedev.commons.jsymbol.php.symbols.PhpSymbol;
import io.onedev.commons.jsymbol.php.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.php.symbols.VariableSymbol;
import io.onedev.commons.jsymbol.php.symbols.Visibility;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.PlanarRange;

public class PhpExtractor extends AbstractSymbolExtractor<PhpSymbol> {

	private static final Set<String> MODIFIERS = new HashSet<>();

	static {
		MODIFIERS.add("abstract");
		MODIFIERS.add("final");
		MODIFIERS.add("public");
		MODIFIERS.add("protected");
		MODIFIERS.add("private");
		MODIFIERS.add("static");
		MODIFIERS.add("readonly");
		MODIFIERS.add("partial");
	}

	@Override
	public List<PhpSymbol> extract(String fileName, String fileContent) {
		List<PhpSymbol> symbols = new ArrayList<>();
		new Parser(fileContent, new Lexer(fileContent).lex()).parse(symbols);
		return symbols;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "php");
	}

	@Override
	public int getVersion() {
		return 1;
	}

	private static class Parser {

		private final String source;

		private final List<Token> tokens;

		private int pos;

		private Parser(String source, List<Token> tokens) {
			this.source = source;
			this.tokens = tokens;
		}

		private void parse(List<PhpSymbol> symbols) {
			parseStatements(symbols, null, null);
		}

		private void parseStatements(List<PhpSymbol> symbols, @Nullable PhpSymbol parentSymbol, @Nullable String terminator) {
			NamespaceSymbol namespaceSymbol = null;
			Token namespaceStart = null;
			Token lastStatementEnd = null;
			while (!isEnd() && (terminator == null || !peek(terminator))) {
				Token token = peek();
				int before = pos;
				if (peek("namespace")) {
					if (namespaceSymbol != null && namespaceStart != null && lastStatementEnd != null)
						namespaceSymbol.setScope(range(namespaceStart, lastStatementEnd));
					namespaceSymbol = parseNamespace(symbols);
					namespaceStart = token;
					lastStatementEnd = previousMeaningful(token);
				} else {
					parseStatement(symbols, namespaceSymbol != null? namespaceSymbol: parentSymbol);
					lastStatementEnd = previousMeaningful(token);
				}
				if (pos == before)
					pos++;
			}
			if (namespaceSymbol != null && namespaceStart != null && lastStatementEnd != null)
				namespaceSymbol.setScope(range(namespaceStart, lastStatementEnd));
			if (terminator != null && peek(terminator))
				pos++;
		}

		@Nullable
		private NamespaceSymbol parseNamespace(List<PhpSymbol> symbols) {
			Token start = consume();
			List<Token> parts = parseNameParts();
			NamespaceSymbol namespaceSymbol = null;
			if (!parts.isEmpty()) {
				String name = parts.get(parts.size()-1).text;
				StringBuilder prefix = new StringBuilder();
				for (int i=0; i<parts.size()-1; i++)
					prefix.append(parts.get(i).text).append("\\");
				namespaceSymbol = new NamespaceSymbol(null, new QualifiedName(name,
						prefix.length()!=0? prefix.toString(): null, null), range(parts.get(0), parts.get(parts.size()-1)),
						range(start, currentStatementEnd(start)));
				symbols.add(namespaceSymbol);
			}
			if (peek("{")) {
				pos++;
				parseStatements(symbols, namespaceSymbol, "}");
				return null;
			} else {
				skipToStatementEnd();
				return namespaceSymbol;
			}
		}

		private void parseStatement(List<PhpSymbol> symbols, @Nullable PhpSymbol parentSymbol) {
			skipAttributes();
			Token token = peek();
			if (token == null)
				return;
			if (peek("{")) {
				pos++;
				parseStatements(symbols, parentSymbol, "}");
			} else if (peek("use")) {
				parseUse(symbols, parentSymbol);
			} else if (peek("define")) {
				parseDefine(symbols, parentSymbol);
			} else if (peek("const")) {
				parseConstDeclaration(symbols, parentSymbol, Visibility.PUBLIC);
			} else if (peek("function")) {
				parseFunction(symbols, parentSymbol, Visibility.PUBLIC);
			} else if (isClassStart()) {
				parseType(symbols, parentSymbol);
			} else if (peek("global")) {
				parseGlobalVariables(symbols);
			} else if (peek("static") && !(parentSymbol instanceof TypeSymbol)) {
				parseStaticVariables(symbols, parentSymbol);
			} else if (token.type == TokenType.VARIABLE && isNamespaceScope(parentSymbol)) {
				parseAssignmentVariable(symbols, token);
			} else {
				skipStatement(symbols, parentSymbol);
			}
		}

		private void parseUse(List<PhpSymbol> symbols, @Nullable PhpSymbol parentSymbol) {
			consume();
			if (peek("function") || peek("const"))
				pos++;
			while (!isEnd() && !peek(";")) {
				List<Token> prefix = parseNameParts();
				if (peek("{")) {
					pos++;
					while (!isEnd() && !peek("}")) {
						parseNameParts();
						if (peek("as")) {
							pos++;
							Token alias = consumeIdentifier();
							if (alias != null)
								symbols.add(new ImportedSymbol(parentSymbol, alias.text, range(alias), null));
						}
						if (peek(","))
							pos++;
						else if (!peek("}"))
							pos++;
					}
					if (peek("}"))
						pos++;
				} else if (!prefix.isEmpty()) {
					Token imported = prefix.get(prefix.size()-1);
					if (peek("as")) {
						pos++;
						Token alias = consumeIdentifier();
						if (alias != null)
							imported = alias;
					}
					symbols.add(new ImportedSymbol(parentSymbol, imported.text, range(imported), null));
				} else {
					pos++;
				}
				if (peek(","))
					pos++;
			}
			skipToStatementEnd();
		}

		private void parseDefine(List<PhpSymbol> symbols, @Nullable PhpSymbol parentSymbol) {
			consume();
			if (!peek("(")) {
				skipToStatementEnd();
				return;
			}
			pos++;
			Token nameToken = peek();
			String name = null;
			if (nameToken != null && nameToken.type == TokenType.STRING && !nameToken.text.contains("$"))
				name = nameToken.value;
			else if (nameToken != null && nameToken.type == TokenType.IDENTIFIER)
				name = nameToken.text;
			if (name != null)
				symbols.add(new ConstantSymbol(findNamespaceParent(parentSymbol), name, Visibility.PUBLIC, range(nameToken)));
			skipPlainStatement();
		}

		private void parseConstDeclaration(List<PhpSymbol> symbols, @Nullable PhpSymbol parentSymbol, Visibility visibility) {
			consume();
			while (!isEnd() && !peek(";") && !peek("}")) {
				Token identifier = consumeIdentifier();
				if (identifier != null)
					symbols.add(new ConstantSymbol(parentSymbol, identifier.text, visibility, range(identifier)));
				skipInitializer();
				if (peek(","))
					pos++;
				else if (!peek(";"))
					break;
			}
			skipToStatementEnd();
		}

		private void parseFunction(List<PhpSymbol> symbols, @Nullable PhpSymbol parentSymbol, Visibility visibility) {
			Token start = consume();
			if (peek("&"))
				pos++;
			Token name = consumeIdentifier();
			if (name == null) {
				skipStatement(symbols, parentSymbol);
				return;
			}
			String params = parseParams();
			String returnType = parseReturnType();
			Token scopeEnd = currentStatementEnd(start);
			FunctionSymbol functionSymbol = new FunctionSymbol(findNamespaceParent(parentSymbol), name.text, visibility,
					params, returnType, range(name), range(start, scopeEnd));
			if (parentSymbol instanceof TypeSymbol)
				functionSymbol = new FunctionSymbol(parentSymbol, name.text, visibility, params, returnType, range(name),
						range(start, scopeEnd));
			symbols.add(functionSymbol);
			if (peek("{"))
				skipBlock();
			else
				skipToStatementEnd();
		}

		private void parseType(List<PhpSymbol> symbols, @Nullable PhpSymbol parentSymbol) {
			skipAttributes();
			while (isModifier())
				pos++;
			Token typeToken = consume();
			TypeSymbol.Kind kind;
			if ("trait".equals(typeToken.text))
				kind = TypeSymbol.Kind.TRAIT;
			else if ("interface".equals(typeToken.text))
				kind = TypeSymbol.Kind.INTERFACE;
			else
				kind = TypeSymbol.Kind.CLASS;
			Token name = consumeIdentifier();
			if (name == null) {
				skipStatement(symbols, parentSymbol);
				return;
			}
			Token scopeEnd = currentStatementEnd(typeToken);
			TypeSymbol typeSymbol = new TypeSymbol(findNamespaceParent(parentSymbol), name.text, kind, range(name),
					range(typeToken, scopeEnd));
			symbols.add(typeSymbol);
			while (!isEnd() && !peek("{") && !peek(";"))
				pos++;
			if (peek("{")) {
				pos++;
				parseClassBody(symbols, typeSymbol);
			} else {
				skipToStatementEnd();
			}
		}

		private void parseClassBody(List<PhpSymbol> symbols, TypeSymbol typeSymbol) {
			while (!isEnd() && !peek("}")) {
				Token token = peek();
				int before = pos;
				skipAttributes();
				if (peek("{")) {
					pos++;
					parseStatements(symbols, typeSymbol, "}");
				} else if (isClassStart()) {
					parseType(symbols, typeSymbol);
				} else {
					Visibility visibility = Visibility.PUBLIC;
					while (isModifier()) {
						if (peek("private"))
							visibility = Visibility.PRIVATE;
						else if (peek("protected"))
							visibility = Visibility.PROTECTED;
						else if (peek("public"))
							visibility = Visibility.PUBLIC;
						pos++;
					}
					if (peek("function")) {
						parseFunction(symbols, typeSymbol, visibility);
					} else if (peek("const")) {
						parseConstDeclaration(symbols, typeSymbol, visibility);
					} else if (peek("var")) {
						pos++;
						parsePropertyDeclaration(symbols, typeSymbol, Visibility.PUBLIC);
					} else if (peek() != null && peek().type == TokenType.VARIABLE) {
						parsePropertyDeclaration(symbols, typeSymbol, visibility);
					} else if (hasVariableBeforeStatementEnd()) {
						parsePropertyDeclaration(symbols, typeSymbol, visibility);
					} else {
						skipStatement(symbols, typeSymbol);
					}
				}
				if (pos == before)
					pos++;
				if (token == peek())
					pos++;
			}
			if (peek("}"))
				pos++;
		}

		private void parsePropertyDeclaration(List<PhpSymbol> symbols, TypeSymbol typeSymbol, Visibility visibility) {
			while (!isEnd() && !peek(";") && !peek("}")) {
				Token variable = peek();
				if (variable != null && variable.type == TokenType.VARIABLE) {
					symbols.add(new VariableSymbol(typeSymbol, variable.text, visibility, range(variable)));
					pos++;
				} else {
					pos++;
				}
				skipInitializer();
				if (peek(","))
					pos++;
			}
			skipToStatementEnd();
		}

		private boolean hasVariableBeforeStatementEnd() {
			int parens = 0;
			int brackets = 0;
			for (int i=pos; i<tokens.size(); i++) {
				Token token = tokens.get(i);
				if ("(".equals(token.text))
					parens++;
				else if (")".equals(token.text))
					parens--;
				else if ("[".equals(token.text))
					brackets++;
				else if ("]".equals(token.text))
					brackets--;
				else if (parens <= 0 && brackets <= 0 && (";".equals(token.text) || "}".equals(token.text)))
					return false;
				else if (parens <= 0 && brackets <= 0 && token.type == TokenType.VARIABLE)
					return true;
			}
			return false;
		}

		private void parseGlobalVariables(List<PhpSymbol> symbols) {
			consume();
			while (!isEnd() && !peek(";")) {
				Token variable = peek();
				if (variable != null && variable.type == TokenType.VARIABLE)
					addGlobalVariable(symbols, variable);
				pos++;
			}
			skipToStatementEnd();
		}

		private void parseStaticVariables(List<PhpSymbol> symbols, @Nullable PhpSymbol parentSymbol) {
			if (!isNamespaceScope(parentSymbol)) {
				skipStatement(symbols, parentSymbol);
				return;
			}
			consume();
			while (!isEnd() && !peek(";")) {
				Token variable = peek();
				if (variable != null && variable.type == TokenType.VARIABLE)
					addGlobalVariable(symbols, variable);
				pos++;
				skipInitializer();
				if (peek(","))
					pos++;
			}
			skipToStatementEnd();
		}

		private void parseAssignmentVariable(List<PhpSymbol> symbols, Token variable) {
			if (pos+1 < tokens.size() && "=".equals(tokens.get(pos+1).text)) {
				addGlobalVariable(symbols, variable);
				pos += 2;
			}
			skipPlainStatement();
		}

		private void skipStatement(List<PhpSymbol> symbols, @Nullable PhpSymbol parentSymbol) {
			int parens = 0;
			int brackets = 0;
			while (!isEnd()) {
				if (peek("(")) {
					parens++;
				} else if (peek(")")) {
					parens--;
				} else if (peek("[")) {
					brackets++;
				} else if (peek("]")) {
					brackets--;
				} else if (parens <= 0 && brackets <= 0 && peek("{")) {
					pos++;
					parseStatements(symbols, parentSymbol, "}");
					return;
				} else if (parens <= 0 && brackets <= 0 && peek(";")) {
					pos++;
					return;
				} else if (parens <= 0 && brackets <= 0 && peek("}")) {
					return;
				}
				pos++;
			}
		}

		private void skipBlock() {
			if (!peek("{"))
				return;
			Token open = consume();
			Token close = matchingClose(open, "{", "}");
			if (close != null)
				pos = close.index + 1;
		}

		private void skipAttributes() {
			while (peek("#") && pos+1 < tokens.size() && "[".equals(tokens.get(pos+1).text)) {
				pos++;
				skipBracketed("[", "]");
			}
			while (peek("[")) {
				int oldPos = pos;
				skipBracketed("[", "]");
				if (!isAttributeTarget()) {
					pos = oldPos;
					break;
				}
			}
		}

		private void skipBracketed(String openText, String closeText) {
			Token open = peek();
			if (open == null || !openText.equals(open.text))
				return;
			Token close = matchingClose(open, openText, closeText);
			if (close != null)
				pos = close.index + 1;
			else
				pos++;
		}

		private boolean isAttributeTarget() {
			int index = pos;
			while (index < tokens.size() && MODIFIERS.contains(tokens.get(index).text))
				index++;
			if (index == tokens.size())
				return false;
			String text = tokens.get(index).text;
			return "function".equals(text) || "const".equals(text) || "class".equals(text)
					|| "trait".equals(text) || "interface".equals(text) || "var".equals(text)
					|| index != pos && index+1 < tokens.size() && tokens.get(index+1).type == TokenType.VARIABLE;
		}

		private String parseParams() {
			if (!peek("("))
				return "()";
			Token open = consume();
			Token close = matchingClose(open, "(", ")");
			if (close == null)
				return "()";
			pos = close.index + 1;
			if (open.endOffset > close.startOffset)
				return "()";
			String params = source.substring(open.endOffset, close.startOffset);
			if (params.trim().length() == 0)
				return "()";
			else
				return "(" + params + ")";
		}

		@Nullable
		private String parseReturnType() {
			if (!peek(":"))
				return null;
			Token colon = consume();
			Token end = colon;
			int nested = 0;
			while (!isEnd()) {
				if (nested == 0 && (peek("{") || peek(";")))
					break;
				if (peek("(") || peek("["))
					nested++;
				else if (peek(")") || peek("]"))
					nested--;
				end = consume();
			}
			return source.substring(colon.startOffset, end.endOffset);
		}

		private List<Token> parseNameParts() {
			List<Token> parts = new ArrayList<>();
			while (peek("\\"))
				pos++;
			while (!isEnd()) {
				Token identifier = consumeIdentifier();
				if (identifier == null)
					break;
				parts.add(identifier);
				if (peek("\\"))
					pos++;
				else
					break;
			}
			return parts;
		}

		private void skipInitializer() {
			if (!peek("="))
				return;
			pos++;
			int parens = 0;
			int brackets = 0;
			int braces = 0;
			while (!isEnd()) {
				if (parens == 0 && brackets == 0 && braces == 0 && (peek(",") || peek(";") || peek("}")))
					break;
				if (peek("("))
					parens++;
				else if (peek(")"))
					parens--;
				else if (peek("["))
					brackets++;
				else if (peek("]"))
					brackets--;
				else if (peek("{"))
					braces++;
				else if (peek("}"))
					braces--;
				pos++;
			}
		}

		private void skipToStatementEnd() {
			while (!isEnd() && !peek(";") && !peek("}"))
				pos++;
			if (peek(";"))
				pos++;
		}

		private Token currentStatementEnd(Token fallback) {
			Token end = fallback;
			int i = pos;
			int nested = 0;
			while (i < tokens.size()) {
				Token token = tokens.get(i);
				if (nested == 0 && (";".equals(token.text) || "}".equals(token.text)))
					return token;
				if ("{".equals(token.text) || "(".equals(token.text) || "[".equals(token.text))
					nested++;
				else if ("}".equals(token.text) || ")".equals(token.text) || "]".equals(token.text))
					nested--;
				end = token;
				i++;
			}
			return end;
		}

		@Nullable
		private Token matchingClose(Token open, String openText, String closeText) {
			int nested = 0;
			for (int i=open.index; i<tokens.size(); i++) {
				Token token = tokens.get(i);
				if (openText.equals(token.text))
					nested++;
				else if (closeText.equals(token.text)) {
					nested--;
					if (nested == 0)
						return token;
				}
			}
			return null;
		}

		private void skipPlainStatement() {
			int parens = 0;
			int brackets = 0;
			int braces = 0;
			while (!isEnd()) {
				if (peek("(")) {
					parens++;
				} else if (peek(")")) {
					parens--;
				} else if (peek("[")) {
					brackets++;
				} else if (peek("]")) {
					brackets--;
				} else if (peek("{")) {
					braces++;
				} else if (peek("}")) {
					if (braces == 0)
						return;
					braces--;
				} else if (parens <= 0 && brackets <= 0 && braces <= 0 && peek(";")) {
					pos++;
					return;
				}
				pos++;
			}
		}

		private boolean isClassStart() {
			int index = pos;
			while (index < tokens.size() && MODIFIERS.contains(tokens.get(index).text))
				index++;
			if (index == tokens.size())
				return false;
			String text = tokens.get(index).text;
			return "class".equals(text) || "trait".equals(text) || "interface".equals(text);
		}

		private boolean isModifier() {
			return peek() != null && MODIFIERS.contains(peek().text);
		}

		private boolean isNamespaceScope(@Nullable PhpSymbol parentSymbol) {
			return parentSymbol == null || parentSymbol instanceof NamespaceSymbol;
		}

		@Nullable
		private NamespaceSymbol findNamespaceParent(@Nullable PhpSymbol parentSymbol) {
			while (parentSymbol != null) {
				if (parentSymbol instanceof NamespaceSymbol)
					return (NamespaceSymbol) parentSymbol;
				parentSymbol = parentSymbol.getParent();
			}
			return null;
		}

		private void addGlobalVariable(List<PhpSymbol> symbols, Token variable) {
			if (findGlobalVariable(symbols, variable.text) == null)
				symbols.add(new VariableSymbol(null, variable.text, Visibility.PUBLIC, range(variable)));
		}

		@Nullable
		private VariableSymbol findGlobalVariable(List<PhpSymbol> symbols, String varName) {
			for (PhpSymbol symbol: symbols) {
				if (symbol.getParent() == null && symbol instanceof VariableSymbol && symbol.getName().equals(varName))
					return (VariableSymbol) symbol;
			}
			return null;
		}

		@Nullable
		private Token consumeIdentifier() {
			if (peek() != null && peek().type == TokenType.IDENTIFIER)
				return consume();
			return null;
		}

		private Token consume() {
			return tokens.get(pos++);
		}

		@Nullable
		private Token peek() {
			if (pos < tokens.size())
				return tokens.get(pos);
			return null;
		}

		private boolean peek(String text) {
			return peek() != null && text.equals(peek().text);
		}

		private boolean isEnd() {
			return pos >= tokens.size();
		}

		private Token previousMeaningful(Token fallback) {
			if (pos > 0)
				return tokens.get(pos-1);
			else
				return fallback;
		}

		private PlanarRange range(Token token) {
			return range(token, token);
		}

		private PlanarRange range(Token from, Token to) {
			return new PlanarRange(from.line, from.column, to.endLine, to.endColumn);
		}

	}

	private enum TokenType {IDENTIFIER, VARIABLE, STRING, SYMBOL}

	private static class Token {

		private final TokenType type;

		private final String text;

		private final String value;

		private final int startOffset;

		private final int endOffset;

		private final int line;

		private final int column;

		private final int endLine;

		private final int endColumn;

		private int index;

		private Token(TokenType type, String text, String value, int startOffset, int endOffset, int line, int column,
				int endLine, int endColumn) {
			this.type = type;
			this.text = text;
			this.value = value;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
			this.line = line;
			this.column = column;
			this.endLine = endLine;
			this.endColumn = endColumn;
		}

	}

	private static class Lexer {

		private final String source;

		private final List<Token> tokens = new ArrayList<>();

		private int pos;

		private int line;

		private int column;

		private boolean phpMode;

		private Lexer(String source) {
			this.source = source;
		}

		private List<Token> lex() {
			phpMode = !source.contains("<?");
			while (pos < source.length()) {
				if (!phpMode) {
					if (source.startsWith("<?php", pos) || source.startsWith("<?=", pos) || source.startsWith("<?", pos)) {
						advance(source.startsWith("<?php", pos)? 5: 2);
						phpMode = true;
					} else {
						advance();
					}
				} else if (source.startsWith("?>", pos)) {
					advance(2);
					phpMode = false;
				} else {
					lexPhpToken();
				}
			}
			for (int i=0; i<tokens.size(); i++)
				tokens.get(i).index = i;
			return tokens;
		}

		private void lexPhpToken() {
			char ch = source.charAt(pos);
			if (Character.isWhitespace(ch)) {
				advance();
			} else if (source.startsWith("//", pos) || source.startsWith("#", pos) && !source.startsWith("#[", pos)) {
				while (pos < source.length() && source.charAt(pos) != '\n' && !source.startsWith("?>", pos))
					advance();
			} else if (source.startsWith("/*", pos)) {
				advance(2);
				while (pos < source.length() && !source.startsWith("*/", pos))
					advance();
				if (source.startsWith("*/", pos))
					advance(2);
			} else if (ch == '\'' || ch == '"') {
				readString(ch);
			} else if (ch == '$') {
				readVariable();
			} else if (isIdentifierStart(ch)) {
				readIdentifier();
			} else {
				addToken(TokenType.SYMBOL, String.valueOf(ch), String.valueOf(ch), pos, pos+1, line, column,
						line, column+1);
				advance();
			}
		}

		private void readString(char quote) {
			int start = pos;
			int startLine = line;
			int startColumn = column;
			advance();
			StringBuilder value = new StringBuilder();
			while (pos < source.length()) {
				char ch = source.charAt(pos);
				if (ch == '\\') {
					advance();
					if (pos < source.length()) {
						value.append(source.charAt(pos));
						advance();
					}
				} else if (ch == quote) {
					advance();
					break;
				} else {
					value.append(ch);
					advance();
				}
			}
			addToken(TokenType.STRING, source.substring(start, pos), value.toString(), start, pos, startLine, startColumn,
					line, column);
		}

		private void readVariable() {
			int start = pos;
			int startLine = line;
			int startColumn = column;
			advance();
			while (pos < source.length() && isIdentifierPart(source.charAt(pos)))
				advance();
			addToken(TokenType.VARIABLE, source.substring(start, pos), source.substring(start, pos), start, pos,
					startLine, startColumn, line, column);
		}

		private void readIdentifier() {
			int start = pos;
			int startLine = line;
			int startColumn = column;
			advance();
			while (pos < source.length() && isIdentifierPart(source.charAt(pos)))
				advance();
			String text = source.substring(start, pos);
			addToken(TokenType.IDENTIFIER, text, text, start, pos, startLine, startColumn, line, column);
		}

		private void addToken(TokenType type, String text, String value, int startOffset, int endOffset, int startLine,
				int startColumn, int endLine, int endColumn) {
			tokens.add(new Token(type, text, value, startOffset, endOffset, startLine, startColumn, endLine, endColumn));
		}

		private void advance() {
			if (source.charAt(pos) == '\n') {
				line++;
				column = 0;
			} else {
				column++;
			}
			pos++;
		}

		private void advance(int count) {
			for (int i=0; i<count && pos<source.length(); i++)
				advance();
		}

		private boolean isIdentifierStart(char ch) {
			return Character.isLetter(ch) || ch == '_';
		}

		private boolean isIdentifierPart(char ch) {
			return Character.isLetterOrDigit(ch) || ch == '_';
		}

	}

}
