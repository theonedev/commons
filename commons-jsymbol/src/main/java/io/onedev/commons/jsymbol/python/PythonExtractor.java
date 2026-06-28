package io.onedev.commons.jsymbol.python;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Preconditions;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.python.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.python.symbols.ImportedSymbol;
import io.onedev.commons.jsymbol.python.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.python.symbols.PythonSymbol;
import io.onedev.commons.jsymbol.python.symbols.VariableSymbol;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.PlanarRange;

public class PythonExtractor extends AbstractSymbolExtractor<PythonSymbol> {

	private static final Pattern DEF_PATTERN = Pattern.compile("^(?:async\\s+)?def\\s+([A-Za-z_]\\w*)\\s*\\(");

	private static final Pattern CLASS_PATTERN = Pattern.compile("^class\\s+([A-Za-z_]\\w*)\\s*(?:\\(|:|$)");

	private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z_]\\w*");

	@Override
	public List<PythonSymbol> extract(String fileName, String fileContent) {
		List<PythonSymbol> symbols = new ArrayList<>();
		List<String> lines = splitLines(fileContent);
		List<Statement> statements = getStatements(lines);
		extract(statements, 0, -1, null, symbols);

		for (Iterator<PythonSymbol> it = symbols.iterator(); it.hasNext();) {
			PythonSymbol symbol = it.next();
			if (symbol instanceof VariableSymbol) {
				VariableSymbol variableSymbol = (VariableSymbol) symbol;
				if (variableSymbol.isLocal() && !variableSymbol.isAttribute()
						&& getChildren(symbols, symbol).isEmpty()) {
					it.remove();
				}
			}
		}

		return symbols;
	}

	private List<String> splitLines(String fileContent) {
		List<String> lines = new ArrayList<>();
		for (String line: fileContent.split("\\n", -1)) {
			if (line.endsWith("\r"))
				line = line.substring(0, line.length()-1);
			lines.add(line);
		}
		return lines;
	}

	private int extract(List<Statement> statements, int index, int parentIndent, @Nullable PythonSymbol parentSymbol,
			List<PythonSymbol> symbols) {
		while (index < statements.size()) {
			Statement statement = statements.get(index);
			if (statement.indent <= parentIndent)
				break;

			Matcher defMatcher = DEF_PATTERN.matcher(statement.scanText);
			Matcher classMatcher = CLASS_PATTERN.matcher(statement.scanText);
			if (defMatcher.find()) {
				NameLocation name = locateName(statement, defMatcher.group(1), 0);
				MethodSymbol methodSymbol = new MethodSymbol(parentSymbol, name.text, range(name),
						getScope(statements, index), getParameters(statement.text, defMatcher.end()-1));
				symbols.add(methodSymbol);
				index = extract(statements, index+1, statement.indent, methodSymbol, symbols);
			} else if (classMatcher.find()) {
				NameLocation name = locateName(statement, classMatcher.group(1), 0);
				ClassSymbol classSymbol = new ClassSymbol(parentSymbol, name.text, range(name),
						getScope(statements, index), getClassParameters(statement.text, classMatcher.end()-1));
				symbols.add(classSymbol);
				index = extract(statements, index+1, statement.indent, classSymbol, symbols);
			} else {
				extractStatement(statement, parentSymbol, symbols);
				index++;
			}
		}
		return index;
	}

	private void extractStatement(Statement statement, @Nullable PythonSymbol parentSymbol, List<PythonSymbol> symbols) {
		String text = statement.scanText;
		if (text.startsWith("import ")) {
			for (String imported: splitTopLevel(text.substring("import ".length()), ',')) {
				imported = imported.trim();
				if (imported.length() != 0)
					processImport(statement, imported, parentSymbol, symbols);
			}
		} else if (text.startsWith("from ")) {
			processFromImport(statement, parentSymbol, symbols);
		} else if (!text.startsWith("@")) {
			List<String> assignments = splitAssignments(statement.scanText);
			if (!assignments.isEmpty()) {
				List<List<NameLocation>> variablePaths = getVariablePaths(statement, assignments.get(0));
				if (!variablePaths.isEmpty()) {
					for (int i=1; i<assignments.size(); i++) {
						for (List<NameLocation> variablePath: variablePaths)
							processAssignment(symbols, parentSymbol, variablePath);
						variablePaths = getVariablePaths(statement, assignments.get(i));
						if (variablePaths.isEmpty())
							break;
					}
				}
			}
		}
	}

	private void processImport(Statement statement, String imported, @Nullable PythonSymbol parentSymbol,
			List<PythonSymbol> symbols) {
		String name;
		int asIndex = imported.indexOf(" as ");
		if (asIndex != -1)
			name = imported.substring(asIndex + " as ".length()).trim();
		else
			name = imported.trim();
		if (isQualifiedName(name)) {
			NameLocation location = locateName(statement, getUnqualified(name), 0);
			symbols.add(new ImportedSymbol(parentSymbol, new QualifiedName(name, "."), range(location),
					range(statement)));
		}
	}

	private void processFromImport(Statement statement, @Nullable PythonSymbol parentSymbol, List<PythonSymbol> symbols) {
		int importIndex = statement.scanText.indexOf(" import ");
		if (importIndex == -1)
			return;
		String importedNames = statement.scanText.substring(importIndex + " import ".length()).trim();
		if (importedNames.startsWith("(") && importedNames.endsWith(")"))
			importedNames = importedNames.substring(1, importedNames.length()-1);
		for (String imported: splitTopLevel(importedNames, ',')) {
			imported = imported.trim();
			if (imported.length() == 0 || imported.equals("*"))
				continue;
			String name;
			int asIndex = imported.indexOf(" as ");
			if (asIndex != -1)
				name = imported.substring(asIndex + " as ".length()).trim();
			else
				name = imported.trim();
			if (isQualifiedName(name)) {
				NameLocation location = locateName(statement, getUnqualified(name), importIndex);
				symbols.add(new ImportedSymbol(parentSymbol, new QualifiedName(name), range(location),
						range(statement)));
			}
		}
	}

	private List<Statement> getStatements(List<String> lines) {
		List<Statement> statements = new ArrayList<>();
		List<String> strippedLines = stripStringsAndComments(lines);
		StatementBuilder builder = null;
		int depth = 0;
		for (int i=0; i<strippedLines.size(); i++) {
			String strippedLine = strippedLines.get(i);
				String strippedTrimmed = strippedLine.trim();
				if (strippedTrimmed.length() == 0)
					continue;
				if (builder == null)
					builder = new StatementBuilder(lines, i, countIndent(lines.get(i)));
				builder.append(i, stripComments(lines.get(i)).trim(), strippedTrimmed);
			depth += getDepthDelta(strippedLine);
			if (depth <= 0 && !isLineContinued(strippedLine)) {
				statements.add(builder.build());
				builder = null;
				depth = 0;
			}
		}
		if (builder != null)
			statements.add(builder.build());
		return statements;
	}

	private List<String> stripStringsAndComments(List<String> lines) {
		List<String> strippedLines = new ArrayList<>();
		String tripleQuote = null;
		for (String line: lines) {
			StringBuilder builder = new StringBuilder();
			for (int i=0; i<line.length(); i++) {
				char ch = line.charAt(i);
				if (tripleQuote != null) {
					if (line.startsWith(tripleQuote, i)) {
						builder.append("   ");
						i += 2;
						tripleQuote = null;
					} else {
						builder.append(' ');
					}
				} else if (line.startsWith("\"\"\"", i) || line.startsWith("'''", i)) {
					tripleQuote = line.substring(i, i+3);
					builder.append("   ");
					i += 2;
				} else if (ch == '"' || ch == '\'') {
					char quote = ch;
					builder.append(' ');
					i++;
					for (; i<line.length(); i++) {
						builder.append(' ');
						if (line.charAt(i) == '\\') {
							if (i+1 < line.length()) {
								i++;
								builder.append(' ');
							}
						} else if (line.charAt(i) == quote) {
							break;
						}
					}
				} else if (ch == '#') {
					while (i<line.length()) {
						builder.append(' ');
						i++;
					}
				} else {
					builder.append(ch);
				}
			}
			strippedLines.add(builder.toString());
		}
		return strippedLines;
	}

	private String stripComments(String line) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<line.length(); i++) {
			char ch = line.charAt(i);
			if (ch == '"' || ch == '\'') {
				char quote = ch;
				builder.append(ch);
				i++;
				for (; i<line.length(); i++) {
					ch = line.charAt(i);
					builder.append(ch);
					if (ch == '\\') {
						if (i+1 < line.length()) {
							i++;
							builder.append(line.charAt(i));
						}
					} else if (ch == quote) {
						break;
					}
				}
			} else if (ch == '#') {
				break;
			} else {
				builder.append(ch);
			}
		}
		return builder.toString();
	}

	private int countIndent(String line) {
		int indent = 0;
		while (indent < line.length()) {
			char ch = line.charAt(indent);
			if (ch == ' ' || ch == '\t')
				indent++;
			else
				break;
		}
		return indent;
	}

	private int getDepthDelta(String line) {
		int delta = 0;
		for (int i=0; i<line.length(); i++) {
			char ch = line.charAt(i);
			if (ch == '(' || ch == '[' || ch == '{')
				delta++;
			else if (ch == ')' || ch == ']' || ch == '}')
				delta--;
		}
		return delta;
	}

	private boolean isLineContinued(String line) {
		int index = line.length()-1;
		while (index >= 0 && Character.isWhitespace(line.charAt(index)))
			index--;
		return index >= 0 && line.charAt(index) == '\\';
	}

	private PlanarRange getScope(List<Statement> statements, int index) {
		Statement statement = statements.get(index);
		int endLine = statement.endLine;
		for (int i=index+1; i<statements.size(); i++) {
			Statement child = statements.get(i);
			if (child.indent <= statement.indent)
				break;
			endLine = child.endLine;
		}
		return new PlanarRange(statement.startLine, statement.indent, endLine, statements.get(index).lines.get(endLine).length());
	}

	private String getParameters(String text, int openParen) {
		int closeParen = findMatchingParen(text, openParen);
		if (closeParen != -1)
			return "(" + compact(text.substring(openParen+1, closeParen)) + ")";
		else
			return "()";
	}

	@Nullable
	private String getClassParameters(String text, int start) {
		int openParen = text.indexOf('(', Math.max(0, start-1));
		if (openParen != -1) {
			int closeParen = findMatchingParen(text, openParen);
			if (closeParen != -1)
				return "(" + compact(text.substring(openParen+1, closeParen)) + ")";
		}
		return null;
	}

	private int findMatchingParen(String text, int openParen) {
		int depth = 0;
		for (int i=openParen; i<text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == '(')
				depth++;
			else if (ch == ')') {
				depth--;
				if (depth == 0)
					return i;
			}
		}
		return -1;
	}

	private String compact(String text) {
		StringBuilder builder = new StringBuilder();
		boolean inString = false;
		char quote = 0;
		for (int i=0; i<text.length(); i++) {
			char ch = text.charAt(i);
			if (inString) {
				builder.append(ch);
				if (ch == '\\') {
					if (i+1 < text.length()) {
						i++;
						builder.append(text.charAt(i));
					}
				} else if (ch == quote) {
					inString = false;
				}
			} else if (ch == '"' || ch == '\'') {
				inString = true;
				quote = ch;
				builder.append(ch);
			} else if (!Character.isWhitespace(ch)) {
				builder.append(ch);
			}
		}
		return builder.toString();
	}

	private List<String> splitAssignments(String text) {
		List<String> parts = new ArrayList<>();
		int start = 0;
		int depth = 0;
		for (int i=0; i<text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == '(' || ch == '[' || ch == '{') {
				depth++;
			} else if (ch == ')' || ch == ']' || ch == '}') {
				depth--;
			} else if (ch == '=' && depth == 0 && !isComparisonOrArrow(text, i)) {
				parts.add(text.substring(start, i));
				start = i+1;
			}
		}
		if (!parts.isEmpty())
			parts.add(text.substring(start));
		return parts;
	}

	private boolean isComparisonOrArrow(String text, int index) {
		char before = index > 0? text.charAt(index-1): 0;
		char after = index+1 < text.length()? text.charAt(index+1): 0;
		return before == '=' || before == '!' || before == '<' || before == '>'
				|| before == ':' || after == '=' || after == '>';
	}

	private List<List<NameLocation>> getVariablePaths(Statement statement, String text) {
		List<List<NameLocation>> variablePaths = new ArrayList<>();
		int searchFrom = 0;
		for (String part: splitTopLevel(text, ',')) {
			part = part.trim();
			if (part.length() == 0)
				continue;
			if (!isQualifiedName(part))
				return new ArrayList<>();
			List<NameLocation> variablePath = new ArrayList<>();
			for (String segment: part.split("\\.")) {
				segment = segment.trim();
				NameLocation location = locateName(statement, segment, searchFrom);
				variablePath.add(location);
				searchFrom = location.searchIndex + segment.length();
			}
			variablePaths.add(variablePath);
		}
		return variablePaths;
	}

	private List<String> splitTopLevel(String text, char separator) {
		List<String> parts = new ArrayList<>();
		int start = 0;
		int depth = 0;
		for (int i=0; i<text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == '(' || ch == '[' || ch == '{')
				depth++;
			else if (ch == ')' || ch == ']' || ch == '}')
				depth--;
			else if (ch == separator && depth == 0) {
				parts.add(text.substring(start, i));
				start = i+1;
			}
		}
		parts.add(text.substring(start));
		return parts;
	}

	private boolean isQualifiedName(String text) {
		return text.matches("[A-Za-z_]\\w*(\\s*\\.\\s*[A-Za-z_]\\w*)*");
	}

	private String getUnqualified(String name) {
		int index = name.lastIndexOf('.');
		if (index != -1)
			return name.substring(index+1).trim();
		else
			return name.trim();
	}

	private NameLocation locateName(Statement statement, String name, int searchFrom) {
		Matcher matcher = NAME_PATTERN.matcher(statement.text);
		while (matcher.find(Math.max(0, searchFrom))) {
			if (matcher.group().equals(name)) {
				Position position = statement.getPosition(matcher.start());
				return new NameLocation(name, position.line, position.column, matcher.start());
			}
			searchFrom = matcher.end();
		}
		return new NameLocation(name, statement.startLine, statement.indent, searchFrom);
	}

	private PlanarRange range(NameLocation location) {
		return new PlanarRange(location.line, location.column, location.line, location.column + location.text.length());
	}

	private PlanarRange range(Statement statement) {
		return new PlanarRange(statement.startLine, statement.indent, statement.endLine,
				statement.lines.get(statement.endLine).length());
	}

	private List<PythonSymbol> getChildren(List<PythonSymbol> symbols, @Nullable PythonSymbol parentSymbol) {
		List<PythonSymbol> children = new ArrayList<>();
		for (PythonSymbol symbol: symbols) {
			if (symbol.getParent() == parentSymbol)
				children.add(symbol);
		}
		return children;
	}

	@Nullable
	private PythonSymbol getChild(List<PythonSymbol> symbols, @Nullable PythonSymbol parent, String... childPath) {
		Preconditions.checkArgument(childPath.length != 0);
		String childName = childPath[0];
		PythonSymbol child = null;
		for (PythonSymbol symbol: symbols) {
			if (symbol.getParent() == parent && childName.equals(symbol.getName())) {
				child = symbol;
				break;
			}
		}
		if (child != null && childPath.length > 1)
			return getChild(symbols, child, Arrays.copyOfRange(childPath, 1, childPath.length));
		else
			return child;
	}

	@Nullable
	private PythonSymbol getSymbolInHierarchy(List<PythonSymbol> symbols, @Nullable PythonSymbol parent,
			String symbolName) {
		while (true) {
			PythonSymbol symbol = getChild(symbols, parent, symbolName);
			if (symbol != null)
				return symbol;
			else if (parent == null)
				return null;
			parent = (PythonSymbol) parent.getParent();
		}
	}

	private ClassSymbol findEnclosingClass(PythonSymbol parentSymbol) {
		while (parentSymbol != null && !(parentSymbol instanceof ClassSymbol))
			parentSymbol = parentSymbol.getParent();
		return (ClassSymbol) parentSymbol;
	}

	private void processAssignment(List<PythonSymbol> symbols, PythonSymbol parentSymbol,
			List<NameLocation> namePath) {
		if (!namePath.isEmpty()) {
			NameLocation rootName = namePath.get(0);
			PythonSymbol symbol = null;
			boolean isSelf = rootName.text.equals("self");
			if (isSelf)
				symbol = findEnclosingClass(parentSymbol);
			if (symbol == null) {
				if (namePath.size() != 1)
					symbol = getSymbolInHierarchy(symbols, parentSymbol, rootName.text);
				else
					symbol = getChild(symbols, parentSymbol, rootName.text);
			}
			if (symbol == null) {
				symbol = new VariableSymbol(parentSymbol, rootName.text, range(rootName), null,
						parentSymbol instanceof ClassSymbol);
				symbols.add(symbol);
			}
			for (int i=1; i<namePath.size(); i++) {
				NameLocation name = namePath.get(i);
				PythonSymbol childSymbol = getChild(symbols, symbol, name.text);
				if (childSymbol == null) {
					childSymbol = new VariableSymbol(symbol, name.text, range(name), null, true);
					if (symbol instanceof ClassSymbol && isSelf)
						symbols.add(getPropertyInsertPosition(symbols, (ClassSymbol) symbol), childSymbol);
					else
						symbols.add(childSymbol);
				}
				symbol = childSymbol;
			}
		}
	}

	private int getPropertyInsertPosition(List<PythonSymbol> symbols, ClassSymbol classSymbol) {
		int index = symbols.indexOf(classSymbol) + 1;
		while (index < symbols.size()) {
			PythonSymbol symbol = symbols.get(index);
			if (symbol.getParent() != classSymbol || !(symbol instanceof VariableSymbol))
				break;
			else
				index++;
		}
		return index;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "py");
	}

	@Override
	public int getVersion() {
		return 2;
	}

	private static class Statement {

		private final List<String> lines;

		private final String text;

		private final String scanText;

		private final List<Position> positions;

		private final int startLine;

		private final int endLine;

		private final int indent;

		private Statement(List<String> lines, String text, String scanText, List<Position> positions,
				int startLine, int endLine, int indent) {
			this.lines = lines;
			this.text = text;
			this.scanText = scanText;
			this.positions = positions;
			this.startLine = startLine;
			this.endLine = endLine;
			this.indent = indent;
		}

		private Position getPosition(int index) {
			if (index >= positions.size())
				return positions.get(positions.size()-1);
			else
				return positions.get(index);
		}

	}

	private static class StatementBuilder {

		private final List<String> lines;

		private final StringBuilder text = new StringBuilder();

		private final StringBuilder scanText = new StringBuilder();

		private final List<Position> positions = new ArrayList<>();

		private final int startLine;

		private final int indent;

		private int endLine;

		private StatementBuilder(List<String> lines, int startLine, int indent) {
			this.lines = lines;
			this.startLine = startLine;
			this.indent = indent;
		}

		private void append(int lineIndex, String trimmed, String strippedTrimmed) {
			if (text.length() != 0) {
				text.append(' ');
				scanText.append(' ');
				positions.add(new Position(lineIndex, 0));
			}
			int column = lines.get(lineIndex).indexOf(trimmed);
			for (int i=0; i<trimmed.length(); i++)
				positions.add(new Position(lineIndex, column+i));
			text.append(trimmed);
			scanText.append(strippedTrimmed);
			endLine = lineIndex;
		}

		private Statement build() {
			if (positions.isEmpty())
				positions.add(new Position(startLine, indent));
			return new Statement(lines, text.toString(), scanText.toString(), positions, startLine, endLine, indent);
		}

	}

	private static class Position {

		private final int line;

		private final int column;

		private Position(int line, int column) {
			this.line = line;
			this.column = column;
		}

	}

	private static class NameLocation {

		private final String text;

		private final int line;

		private final int column;

		private final int searchIndex;

		private NameLocation(String text, int line, int column, int searchIndex) {
			this.text = text;
			this.line = line;
			this.column = column;
			this.searchIndex = searchIndex;
		}

	}

}
