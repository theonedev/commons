package io.onedev.commons.jsymbol.golang;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.golang.GoScanner.Token;
import io.onedev.commons.jsymbol.golang.GoScanner.TokenType;
import io.onedev.commons.jsymbol.golang.symbols.*;
import io.onedev.commons.utils.PlanarRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A Go symbol extractor using a hand-written recursive descent parser.
 * This is much faster than ANTLR-based parsing.
 */
public class GolangExtractor extends AbstractSymbolExtractor<GolangSymbol> {

    private static final Logger logger = LoggerFactory.getLogger(GolangExtractor.class);
    
    private List<Token> tokens;
    private int current;
    private List<GolangSymbol> symbols;
    private GolangSymbol packageSymbol;
    private int braceDepth;
    
    @Override
    public List<GolangSymbol> extract(String fileName, String fileContent) {
        this.symbols = new ArrayList<>();
        this.current = 0;
        this.braceDepth = 0;
        
        try {
            GoScanner scanner = new GoScanner(fileContent);
            this.tokens = scanner.scanAll();
            
            parseSourceFile();
        } catch (Exception e) {
            logger.trace("Error parsing Go file: {}", e.getMessage());
        }
        
        return symbols;
    }
    
    private void parseSourceFile() {
        // Skip to package declaration
        skipNewlines();
        
        // Parse package clause
        if (check(TokenType.PACKAGE)) {
            advance(); // consume 'package'
            skipNewlines();
            if (check(TokenType.IDENT)) {
                Token nameToken = advance();
                PlanarRange position = tokenPosition(nameToken);
                PlanarRange scope = position; // Package scope is just the name
                packageSymbol = new PackageSymbol(nameToken.value, position, scope);
                symbols.add(packageSymbol);
            }
        }
        
        // Parse top-level declarations
        // 
        // Design principle: Each parseXxxDecl() function is responsible for fully
        // consuming its declaration, including any body. If parsing goes wrong,
        // the braceDepth fallback attempts recovery by tracking unmatched braces.
        // 
        // The braceDepth > 0 case handles orphaned tokens from failed parsing.
        // This is a safety net, not the primary parsing strategy.
        while (!isAtEnd()) {
            skipNewlines();
            if (isAtEnd()) break;
            
            if (braceDepth == 0) {
                // At top level - look for declarations
                if (check(TokenType.TYPE)) {
                    parseTypeDecl();
                } else if (check(TokenType.FUNC)) {
                    parseFuncDecl();
                } else if (check(TokenType.VAR)) {
                    parseVarDecl();
                } else if (check(TokenType.CONST)) {
                    parseConstDecl();
                } else if (check(TokenType.IMPORT)) {
                    skipImport();
                } else if (check(TokenType.LBRACE)) {
                    // Orphaned brace (shouldn't happen if parsing is correct)
                    advance();
                    braceDepth++;
                } else if (check(TokenType.RBRACE)) {
                    // Orphaned brace - try to recover
                    advance();
                    braceDepth = Math.max(0, braceDepth - 1);
                } else {
                    advance();
                }
            } else {
                // Inside unmatched braces (recovery mode) - just track braces
                if (check(TokenType.LBRACE)) {
                    advance();
                    braceDepth++;
                } else if (check(TokenType.RBRACE)) {
                    advance();
                    braceDepth--;
                } else {
                    advance();
                }
            }
        }
    }
    
    private void skipImport() {
        advance(); // consume 'import'
        skipNewlines();
        if (check(TokenType.LPAREN)) {
            advance();
            int depth = 1;
            while (!isAtEnd() && depth > 0) {
                if (check(TokenType.LPAREN)) depth++;
                else if (check(TokenType.RPAREN)) depth--;
                advance();
            }
        } else {
            // Single import
            while (!isAtEnd() && !check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON)) {
                advance();
            }
        }
    }
    
    private void parseTypeDecl() {
        advance(); // consume 'type'
        skipNewlines();
        
        if (check(TokenType.LPAREN)) {
            // Grouped type declarations: type ( ... )
            advance(); // consume '('
            skipNewlines();
            
            while (!isAtEnd() && !check(TokenType.RPAREN)) {
                int before = current;
                parseTypeSpec();
                skipNewlines();
                // Safety: if we didn't advance, skip the problematic token
                if (current == before && !isAtEnd() && !check(TokenType.RPAREN)) {
                    advance();
                }
            }
            
            if (check(TokenType.RPAREN)) {
                advance();
            }
        } else {
            // Single type declaration
            parseTypeSpec();
        }
    }
    
    private void parseTypeSpec() {
        skipNewlines();
        if (!check(TokenType.IDENT)) return;
        
        Token nameToken = advance();
        String typeName = nameToken.value;
        PlanarRange position = tokenPosition(nameToken);
        int startLine = nameToken.line;
        int startCol = nameToken.column;
        
        skipNewlines();
        
        // Skip type parameters for generics: [T any]
        if (check(TokenType.LBRACK)) {
            skipBrackets();
            skipNewlines();
        }
        
        // Check for alias: type X = Y
        boolean isAlias = check(TokenType.ASSIGN);
        if (isAlias) {
            advance(); // consume '='
            skipNewlines();
        }
        
        // Determine if it's an interface or struct
        boolean isInterface = check(TokenType.INTERFACE);
        boolean isStruct = check(TokenType.STRUCT);
        
        TypeSymbol typeSymbol = new TypeSymbol(packageSymbol, typeName, position, null, isInterface, true);
        symbols.add(typeSymbol);
        
        // Parse the type body
        if (isStruct) {
            advance(); // consume 'struct'
            skipNewlines();
            if (check(TokenType.LBRACE)) {
                advance();
                parseStructBody(typeSymbol);
                Token endToken = previous();
                typeSymbol = replaceTypeSymbolWithScope(typeSymbol, startLine, startCol, endToken, false);
            }
        } else if (isInterface) {
            advance(); // consume 'interface'
            skipNewlines();
            if (check(TokenType.LBRACE)) {
                advance();
                parseInterfaceBody(typeSymbol);
                Token endToken = previous();
                typeSymbol = replaceTypeSymbolWithScope(typeSymbol, startLine, startCol, endToken, true);
            }
        } else {
            // Type alias or other type definition - just skip the type expression
            skipTypeExpr();
        }
    }
    
    private TypeSymbol replaceTypeSymbolWithScope(TypeSymbol old, int startLine, int startCol, Token endToken, boolean isInterface) {
        PlanarRange scope = new PlanarRange(startLine, startCol, endToken.line, endToken.column + endToken.value.length());
        TypeSymbol newSymbol = new TypeSymbol(old.getParent(), old.getName(), old.getPosition(), scope,
                isInterface, old.isSearchable());
        
        // Replace in symbols list
        int idx = symbols.indexOf(old);
        if (idx >= 0) {
            symbols.set(idx, newSymbol);
        }
        
        // Update parent references
        for (GolangSymbol s : symbols) {
            if (s.getParent() == old) {
                s.setParent(newSymbol);
            }
        }
        
        return newSymbol;
    }
    
    private void parseStructBody(GolangSymbol parent) {
        skipNewlines();
        
        while (!isAtEnd() && !check(TokenType.RBRACE)) {
            int before = current;
            parseFieldDecl(parent);
            skipNewlines();
            // Safety: if we didn't advance, skip the problematic token
            if (current == before && !isAtEnd() && !check(TokenType.RBRACE)) {
                advance();
            }
        }
        
        if (check(TokenType.RBRACE)) {
            advance();
        }
    }
    
    private void parseFieldDecl(GolangSymbol parent) {
        skipNewlines();
        if (check(TokenType.RBRACE)) return;
        
        // Collect field names
        List<Token> identTokens = new ArrayList<>();
        
        // Check if this is an embedded field (starts with * or identifier followed by no more identifiers before type)
        if (check(TokenType.STAR)) {
            // Embedded pointer field: *TypeName
            advance(); // consume '*'
            if (check(TokenType.IDENT)) {
                Token nameToken = advance();
                String fieldName = nameToken.value;
                // Handle qualified names like a.Block
                if (check(TokenType.DOT)) {
                    advance();
                    if (check(TokenType.IDENT)) {
                        Token qualified = advance();
                        fieldName = nameToken.value + "." + qualified.value;
                    }
                }
                PlanarRange position = tokenPosition(nameToken);
                symbols.add(new VariableSymbol(parent, fieldName, null, position, false));
            }
            skipToEndOfField();
            return;
        }
        
        if (!check(TokenType.IDENT)) {
            skipToEndOfField();
            return;
        }
        
        Token firstIdent = advance();
        
        // Check what comes next to determine if this is field list or embedded field
        if (check(TokenType.DOT)) {
            // Embedded field with package qualifier: pkg.Type
            advance();
            if (check(TokenType.IDENT)) {
                Token typeName = advance();
                String fieldName = firstIdent.value + "." + typeName.value;
                PlanarRange position = tokenPosition(firstIdent);
                symbols.add(new VariableSymbol(parent, fieldName, null, position, false));
            }
            skipToEndOfField();
            return;
        }
        
        if (check(TokenType.COMMA)) {
            // Multiple field names: x, y, z Type
            identTokens.add(firstIdent);
            while (check(TokenType.COMMA)) {
                advance(); // consume ','
                skipNewlines();
                if (check(TokenType.IDENT)) {
                    identTokens.add(advance());
                }
            }
            
            // Check if the type is an inline struct/interface
            if (check(TokenType.STRUCT) || check(TokenType.INTERFACE)) {
                boolean isInterface = check(TokenType.INTERFACE);
                advance(); // consume 'struct' or 'interface'
                skipNewlines();
                
                if (check(TokenType.LBRACE)) {
                    // Multiple fields with inline struct/interface type
                    advance(); // consume '{'
                    
                    // Create all field symbols first
                    List<VariableSymbol> fieldSymbols = new ArrayList<>();
                    for (Token t : identTokens) {
                        PlanarRange position = tokenPosition(t);
                        VariableSymbol fieldSymbol = new VariableSymbol(parent, t.value, null, position, true);
                        symbols.add(fieldSymbol);
                        fieldSymbols.add(fieldSymbol);
                    }
                    
                    // Parse the body once and add children to all fields
                    // (In Go, all fields share the same type, so they share the same children structure)
                    if (isInterface) {
                        parseInterfaceBody(fieldSymbols.get(0));
                    } else {
                        parseStructBody(fieldSymbols.get(0));
                    }
                    return; // Already consumed the body
                } else {
                    // struct/interface without body
                    for (Token t : identTokens) {
                        PlanarRange position = tokenPosition(t);
                        symbols.add(new VariableSymbol(parent, t.value, isInterface ? "interface" : "struct", position, true));
                    }
                }
            } else {
                // Regular type - try to extract nested struct/interface (e.g., *struct { ... })
                // For multiple fields, only the first field gets children (they share the same type)
                List<VariableSymbol> fieldSymbols = new ArrayList<>();
                for (Token t : identTokens) {
                    PlanarRange position = tokenPosition(t);
                    VariableSymbol fieldSymbol = new VariableSymbol(parent, t.value, null, position, true);
                    symbols.add(fieldSymbol);
                    fieldSymbols.add(fieldSymbol);
                }
                
                if (!tryExtractNestedType(fieldSymbols.get(0))) {
                    // No nested struct/interface, set the type string for all fields
                    String type = parseTypeExprAsString();
                    for (VariableSymbol fs : fieldSymbols) {
                        fs.setType(type);
                    }
                }
            }
        } else if (check(TokenType.STRUCT) || check(TokenType.INTERFACE)) {
            // Field with inline struct/interface type: x struct { ... }
            boolean isInterface = check(TokenType.INTERFACE);
            advance(); // consume 'struct' or 'interface'
            skipNewlines();
            
            if (check(TokenType.LBRACE)) {
                // Inline struct/interface with body - create field with nested children
                advance(); // consume '{'
                PlanarRange position = tokenPosition(firstIdent);
                VariableSymbol fieldSymbol = new VariableSymbol(parent, firstIdent.value, null, position, true);
                symbols.add(fieldSymbol);
                
                // Parse the body with the field as parent
                if (isInterface) {
                    parseInterfaceBody(fieldSymbol);
                } else {
                    parseStructBody(fieldSymbol);
                }
                return; // Already consumed the body, no need to call skipToEndOfField
            } else {
                // Just 'struct' or 'interface' keyword without body (rare but possible)
                PlanarRange position = tokenPosition(firstIdent);
                symbols.add(new VariableSymbol(parent, firstIdent.value, isInterface ? "interface" : "struct", position, true));
            }
        } else if (check(TokenType.IDENT) || check(TokenType.STAR) || check(TokenType.LBRACK) || 
                   check(TokenType.MAP) || check(TokenType.CHAN) || check(TokenType.FUNC) ||
                   check(TokenType.ARROW)) {
            // Single field with type: x Type (ARROW is for <-chan)
            // Try to extract nested struct/interface (e.g., *struct { ... }, []struct { ... })
            PlanarRange position = tokenPosition(firstIdent);
            VariableSymbol fieldSymbol = new VariableSymbol(parent, firstIdent.value, null, position, true);
            symbols.add(fieldSymbol);
            
            if (!tryExtractNestedType(fieldSymbol)) {
                // No nested struct/interface found, set the type string
                fieldSymbol.setType(parseTypeExprAsString());
            }
        } else {
            // Embedded field (just a type name)
            PlanarRange position = tokenPosition(firstIdent);
            symbols.add(new VariableSymbol(parent, firstIdent.value, null, position, false));
        }
        
        skipToEndOfField();
    }
    
    private void skipToEndOfField() {
        // Skip until newline, semicolon, or closing brace
        while (!isAtEnd() && !check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON) && !check(TokenType.RBRACE)) {
            if (check(TokenType.LBRACE)) {
                skipBraces();
            } else if (check(TokenType.LPAREN)) {
                skipParens();
            } else if (check(TokenType.LBRACK)) {
                skipBrackets();
            } else {
                advance();
            }
        }
        if (check(TokenType.SEMICOLON)) advance();
    }
    
    /**
     * Try to extract nested struct/interface from a type expression.
     * Handles patterns like: *struct { ... }, []struct { ... }, map[K]struct { ... }, chan struct { ... }
     * Returns true if a nested struct/interface was found and extracted, false otherwise.
     * If returns true, the type body has been consumed. If returns false, position is unchanged.
     */
    private boolean tryExtractNestedType(GolangSymbol parent) {
        int savedPosition = current;
        
        // Skip through type prefixes to find struct/interface
        while (!isAtEnd() && !check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON) && 
               !check(TokenType.RBRACE) && !check(TokenType.COMMA) && !check(TokenType.RPAREN)) {
            
            if (check(TokenType.STRUCT) || check(TokenType.INTERFACE)) {
                boolean isInterface = check(TokenType.INTERFACE);
                advance(); // consume 'struct' or 'interface'
                skipNewlines();
                
                if (check(TokenType.LBRACE)) {
                    // Check if it's an empty struct{} or interface{} - these should remain as type strings
                    if (current + 1 < tokens.size() && tokens.get(current + 1).type == TokenType.RBRACE) {
                        // Empty struct{} or interface{} - don't extract as nested, use as type string
                        current = savedPosition;
                        return false;
                    }
                    
                    // Found nested struct/interface with body
                    advance(); // consume '{'
                    if (isInterface) {
                        parseInterfaceBody(parent);
                    } else {
                        parseStructBody(parent);
                    }
                    return true;
                } else {
                    // struct/interface without body (e.g., just 'struct' keyword)
                    // Restore and let normal type parsing handle it
                    current = savedPosition;
                    return false;
                }
            } else if (check(TokenType.STAR)) {
                // Pointer type: *T
                advance();
            } else if (check(TokenType.LBRACK)) {
                // Array/slice type: []T or [n]T
                advance();
                // Skip until ]
                int depth = 1;
                while (!isAtEnd() && depth > 0) {
                    if (check(TokenType.LBRACK)) depth++;
                    else if (check(TokenType.RBRACK)) depth--;
                    advance();
                }
            } else if (check(TokenType.MAP)) {
                // Map type: map[K]V
                advance(); // consume 'map'
                if (check(TokenType.LBRACK)) {
                    advance(); // consume '['
                    // Skip key type until ]
                    int depth = 1;
                    while (!isAtEnd() && depth > 0) {
                        if (check(TokenType.LBRACK)) depth++;
                        else if (check(TokenType.RBRACK)) depth--;
                        advance();
                    }
                }
            } else if (check(TokenType.CHAN)) {
                // Channel type: chan T or <-chan T
                advance();
            } else if (check(TokenType.ARROW)) {
                // Receive-only channel: <-chan T
                advance();
            } else if (check(TokenType.FUNC)) {
                // Function type - skip params and result, but check result for struct/interface
                advance(); // consume 'func'
                if (check(TokenType.LPAREN)) {
                    skipParens(); // skip params
                }
                // Continue to check result type for struct/interface
            } else if (check(TokenType.LPAREN)) {
                // Parenthesized type: (T)
                advance();
                // Continue inside the parens
            } else if (check(TokenType.RPAREN)) {
                // End of parenthesized type
                advance();
            } else if (check(TokenType.IDENT) || check(TokenType.DOT)) {
                // Named type or qualified type - no nested struct/interface here
                // But could have generic params, so skip this type and break
                current = savedPosition;
                return false;
            } else {
                // Unknown token, restore and return false
                current = savedPosition;
                return false;
            }
        }
        
        // Didn't find struct/interface, restore position
        current = savedPosition;
        return false;
    }
    
    private void parseInterfaceBody(GolangSymbol parent) {
        skipNewlines();
        
        while (!isAtEnd() && !check(TokenType.RBRACE)) {
            int before = current;
            parseInterfaceElement(parent);
            skipNewlines();
            // Safety: if we didn't advance, skip the problematic token
            if (current == before && !isAtEnd() && !check(TokenType.RBRACE)) {
                advance();
            }
        }
        
        if (check(TokenType.RBRACE)) {
            advance();
        }
    }
    
    private void parseInterfaceElement(GolangSymbol parent) {
        skipNewlines();
        if (check(TokenType.RBRACE)) return;
        
        // Check for embedded interface or method
        if (check(TokenType.IDENT)) {
            Token nameToken = advance();
            
            if (check(TokenType.LPAREN)) {
                // Method signature: name(params) result
                int startLine = nameToken.line;
                int startCol = nameToken.column;
                String methodName = nameToken.value;
                PlanarRange position = tokenPosition(nameToken);
                
                String params = parseParamsAsString();
                String returnType = null;
                
                // Parse result type if on the same line (interface{} and struct{} are valid return types)
                if (!check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON) && !check(TokenType.RBRACE)) {
                    returnType = parseResultAsString();
                }
                
                Token endToken = previous();
                PlanarRange scope = new PlanarRange(startLine, startCol, endToken.line, endToken.column + endToken.value.length());
                symbols.add(new FunctionSymbol(parent, methodName, params, returnType, null, position, scope));
            } else {
                // Embedded interface type (no parens means it's a type, not a method)
                // Skip any type expression
                skipToEndOfField();
            }
        } else if (check(TokenType.INTERFACE)) {
            // Embedded anonymous interface
            advance();
            skipNewlines();
            if (check(TokenType.LBRACE)) {
                advance();
                parseInterfaceBody(parent);
            }
        } else if (check(TokenType.STRUCT)) {
            // Embedded anonymous struct in interface type constraint
            advance();
            skipNewlines();
            if (check(TokenType.LBRACE)) {
                advance();
                parseStructBody(parent);
            }
        } else if (check(TokenType.LPAREN)) {
            // Type constraint like (struct { ... })
            advance();
            skipNewlines();
            if (check(TokenType.STRUCT)) {
                advance();
                skipNewlines();
                if (check(TokenType.LBRACE)) {
                    advance();
                    parseStructBody(parent);
                }
            } else {
                // Skip to closing paren
                int depth = 1;
                while (!isAtEnd() && depth > 0) {
                    if (check(TokenType.LPAREN)) depth++;
                    else if (check(TokenType.RPAREN)) depth--;
                    if (check(TokenType.STRUCT)) {
                        advance();
                        skipNewlines();
                        if (check(TokenType.LBRACE)) {
                            advance();
                            parseStructBody(parent);
                            continue;
                        }
                    }
                    advance();
                }
            }
        } else if (check(TokenType.STAR) || check(TokenType.LBRACK) || check(TokenType.MAP) || check(TokenType.CHAN)) {
            // Type constraint
            skipTypeExpr();
        } else {
            advance();
        }
        
        // Skip to end of line
        while (!isAtEnd() && !check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON) && !check(TokenType.RBRACE)) {
            if (check(TokenType.LBRACE)) {
                skipBraces();
            } else if (check(TokenType.LPAREN)) {
                skipParens();
            } else if (check(TokenType.LBRACK)) {
                skipBrackets();
            } else {
                advance();
            }
        }
        if (check(TokenType.SEMICOLON)) advance();
    }
    
    private void parseFuncDecl() {
        Token funcToken = advance(); // consume 'func'
        int startLine = funcToken.line;
        int startCol = funcToken.column;
        skipNewlines();
        
        String receiver = null;
        
        // Check for receiver
        if (check(TokenType.LPAREN)) {
            receiver = parseReceiverAsString();
            skipNewlines();
        }
        
        // Skip type parameters for generics
        if (check(TokenType.LBRACK)) {
            skipBrackets();
            skipNewlines();
        }
        
        if (!check(TokenType.IDENT)) {
            // Anonymous function or error
            skipFuncBody();
            return;
        }
        
        Token nameToken = advance();
        String funcName = nameToken.value;
        PlanarRange position = tokenPosition(nameToken);
        
        // Skip type parameters for generics on function name
        if (check(TokenType.LBRACK)) {
            skipBrackets();
        }
        
        String params = "";
        String returnType = null;
        
        if (check(TokenType.LPAREN)) {
            params = parseParamsAsString();
        }
        
        skipNewlines();
        
        // Parse return type
        if (!check(TokenType.LBRACE) && !check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON) && !isAtEnd()) {
            returnType = parseResultAsString();
        }
        
        skipNewlines();
        
        // Find end of function (skip body)
        Token endToken = previous();
        if (check(TokenType.LBRACE)) {
            advance();
            skipBraceContent();
            endToken = previous();
        }
        
        PlanarRange scope = new PlanarRange(startLine, startCol, endToken.line, endToken.column + endToken.value.length());
        
        FunctionSymbol funcSymbol = new FunctionSymbol(packageSymbol, funcName, params, returnType, receiver, position, scope);
        symbols.add(funcSymbol);
    }
    
    private void skipFuncBody() {
        // Skip until we find the body or end of declaration
        while (!isAtEnd() && !check(TokenType.LBRACE) && !check(TokenType.NEWLINE)) {
            if (check(TokenType.LPAREN)) {
                skipParens();
            } else if (check(TokenType.LBRACK)) {
                skipBrackets();
            } else {
                advance();
            }
        }
        if (check(TokenType.LBRACE)) {
            advance();
            skipBraceContent();
        }
    }
    
    private String parseReceiverAsString() {
        if (!check(TokenType.LPAREN)) return null;
        
        advance(); // consume '('
        StringBuilder typeParams = new StringBuilder();
        boolean hasPointer = false;
        String typeName = null;
        String varName = null;
        
        while (!isAtEnd() && !check(TokenType.RPAREN)) {
            Token t = peek();
            if (t.type == TokenType.STAR) {
                advance();
                hasPointer = true;
            } else if (t.type == TokenType.IDENT) {
                advance();
                if (varName == null) {
                    // First identifier could be var name or type name
                    // Check if next is another identifier, star, or bracket, or paren (then this is var name)
                    if (check(TokenType.IDENT) || check(TokenType.STAR) || check(TokenType.LBRACK) || check(TokenType.LPAREN)) {
                        varName = t.value;
                    } else {
                        // This is the type name (no var name case)
                        typeName = t.value;
                    }
                } else {
                    // This is the type name
                    typeName = t.value;
                }
            } else if (t.type == TokenType.LBRACK) {
                // Generic receiver: [E, V any]
                advance();
                typeParams.append("[");
                int depth = 1;
                while (!isAtEnd() && depth > 0) {
                    Token inner = advance();
                    if (inner.type == TokenType.LBRACK) depth++;
                    else if (inner.type == TokenType.RBRACK) {
                        depth--;
                        if (depth == 0) break;
                    }
                    if (inner.type == TokenType.IDENT) {
                        if (typeParams.length() > 1) typeParams.append(",");
                        typeParams.append(inner.value);
                    }
                }
                typeParams.append("]");
            } else if (t.type == TokenType.LPAREN) {
                // Handle parenthesized type like *(HostUtil) or *((HostUtil))
                // Skip the parentheses and extract the type name from within
                advance(); // consume '('
                int depth = 1;
                while (!isAtEnd() && depth > 0) {
                    Token inner = peek();
                    if (inner.type == TokenType.LPAREN) {
                        depth++;
                        advance();
                    } else if (inner.type == TokenType.RPAREN) {
                        depth--;
                        advance(); // consume all RPARENs including the final one
                    } else if (inner.type == TokenType.IDENT && typeName == null) {
                        typeName = inner.value;
                        advance();
                    } else if (inner.type == TokenType.STAR) {
                        hasPointer = true;
                        advance();
                    } else {
                        advance();
                    }
                }
            } else {
                advance();
            }
        }
        
        if (check(TokenType.RPAREN)) {
            advance();
        }
        
        // If we only saw one identifier (stored in varName) and no separate typeName,
        // then that identifier is actually the type name (no-variable-name case).
        // Examples: (testEncodableMap[K]) - only one ident with generic params
        if (typeName == null && varName != null) {
            typeName = varName;
            varName = null;  // Clear varName since there's no separate variable name
        }
        
        if (typeName != null) {
            StringBuilder result = new StringBuilder();
            if (varName != null) {
                result.append(varName).append(" ");
            }
            if (hasPointer) {
                result.append("*");
            }
            result.append(typeName).append(typeParams.toString());
            return result.toString();
        }
        return null;
    }
    
    private String parseParamsAsString() {
        if (!check(TokenType.LPAREN)) return "()";
        
        TypeStringBuilder tsb = new TypeStringBuilder(true);  // parameter mode
        tsb.append(TokenType.LPAREN, "(");
        advance(); // consume '('
        
        int depth = 1;
        while (!isAtEnd() && depth > 0) {
            Token t = peek();
            if (t.type == TokenType.LPAREN) {
                depth++;
                tsb.append(t);
                advance();
            } else if (t.type == TokenType.RPAREN) {
                depth--;
                if (depth > 0) {
                    tsb.append(t);
                    advance();
                } else {
                    advance();
                }
            } else if (t.type == TokenType.NEWLINE) {
                advance();
            } else if (t.type == TokenType.LBRACK) {
                tsb.append(TokenType.LBRACK, "[");
                advance();
                int bracketDepth = 1;
                while (!isAtEnd() && bracketDepth > 0) {
                    Token inner = advance();
                    if (inner.type == TokenType.LBRACK) bracketDepth++;
                    else if (inner.type == TokenType.RBRACK) bracketDepth--;
                    if (bracketDepth > 0) tsb.appendRaw(inner.value);
                }
                tsb.append(TokenType.RBRACK, "]");
            } else if (t.type == TokenType.LBRACE) {
                // Check if this is struct{} or interface{} (empty type)
                TokenType lastType = tsb.getLastType();
                if ((lastType == TokenType.STRUCT || lastType == TokenType.INTERFACE) && 
                    current + 1 < tokens.size() && tokens.get(current + 1).type == TokenType.RBRACE) {
                    tsb.appendRaw("{}");
                    advance(); // consume {
                    advance(); // consume }
                } else {
                    tsb.append(t);
                    advance();
                }
            } else {
                tsb.append(t);
                advance();
            }
        }
        
        tsb.append(TokenType.RPAREN, ")");
        return tsb.build();
    }
    
    /**
     * Helper class to build type strings with proper spacing.
     * Handles Go's spacing rules at the parser layer instead of regex post-processing.
     * 
     * Key spacing rules:
     * - "items []T" → space before [ (param name + slice type)
     * - "client SomeType[T]" → no space before [ (generic type)
     * - "pkg.Type[T]" → no space before [ (qualified generic type)
     * - "func(...) ReturnType[T]" → no space before [ (return type is part of type expr)
     * - "args ...string" → space before ... (variadic)
     * - "func() (int, error)" → space before ( for tuple returns
     * - "chan struct{}" → space after chan
     * 
     * The parameterMode flag controls whether spaces are added before [] after identifiers.
     * It's true when parsing function parameters/returns, false for general type expressions.
     * When parsing func types inside a type expression, a nested TypeStringBuilder with
     * parameterMode=true is used for the function's parameters.
     */
    private static class TypeStringBuilder {
        private final StringBuilder sb = new StringBuilder();
        private TokenType lastType = null;
        // Track if last IDENT is part of type expression vs a parameter name.
        // An IDENT is a type part if it follows: DOT, IDENT, RBRACK, RPAREN, STAR, or type keywords.
        // This distinguishes "items []T" (need space) from "SomeType[T]" (no space).
        // RPAREN is included for func(...) ReturnType[T] where ReturnType is a type, not param name.
        private boolean lastIdentIsTypePart = false;
        private final boolean parameterMode;  // In parameter mode, add space before [ after param names
        
        TypeStringBuilder() {
            this(false);
        }
        
        TypeStringBuilder(boolean parameterMode) {
            this.parameterMode = parameterMode;
        }
        
        void append(Token token) {
            append(token.type, token.value);
        }
        
        void append(TokenType type, String value) {
            // Special handling for comma: always format as ", "
            if (type == TokenType.COMMA) {
                sb.append(", ");
                lastType = type;
                lastIdentIsTypePart = false;  // Reset: next ident after comma is a new param name
                return;
            }
            
            if (sb.length() > 0 && needsSpaceBefore(type, value)) {
                sb.append(" ");
            }
            sb.append(value);
            
            // Track if this IDENT is part of a type expression (not a parameter name)
            // An IDENT is part of a type if it follows: DOT, another IDENT, RBRACK, RPAREN, STAR, or type keywords
            // RPAREN is included because after ) in func(...) ReturnType, the ReturnType is part of type
            if (type == TokenType.IDENT) {
                lastIdentIsTypePart = (lastType == TokenType.DOT || lastType == TokenType.IDENT ||
                                       lastType == TokenType.RBRACK || lastType == TokenType.RPAREN ||
                                       lastType == TokenType.STAR || lastType == TokenType.MAP ||
                                       lastType == TokenType.CHAN || lastType == TokenType.FUNC);
            }
            lastType = type;
        }
        
        void appendRaw(String value) {
            sb.append(value);
            // Don't reset lastType - keep it for proper spacing decisions
        }
        
        private boolean needsSpaceBefore(TokenType type, String value) {
            if (lastType == null) return false;
            
            // Never space before these
            if (type == TokenType.COMMA || type == TokenType.RPAREN || 
                type == TokenType.RBRACK || type == TokenType.RBRACE ||
                type == TokenType.DOT || type == TokenType.SEMICOLON ||
                type == TokenType.LBRACE) {  // No space before { in struct{}
                return false;
            }
            
            // Never space after these
            if (lastType == TokenType.LPAREN || lastType == TokenType.LBRACK ||
                lastType == TokenType.DOT || lastType == TokenType.LBRACE ||
                lastType == TokenType.RBRACK ||  // No space after ] in []T, map[K]V
                lastType == TokenType.STAR) {    // No space after *
                return false;
            }
            
            // Space after ) before return type (including tuple return like "(int, error)")
            if (lastType == TokenType.RPAREN) {
                return type == TokenType.IDENT || type == TokenType.STAR ||
                       type == TokenType.LBRACK || type == TokenType.MAP ||
                       type == TokenType.CHAN || type == TokenType.FUNC ||
                       type == TokenType.STRUCT || type == TokenType.INTERFACE ||
                       type == TokenType.LPAREN;  // For tuple returns: func() (int, error)
            }
            
            // Space after chan keyword
            if (lastType == TokenType.CHAN) {
                return type == TokenType.IDENT || type == TokenType.STAR ||
                       type == TokenType.LBRACK || type == TokenType.MAP ||
                       type == TokenType.STRUCT || type == TokenType.INTERFACE ||
                       type == TokenType.FUNC || type == TokenType.CHAN;
            }
            
            // Space after struct/interface keyword before identifier (field name)
            if (lastType == TokenType.STRUCT || lastType == TokenType.INTERFACE) {
                return type == TokenType.IDENT;
            }
            
            // Space after identifier before type or before <-chan
            if (lastType == TokenType.IDENT) {
                // In parameter mode, add space before [ or ... for parameter types
                // But NOT if the ident is part of a type expression (like pkg.Type[T] or SomeType[T])
                if (type == TokenType.LBRACK || type == TokenType.ELLIPSIS) {
                    return parameterMode && !lastIdentIsTypePart;
                }
                return type == TokenType.IDENT || type == TokenType.STAR ||
                       type == TokenType.MAP || type == TokenType.CHAN || 
                       type == TokenType.FUNC || type == TokenType.STRUCT || 
                       type == TokenType.INTERFACE || type == TokenType.ARROW;
            }
            
            // After comma: space is already added in append(), so no extra space needed
            if (lastType == TokenType.COMMA) {
                return false;
            }
            
            return false;
        }
        
        String build() {
            return sb.toString().trim();
        }
        
        TokenType getLastType() {
            return lastType;
        }
    }
    
    private String parseResultAsString() {
        // Use parameter mode for multi-value returns which can have named values like (x, y int)
        TypeStringBuilder tsb = new TypeStringBuilder(check(TokenType.LPAREN));
        
        if (check(TokenType.LPAREN)) {
            // Multiple return values
            tsb.append(TokenType.LPAREN, "(");
            advance();
            int depth = 1;
            while (!isAtEnd() && depth > 0) {
                Token t = peek();
                if (t.type == TokenType.LPAREN) {
                    depth++;
                    tsb.append(t);
                } else if (t.type == TokenType.RPAREN) {
                    depth--;
                    if (depth > 0) tsb.append(t);
                } else if (t.type == TokenType.NEWLINE) {
                    // skip
                } else if (t.type == TokenType.LBRACE) {
                    // Check if this is struct{} or interface{} (empty type)
                    TokenType lastType = tsb.getLastType();
                    if ((lastType == TokenType.STRUCT || lastType == TokenType.INTERFACE) && 
                        current + 1 < tokens.size() && tokens.get(current + 1).type == TokenType.RBRACE) {
                        tsb.appendRaw("{}");
                        advance(); // consume {
                        advance(); // consume }
                        continue;
                    }
                    tsb.append(t);
                } else if (t.type == TokenType.LBRACK) {
                    tsb.append(TokenType.LBRACK, "[");
                    advance();
                    int bracketDepth = 1;
                    while (!isAtEnd() && bracketDepth > 0) {
                        Token inner = advance();
                        if (inner.type == TokenType.LBRACK) bracketDepth++;
                        else if (inner.type == TokenType.RBRACK) bracketDepth--;
                        if (bracketDepth > 0) tsb.appendRaw(inner.value);
                    }
                    tsb.append(TokenType.RBRACK, "]");
                    continue;
                } else {
                    tsb.append(t);
                }
                advance();
            }
            tsb.append(TokenType.RPAREN, ")");
        } else {
            // Single return type
            while (!isAtEnd() && !check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON)) {
                Token t = peek();
                if (t.type == TokenType.LBRACE) {
                    // Check if this is struct{} or interface{} (empty type)
                    TokenType lastType = tsb.getLastType();
                    if ((lastType == TokenType.STRUCT || lastType == TokenType.INTERFACE) && 
                        current + 1 < tokens.size() && tokens.get(current + 1).type == TokenType.RBRACE) {
                        tsb.appendRaw("{}");
                        advance(); // consume {
                        advance(); // consume }
                        continue;
                    }
                    // Otherwise it's a function body, stop here
                    break;
                } else if (t.type == TokenType.LBRACK) {
                    tsb.append(TokenType.LBRACK, "[");
                    advance();
                    int depth = 1;
                    while (!isAtEnd() && depth > 0) {
                        Token inner = advance();
                        if (inner.type == TokenType.LBRACK) depth++;
                        else if (inner.type == TokenType.RBRACK) depth--;
                        if (depth > 0) tsb.appendRaw(inner.value);
                    }
                    tsb.append(TokenType.RBRACK, "]");
                } else {
                    tsb.append(t);
                    advance();
                }
            }
        }
        
        String result = tsb.build();
        return result.isEmpty() ? null : result;
    }
    
    private String parseTypeExprAsString() {
        TypeStringBuilder tsb = new TypeStringBuilder();
        
        while (!isAtEnd() && !check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON) && 
               !check(TokenType.RBRACE) && !check(TokenType.COMMA) && !check(TokenType.RPAREN) &&
               !check(TokenType.ASSIGN)) {
            Token t = peek();
            
            if (t.type == TokenType.LBRACE) {
                // Check if this is struct{} or interface{} (empty type)
                TokenType lastType = tsb.getLastType();
                if ((lastType == TokenType.STRUCT || lastType == TokenType.INTERFACE) && 
                    current + 1 < tokens.size() && tokens.get(current + 1).type == TokenType.RBRACE) {
                    tsb.appendRaw("{}");
                    advance(); // consume {
                    advance(); // consume }
                    continue;
                }
                // Otherwise it's a struct/map literal or body, stop here
                break;
            } else if (t.type == TokenType.LBRACK) {
                tsb.append(TokenType.LBRACK, "[");
                advance();
                int depth = 1;
                while (!isAtEnd() && depth > 0) {
                    Token inner = advance();
                    if (inner.type == TokenType.LBRACK) depth++;
                    else if (inner.type == TokenType.RBRACK) depth--;
                    if (depth > 0) tsb.appendRaw(inner.value);
                }
                tsb.append(TokenType.RBRACK, "]");
            } else if (t.type == TokenType.LPAREN) {
                // Check if this is function parameters (after FUNC) or tuple return type (after RPAREN)
                // Both need parameter-mode spacing for "name []Type" patterns
                TokenType lastType = tsb.getLastType();
                boolean needsParamMode = (lastType == TokenType.FUNC || lastType == TokenType.RPAREN);
                
                advance(); // consume '('
                
                // Use a separate TypeStringBuilder with appropriate mode for the parenthesized content
                TypeStringBuilder innerTsb = new TypeStringBuilder(needsParamMode);
                int depth = 1;
                while (!isAtEnd() && depth > 0) {
                    Token inner = peek();
                    if (inner.type == TokenType.LPAREN) {
                        depth++;
                        innerTsb.append(inner);
                    } else if (inner.type == TokenType.RPAREN) {
                        depth--;
                        if (depth > 0) innerTsb.append(inner);
                    } else if (inner.type == TokenType.NEWLINE) {
                        // skip
                    } else {
                        innerTsb.append(inner);
                    }
                    advance();
                }
                
                // Append the parenthesized content to the main builder
                tsb.append(TokenType.LPAREN, "(");
                tsb.appendRaw(innerTsb.build());
                tsb.append(TokenType.RPAREN, ")");
            } else if (t.type == TokenType.STRING || t.type == TokenType.RAW_STRING) {
                // Stop at string literals (probably reached the tag)
                break;
            } else {
                tsb.append(t);
                advance();
            }
        }
        
        String result = tsb.build();
        return result.isEmpty() ? null : result;
    }
    
    private void parseVarDecl() {
        advance(); // consume 'var'
        skipNewlines();
        
        if (check(TokenType.LPAREN)) {
            // Grouped var declarations
            advance();
            skipNewlines();
            while (!isAtEnd() && !check(TokenType.RPAREN)) {
                int before = current;
                parseVarSpec();
                skipNewlines();
                // Safety: if we didn't advance, skip the problematic token
                if (current == before && !isAtEnd() && !check(TokenType.RPAREN)) {
                    advance();
                }
            }
            if (check(TokenType.RPAREN)) advance();
        } else {
            parseVarSpec();
        }
    }
    
    private void parseVarSpec() {
        skipNewlines();
        if (!check(TokenType.IDENT)) return;
        
        List<Token> identTokens = new ArrayList<>();
        identTokens.add(advance());
        
        while (check(TokenType.COMMA)) {
            advance();
            skipNewlines();
            if (check(TokenType.IDENT)) {
                identTokens.add(advance());
            }
        }
        
        // Check for type - try to extract nested struct/interface
        if (!check(TokenType.ASSIGN) && !check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON) && !check(TokenType.RPAREN)) {
            // Create variable symbols first
            List<VariableSymbol> varSymbols = new ArrayList<>();
            for (Token t : identTokens) {
                PlanarRange position = tokenPosition(t);
                VariableSymbol varSymbol = new VariableSymbol(packageSymbol, t.value, null, position, true);
                symbols.add(varSymbol);
                varSymbols.add(varSymbol);
            }
            
            // Try to extract nested struct/interface (e.g., var x struct { ... })
            if (!tryExtractNestedType(varSymbols.get(0))) {
                // No nested struct/interface, set the type string for all variables
                String type = parseTypeExprAsString();
                for (VariableSymbol vs : varSymbols) {
                    vs.setType(type);
                }
            }
        } else {
            // No type, just initializer (var x = value)
            for (Token t : identTokens) {
                PlanarRange position = tokenPosition(t);
                symbols.add(new VariableSymbol(packageSymbol, t.value, null, position, true));
            }
        }
        
        // Skip initializer if present
        if (check(TokenType.ASSIGN)) {
            advance();
            skipInitializer();
        }
    }
    
    private void parseConstDecl() {
        advance(); // consume 'const'
        skipNewlines();
        
        if (check(TokenType.LPAREN)) {
            // Grouped const declarations
            advance();
            skipNewlines();
            while (!isAtEnd() && !check(TokenType.RPAREN)) {
                int before = current;
                parseConstSpec();
                skipNewlines();
                // Safety: if we didn't advance, skip the problematic token
                if (current == before && !isAtEnd() && !check(TokenType.RPAREN)) {
                    advance();
                }
            }
            if (check(TokenType.RPAREN)) advance();
        } else {
            parseConstSpec();
        }
    }
    
    private void parseConstSpec() {
        skipNewlines();
        if (!check(TokenType.IDENT)) return;
        
        List<Token> identTokens = new ArrayList<>();
        identTokens.add(advance());
        
        while (check(TokenType.COMMA)) {
            advance();
            skipNewlines();
            if (check(TokenType.IDENT)) {
                identTokens.add(advance());
            }
        }
        
        String type = null;
        if (!check(TokenType.ASSIGN) && !check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON) && !check(TokenType.RPAREN)) {
            type = parseTypeExprAsString();
        }
        
        // Skip initializer if present
        if (check(TokenType.ASSIGN)) {
            advance();
            skipInitializer();
        }
        
        for (Token t : identTokens) {
            PlanarRange position = tokenPosition(t);
            symbols.add(new VariableSymbol(packageSymbol, t.value, type, position, true));
        }
    }
    
    /**
     * Skip a value expression (var/const initializer).
     * 
     * VALUE expressions can contain function literals with full statement blocks,
     * including semicolons inside if/for statements. Therefore, we MUST track
     * brace depth and only stop on SEMICOLON/NEWLINE when at the top level.
     * 
     * This is different from TYPE expressions which never contain statements.
     */
    private void skipInitializer() {
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        
        while (!isAtEnd()) {
            Token t = peek();
            
            // Track nesting depth
            if (t.type == TokenType.LPAREN) parenDepth++;
            else if (t.type == TokenType.RPAREN) {
                if (parenDepth > 0) parenDepth--;
                else if (!isInParenGroup()) break; // End of grouped decl like var ( x = 1 )
            }
            else if (t.type == TokenType.LBRACK) bracketDepth++;
            else if (t.type == TokenType.RBRACK) bracketDepth--;
            else if (t.type == TokenType.LBRACE) braceDepth++;
            else if (t.type == TokenType.RBRACE) braceDepth--;
            
            boolean atTopLevel = (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0);
            
            // IMPORTANT: Only stop on SEMICOLON when at top level!
            // Semicolons appear inside function literals: func() { if x; y { } }
            if (t.type == TokenType.SEMICOLON && atTopLevel) break;
            
            if (t.type == TokenType.NEWLINE && atTopLevel) {
                // Check if expression continues on next line
                Token prev = previous();
                if (prev != null && expressionContinuesAfter(prev.type)) {
                    advance();
                    continue;
                }
                
                // Check if next non-newline token is a continuation
                int savedPos = current;
                advance();
                skipNewlines();
                if (!isAtEnd() && (check(TokenType.DOT) || check(TokenType.COMMA))) {
                    continue;
                }
                current = savedPos;
                break;
            }
            
            advance();
        }
    }
    
    private boolean expressionContinuesAfter(TokenType type) {
        // Tokens at end of line that mean expression continues on next line
        return type == TokenType.DOT || type == TokenType.COMMA ||
               type == TokenType.LPAREN || type == TokenType.LBRACK || type == TokenType.LBRACE ||
               type == TokenType.STAR || type == TokenType.ASSIGN || type == TokenType.COLON ||
               type == TokenType.OTHER;
    }
    
    private boolean isInParenGroup() {
        // Check if we're inside a grouped declaration: var ( x = 1 )
        int parenDepth = 0;
        for (int i = current - 1; i >= 0; i--) {
            Token t = tokens.get(i);
            if (t.type == TokenType.RPAREN) parenDepth++;
            else if (t.type == TokenType.LPAREN) {
                if (parenDepth == 0) return true;
                parenDepth--;
            }
            else if (t.type == TokenType.VAR || t.type == TokenType.CONST || t.type == TokenType.TYPE) {
                return false;
            }
        }
        return false;
    }
    
    private void skipTypeExpr() {
        while (!isAtEnd() && !check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON) && !check(TokenType.RBRACE)) {
            if (check(TokenType.LBRACE)) {
                skipBraces();
                break;
            } else if (check(TokenType.LPAREN)) {
                skipParens();
            } else if (check(TokenType.LBRACK)) {
                skipBrackets();
            } else {
                advance();
            }
        }
    }
    
    private void skipBraces() {
        if (!check(TokenType.LBRACE)) return;
        advance();
        skipBraceContent();
    }
    
    private void skipBraceContent() {
        int depth = 1;
        while (!isAtEnd() && depth > 0) {
            if (check(TokenType.LBRACE)) depth++;
            else if (check(TokenType.RBRACE)) depth--;
            advance();
        }
    }
    
    private void skipParens() {
        if (!check(TokenType.LPAREN)) return;
        advance();
        int depth = 1;
        while (!isAtEnd() && depth > 0) {
            if (check(TokenType.LPAREN)) depth++;
            else if (check(TokenType.RPAREN)) depth--;
            advance();
        }
    }
    
    private void skipBrackets() {
        if (!check(TokenType.LBRACK)) return;
        advance();
        int depth = 1;
        while (!isAtEnd() && depth > 0) {
            if (check(TokenType.LBRACK)) depth++;
            else if (check(TokenType.RBRACK)) depth--;
            advance();
        }
    }
    
    private void skipNewlines() {
        while (check(TokenType.NEWLINE) || check(TokenType.SEMICOLON)) {
            advance();
        }
    }
    
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }
    
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    
    private boolean isAtEnd() {
        return current >= tokens.size() || tokens.get(current).type == TokenType.EOF;
    }
    
    private Token peek() {
        if (current >= tokens.size()) {
            return new Token(TokenType.EOF, "", 0, 0, 0);
        }
        return tokens.get(current);
    }
    
    private Token previous() {
        return tokens.get(current - 1);
    }
    
    private PlanarRange tokenPosition(Token token) {
        return new PlanarRange(token.line, token.column, token.line, token.column + token.value.length());
    }
    
    @Override
    public boolean accept(String fileName) {
        return acceptExtensions(fileName, "go");
    }

    @Override
    public int getVersion() {
        return 9;
    }

}
