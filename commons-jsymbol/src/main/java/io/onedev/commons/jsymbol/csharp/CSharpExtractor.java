package io.onedev.commons.jsymbol.csharp;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import com.google.common.base.Joiner;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.csharp.symbols.CSharpSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.FieldSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.NamespaceSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.TypeSymbol.Kind;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.PlanarRange;

public class CSharpExtractor extends AbstractSymbolExtractor<CSharpSymbol> {

	private static final Set<String> MODIFIERS = new HashSet<>();

	static {
		for (CSharpSymbol.Modifier modifier: CSharpSymbol.Modifier.values())
			MODIFIERS.add(modifier.name().toLowerCase());
	}

	@Override
	public List<CSharpSymbol> extract(String fileName, String fileContent) {
		List<CSharpSymbol> symbols = new ArrayList<>();
		Parser parser = new Parser(preprocess(fileContent), symbols);
		parser.parseCompilationUnit(0, parser.tokens.size(), null);
		return symbols;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "cs");
	}

	@Override
	public int getVersion() {
		return 4;
	}

	private String preprocess(String source) {
		String[] lines = source.split("\n", -1);
		StringBuilder builder = new StringBuilder(source.length());
		Set<String> defines = new HashSet<>();
		List<PreprocessFrame> frames = new ArrayList<>();
		boolean active = true;
		for (String line: lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith("#")) {
				String directive = trimmed.substring(1).trim();
				if (directive.startsWith("define ")) {
					if (active)
						defines.add(directive.substring("define ".length()).trim());
				} else if (directive.startsWith("undef ")) {
					if (active)
						defines.remove(directive.substring("undef ".length()).trim());
				} else if (directive.startsWith("if ")) {
					boolean parentActive = active;
					boolean condition = parentActive && new Expression(directive.substring(3), defines).parse();
					frames.add(new PreprocessFrame(parentActive, condition, condition));
					active = condition;
				} else if (directive.startsWith("elif ")) {
					if (!frames.isEmpty()) {
						PreprocessFrame frame = frames.get(frames.size()-1);
						boolean condition = frame.parentActive && !frame.branchMatched
								&& new Expression(directive.substring(5), defines).parse();
						frame.branchMatched |= condition;
						frame.currentActive = condition;
						active = condition;
					}
				} else if (directive.startsWith("else")) {
					if (!frames.isEmpty()) {
						PreprocessFrame frame = frames.get(frames.size()-1);
						boolean condition = frame.parentActive && !frame.branchMatched;
						frame.branchMatched = true;
						frame.currentActive = condition;
						active = condition;
					}
				} else if (directive.startsWith("endif")) {
					if (!frames.isEmpty()) {
						frames.remove(frames.size()-1);
						active = frames.isEmpty() || frames.get(frames.size()-1).currentActive;
					}
				}
				appendSpaces(builder, line.length());
			} else if (active) {
				builder.append(line);
			} else {
				appendSpaces(builder, line.length());
			}
			builder.append('\n');
		}
		return builder.toString();
	}

	private void appendSpaces(StringBuilder builder, int count) {
		for (int i=0; i<count; i++)
			builder.append(' ');
	}

	private static class PreprocessFrame {

		private final boolean parentActive;

		private boolean currentActive;

		private boolean branchMatched;

		private PreprocessFrame(boolean parentActive, boolean currentActive, boolean branchMatched) {
			this.parentActive = parentActive;
			this.currentActive = currentActive;
			this.branchMatched = branchMatched;
		}

	}

	private static class Expression {

		private final String expression;

		private final Set<String> defines;

		private int pos;

		private Expression(String expression, Set<String> defines) {
			this.expression = expression;
			this.defines = defines;
		}

		private boolean parse() {
			return parseOr();
		}

		private boolean parseOr() {
			boolean value = parseAnd();
			while (true) {
				skipSpaces();
				if (match("||"))
					value |= parseAnd();
				else
					return value;
			}
		}

		private boolean parseAnd() {
			boolean value = parseUnary();
			while (true) {
				skipSpaces();
				if (match("&&"))
					value &= parseUnary();
				else
					return value;
			}
		}

		private boolean parseUnary() {
			skipSpaces();
			if (match("!"))
				return !parseUnary();
			else if (match("(")) {
				boolean value = parseOr();
				match(")");
				return value;
			} else {
				String identifier = readIdentifier();
				if ("true".equals(identifier))
					return true;
				else if ("false".equals(identifier))
					return false;
				else
					return defines.contains(identifier);
			}
		}

		private boolean match(String value) {
			skipSpaces();
			if (expression.startsWith(value, pos)) {
				pos += value.length();
				return true;
			} else {
				return false;
			}
		}

		private String readIdentifier() {
			skipSpaces();
			int start = pos;
			while (pos < expression.length()
					&& (Character.isLetterOrDigit(expression.charAt(pos)) || expression.charAt(pos) == '_'))
				pos++;
			return expression.substring(start, pos);
		}

		private void skipSpaces() {
			while (pos < expression.length() && Character.isWhitespace(expression.charAt(pos)))
				pos++;
		}

	}

	private static class Parser {

		private final List<Token> tokens;

		private final List<CSharpSymbol> symbols;

		private Parser(String source, List<CSharpSymbol> symbols) {
			this.tokens = tokenize(source);
			this.symbols = symbols;
		}

		private void parseCompilationUnit(int start, int end, @Nullable CSharpSymbol parentSymbol) {
			int i = start;
			while (i < end) {
				int declaration = skipAttributesAndModifiers(i, end);
				if (is(declaration, "namespace")) {
					i = parseNamespace(declaration, end, parentSymbol);
				} else {
					int next = parseTypeDeclaration(i, end, parentSymbol);
					i = next > i? next: i+1;
				}
			}
		}

		private int parseNamespace(int index, int end, @Nullable CSharpSymbol parentSymbol) {
			int nameStart = index + 1;
			int nameEnd = nameStart;
			while (nameEnd < end && !is(nameEnd, "{") && !is(nameEnd, ";"))
				nameEnd++;
			CSharpSymbol namespaceParent = extractNamespaceNames(nameStart, nameEnd, range(index, nameEnd), parentSymbol);
			if (nameEnd < end && is(nameEnd, "{")) {
				int close = matching(nameEnd, end);
				parseCompilationUnit(nameEnd+1, close, namespaceParent);
				return close + 1;
			} else {
				parseCompilationUnit(nameEnd+1, end, namespaceParent);
				return end;
			}
		}

		private CSharpSymbol extractNamespaceNames(int start, int end, PlanarRange scope,
				@Nullable CSharpSymbol parentSymbol) {
			for (int i=start; i<end; i++) {
				if (tokens.get(i).isIdentifier()) {
					String namespaceName = text(i);
					NamespaceSymbol namespaceSymbol = null;
					for (CSharpSymbol symbol: symbols) {
						if (symbol instanceof NamespaceSymbol
								&& symbol.getParent() == parentSymbol
								&& symbol.getName().equals(namespaceName)) {
							namespaceSymbol = (NamespaceSymbol) symbol;
							break;
						}
					}
					if (namespaceSymbol == null) {
						namespaceSymbol = new NamespaceSymbol(parentSymbol, namespaceName, range(i, i+1), scope);
						symbols.add(namespaceSymbol);
					}
					parentSymbol = namespaceSymbol;
				}
			}
			return parentSymbol;
		}

		private int parseTypeDeclaration(int index, int end, @Nullable CSharpSymbol parentSymbol) {
			int originalIndex = index;
			int declarationStart = index;
			EnumSet<CSharpSymbol.Modifier> modifiers = getModifiers(index, end);
			index = skipAttributesAndModifiers(index, end);
			if (index >= end)
				return end;

			if (is(index, "class"))
				return parseClassOrStruct(index, end, declarationStart, parentSymbol, modifiers, Kind.CLASS);
			else if (is(index, "struct"))
				return parseClassOrStruct(index, end, declarationStart, parentSymbol, modifiers, Kind.STRUCT);
			else if (is(index, "interface"))
				return parseInterface(index, end, declarationStart, parentSymbol, modifiers);
			else if (is(index, "enum"))
				return parseEnum(index, end, declarationStart, parentSymbol, modifiers);
			else if (is(index, "delegate"))
				return parseDelegate(index, end, declarationStart, parentSymbol, modifiers);
			else
				return originalIndex;
		}

		private int parseClassOrStruct(int keyword, int end, int declarationStart, @Nullable CSharpSymbol parentSymbol,
				EnumSet<CSharpSymbol.Modifier> modifiers, Kind kind) {
			int name = nextIdentifier(keyword+1, end);
			if (name == -1)
				return keyword + 1;
			int body = findTopLevel(keyword, end, "{");
			if (body == -1)
				return name + 1;
			int close = matching(body, end);
			TypeSymbol symbol = new TypeSymbol(parentSymbol, text(name), range(name, name+1),
					range(declarationStart, close+1), kind, typeParameters(name+1, body), modifiers);
			parseMembers(body+1, close, symbol, text(name), kind == Kind.INTERFACE);
			symbols.add(symbol);
			return close + 1;
		}

		private int parseInterface(int keyword, int end, int declarationStart, @Nullable CSharpSymbol parentSymbol,
				EnumSet<CSharpSymbol.Modifier> modifiers) {
			int name = nextIdentifier(keyword+1, end);
			if (name == -1)
				return keyword + 1;
			int body = findTopLevel(keyword, end, "{");
			if (body == -1)
				return name + 1;
			int close = matching(body, end);
			TypeSymbol symbol = new TypeSymbol(parentSymbol, text(name), range(name, name+1),
					range(declarationStart, close+1), Kind.INTERFACE, typeParameters(name+1, body), modifiers);
			symbols.add(symbol);
			parseMembers(body+1, close, symbol, text(name), true);
			return close + 1;
		}

		private int parseEnum(int keyword, int end, int declarationStart, @Nullable CSharpSymbol parentSymbol,
				EnumSet<CSharpSymbol.Modifier> modifiers) {
			int name = nextIdentifier(keyword+1, end);
			if (name == -1)
				return keyword + 1;
			int body = findTopLevel(keyword, end, "{");
			if (body == -1)
				return name + 1;
			int close = matching(body, end);
			TypeSymbol symbol = new TypeSymbol(parentSymbol, text(name), range(name, name+1),
					range(declarationStart, close+1), Kind.ENUM, null, modifiers);
			for (int i=body+1; i<close; i++) {
				if (is(i, "[")) {
					i = matching(i, close);
				} else if (tokens.get(i).isIdentifier()) {
					symbols.add(new FieldSymbol(symbol, FieldSymbol.Kind.ENUM_ITEM, text(i), range(i, i+1),
							range(i, i+1), null, null, EnumSet.noneOf(CSharpSymbol.Modifier.class)));
					i = skipUntilTopLevel(i+1, close, ",", "}");
				}
			}
			symbols.add(symbol);
			return close + 1;
		}

		private int parseDelegate(int keyword, int end, int declarationStart, @Nullable CSharpSymbol parentSymbol,
				EnumSet<CSharpSymbol.Modifier> modifiers) {
			int open = findTopLevel(keyword, end, "(");
			if (open == -1)
				return keyword + 1;
			int name = methodNameBeforeOpen(open, keyword);
			if (name == -1)
				return open + 1;
			int close = matching(open, end);
			int semi = skipUntilTopLevel(close+1, end, ";", "}");
			String returnType = join(keyword+1, name);
			returnType = normalizeReturnType(returnType);
			MethodSymbol symbol = new MethodSymbol(parentSymbol, MethodSymbol.Kind.DELEGATE, text(name),
					range(name, name+1), range(declarationStart, semi+1), typeParameters(name+1, open),
					returnType, methodParams(open+1, close), null, modifiers);
			symbols.add(symbol);
			return semi + 1;
		}

		private void parseMembers(int start, int end, CSharpSymbol parentSymbol, String typeName, boolean inInterface) {
			int i = start;
			while (i < end) {
				int declaration = skipAttributesAndModifiers(i, end);
				if (declaration >= end)
					break;
				int nextType = parseTypeDeclaration(i, end, parentSymbol);
				if (nextType > i) {
					i = nextType;
					continue;
				}
				int memberEnd = findMemberEnd(i, end);
				parseMember(i, memberEnd, parentSymbol, typeName, inInterface);
				i = Math.max(memberEnd + 1, i + 1);
			}
		}

		private void parseMember(int start, int end, CSharpSymbol parentSymbol, String typeName, boolean inInterface) {
			EnumSet<CSharpSymbol.Modifier> modifiers = inInterface?
					EnumSet.noneOf(CSharpSymbol.Modifier.class): getModifiers(start, end);
			int index = skipAttributesAndModifiers(start, end);
			if (index >= end)
				return;

			if (is(index, "~")) {
				int name = nextIdentifier(index+1, end);
				if (name != -1)
					symbols.add(new MethodSymbol(parentSymbol, MethodSymbol.Kind.NORMAL_METHOD, "~" + typeName,
							range(name, name+1), range(index, end), null, null, null, null, modifiers));
			} else if (is(index, "delegate")) {
				parseDelegate(index, end, start, parentSymbol, modifiers);
			} else if (is(index, "event")) {
				parseEvent(index, end, parentSymbol, modifiers);
			} else if (is(index, "fixed")) {
				parseFixedBuffer(index, end, parentSymbol, modifiers);
			} else if (is(index, "implicit") || is(index, "explicit")) {
				parseConversionOperator(index, end, parentSymbol, modifiers);
			} else {
				int operator = findTopLevel(index, end, "operator");
				if (operator != -1)
					parseOperator(index, end, operator, parentSymbol, modifiers);
				else
					parseOrdinaryMember(index, end, parentSymbol, typeName, modifiers);
			}
		}

		private void parseEvent(int index, int end, CSharpSymbol parentSymbol, EnumSet<CSharpSymbol.Modifier> modifiers) {
			int name = previousIdentifier(skipUntilTopLevel(index+1, end, ";", "{"), index);
			if (name == -1)
				return;
			String type = join(index+1, name) + "(...)";
			if (findTopLevel(index, end, "{") != -1) {
				symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.EVENT, text(name), range(name, name+1),
						range(index, end), type, null, modifiers));
			} else {
				int typeEnd = name;
				for (int i=name; i<=end; i++) {
					if (i == end || is(i, ",") || is(i, ";")) {
						int item = previousIdentifier(i-1, typeEnd-1);
						if (item != -1) {
							symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.EVENT, text(item),
									range(item, item+1), range(item, i), type, null, modifiers));
						}
						typeEnd = i + 1;
					}
				}
			}
		}

		private void parseFixedBuffer(int index, int end, CSharpSymbol parentSymbol,
				EnumSet<CSharpSymbol.Modifier> modifiers) {
			int typeStart = index + 1;
			List<Integer> names = new ArrayList<>();
			for (int i=typeStart; i<end; i++) {
				if (tokens.get(i).isIdentifier() && is(i+1, "[")) {
					names.add(i);
					i = matching(i+1, end);
				}
			}
			if (names.isEmpty())
				return;
			String type = join(typeStart, names.get(0)) + "*";
			for (int name: names) {
				symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.NORMAL_FIELD, text(name),
						range(name, name+1), range(name, skipUntilTopLevel(name+1, end, ",", ";", "}")),
						type, null, modifiers));
			}
		}

		private void parseConversionOperator(int index, int end, CSharpSymbol parentSymbol,
				EnumSet<CSharpSymbol.Modifier> modifiers) {
			int operator = findTopLevel(index, end, "operator");
			int open = findTopLevel(operator+1, end, "(");
			if (operator == -1 || open == -1)
				return;
			int close = matching(open, end);
			String prefix = text(index) + " operator";
			QualifiedName name = new QualifiedName(join(operator+1, open), null, null);
			symbols.add(new MethodSymbol(parentSymbol, MethodSymbol.Kind.OPERATOR, name, range(operator+1, open),
					range(index, end), null, null, singleParamType(open+1, close), prefix, modifiers));
		}

		private void parseOperator(int index, int end, int operator, CSharpSymbol parentSymbol,
				EnumSet<CSharpSymbol.Modifier> modifiers) {
			int open = findTopLevel(operator+1, end, "(");
			if (open == -1)
				return;
			int close = matching(open, end);
			symbols.add(new MethodSymbol(parentSymbol, MethodSymbol.Kind.OPERATOR, join(operator+1, open),
					range(operator+1, open), range(index, end), null, join(index, operator),
					methodParams(open+1, close), "operator", modifiers));
		}

		private void parseOrdinaryMember(int index, int end, CSharpSymbol parentSymbol, String typeName,
				EnumSet<CSharpSymbol.Modifier> modifiers) {
			int open = findTopLevel(index, end, "(");
			int brace = findTopLevel(index, end, "{");
			int arrow = findTopLevel(index, end, "=>");
			int equals = findTopLevel(index, end, "=");
			if (open != -1 && (brace == -1 || open < brace) && (arrow == -1 || open < arrow)
					&& (equals == -1 || open < equals)) {
				int name = methodNameBeforeOpen(open, index-1);
				if (name == -1) {
					parseFields(index, end, parentSymbol, modifiers);
					return;
				}
				int close = matching(open, end);
				if (text(name).equals(typeName) && join(index, name).length() == 0) {
					symbols.add(new MethodSymbol(parentSymbol, MethodSymbol.Kind.NORMAL_METHOD, text(name),
							range(name, name+1), range(index, end), null, null, methodParams(open+1, close),
							null, modifiers));
				} else {
					int nameStart = explicitMethodNameStart(index, name);
					String returnType = join(index, nameStart);
					QualifiedName methodName = new QualifiedName(join(nameStart, name+1), "~", "::", ".");
					symbols.add(new MethodSymbol(parentSymbol, MethodSymbol.Kind.NORMAL_METHOD, methodName,
							range(name, name+1), range(index, end), typeParameters(name+1, open),
							returnType.length()!=0? normalizeReturnType(returnType): "void",
							methodParams(open+1, close), null, modifiers));
				}
			} else if ((brace != -1 && (equals == -1 || brace < equals))
					|| (arrow != -1 && (equals == -1 || arrow < equals))) {
				int marker = brace != -1 && (equals == -1 || brace < equals)? brace: arrow;
				int thisIndex = findTopLevel(index, marker, "this");
				if (thisIndex != -1) {
					int typeEnd = thisIndex;
					String propertyName = "this";
					if (is(thisIndex-1, "."))
						propertyName = join(index+1, thisIndex-1) + ".this";
					String params = null;
					int bracket = findTopLevel(thisIndex+1, marker, "[");
					if (bracket != -1) {
						int close = matching(bracket, marker);
						params = "[" + methodParams(bracket+1, close) + "]";
					}
					symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.PROPERTY, propertyName,
							range(thisIndex, thisIndex+1), range(index, end), join(index, typeEnd), params, modifiers));
				} else {
					int name = previousIdentifier(marker-1, index-1);
					if (name != -1) {
						int nameStart = explicitMethodNameStart(index, name);
						symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.PROPERTY, join(nameStart, name+1),
								range(name, name+1), range(index, end), join(index, nameStart), null, modifiers));
					}
				}
			} else {
				parseFields(index, end, parentSymbol, modifiers);
			}
		}

		private void parseFields(int index, int end, CSharpSymbol parentSymbol, EnumSet<CSharpSymbol.Modifier> modifiers) {
			if (is(index, "const"))
				index++;
			List<Integer> names = new ArrayList<>();
			int delimiter = skipUntilTopLevel(index, end, "=", ",", ";", "}");
			int firstName = previousIdentifier(delimiter-1, index-1);
			if (firstName == -1)
				return;
			String type = join(index, firstName);
			names.add(firstName);
			while (delimiter < end && is(delimiter, ",")) {
				int nextDelimiter = skipUntilTopLevel(delimiter+1, end, "=", ",", ";", "}");
				int name = previousIdentifier(nextDelimiter-1, delimiter);
				if (name != -1)
					names.add(name);
				delimiter = nextDelimiter;
			}
			for (int name: names) {
				symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.NORMAL_FIELD, text(name),
						range(name, name+1), range(name, name+1), type, null, modifiers));
			}
		}

		private int explicitMethodNameStart(int index, int name) {
			if (is(name-1, ".")) {
				int start = index + 1;
				if (is(index, "ref") && start < name)
					start++;
				return start;
			}
			return name;
		}

		private int methodNameBeforeOpen(int open, int before) {
			if (is(open-1, ">")) {
				int typeParametersStart = reverseMatching(open-1, before+1);
				return previousIdentifier(typeParametersStart-1, before);
			} else {
				return previousIdentifier(open-1, before);
			}
		}

		private String normalizeReturnType(String returnType) {
			if (returnType.startsWith("ref"))
				return returnType.substring("ref".length());
			else
				return returnType;
		}

		private int findMemberEnd(int index, int end) {
			int parens = 0;
			int braces = 0;
			int brackets = 0;
			boolean seenEquals = false;
			for (int i=index; i<end; i++) {
				if (is(i, "("))
					parens++;
				else if (is(i, ")"))
					parens--;
				else if (is(i, "{")) {
					if (parens == 0 && braces == 0 && brackets == 0 && !seenEquals)
						return matching(i, end);
					braces++;
				} else if (is(i, "}")) {
					if (braces == 0)
						return i;
					braces--;
				}
				else if (is(i, "["))
					brackets++;
				else if (is(i, "]"))
					brackets--;
				else if (parens == 0 && braces == 0 && brackets == 0 && is(i, "="))
					seenEquals = true;
				else if (parens == 0 && braces == 0 && brackets == 0 && is(i, ";"))
					return i;
				else if (parens == 0 && braces == 0 && brackets == 0 && !seenEquals && is(i, "=>"))
					return skipUntilTopLevel(i+1, end, ";", "}");
			}
			return end;
		}

		private EnumSet<CSharpSymbol.Modifier> getModifiers(int start, int end) {
			EnumSet<CSharpSymbol.Modifier> modifiers = EnumSet.noneOf(CSharpSymbol.Modifier.class);
			for (int i=start; i<end; i++) {
				if (is(i, "["))
					i = matching(i, end);
				else if (MODIFIERS.contains(text(i)))
					modifiers.add(CSharpSymbol.Modifier.valueOf(text(i).toUpperCase()));
				else
					break;
			}
			return modifiers;
		}

		private int skipAttributesAndModifiers(int index, int end) {
			while (index < end) {
				if (is(index, "["))
					index = matching(index, end) + 1;
				else if (MODIFIERS.contains(text(index)))
					index++;
				else
					break;
			}
			return index;
		}

		@Nullable
		private String typeParameters(int start, int end) {
			if (start < end && is(start, "<")) {
				int close = matching(start, end);
				if (close < end) {
					List<String> params = new ArrayList<>();
					int paramStart = start + 1;
					while (paramStart < close) {
						int comma = skipUntilTopLevel(paramStart, close, ",", ">");
						int name = previousIdentifier(comma-1, paramStart-1);
						if (name != -1)
							params.add(text(name));
						paramStart = comma + 1;
					}
					if (!params.isEmpty())
						return "<" + Joiner.on(", ").join(params) + ">";
				}
			}
			return null;
		}

		@Nullable
		private String methodParams(int start, int end) {
			List<String> params = new ArrayList<>();
			int i = start;
			while (i < end) {
				int comma = skipUntilTopLevel(i, end, ",", ")");
				String param = paramType(i, comma);
				if (param.length() != 0)
					params.add(param);
				i = comma + 1;
			}
			if (params.isEmpty())
				return null;
			else
				return Joiner.on(", ").join(params);
		}

		private String singleParamType(int start, int end) {
			return paramType(start, skipUntilTopLevel(start, end, ",", ")"));
		}

		private String paramType(int start, int end) {
			start = skipParameterAttributes(start, end);
			if (start >= end)
				return "";
			String modifier = null;
			int initializer = findTopLevel(start, end, "=");
			if (initializer != -1)
				end = initializer;
			if (is(start, "ref") || is(start, "out") || is(start, "in") || is(start, "this")) {
				modifier = text(start);
				start++;
				start = skipParameterAttributes(start, end);
			} else if (is(start, "params")) {
				modifier = "params";
				start++;
			}
			int name = previousIdentifier(end-1, start-1);
			String type;
			if (name != -1 && name > start)
				type = join(start, name);
			else
				type = join(start, end);
			if (modifier != null)
				return modifier + " " + type;
			else
				return type;
		}

		private int skipParameterAttributes(int start, int end) {
			while (start < end && is(start, "["))
				start = matching(start, end) + 1;
			return start;
		}

		private int nextIdentifier(int start, int end) {
			for (int i=start; i<end; i++) {
				if (tokens.get(i).isIdentifier())
					return i;
			}
			return -1;
		}

		private int previousIdentifier(int start, int before) {
			for (int i=start; i>before; i--) {
				if (tokens.get(i).isIdentifier())
					return i;
			}
			return -1;
		}

		private int findTopLevel(int start, int end, String value) {
			int parens = 0;
			int braces = 0;
			int brackets = 0;
			int angles = 0;
			for (int i=start; i<end; i++) {
				if (parens == 0 && braces == 0 && brackets == 0 && angles == 0 && is(i, value))
					return i;
				if (is(i, "("))
					parens++;
				else if (is(i, ")"))
					parens--;
				else if (is(i, "{"))
					braces++;
				else if (is(i, "}"))
					braces--;
				else if (is(i, "["))
					brackets++;
				else if (is(i, "]"))
					brackets--;
				else if (is(i, "<"))
					angles++;
				else if (is(i, ">") && angles > 0)
					angles--;
			}
			return -1;
		}

		private int skipUntilTopLevel(int start, int end, String... values) {
			int parens = 0;
			int braces = 0;
			int brackets = 0;
			int angles = 0;
			for (int i=start; i<end; i++) {
				if (parens == 0 && braces == 0 && brackets == 0 && angles == 0) {
					for (String value: values) {
						if (is(i, value))
							return i;
					}
				}
				if (is(i, "("))
					parens++;
				else if (is(i, ")"))
					parens--;
				else if (is(i, "{"))
					braces++;
				else if (is(i, "}"))
					braces--;
				else if (is(i, "["))
					brackets++;
				else if (is(i, "]"))
					brackets--;
				else if (is(i, "<"))
					angles++;
				else if (is(i, ">") && angles > 0)
					angles--;
			}
			return end;
		}

		private int matching(int open, int end) {
			String close;
			if (is(open, "("))
				close = ")";
			else if (is(open, "{"))
				close = "}";
			else if (is(open, "["))
				close = "]";
			else if (is(open, "<"))
				close = ">";
			else
				return open;
			int depth = 0;
			for (int i=open; i<end; i++) {
				if (is(i, text(open)))
					depth++;
				else if (is(i, close)) {
					depth--;
					if (depth == 0)
						return i;
				}
			}
			return end - 1;
		}

		private int reverseMatching(int close, int start) {
			String open;
			if (is(close, ")"))
				open = "(";
			else if (is(close, "}"))
				open = "{";
			else if (is(close, "]"))
				open = "[";
			else if (is(close, ">"))
				open = "<";
			else
				return close;
			int depth = 0;
			for (int i=close; i>=start; i--) {
				if (is(i, text(close)))
					depth++;
				else if (is(i, open)) {
					depth--;
					if (depth == 0)
						return i;
				}
			}
			return start;
		}

		private String join(int start, int end) {
			StringBuilder builder = new StringBuilder();
			for (int i=start; i<end; i++)
				builder.append(text(i));
			return builder.toString();
		}

		private boolean is(int index, String text) {
			return index >= 0 && index < tokens.size() && tokens.get(index).text.equals(text);
		}

		private String text(int index) {
			return StringUtils.remove(tokens.get(index).text, '@');
		}

		private PlanarRange range(int start, int end) {
			Token from = tokens.get(Math.max(0, Math.min(start, tokens.size()-1)));
			Token to = tokens.get(Math.max(0, Math.min(end-1, tokens.size()-1)));
			return new PlanarRange(from.row, from.column, to.endRow, to.endColumn);
		}

		private static List<Token> tokenize(String source) {
			List<Token> tokens = new ArrayList<>();
			int row = 0;
			int column = 0;
			int i = 0;
			while (i < source.length()) {
				char ch = source.charAt(i);
				if (ch == '\r') {
					i++;
				} else if (ch == '\n') {
					row++;
					column = 0;
					i++;
				} else if (Character.isWhitespace(ch)) {
					column++;
					i++;
				} else if (ch == '/' && i+1 < source.length() && source.charAt(i+1) == '/') {
					i += 2;
					column += 2;
					while (i < source.length() && source.charAt(i) != '\n') {
						i++;
						column++;
					}
				} else if (ch == '/' && i+1 < source.length() && source.charAt(i+1) == '*') {
					i += 2;
					column += 2;
					while (i+1 < source.length() && !(source.charAt(i) == '*' && source.charAt(i+1) == '/')) {
						if (source.charAt(i) == '\n') {
							row++;
							column = 0;
						} else {
							column++;
						}
						i++;
					}
					if (i+1 < source.length()) {
						i += 2;
						column += 2;
					}
				} else if (ch == '"' || ch == '\'') {
					int startRow = row;
					int startColumn = column;
					char quote = ch;
					i++;
					column++;
					while (i < source.length()) {
						ch = source.charAt(i);
						if (ch == '\\') {
							i += 2;
							column += 2;
						} else if (ch == quote) {
							i++;
							column++;
							break;
						} else if (ch == '\n') {
							row++;
							column = 0;
							i++;
						} else {
							i++;
							column++;
						}
					}
					tokens.add(new Token("literal", startRow, startColumn, row, column, false));
				} else if (isIdentifierStart(ch)) {
					int start = i;
					int startColumn = column;
					i++;
					column++;
					while (i < source.length() && isIdentifierPart(source.charAt(i))) {
						i++;
						column++;
					}
					tokens.add(new Token(source.substring(start, i), row, startColumn, row, column, true));
				} else if (Character.isDigit(ch)) {
					int start = i;
					int startColumn = column;
					i++;
					column++;
					while (i < source.length() && (Character.isLetterOrDigit(source.charAt(i))
							|| source.charAt(i) == '_' || source.charAt(i) == '.')) {
						i++;
						column++;
					}
					tokens.add(new Token(source.substring(start, i), row, startColumn, row, column, false));
				} else {
					int startColumn = column;
					String text;
					if (i+1 < source.length() && ("=>".equals(source.substring(i, i+2))
							|| "::".equals(source.substring(i, i+2))
							|| "==".equals(source.substring(i, i+2))
							|| "!=".equals(source.substring(i, i+2))
							|| ">=".equals(source.substring(i, i+2))
							|| "<=".equals(source.substring(i, i+2))
							|| "++".equals(source.substring(i, i+2))
							|| "--".equals(source.substring(i, i+2)))) {
						text = source.substring(i, i+2);
						i += 2;
						column += 2;
					} else {
						text = String.valueOf(ch);
						i++;
						column++;
					}
					tokens.add(new Token(text, row, startColumn, row, column, false));
				}
			}
			return tokens;
		}

		private static boolean isIdentifierStart(char ch) {
			return Character.isLetter(ch) || ch == '_' || ch == '@';
		}

		private static boolean isIdentifierPart(char ch) {
			return Character.isLetterOrDigit(ch) || ch == '_' || ch == '@';
		}

	}

	private static class Token {

		private final String text;

		private final int row;

		private final int column;

		private final int endRow;

		private final int endColumn;

		private final boolean identifier;

		private Token(String text, int row, int column, int endRow, int endColumn, boolean identifier) {
			this.text = text;
			this.row = row;
			this.column = column;
			this.endRow = endRow;
			this.endColumn = endColumn;
			this.identifier = identifier;
		}

		private boolean isIdentifier() {
			return identifier;
		}

	}

}
