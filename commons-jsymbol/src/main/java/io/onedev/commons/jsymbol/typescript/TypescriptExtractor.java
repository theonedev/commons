package io.onedev.commons.jsymbol.typescript;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.typescript.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.typescript.symbols.TypeScriptSymbol;
import io.onedev.commons.jsymbol.typescript.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.typescript.symbols.VariableSymbol;
import io.onedev.commons.utils.PlanarRange;

public class TypescriptExtractor extends AbstractSymbolExtractor<TypeScriptSymbol> {

	@Override
	public List<TypeScriptSymbol> extract(String fileName, String fileContent) {
		return new Scanner(fileContent).scan();
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "js", "jsx", "ts", "tsx", "mts", "cts");
	}

	@Override
	public int getVersion() {
		return 19;
	}

	private static class Scanner {

		private final String source;

		private final String code;

		private final int[] rows;

		private final int[] columns;

		private final List<TypeScriptSymbol> symbols = new ArrayList<>();

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

		List<TypeScriptSymbol> scan() {
			scanDeclarations(0, code.length(), null, false);
			applyExportDirectives();
			return symbols;
		}

		private void applyExportDirectives() {
			Set<String> exportedNames = new HashSet<>();
			int depth = 0;
			for (int index=0; index<code.length(); index++) {
				char ch = code.charAt(index);
				if (ch == '{' || ch == '(' || ch == '[')
					depth++;
				else if (ch == '}' || ch == ')' || ch == ']')
					depth = Math.max(0, depth-1);
				else if (depth == 0 && startsWithWord(index, "export")) {
					index = readIdentifierEnd(index);
					ExportPrefix exportPrefix = skipExportPrefix(index);
					index = exportPrefix.index;
					if (index >= code.length())
						break;
					if (code.charAt(index) == '{') {
						int close = findMatching(index, code.length(), '{', '}');
						if (close == -1)
							continue;
						if (!hasExportFromClause(close+1, code.length()))
							collectExportListNames(index+1, close, exportedNames);
						index = close;
					} else if (code.charAt(index) == '*') {
						continue;
					} else if (startsWithWord(index, "type")) {
						index = skipWhitespace(readIdentifierEnd(index), code.length());
						if (index < code.length() && code.charAt(index) == '{') {
							int close = findMatching(index, code.length(), '{', '}');
							if (close != -1 && !hasExportFromClause(close+1, code.length()))
								collectExportListNames(index+1, close, exportedNames);
						}
					} else if (exportPrefix.defaultExport) {
						if (isIdentifierStart(code.charAt(index))) {
							int nameEnd = readIdentifierEnd(index);
							String name = code.substring(index, nameEnd);
							if (!isTypeKeyword(name) && !"function".equals(name) && !"class".equals(name))
								exportedNames.add(name);
						}
					}
				}
			}
			for (TypeScriptSymbol symbol: symbols) {
				if (symbol.getParent() == null && exportedNames.contains(symbol.getName()))
					symbol.markExported();
			}
		}

		private boolean hasExportFromClause(int index, int end) {
			index = skipWhitespace(index, end);
			return index < end && startsWithWord(index, "from");
		}

		private void collectExportListNames(int start, int end, Set<String> names) {
			int index = start;
			while (index < end) {
				index = skipWhitespace(index, end);
				if (index >= end || code.charAt(index) == '}')
					break;
				if (!isIdentifierStart(code.charAt(index))) {
					index++;
					continue;
				}
				int nameEnd = readIdentifierEnd(index);
				names.add(code.substring(index, nameEnd));
				index = skipWhitespace(nameEnd, end);
				if (index < end && startsWithWord(index, "as"))
					index = skipWhitespace(readIdentifierEnd(index), end);
				index = skipWhitespace(index, end);
				if (index < end && code.charAt(index) == ',')
					index++;
			}
		}

		private void scanDeclarations(int start, int end, @Nullable TypeScriptSymbol parent, boolean exportedContext) {
			int depth = 0;
			boolean pendingExport = false;
			boolean pendingDefaultExport = false;
			for (int index=start; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '{') {
					depth++;
				} else if (ch == '}') {
					depth = Math.max(0, depth-1);
				} else if (depth == 0 && isIdentifierStart(ch)) {
					int wordEnd = readIdentifierEnd(index);
					String word = code.substring(index, wordEnd);
					boolean exported = exportedContext || pendingExport;
					boolean defaultExport = pendingDefaultExport;
					if ("export".equals(word)) {
						exported = true;
						pendingExport = true;
						ExportPrefix exportPrefix = skipExportPrefix(wordEnd);
						index = exportPrefix.index;
						defaultExport = exportPrefix.defaultExport;
						pendingDefaultExport = defaultExport;
						if (index >= end)
							break;
						if (code.charAt(index) == '{' || code.charAt(index) == '*') {
							pendingExport = false;
							pendingDefaultExport = false;
							index = skipExportList(index, end)-1;
							continue;
						}
						wordEnd = readIdentifierEnd(index);
						word = code.substring(index, wordEnd);
						if ("type".equals(word)) {
							int typeExportStart = skipWhitespace(wordEnd, end);
							if (typeExportStart < end && code.charAt(typeExportStart) == '{') {
								pendingExport = false;
								pendingDefaultExport = false;
								index = skipExportList(typeExportStart, end)-1;
								continue;
							}
						}
					}
					if ("declare".equals(word) || "default".equals(word) || "abstract".equals(word) || "async".equals(word)) {
						index = wordEnd-1;
					} else if (isTypeKeyword(word)) {
						pendingExport = false;
						pendingDefaultExport = false;
						index = scanType(word, index, wordEnd, end, parent, exported, defaultExport)-1;
					} else if ("namespace".equals(word) || "module".equals(word)) {
						pendingExport = false;
						pendingDefaultExport = false;
						index = scanType("namespace", index, wordEnd, end, parent, exported, defaultExport)-1;
					} else if ("function".equals(word)) {
						pendingExport = false;
						pendingDefaultExport = false;
						index = scanFunction(index, wordEnd, end, parent, exported, defaultExport)-1;
					} else if (isVariableKeyword(word)) {
						pendingExport = false;
						pendingDefaultExport = false;
						index = scanVariables(word, wordEnd, end, parent, exported)-1;
					} else {
						int afterWord = skipWhitespace(wordEnd, end);
						if (afterWord < end && code.charAt(afterWord) == '='
								&& (afterWord + 1 >= end
										|| (code.charAt(afterWord + 1) != '=' && code.charAt(afterWord + 1) != '>'))) {
							index = scanAssignment(index, wordEnd, end, parent, exported)-1;
						} else {
							index = wordEnd-1;
						}
					}
				}
			}
		}

		private int scanType(String kind, int keywordStart, int afterKeyword, int end, @Nullable TypeScriptSymbol parent,
				boolean exported, boolean defaultExport) {
			int nameStart = skipWhitespace(afterKeyword, end);
			boolean anonymousDefault = defaultExport && "class".equals(kind)
					&& nameStart < end && !isIdentifierStart(code.charAt(nameStart));
			if (nameStart >= end || !isIdentifierStart(code.charAt(nameStart)) && !anonymousDefault)
				return afterKeyword;
			int nameEnd = anonymousDefault? afterKeyword: readIdentifierEnd(nameStart);
			TypeSymbol symbol = new TypeSymbol(parent, anonymousDefault? "default": code.substring(nameStart, nameEnd),
					kind, anonymousDefault? position(keywordStart, afterKeyword): position(nameStart, nameEnd),
					null, isLocal(parent, exported, false));
			symbols.add(symbol);
			if ("type".equals(kind)) {
				int eq = findNextTopLevel(nameEnd, end, '=', ';');
				if (eq < end && code.charAt(eq) == '=') {
					int afterEq = skipWhitespace(eq+1, end);
					if (afterEq < end && code.charAt(afterEq) == '{') {
						int objEnd = findMatching(afterEq, end, '{', '}');
						if (objEnd != -1) {
							scanMembers(afterEq+1, objEnd, symbol);
							return objEnd+1;
						}
					}
				}
				int statementEnd = findStatementEnd(nameEnd, end);
				return statementEnd < end? statementEnd+1: nameEnd;
			}
			int afterName = skipTypeParameters(skipWhitespace(nameEnd, end), end);
			int bodyStart = findNextTopLevel(afterName, end, '{', ';');
			if (bodyStart < end && code.charAt(bodyStart) == '{') {
				int bodyEnd = findMatching(bodyStart, end, '{', '}');
				if (bodyEnd != -1) {
					if ("namespace".equals(kind))
						scanDeclarations(bodyStart+1, bodyEnd, symbol, true);
					else if ("enum".equals(kind))
						scanEnumMembers(bodyStart+1, bodyEnd, symbol);
					else
						scanMembers(bodyStart+1, bodyEnd, symbol);
					return bodyEnd+1;
				}
				return end;
			}
			return nameEnd;
		}

		private int scanFunction(int keywordStart, int afterKeyword, int end, @Nullable TypeScriptSymbol parent,
				boolean exported, boolean defaultExport) {
			int nameStart = skipWhitespace(afterKeyword, end);
			boolean anonymousDefault = defaultExport && nameStart < end && code.charAt(nameStart) == '(';
			if (nameStart >= end || !isIdentifierStart(code.charAt(nameStart)) && !anonymousDefault)
				return afterKeyword;
			int nameEnd = anonymousDefault? afterKeyword: readIdentifierEnd(nameStart);
			int afterName = skipTypeParameters(skipWhitespace(anonymousDefault? nameStart: nameEnd, end), end);
			String params = "";
			int paramsEnd = -1;
			if (afterName < end && code.charAt(afterName) == '(') {
				paramsEnd = findMatching(afterName, end, '(', ')');
				if (paramsEnd != -1)
					params = source.substring(afterName, paramsEnd+1);
			}
			FunctionSymbol symbol = new FunctionSymbol(parent, anonymousDefault? "default": code.substring(nameStart, nameEnd),
					params, anonymousDefault? position(keywordStart, afterKeyword): position(nameStart, nameEnd),
					null, isLocal(parent, exported, false));
			symbols.add(symbol);
			int searchFrom = paramsEnd != -1? skipWhitespace(paramsEnd+1, end): afterName;
			int bodyStart = findNextTopLevel(searchFrom, end, '{', ';');
			if (bodyStart < end && code.charAt(bodyStart) == '{') {
				int bodyEnd = findMatching(bodyStart, end, '{', '}');
				if (bodyEnd != -1)
					return bodyEnd+1;
			}
			int statementEnd = findStatementEnd(keywordStart, end);
			return statementEnd < end? statementEnd+1: nameEnd;
		}

		private int scanVariables(String kind, int afterKeyword, int end, @Nullable TypeScriptSymbol parent, boolean exported) {
			int statementEnd = findStatementEnd(afterKeyword, end);
			int index = afterKeyword;
			while (index < statementEnd) {
				index = skipWhitespace(index, statementEnd);
				if (index < statementEnd && isIdentifierStart(code.charAt(index))) {
					int nameEnd = readIdentifierEnd(index);
					VariableSymbol variable = new VariableSymbol(parent, code.substring(index, nameEnd), kind,
							position(index, nameEnd), null, isLocal(parent, exported, false));
					symbols.add(variable);
					scanObjectInitializer(nameEnd, statementEnd, variable);
					index = nameEnd;
				} else if (index < statementEnd && (code.charAt(index) == '{' || code.charAt(index) == '[')) {
					int patternEnd = findMatching(index, statementEnd, code.charAt(index), code.charAt(index)=='{'? '}': ']');
					if (patternEnd != -1) {
						scanBindingPattern(index+1, patternEnd, kind, parent, exported);
						index = patternEnd+1;
					}
				}
				index = skipVariableInitializer(index, statementEnd);
				if (index < statementEnd && code.charAt(index) == ',')
					index++;
			}
			return statementEnd+1;
		}

		private void scanBindingPattern(int start, int end, String kind, @Nullable TypeScriptSymbol parent, boolean exported) {
			int depth = 0;
			for (int index=start; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '{' || ch == '[' || ch == '(') {
					depth++;
				} else if (ch == '}' || ch == ']' || ch == ')') {
					depth--;
				} else if (depth == 0 && isIdentifierStart(ch)) {
					int nameEnd = readIdentifierEnd(index);
					String name = code.substring(index, nameEnd);
					int next = skipWhitespace(nameEnd, end);
					if (next < end && code.charAt(next) == ':') {
						int valueStart = skipWhitespace(next+1, end);
						if (valueStart < end && isIdentifierStart(code.charAt(valueStart))) {
							int valueEnd = readIdentifierEnd(valueStart);
							symbols.add(new VariableSymbol(parent, code.substring(valueStart, valueEnd), kind,
									position(valueStart, valueEnd), null, isLocal(parent, exported, false)));
							index = valueEnd-1;
						} else if (valueStart < end && (code.charAt(valueStart) == '{' || code.charAt(valueStart) == '[')) {
							int valueEnd = findMatching(valueStart, end, code.charAt(valueStart),
									code.charAt(valueStart)=='{'? '}': ']');
							if (valueEnd != -1) {
								scanBindingPattern(valueStart+1, valueEnd, kind, parent, exported);
								index = valueEnd;
							}
						}
				} else if (!"_".equals(name)) {
					symbols.add(new VariableSymbol(parent, name, kind, position(index, nameEnd), null,
							isLocal(parent, exported, false)));
					index = nameEnd-1;
				}
			}
		}
	}

		private int scanAssignment(int nameStart, int nameEnd, int end, @Nullable TypeScriptSymbol parent, boolean exported) {
			int eq = skipWhitespace(nameEnd, end);
			if (eq >= end || code.charAt(eq) != '=')
				return nameEnd;
			AssignmentTarget target = parseAssignmentTarget(nameStart, nameEnd);
			int valueStart = skipWhitespace(eq+1, end);
			if (target != null)
				addAssignedSymbol(target, valueStart, end, parent, exported);
			return skipAssignedValue(valueStart, end);
		}

		private int skipAssignedValue(int valueStart, int end) {
			if (valueStart < end && code.charAt(valueStart) == '{') {
				int objEnd = findMatching(valueStart, end, '{', '}');
				if (objEnd != -1)
					return objEnd+1;
			}
			int statementEnd = findStatementEnd(valueStart, end);
			return statementEnd < end? statementEnd+1: end;
		}

		private void addAssignedSymbol(AssignmentTarget target, int valueStart, int end,
				@Nullable TypeScriptSymbol parent, boolean exported) {
			IdentifierSegment segment = target.getLastSegment();
			TypeScriptSymbol targetParent = getOrCreateAssignmentParent(target, parent, exported);
			String params = functionValueParams(valueStart, end);
			if (params != null) {
				symbols.add(new FunctionSymbol(targetParent, segment.name, params, position(segment.start, segment.end),
						null, target.isSimple() && isLocal(parent, exported, false)));
				return;
			}
			VariableSymbol variable = new VariableSymbol(targetParent, segment.name, target.isSimple()? "variable": "property",
					position(segment.start, segment.end), null, target.isSimple() && isLocal(parent, exported, false));
			symbols.add(variable);
			if (valueStart < end && code.charAt(valueStart) == '{') {
				int objEnd = findMatching(valueStart, end, '{', '}');
				if (objEnd != -1)
					scanObjectMembers(valueStart+1, objEnd, variable);
			}
		}

		private TypeScriptSymbol getOrCreateAssignmentParent(AssignmentTarget target,
				@Nullable TypeScriptSymbol parent, boolean exported) {
			TypeScriptSymbol currentParent = parent;
			for (int i=0; i<target.segments.size()-1; i++) {
				IdentifierSegment segment = target.segments.get(i);
				TypeScriptSymbol symbol = findSymbol(currentParent, segment.name);
				if (symbol == null) {
					symbol = new VariableSymbol(currentParent, segment.name, currentParent == parent? "variable": "property",
							position(segment.start, segment.end), null,
							currentParent == parent && isLocal(parent, exported, false));
					symbols.add(symbol);
				}
				currentParent = symbol;
			}
			return currentParent;
		}

		private @Nullable AssignmentTarget parseAssignmentTarget(int nameStart, int nameEnd) {
			List<IdentifierSegment> segments = new ArrayList<>();
			while (true) {
				segments.add(0, new IdentifierSegment(code.substring(nameStart, nameEnd), nameStart, nameEnd));
				int index = nameStart-1;
				while (index >= 0 && Character.isWhitespace(code.charAt(index)))
					index--;
				if (index < 0 || code.charAt(index) != '.')
					break;
				index--;
				while (index >= 0 && Character.isWhitespace(code.charAt(index)))
					index--;
				if (index >= 0 && code.charAt(index) == '?')
					index--;
				while (index >= 0 && Character.isWhitespace(code.charAt(index)))
					index--;
				if (index < 0 || !isIdentifierPart(code.charAt(index)))
					return null;
				nameEnd = index+1;
				while (index >= 0 && isIdentifierPart(code.charAt(index)))
					index--;
				nameStart = index+1;
			}
			String rootName = segments.get(0).name;
			if ("this".equals(rootName) || "super".equals(rootName))
				return null;
			return new AssignmentTarget(segments);
		}

		private @Nullable TypeScriptSymbol findSymbol(@Nullable TypeScriptSymbol parent, String name) {
			TypeScriptSymbol found = null;
			for (TypeScriptSymbol symbol: symbols) {
				if (name.equals(symbol.getName()) && parent == symbol.getParent())
					found = symbol;
			}
			return found;
		}

		private static class AssignmentTarget {

			private final List<IdentifierSegment> segments;

			private AssignmentTarget(List<IdentifierSegment> segments) {
				this.segments = segments;
			}

			private IdentifierSegment getLastSegment() {
				return segments.get(segments.size()-1);
			}

			private boolean isSimple() {
				return segments.size() == 1;
			}

		}

		private static class IdentifierSegment {

			private final String name;

			private final int start;

			private final int end;

			private IdentifierSegment(String name, int start, int end) {
				this.name = name;
				this.start = start;
				this.end = end;
			}

		}

		private void scanObjectInitializer(int nameEnd, int end, TypeScriptSymbol parent) {
			int afterName = skipWhitespace(nameEnd, end);
			if (afterName < end && code.charAt(afterName) == '=') {
				int valueStart = skipWhitespace(afterName+1, end);
				if (valueStart < end && code.charAt(valueStart) == '{') {
					int objEnd = findMatching(valueStart, end, '{', '}');
					if (objEnd != -1)
						scanObjectMembers(valueStart+1, objEnd, parent);
				}
			}
		}

		private void scanObjectMembers(int start, int end, TypeScriptSymbol parent) {
			int index = start;
			while (index < end) {
				index = skipWhitespace(index, end);
				if (index >= end)
					break;
				char ch = code.charAt(index);
				if (ch == ',' || ch == ';') {
					index++;
					continue;
				}
				if (ch == '}')
					break;
				if (ch == '@') {
					index = skipLine(index, end);
					continue;
				}
				if (ch == '.' && index+2 < end && code.charAt(index+1) == '.' && code.charAt(index+2) == '.') {
					index = advanceObjectMember(index, skipObjectValue(index, end), end);
					continue;
				}
				int keyStart = index;
				int nameEnd;
				String name;
				char srcCh = source.charAt(index);
				if (srcCh == '"' || srcCh == '\'') {
					nameEnd = keyStart+1;
					while (nameEnd < end && source.charAt(nameEnd) != srcCh)
						nameEnd++;
					name = source.substring(keyStart+1, nameEnd);
					nameEnd++;
				} else if (ch == '[') {
					int close = findMatching(keyStart, end, '[', ']');
					if (close != -1) {
						int inner = skipWhitespace(keyStart+1, close);
						if (inner < close && isIdentifierStart(code.charAt(inner)))
							name = code.substring(inner, readIdentifierEnd(inner));
						else
							name = null;
						nameEnd = close+1;
					} else {
						break;
					}
				} else if (isIdentifierStart(ch)) {
					nameEnd = readIdentifierEnd(keyStart);
					name = code.substring(keyStart, nameEnd);
				} else {
					index = advanceObjectMember(index, skipObjectValue(index, end), end);
					continue;
				}
				int afterKey = skipWhitespace(nameEnd, end);
				if (name != null && ("get".equals(name) || "set".equals(name) || "async".equals(name))
						&& afterKey < end && isIdentifierStart(code.charAt(afterKey))) {
					keyStart = afterKey;
					nameEnd = readIdentifierEnd(afterKey);
					name = code.substring(afterKey, nameEnd);
					afterKey = skipWhitespace(nameEnd, end);
				}
				index = advanceObjectMember(index, scanObjectMemberValue(keyStart, nameEnd, name, afterKey, end, parent), end);
			}
		}

		private int advanceObjectMember(int current, int next, int end) {
			return next > current? next: end;
		}

		private int scanObjectMemberValue(int keyStart, int nameEnd, String name, int afterKey, int end,
				TypeScriptSymbol parent) {
			if (name == null || name.isEmpty())
				return skipObjectValue(afterKey, end);
			if (afterKey < end && code.charAt(afterKey) == '(') {
				int paramsEnd = findMatching(afterKey, end, '(', ')');
				String params = paramsEnd != -1? source.substring(afterKey, paramsEnd+1): "()";
				symbols.add(new FunctionSymbol(parent, name, params, position(keyStart, nameEnd), null, false));
				if (paramsEnd != -1) {
					int bodyStart = findNextTopLevel(paramsEnd+1, end, '{', ',');
					if (bodyStart < end && code.charAt(bodyStart) == '{') {
						int bodyEnd = findMatching(bodyStart, end, '{', '}');
						if (bodyEnd != -1)
							return skipObjectValue(bodyEnd+1, end);
					}
				}
				return skipObjectValue(afterKey, end);
			}
			if (afterKey < end && code.charAt(afterKey) == ':') {
				int valueStart = skipWhitespace(afterKey+1, end);
				if (valueStart < end && code.charAt(valueStart) == '{') {
					int objEnd = findMatching(valueStart, end, '{', '}');
					if (objEnd != -1) {
						VariableSymbol variable = new VariableSymbol(parent, name, "property",
								position(keyStart, nameEnd), null, false);
						symbols.add(variable);
						scanObjectMembers(valueStart+1, objEnd, variable);
						return objEnd+1;
					}
				}
				String params = functionValueParams(valueStart, end);
				if (params != null) {
					symbols.add(new FunctionSymbol(parent, name, params, position(keyStart, nameEnd), null, false));
					return skipObjectValue(valueStart, end);
				}
				symbols.add(new VariableSymbol(parent, name, "property", position(keyStart, nameEnd), null, false));
				return skipObjectValue(afterKey, end);
			}
			if (!isControlWord(name))
				symbols.add(new VariableSymbol(parent, name, "property", position(keyStart, nameEnd), null, false));
			return nameEnd;
		}

		private int skipObjectValue(int start, int end) {
			int depth = 0;
			for (int index=start; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '(' || ch == '[' || ch == '{')
					depth++;
				else if (ch == ')' || ch == ']' || ch == '}') {
					if (depth == 0)
						return index;
					depth--;
				} else if (depth == 0 && (ch == ',' || ch == ';'))
					return index;
			}
			return end;
		}

		private String functionValueParams(int start, int end) {
			int index = start;
			if (startsWithWord(index, "async"))
				index = skipWhitespace(readIdentifierEnd(index), end);
			if (startsWithWord(index, "function")) {
				int afterFn = readIdentifierEnd(index);
				int nameOrParams = skipWhitespace(afterFn, end);
				if (nameOrParams < end && isIdentifierStart(code.charAt(nameOrParams)))
					nameOrParams = skipWhitespace(readIdentifierEnd(nameOrParams), end);
				if (nameOrParams < end && code.charAt(nameOrParams) == '(') {
					int paramsEnd = findMatching(nameOrParams, end, '(', ')');
					if (paramsEnd != -1)
						return source.substring(nameOrParams, paramsEnd+1);
				}
				return "()";
			}
			if (index < end && code.charAt(index) == '(') {
				int paramsEnd = findMatching(index, end, '(', ')');
				if (paramsEnd != -1) {
					int arrow = skipWhitespace(paramsEnd+1, end);
					if (arrow+1 < end && code.charAt(arrow) == '=' && code.charAt(arrow+1) == '>')
						return source.substring(index, paramsEnd+1);
				}
				return null;
			}
			if (index < end && isIdentifierStart(code.charAt(index))) {
				int nameEnd = readIdentifierEnd(index);
				int arrow = skipWhitespace(nameEnd, end);
				if (arrow+1 < end && code.charAt(arrow) == '=' && code.charAt(arrow+1) == '>')
					return "(" + source.substring(index, nameEnd) + ")";
			}
			return null;
		}

		private void scanMembers(int start, int end, TypeScriptSymbol parent) {
			int depth = 0;
			for (int index=start; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '{') {
					depth++;
				} else if (ch == '}') {
					depth--;
				} else if (depth == 0 && ch == '@') {
					index = skipLine(index, end);
				} else if (depth == 0 && isIdentifierStart(ch)) {
					index = skipMemberModifiers(index, end);
					if (index >= end || !isIdentifierStart(code.charAt(index)))
						continue;
					int nameEnd = readIdentifierEnd(index);
					String name = code.substring(index, nameEnd);
					int afterName = skipWhitespace(nameEnd, end);
					boolean accessor = false;
					if ("get".equals(name) || "set".equals(name) || "async".equals(name)) {
						int realNameStart = afterName;
						if (realNameStart < end && isIdentifierStart(code.charAt(realNameStart))) {
							accessor = "get".equals(name) || "set".equals(name);
							nameEnd = readIdentifierEnd(realNameStart);
							name = code.substring(realNameStart, nameEnd);
							index = realNameStart;
							afterName = skipWhitespace(nameEnd, end);
						}
					}
					boolean local = hasPrivateModifier(index, start);
					if (afterName < end && code.charAt(afterName) == '(') {
						int paramsEnd = findMatching(afterName, end, '(', ')');
						String params = paramsEnd!=-1? source.substring(afterName, paramsEnd+1): "";
						symbols.add(new FunctionSymbol(parent, name, params, position(index, nameEnd), null, local));
						index = skipMemberTail(afterName, end)-1;
					} else if (accessor) {
						symbols.add(new FunctionSymbol(parent, name, "()", position(index, nameEnd), null, local));
						index = skipMemberTail(nameEnd, end)-1;
					} else if (!isControlWord(name)) {
						symbols.add(new VariableSymbol(parent, name, "property", position(index, nameEnd), null, local));
						index = skipMemberTail(nameEnd, end)-1;
					}
				}
			}
		}

		private void scanEnumMembers(int start, int end, TypeScriptSymbol parent) {
			int depth = 0;
			for (int index=start; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '(' || ch == '[' || ch == '{') {
					depth++;
				} else if (ch == ')' || ch == ']' || ch == '}') {
					depth--;
				} else if (depth == 0 && isIdentifierStart(ch)) {
					int nameEnd = readIdentifierEnd(index);
					symbols.add(new VariableSymbol(parent, code.substring(index, nameEnd), "member",
							position(index, nameEnd), null, false));
					index = skipVariableInitializer(nameEnd, end);
				}
			}
		}

		private boolean isLocal(@Nullable TypeScriptSymbol parent, boolean exported, boolean privateMember) {
			return privateMember || parent == null && !exported;
		}

		private boolean isTypeKeyword(String word) {
			return "class".equals(word) || "interface".equals(word) || "enum".equals(word) || "type".equals(word);
		}

		private boolean isVariableKeyword(String word) {
			return "const".equals(word) || "let".equals(word) || "var".equals(word);
		}

		private boolean isControlWord(String word) {
			return "if".equals(word) || "for".equals(word) || "while".equals(word) || "switch".equals(word)
					|| "return".equals(word);
		}

		private boolean isMemberAccessTarget(int nameStart) {
			int index = nameStart-1;
			while (index >= 0 && Character.isWhitespace(code.charAt(index)))
				index--;
			if (index >= 0 && code.charAt(index) == '.')
				return true;
			if (index >= 0 && code.charAt(index) == '?') {
				index--;
				while (index >= 0 && Character.isWhitespace(code.charAt(index)))
					index--;
				return index >= 0 && code.charAt(index) == '.';
			}
			return false;
		}

		private ExportPrefix skipExportPrefix(int index) {
			boolean defaultExport = false;
			while (true) {
				index = skipWhitespace(index, code.length());
				if (startsWithWord(index, "default")) {
					defaultExport = true;
					index = readIdentifierEnd(index);
				} else if (startsWithWord(index, "declare") || startsWithWord(index, "abstract")) {
					index = readIdentifierEnd(index);
				} else {
					return new ExportPrefix(index, defaultExport);
				}
			}
		}

		private static class ExportPrefix {

			private final int index;

			private final boolean defaultExport;

			private ExportPrefix(int index, boolean defaultExport) {
				this.index = index;
				this.defaultExport = defaultExport;
			}

		}

		private int skipExportList(int index, int end) {
			if (code.charAt(index) == '*')
				return findStatementEnd(index, end)+1;
			int close = findMatching(index, end, '{', '}');
			return close != -1? close+1: end;
		}

		private int skipMemberModifiers(int index, int end) {
			while (true) {
				index = skipWhitespace(index, end);
				if (startsWithWord(index, "public") || startsWithWord(index, "private")
						|| startsWithWord(index, "protected") || startsWithWord(index, "static")
						|| startsWithWord(index, "readonly") || startsWithWord(index, "abstract")
						|| startsWithWord(index, "override") || startsWithWord(index, "declare")) {
					index = readIdentifierEnd(index);
				} else {
					return index;
				}
			}
		}

		private boolean hasPrivateModifier(int nameStart, int memberStart) {
			String prefix = code.substring(Math.max(memberStart, nameStart-80), nameStart);
			return prefix.matches("(?s).*\\b(private|protected)\\s+$") || source.charAt(nameStart) == '#';
		}

		private int findStatementEnd(int index, int end) {
			int depth = 0;
			for (; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '(' || ch == '[' || ch == '{')
					depth++;
				else if (ch == ')' || ch == ']' || ch == '}')
					depth--;
				else if (depth == 0 && ch == ';')
					return index;
				else if (depth == 0 && ch == '\n' && startsDeclarationAfter(index+1, end))
					return index;
			}
			return end;
		}

		private int skipVariableInitializer(int index, int end) {
			int depth = 0;
			for (; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '(' || ch == '[' || ch == '{')
					depth++;
				else if (ch == ')' || ch == ']' || ch == '}')
					depth--;
				else if (depth == 0 && (ch == ',' || ch == ';'
						|| ch == '\n' && startsDeclarationAfter(index+1, end)))
					return index;
			}
			return index;
		}

		private boolean startsDeclarationAfter(int index, int end) {
			index = skipWhitespace(index, end);
			if (index >= end)
				return false;
			if (startsWithWord(index, "export")) {
				ExportPrefix exportPrefix = skipExportPrefix(readIdentifierEnd(index));
				index = exportPrefix.index;
				if (index >= end)
					return false;
			}
			if (isIdentifierStart(code.charAt(index))) {
				int nameEnd = readIdentifierEnd(index);
				int afterName = skipWhitespace(nameEnd, end);
				if (afterName < end && code.charAt(afterName) == '='
						&& (afterName + 1 >= end
								|| (code.charAt(afterName + 1) != '=' && code.charAt(afterName + 1) != '>'))
						&& !isMemberAccessTarget(index))
					return true;
			}
			if (startsWithWord(index, "async")) {
				int afterAsync = skipWhitespace(readIdentifierEnd(index), end);
				if (startsWithWord(afterAsync, "function"))
					return true;
			}
			return startsWithWord(index, "const") || startsWithWord(index, "let") || startsWithWord(index, "var")
					|| startsWithWord(index, "function") || startsWithWord(index, "class")
					|| startsWithWord(index, "interface") || startsWithWord(index, "type")
					|| startsWithWord(index, "enum") || startsWithWord(index, "namespace")
					|| startsWithWord(index, "module");
		}

		private int skipMemberTail(int index, int end) {
			int depth = 0;
			boolean afterColon = false;
			for (; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '(' || ch == '[' || ch == '{')
					depth++;
				else if (ch == ')' || ch == ']' || ch == '}') {
					if (depth == 0 && ch == '}')
						return index;
					depth--;
					if (depth == 0 && ch == '}')
						return index+1;
				} else if (depth == 0 && ch == ':')
					afterColon = true;
				else if (depth == 0 && ch == '=')
					afterColon = false;
				else if (depth == 0 && (ch == ';' || ch == ','))
					return index+1;
				else if (depth == 0 && ch == '\n' && afterColon)
					return index+1;
			}
			return index;
		}

		private int findNextTopLevel(int index, int end, char first, char second) {
			int depth = 0;
			for (; index<end; index++) {
				char ch = code.charAt(index);
				if (ch == '(' || ch == '[' || ch == '<')
					depth++;
				else if (ch == ')' || ch == ']' || ch == '>')
					depth--;
				else if (depth == 0 && (ch == first || ch == second))
					return index;
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

		private int skipTypeParameters(int index, int end) {
			if (index >= end || code.charAt(index) != '<')
				return index;
			int close = findMatching(index, end, '<', '>');
			return close != -1? close+1: index;
		}

		private int skipWhitespace(int index, int end) {
			while (index < end && Character.isWhitespace(code.charAt(index)))
				index++;
			return index;
		}

		private int skipLine(int index, int end) {
			while (index < end && code.charAt(index) != '\n')
				index++;
			return index;
		}

		private boolean startsWithWord(int index, String word) {
			return index + word.length() <= code.length()
					&& code.startsWith(word, index)
					&& (index == 0 || !isIdentifierPart(code.charAt(index-1)))
					&& (index + word.length() == code.length() || !isIdentifierPart(code.charAt(index + word.length())));
		}

		private int readIdentifierEnd(int index) {
			index++;
			while (index < code.length() && isIdentifierPart(code.charAt(index)))
				index++;
			return index;
		}

		private boolean isIdentifierStart(char ch) {
			return Character.isLetter(ch) || ch == '_' || ch == '$' || ch == '#';
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
				if (ch == '/' && index+1 < builder.length() && builder.charAt(index+1) == '/') {
					builder.setCharAt(index++, ' ');
					builder.setCharAt(index, ' ');
					while (index+1 < builder.length() && builder.charAt(index+1) != '\n')
						builder.setCharAt(++index, ' ');
				} else if (ch == '/' && index+1 < builder.length() && builder.charAt(index+1) == '*') {
					builder.setCharAt(index++, ' ');
					builder.setCharAt(index, ' ');
					while (index+1 < builder.length()) {
						char current = builder.charAt(++index);
						if (current == '\n')
							continue;
						builder.setCharAt(index, ' ');
						if (current == '*' && index+1 < builder.length() && builder.charAt(index+1) == '/') {
							builder.setCharAt(++index, ' ');
							break;
						}
					}
				} else if (ch == '/' && isRegexLiteralStart(builder, index)) {
					builder.setCharAt(index, ' ');
					boolean inCharacterClass = false;
					while (index+1 < builder.length()) {
						char current = builder.charAt(++index);
						if (current == '\n')
							break;
						builder.setCharAt(index, ' ');
						if (current == '\\' && index+1 < builder.length()) {
							if (builder.charAt(index+1) != '\n')
								builder.setCharAt(index+1, ' ');
							index++;
						} else if (current == '[') {
							inCharacterClass = true;
						} else if (current == ']') {
							inCharacterClass = false;
						} else if (current == '/' && !inCharacterClass) {
							while (index+1 < builder.length()
									&& Character.isLetter(builder.charAt(index+1)))
								builder.setCharAt(++index, ' ');
							break;
						}
					}
				} else if (ch == '\'' || ch == '"' || ch == '`') {
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

		private static boolean isRegexLiteralStart(StringBuilder builder, int index) {
			index--;
			while (index >= 0 && Character.isWhitespace(builder.charAt(index)))
				index--;
			if (index < 0)
				return true;
			char previous = builder.charAt(index);
			return "([{=,:;!&|?+-*%^~<>".indexOf(previous) != -1;
		}

	}

}
