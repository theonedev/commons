package io.onedev.commons.jsymbol;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;

import io.onedev.commons.utils.PlanarRange;

/** Verifies scopes cover whole bodies and stop before neighboring declarations. */
public class ExtractorScopeTest {

	public static Collection<Object[]> fixtures() {
		return Arrays.asList(new Object[][] {
			{ "example.java", String.join("\n",
				"class Example {",
				"  void first() {",
				"    if (true) {",
				"      System.out.println(\"}\");",
				"    }",
				"  }",
				"  void second() {}",
				"}",
				""), new Object[][] {
				{ "Example", 0, 7 },
				{ "first", 1, 5 },
				{ "second", 6, 6 },
			} },
			{ "example.cpp", String.join("\n",
				"namespace Example {",
				"class Worker {",
				"  void first() {",
				"    if (true) {",
				"      int x = 1;",
				"    }",
				"  }",
				"  void second() {}",
				"};",
				"}",
				""), new Object[][] {
				{ "Example", 0, 9 },
				{ "Worker", 1, 8 },
				{ "first", 2, 6 },
				{ "second", 7, 7 },
			} },
			{ "example.c", String.join("\n",
				"#define RUN(x) \\",
				"  do { \\",
				"    x++; \\",
				"  } while (0)",
				"#define VALUES \\",
				"  { 1, 2 }",
				"struct Example {",
				"  int value;",
				"};",
				"void first(void) {",
				"  RUN(value);",
				"}",
				""), new Object[][] {
				{ "RUN", 0, 3 },
				{ "VALUES", 4, 5 },
				{ "Example", 6, 8 },
				{ "first", 9, 11 },
			} },
			{ "example.cs", String.join("\n",
				"namespace Example {",
				"  class Worker {",
				"    int Value {",
				"      get { return 1; }",
				"    }",
				"    void first() {",
				"      if (true) {}",
				"    }",
				"    void second() {}",
				"  }",
				"}",
				""), new Object[][] {
				{ "Example", 0, 10 },
				{ "Worker", 1, 9 },
				{ "Value", 2, 4 },
				{ "first", 5, 7 },
				{ "second", 8, 8 },
			} },
			{ "example.go", String.join("\n",
				"package example",
				"type Example struct {",
				"  Nested struct {",
				"    Value int",
				"  }",
				"}",
				"type Worker interface {",
				"  Run()",
				"}",
				"func first() {",
				"  if true {",
				"    println(\"}\")",
				"  }",
				"}",
				"func second() {}",
				""), new Object[][] {
				{ "Example", 1, 5 },
				{ "Nested", 2, 4 },
				{ "Worker", 6, 8 },
				{ "first", 9, 13 },
				{ "second", 14, 14 },
			} },
			{ "example.php", String.join("\n",
				"<?php",
				"namespace Example {",
				"  class Worker {",
				"    function first() {",
				"      if (true) {",
				"        echo \"}\";",
				"      }",
				"    }",
				"    function second() {}",
				"  }",
				"}",
				""), new Object[][] {
				{ "Example", 1, 10 },
				{ "Worker", 2, 9 },
				{ "first", 3, 7 },
				{ "second", 8, 8 },
			} },
			{ "example.py", String.join("\n",
				"class Example:",
				"    def first(self):",
				"        if True:",
				"            print(\"class Fake:\")",
				"",
				"    def second(self):",
				"        pass",
				"",
				"def outside():",
				"    pass",
				""), new Object[][] {
				{ "Example", 0, 6 },
				{ "first", 1, 3 },
				{ "second", 5, 6 },
				{ "outside", 8, 9 },
			} },
			{ "example.r", String.join("\n",
				"first <- function(x) {",
				"  nested <- function(y) {",
				"    y + 1",
				"  }",
				"  nested(x)",
				"}",
				"second <- function() {",
				"  2",
				"}",
				""), new Object[][] {
				{ "first", 0, 5 },
				{ "nested", 1, 3 },
				{ "second", 6, 8 },
			} },
			{ "example.rb", String.join("\n",
				"module Example",
				"  class Worker",
				"    def first(items)",
				"      while items.any? do",
				"        items.each do |item|",
				"          puts \"end\" if item",
				"        end",
				"      end",
				"      if items.empty? then puts \"empty\" end",
				"    end",
				"    def second; 2; end",
				"  end",
				"end",
				""), new Object[][] {
				{ "Example", 0, 12 },
				{ "Worker", 1, 11 },
				{ "first", 2, 9 },
				{ "second", 10, 10 },
			} },
			{ "example.rs", String.join("\n",
				"pub mod example {",
				"  pub struct Worker {",
				"    value: i32,",
				"  }",
				"  pub enum State {",
				"    Ready,",
				"  }",
				"  pub trait Service {",
				"    fn first(&self) {",
				"      if true {}",
				"    }",
				"    fn declaration(&self);",
				"  }",
				"  impl Worker {",
				"    fn second(&self) {",
				"      println!(\"}\");",
				"    }",
				"  }",
				"}",
				"macro_rules! example_macro {",
				"  () => {{ 1 }};",
				"}",
				""), new Object[][] {
				{ "example", 0, 18 },
				{ "Worker", 1, 3 },
				{ "State", 4, 6 },
				{ "Service", 7, 12 },
				{ "first", 8, 10 },
				{ "declaration", 11, -1 },
				{ "Worker", 13, 17 },
				{ "second", 14, 16 },
				{ "example_macro", 19, 21 },
			} },
			{ "example.swift", String.join("\n",
				"class Example {",
				"  var value: Int {",
				"    get { return 1 }",
				"  }",
				"  let closure = {",
				"    return 2",
				"  }",
				"  func first() {",
				"    if true { print(\"}\") }",
				"  }",
				"  init() {",
				"    print(\"init\")",
				"  }",
				"}",
				"extension Example {",
				"  func second() {}",
				"}",
				""), new Object[][] {
				{ "Example", 0, 13 },
				{ "value", 1, 3 },
				{ "closure", 4, 6 },
				{ "first", 7, 9 },
				{ "init", 10, 12 },
				{ "Example", 14, 16 },
				{ "second", 15, 15 },
			} },
			{ "example.js", String.join("\n",
				"class Example {",
				"  first() {",
				"    if (true) { console.log(\"}\"); }",
				"  }",
				"  arrow = () => {",
				"    return 2;",
				"  };",
				"  second() {}",
				"}",
				"const object = {",
				"  run() {",
				"    return 1;",
				"  }",
				"};",
				""), new Object[][] {
				{ "Example", 0, 8 },
				{ "first", 1, 3 },
				{ "arrow", 4, 6 },
				{ "second", 7, 7 },
				{ "object", 9, 13 },
				{ "run", 10, 12 },
			} },
			{ "example.jsx", String.join("\n",
				"class Example {",
				"  first() {",
				"    if (true) { console.log(\"}\"); }",
				"  }",
				"  arrow = () => {",
				"    return 2;",
				"  };",
				"  second() {}",
				"}",
				"const object = {",
				"  run() {",
				"    return 1;",
				"  }",
				"};",
				""), new Object[][] {
				{ "Example", 0, 8 },
				{ "first", 1, 3 },
				{ "arrow", 4, 6 },
				{ "second", 7, 7 },
				{ "object", 9, 13 },
				{ "run", 10, 12 },
			} },
			{ "example.ts", String.join("\n",
				"class Example {",
				"  first() {",
				"    if (true) { console.log(\"}\"); }",
				"  }",
				"  arrow = () => {",
				"    return 2;",
				"  };",
				"  second() {}",
				"}",
				"const object = {",
				"  run() {",
				"    return 1;",
				"  }",
				"};",
				""), new Object[][] {
				{ "Example", 0, 8 },
				{ "first", 1, 3 },
				{ "arrow", 4, 6 },
				{ "second", 7, 7 },
				{ "object", 9, 13 },
				{ "run", 10, 12 },
			} },
			{ "example.tsx", String.join("\n",
				"class Example {",
				"  first() {",
				"    if (true) { console.log(\"}\"); }",
				"  }",
				"  arrow = () => {",
				"    return 2;",
				"  };",
				"  second() {}",
				"}",
				"const object = {",
				"  run() {",
				"    return 1;",
				"  }",
				"};",
				""), new Object[][] {
				{ "Example", 0, 8 },
				{ "first", 1, 3 },
				{ "arrow", 4, 6 },
				{ "second", 7, 7 },
				{ "object", 9, 13 },
				{ "run", 10, 12 },
			} },
			{ "typedef.c", String.join("\n",
				"typedef struct {",
				"  int value;",
				"} Record;",
				"typedef enum {",
				"  READY,",
				"  DONE",
				"} State;",
				""), new Object[][] {
				{ "Record", 0, 2, 2 },
				{ "State", 3, 6, 6 },
			} },
			{ "concept.cpp", String.join("\n",
				"template<typename T>",
				"concept Sized = requires(T value) {",
				"  value.size();",
				"};",
				""), new Object[][] {
				{ "Sized", 0, 3, 1 },
			} },
			{ "shared.go", String.join("\n",
				"package example",
				"type Example struct {",
				"  First, Second *struct {",
				"    Value int",
				"  }",
				"  Third, Fourth interface {",
				"    Run()",
				"  }",
				"}",
				"var One, Two struct {",
				"  Value int",
				"}",
				""), new Object[][] {
				{ "First", 2, 4 },
				{ "Second", 2, 4 },
				{ "Third", 5, 7 },
				{ "Fourth", 5, 7 },
				{ "One", 9, 11 },
				{ "Two", 9, 11 },
			} },
			{ "multiline.swift", String.join("\n",
				"protocol Service {",
				"  func declaration()",
				"  func first()",
				"    -> Int",
				"  {",
				"    return 1",
				"  }",
				"}",
				"class Example {",
				"  init()",
				"  {",
				"    print(\"init\")",
				"  }",
				"}",
				""), new Object[][] {
				{ "Service", 0, 7 },
				{ "declaration", 1, -1 },
				{ "first", 2, 6 },
				{ "Example", 8, 13 },
				{ "init", 9, 12 },
			} },
			{ "macros.rs", String.join("\n",
				"macro_rules! parens (",
				"  () => { 1 };",
				");",
				"macro_rules! brackets [",
				"  () => { 2 };",
				"];",
				"fn last() {}",
				""), new Object[][] {
				{ "parens", 0, 2 },
				{ "brackets", 3, 5 },
				{ "last", 6, 6 },
			} },
			{ "namespaces.php", String.join("\n",
				"<?php",
				"namespace First;",
				"function one() {",
				"  echo \"first\";",
				"}",
				"namespace Second;",
				"function two() {",
				"  echo \"second\";",
				"}",
				""), new Object[][] {
				{ "First", 1, 4 },
				{ "one", 2, 4 },
				{ "Second", 5, 8 },
				{ "two", 6, 8 },
			} },
			{ "file-namespace.cs", String.join("\n",
				"namespace Example;",
				"class Worker {",
				"  void first()",
				"  {",
				"    System.Console.WriteLine(\"hello\");",
				"  }",
				"}",
				""), new Object[][] {
				{ "Example", 0, 6 },
				{ "Worker", 1, 6 },
				{ "first", 2, 5 },
			} },
		});
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("fixtures")
	public void recordsEnclosingScopes(String fileName, String source, Object[][] expected) {
		assertScopes(fileName, source, expected);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("fixtures")
	public void recordsEnclosingScopesWithCrLf(String fileName, String source, Object[][] expected) {
		assertScopes(fileName, source.replace("\n", "\r\n"), expected);
	}

	private void assertScopes(String fileName, String source, Object[][] expected) {
		SymbolExtractor<Symbol> extractor = SymbolExtractorRegistry.getExtractor(fileName);
		assertNotNull(extractor, fileName);
		List<Symbol> symbols = extractor.extract(fileName, source);
		for (Object[] item : expected) {
			String name = (String) item[0];
			int from = (int) item[1];
			int to = (int) item[2];
			int positionRow = item.length > 3? (int) item[3]: from;
			Symbol symbol = symbols.stream().filter(it -> name.equals(it.getName())
					&& it.getPosition() != null && it.getPosition().getFromRow() == positionRow)
					.findFirst().orElseThrow(() -> new AssertionError("Missing " + name + " at row " + from));
			PlanarRange scope = symbol.getScope();
			if (to == -1) {
				assertNull(scope, name);
				continue;
			}
			assertNotNull(scope, name);
			assertEquals(from, scope.getFromRow(), name);
			assertEquals(to, scope.getToRow(), name);
			assertTrue(scope.getToColumn() > 0, name);
		}
		String[] lines = source.split("\n", -1);
		for (Symbol symbol : symbols) {
			PlanarRange scope = symbol.getScope();
			if (scope != null) {
				assertTrue(scope.getFromRow() >= 0 && scope.getToRow() < lines.length, symbol.getName());
				assertTrue(scope.getFromRow() <= scope.getToRow(), symbol.getName());
				assertTrue(scope.getFromColumn() >= 0
						&& scope.getFromColumn() <= lines[scope.getFromRow()].length(), symbol.getName());
				assertTrue(scope.getToColumn() >= 0
						&& scope.getToColumn() <= lines[scope.getToRow()].length(), symbol.getName());
			}
		}
	}

	@BeforeAll
	public static void coversEveryExtractor() {
		Set<Class<?>> covered = new HashSet<>();
		for (Object[] fixture : fixtures())
			covered.add(SymbolExtractorRegistry.getExtractor((String) fixture[0]).getClass());
		Set<Class<?>> available = new HashSet<>();
		for (Class<?> type : new Reflections(SymbolExtractorRegistry.class.getPackage().getName())
				.getSubTypesOf(SymbolExtractor.class)) {
			if (!Modifier.isAbstract(type.getModifiers()))
				available.add(type);
		}
		assertEquals(available, covered, "Add scope fixtures for new extractors");
	}
}
