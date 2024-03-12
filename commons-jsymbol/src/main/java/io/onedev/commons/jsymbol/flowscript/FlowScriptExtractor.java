package io.onedev.commons.jsymbol.flowscript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.javascript.parser.EcmaScriptLexer;
import org.sonar.javascript.parser.JavaScriptParser;
import org.sonar.javascript.tree.impl.declaration.AccessorMethodDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ClassTreeImpl;
import org.sonar.javascript.tree.impl.declaration.DefaultExportDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ExportDefaultBindingImpl;
import org.sonar.javascript.tree.impl.declaration.FieldDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.declaration.FunctionDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.declaration.FunctionTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ImportClauseTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ImportDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.declaration.InitializedBindingElementTreeImpl;
import org.sonar.javascript.tree.impl.declaration.MethodDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ModuleTreeImpl;
import org.sonar.javascript.tree.impl.declaration.NameSpaceImportTreeImpl;
import org.sonar.javascript.tree.impl.declaration.NamedExportDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.declaration.NamedImportExportClauseTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ParameterListTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ScriptTreeImpl;
import org.sonar.javascript.tree.impl.expression.ArrowFunctionTreeImpl;
import org.sonar.javascript.tree.impl.expression.CallExpressionTreeImpl;
import org.sonar.javascript.tree.impl.expression.DotMemberExpressionTreeImpl;
import org.sonar.javascript.tree.impl.expression.FunctionExpressionTreeImpl;
import org.sonar.javascript.tree.impl.expression.IdentifierTreeImpl;
import org.sonar.javascript.tree.impl.expression.NewExpressionTreeImpl;
import org.sonar.javascript.tree.impl.expression.ObjectLiteralTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowArrayTypeWithKeywordTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowDeclareTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowFunctionSignatureTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowFunctionTypeParameterClauseTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowFunctionTypeParameterTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowFunctionTypeTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowGenericParameterClauseTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowGenericParameterTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowIndexerPropertyDefinitionKeyTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowInterfaceDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowIntersectionTypeTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowLiteralTypeTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowMethodPropertyDefinitionKeyTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowModuleExportsTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowModuleTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowObjectTypeTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowOptionalBindingElementTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowOptionalTypeTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowPropertyDefinitionTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowSimplePropertyDefinitionKeyTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowSimpleTypeTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowTypeAliasStatementTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowTypeAnnotationTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowTypedBindingElementTreeImpl;
import org.sonar.javascript.tree.impl.flow.FlowUnionTypeTreeImpl;
import org.sonar.javascript.tree.impl.lexical.InternalSyntaxToken;
import org.sonar.javascript.tree.impl.statement.BlockTreeImpl;
import org.sonar.javascript.tree.impl.statement.EmptyStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.ExpressionStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.VariableDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.statement.VariableStatementTreeImpl;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.declaration.BindingElementTree;
import org.sonar.plugins.javascript.api.tree.declaration.ExportClauseTree;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.MethodDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.SpecifierTree;
import org.sonar.plugins.javascript.api.tree.expression.AssignmentExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.CallExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.DotMemberExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.expression.LiteralTree;
import org.sonar.plugins.javascript.api.tree.expression.ObjectLiteralTree;
import org.sonar.plugins.javascript.api.tree.expression.PairPropertyTree;
import org.sonar.plugins.javascript.api.tree.expression.ParenthesisedExpressionTree;
import org.sonar.plugins.javascript.api.tree.flow.FlowTypeTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.tree.statement.StatementTree;
import org.sonar.plugins.javascript.api.tree.statement.VariableDeclarationTree;
import org.sonar.plugins.javascript.api.tree.statement.VariableStatementTree;

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.RecognitionException;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.flowscript.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.flowscript.symbols.FlowScriptSymbol;
import io.onedev.commons.jsymbol.flowscript.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.flowscript.symbols.MethodAccess;
import io.onedev.commons.jsymbol.flowscript.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.flowscript.symbols.ModuleAccess;
import io.onedev.commons.jsymbol.flowscript.symbols.ObjectSymbol;
import io.onedev.commons.jsymbol.flowscript.symbols.ReferenceSymbol;


public class FlowScriptExtractor extends AbstractSymbolExtractor<FlowScriptSymbol>{

	private final JavaScriptParser parser = new JavaScriptParser(EcmaScriptLexer.SCRIPT);

	/**
	 * @param args
	 * @throws IOException
	 * we use JavaScriptParser(EcmaScriptLexer.SCRIPT) method to parse Js and flowscript
	 */
	private static final Logger logger = LoggerFactory.getLogger(FlowScriptExtractor.class);
	public List<FlowScriptSymbol> extract(String fileName, String fileContent) {
		List<FlowScriptSymbol> symbols = new ArrayList<>();
		
		try {
			Tree tree = parser.parse(fileContent);
			processTree(tree, null, symbols);
		} catch (RecognitionException e) {
			throw new RuntimeException("Error parsing javascript", e);
		}

		// process CommonJS exports
		for (FlowScriptSymbol symbol: symbols) {
			if ("exports".equals(symbol.getName())) {
				if (symbol instanceof ReferenceSymbol) {
					ReferenceSymbol reference = (ReferenceSymbol) symbol;
					FlowScriptSymbol referenced = getSymbolInHierarchy(symbols, reference.getReferencedParent(), 
							reference.getReferencedPath().get(0));
					if (referenced != null && reference.getReferencedPath().size()>1) {
						List<String> childPath = reference.getReferencedPath().subList(1, 
								reference.getReferencedPath().size());
						referenced = getChild(symbols, referenced, childPath.toArray(new String[0]));
					}
					if (referenced != null)
						referenced.setModuleAccess(ModuleAccess.EXPORT);
				} 
				
				for (FlowScriptSymbol child: getChildren(symbols, symbol)) {
					if (child.isProperty())
						child.setModuleAccess(ModuleAccess.EXPORT);
				}
			}
		}

		/*
		 * Remove all symbols not contributing substantial information to outline
		 */ 
		for (Iterator<FlowScriptSymbol> it = symbols.iterator(); it.hasNext();) {
			FlowScriptSymbol symbol = it.next();
			if (symbol.getName() == null && getChildren(symbols, symbol).isEmpty() 
					|| symbol.isLocal() 
							&& getChildren(symbols, symbol).isEmpty() 
							&& (symbol instanceof ObjectSymbol || symbol instanceof ReferenceSymbol)
							&& symbol.getModuleAccess() == ModuleAccess.NORMAL) {
				it.remove();
			} 
		}
		return symbols;
	}	
    
	/*
	 * getChildren method will return all the children whose parent symbol is parameter parent
	 * */
	
	private List<FlowScriptSymbol> getChildren(List<FlowScriptSymbol> symbols, FlowScriptSymbol parent) {
		List<FlowScriptSymbol> children = new ArrayList<>();
		for (FlowScriptSymbol symbol: symbols) {
			if (symbol.getParent() == parent)
				children.add(symbol);
		}
		return children;
	}

	private FlowScriptSymbol getSymbolInHierarchy(List<FlowScriptSymbol> symbols, FlowScriptSymbol parent,
			String symbolName) {
		while (true) {
			FlowScriptSymbol symbol = getChild(symbols, parent, symbolName);
			if (symbol != null)
				return symbol;
			else if (parent == null)
				return null;
			parent = (FlowScriptSymbol) parent.getParent();
		}
	}

	/*
	 * processTree method will proecess ModuleTree
	 * */
	
	public void processTree(Tree tree, FlowScriptSymbol parent, List<FlowScriptSymbol> symbols){
		if(tree instanceof ScriptTreeImpl){
			ScriptTreeImpl scriptTree = (ScriptTreeImpl)tree;
			Iterator<Tree> childrenTree = scriptTree.childrenIterator();
			while (childrenTree.hasNext()){
				Tree child = childrenTree.next();
				if(child instanceof ModuleTreeImpl){
					processModuleTree((ModuleTreeImpl)child, parent, symbols);
				}
			}
		}
	}	
	
	/*
	 * ModuleTree contains abundant subnode that we need
	 * */
	
	private void processModuleTree(ModuleTreeImpl module, FlowScriptSymbol parent, List<FlowScriptSymbol> symbols) {
		Iterator<Tree> childrenTree = module.childrenIterator();
		while(childrenTree.hasNext()){
		    Tree child = childrenTree.next();
		    if(child instanceof FunctionDeclarationTreeImpl){
		    	processFunctionTree((FunctionDeclarationTreeImpl)child, parent, symbols);
		    }
		    else if(child instanceof ClassTreeImpl){
		    	processClassTree((ClassTreeImpl)child, parent, symbols);
		    }
		    else if(child instanceof VariableStatementTreeImpl){
		    	VariableStatementTreeImpl variable = (VariableStatementTreeImpl)child;
		    	processVariableDeclTree((VariableDeclarationTreeImpl)variable.declaration(), parent, symbols);
		    }
		    else if(child instanceof ExpressionStatementTreeImpl){
		    	Tree expression = ((ExpressionStatementTreeImpl)child).expression();
		    	if(expression instanceof ExpressionTree){
		    	     processExpressionTree((ExpressionTree)expression, parent, symbols);
		    	}
		    }
		    else if(child instanceof ImportDeclarationTreeImpl){
		    	processImportDeclarationTree((ImportDeclarationTreeImpl)child, parent, symbols);
		    }
		    else if(child instanceof NamedExportDeclarationTreeImpl){
		    	processNamedExportDecl((NamedExportDeclarationTreeImpl)child, parent, symbols);
		    }
		    else if(child instanceof EmptyStatementTreeImpl){
		    	processEmptyStatementTree((EmptyStatementTreeImpl)child, parent, symbols);
		    }
		    else if(child instanceof FlowTypeAliasStatementTreeImpl){
		    	processFlowTypeAlias((FlowTypeAliasStatementTreeImpl)child, parent, symbols);
		    }
		    else if(child instanceof FlowInterfaceDeclarationTreeImpl){
		    	processFlowInterface((FlowInterfaceDeclarationTreeImpl)child, parent, symbols);
		    }
		    else if(child instanceof DefaultExportDeclarationTreeImpl){
		    	processDefaultExportDeclTree((DefaultExportDeclarationTreeImpl)child, parent, symbols);
		    	
		    }
		    else if(child instanceof FlowDeclareTreeImpl){
		    	processFlowDeclareTree((FlowDeclareTreeImpl)child, parent, symbols);
		    }
		}
	}
	
	/*
	 * don't process emptystatementtree
	 */
	private void processEmptyStatementTree(EmptyStatementTreeImpl child, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
	}

	/*
	 * FlowModuleTree contains flow type annotation
	 * 
	 */
    private void processFlowModuleTree(FlowModuleTreeImpl flowModule, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
    	Iterator<Tree> childrenTree = flowModule.childrenIterator();
		ObjectSymbol moduleSymbol = new ObjectSymbol();
		String name = "";
    	while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof InternalSyntaxToken){
				InternalSyntaxToken token = (InternalSyntaxToken)childTree;
				if(!"module".equals(token.text())){
					if(!"{".equals(token.text()) && !"}".equals(token.text())){
						name = token.text();
						moduleSymbol.setName(name);
						moduleSymbol.setParent(parent);
						moduleSymbol.setPosition(getPosition(token));
						symbols.add(moduleSymbol);
					}else if("{".equals(token.text())){
						moduleSymbol.setScope(getPosition(flowModule.openCurlyBraceToken(),flowModule.closeCurlyBraceToken()));
					}
				}
			}
			else if(childTree instanceof FlowDeclareTreeImpl){
				FlowDeclareTreeImpl flowDeclare = (FlowDeclareTreeImpl)childTree;
				processFlowDeclareTree(flowDeclare, moduleSymbol, symbols);
			}else{
				logger.trace("missing in method processFlowModuleTree:"+childTree.getClass());
			}
		}
	}

    /*
     * process FlowDeclareTree's subnode
     * */
    
	private void processFlowDeclareTree(FlowDeclareTreeImpl flowDeclare, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
    	Iterator<Tree> childrenTree = flowDeclare.childrenIterator();
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof FlowTypeAliasStatementTreeImpl){
				FlowTypeAliasStatementTreeImpl flowType = (FlowTypeAliasStatementTreeImpl)childTree;
			    processFlowTypeAlias(flowType, parent, symbols);
			}
			else if(childTree instanceof FlowInterfaceDeclarationTreeImpl){
				FlowInterfaceDeclarationTreeImpl flowInterface = (FlowInterfaceDeclarationTreeImpl)childTree;
			    processFlowInterface(flowInterface, parent, symbols);
			}
			else if(childTree instanceof FlowFunctionSignatureTreeImpl){
				FlowFunctionSignatureTreeImpl flowFunctionSignature = (FlowFunctionSignatureTreeImpl)childTree;
				processFlowFunctionSignature(flowFunctionSignature, parent, symbols);
			}
			else if(childTree instanceof FlowModuleTreeImpl){
		    	FlowModuleTreeImpl flowModule = (FlowModuleTreeImpl)childTree;
		    	processFlowModuleTree(flowModule, parent, symbols);
		    }else if(childTree instanceof ClassTreeImpl){
		    	ClassTreeImpl classTree = (ClassTreeImpl)childTree;
		    	processClassTree(classTree, parent, symbols);
		    }else if(childTree instanceof VariableStatementTreeImpl){
		    	VariableStatementTreeImpl variable = (VariableStatementTreeImpl)childTree;
		    	processVariableDeclTree((VariableDeclarationTreeImpl)variable.declaration(), parent, symbols);
		    }else if(childTree instanceof FlowModuleExportsTreeImpl){
		    	FlowModuleExportsTreeImpl flowModule = (FlowModuleExportsTreeImpl)childTree;
		    	processFlowModuleExportsTree(flowModule, parent, symbols);
		    }
		}
	}

	/*
	 * pocess Exports Module
	 * */
	
	private void processFlowModuleExportsTree(FlowModuleExportsTreeImpl flowModule, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
		Iterator<Tree> childrenTree = flowModule.childrenIterator();
		ObjectSymbol symbol = new ObjectSymbol();
		String name = "";
		String type = "";
		symbol.setParent(parent);
		symbol.setPosition(getPosition(flowModule.lastToken()));
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof InternalSyntaxToken){
				InternalSyntaxToken token = (InternalSyntaxToken)childTree;
				name += token.text();
			}else if(childTree instanceof FlowTypeAnnotationTreeImpl){
				FlowTypeAnnotationTreeImpl flowType = (FlowTypeAnnotationTreeImpl)childTree;
				type += insertSpaceAfterColon(getFlowTypeAnnotation(flowType));
                name += type;
			}	
		}
		symbol.setName(name);
		symbols.add(symbol);
	}

	private void processFlowFunctionSignature(FlowFunctionSignatureTreeImpl flowFunctionSignature,
			FlowScriptSymbol parent, List<FlowScriptSymbol> symbols) {
		Iterator<Tree> childrenTree = flowFunctionSignature.childrenIterator();
		String name = "";
		String type = "";
		FunctionSymbol functionSymbol = new FunctionSymbol();
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof IdentifierTreeImpl){
				IdentifierTreeImpl identifier = (IdentifierTreeImpl)childTree;
				name = identifier.name();
				functionSymbol.setName(name);
				functionSymbol.setParent(parent);
				functionSymbol.setPosition(getPosition(identifier));
				symbols.add(functionSymbol);
			}
			else if(childTree instanceof FlowFunctionTypeParameterClauseTreeImpl){
				FlowFunctionTypeParameterClauseTreeImpl functionParameter = (FlowFunctionTypeParameterClauseTreeImpl)childTree;
				type += getFlowFunctionTypeParameter(functionParameter);
			}
			else if(childTree instanceof FlowTypeAnnotationTreeImpl){
				FlowTypeAnnotationTreeImpl flowType = (FlowTypeAnnotationTreeImpl)childTree;
				type += insertSpaceAfterColon(getFlowTypeAnnotation(flowType));
			}else if(childTree instanceof InternalSyntaxToken){
				
			}else if(childTree != null){
				type += childTree.toString();
			}
		}
		if(!"".equals(type)){
			functionSymbol.setParameters(type);
		}
	}

	private void processDefaultExportDeclTree(DefaultExportDeclarationTreeImpl defaultExportDecl, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
		Iterator<Tree> childrenTree = defaultExportDecl.childrenIterator();
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof ClassTreeImpl){
				ClassTreeImpl classTree = (ClassTreeImpl)childTree;
				processClassTree(classTree, parent, symbols);
			}
		}
	}

	/*
	 * process Interface
	 * */
	
	private void processFlowInterface(FlowInterfaceDeclarationTreeImpl flowInterface, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
    	ObjectSymbol objectSymbol = new ObjectSymbol();
    	String objectName = "";
    	Iterator<Tree> childrenTree = flowInterface.childrenIterator();
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof IdentifierTreeImpl){
				IdentifierTreeImpl identifier = (IdentifierTreeImpl)childTree;
				objectName = identifier.name();
				objectSymbol.setName(objectName);
				objectSymbol.setParent(parent);
				objectSymbol.setPosition(getPosition(identifier));
				symbols.add(objectSymbol);
			}
			if(childTree instanceof FlowPropertyDefinitionTreeImpl){
				FlowPropertyDefinitionTreeImpl flowProperty = (FlowPropertyDefinitionTreeImpl)childTree;
				processFlowPropertyDefinitionTree(flowProperty, objectSymbol, symbols);
			}
		}
	}

    private void processFlowPropertyDefinitionTree(FlowPropertyDefinitionTreeImpl flowProperty, FlowScriptSymbol parent, List<FlowScriptSymbol> symbols){
    	Iterator<Tree> itera = flowProperty.childrenIterator();
		MethodSymbol symbol = null;
		String name = "";
		String type = "";
		StringBuilder paraBuilder = new StringBuilder();
		while(itera.hasNext()){
			Tree iterTree = itera.next();
			if(iterTree instanceof FlowMethodPropertyDefinitionKeyTreeImpl){
				FlowMethodPropertyDefinitionKeyTreeImpl flowMethod = (FlowMethodPropertyDefinitionKeyTreeImpl)iterTree;
				symbol = new MethodSymbol();
				name += flowMethod.firstToken().text();
				symbol.setName(name);
				symbol.setParent(parent);
				symbol.setPosition(getPosition(flowMethod.firstToken()));
				symbols.add(symbol);
				Iterator<Tree> iterator = flowMethod.childrenIterator();
				while(iterator.hasNext()){
					Tree tree = iterator.next();
					if(tree instanceof FlowFunctionTypeParameterClauseTreeImpl){
						FlowFunctionTypeParameterClauseTreeImpl flowFunction = (FlowFunctionTypeParameterClauseTreeImpl)tree;
						Iterator<Tree> paraIterator= flowFunction.childrenIterator();
						int size = flowFunction.parameters().size();
						if(size  == 0){
							break;
						}
						String[] paraName = new String[size];
						String[] paraType = new String[size];
						int index = 0; 
						while(paraIterator.hasNext()){
							Tree paraTree = paraIterator.next();
							if(paraTree instanceof FlowFunctionTypeParameterTreeImpl){
								FlowFunctionTypeParameterTreeImpl parameter = (FlowFunctionTypeParameterTreeImpl)paraTree;
								Iterator<Tree> paraIter = parameter.childrenIterator();
								while(paraIter.hasNext()){
									Tree para = paraIter.next();
									if(para instanceof IdentifierTreeImpl){
										IdentifierTreeImpl identifier = (IdentifierTreeImpl)para;
										paraType[index] = "";
										paraName[index] = identifier.name();
									}
									if(para instanceof FlowTypeAnnotationTreeImpl){
										FlowTypeAnnotationTreeImpl flowType = (FlowTypeAnnotationTreeImpl)para;
										paraType[index] += insertSpaceAfterColon(getFlowTypeAnnotation(flowType));    
									}
								}
								index ++;
							}
						}
						type += "(";
					    for(int i = 0; i < size; ++i){
					    	paraBuilder.append(paraName[i]);
					    	if(!"".equals(paraType[i])){
					    		paraBuilder.append(paraType[i]);
					    		paraBuilder.append(", ");
					    	}
					    }
					    if(paraBuilder.length() > 0){
					    	type += paraBuilder.substring(0, paraBuilder.length()-2);	
					    }
					    type += ")";
					}
				}
			}
			if(iterTree instanceof FlowTypeAnnotationTreeImpl){
				FlowTypeAnnotationTreeImpl flowTypeA = (FlowTypeAnnotationTreeImpl)iterTree;
				type += insertSpaceAfterColon(getFlowTypeAnnotation(flowTypeA));
			}
			if(iterTree instanceof FlowSimplePropertyDefinitionKeyTreeImpl){
				processFlowSimpleProperty(flowProperty, parent, symbols);
			}
			if(iterTree instanceof FlowIndexerPropertyDefinitionKeyTreeImpl){
				processFlowSimpleProperty(flowProperty, parent, symbols);
			}
		}
		if(symbol != null){
		    symbol.setParameters(type);
		}
    }
	
    /*
     * process flow type alisas
     * */
    
	private void processFlowTypeAlias(FlowTypeAliasStatementTreeImpl flowTypeAlias, FlowScriptSymbol parent, List<FlowScriptSymbol> symbols) {
    	Iterator<Tree> childrenTree = flowTypeAlias.childrenIterator();
		ObjectSymbol objectSymbol = new ObjectSymbol();
		String objectName = "";
		String objectType = "";
    	while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof IdentifierTreeImpl){
				IdentifierTreeImpl identifier = (IdentifierTreeImpl)childTree;
				objectName = identifier.name();
				objectSymbol.setName(objectName);
				objectSymbol.setParent(parent);
				objectSymbol.setPosition(getPosition(identifier));
				symbols.add(objectSymbol);
			}
			if(childTree instanceof FlowGenericParameterClauseTreeImpl){
				FlowGenericParameterClauseTreeImpl flowGeneric = (FlowGenericParameterClauseTreeImpl)childTree;
				Iterator<Tree> iterator = flowGeneric.childrenIterator();
				while(iterator.hasNext()){
					Tree tree = iterator.next();
					if(tree instanceof InternalSyntaxToken){
						InternalSyntaxToken token = (InternalSyntaxToken)tree;
						objectType += token.text();
						if(">".equals(token.text())){
							objectName += objectType;
							break;
						}
					}
					if(tree instanceof FlowGenericParameterTreeImpl){
						FlowGenericParameterTreeImpl flowGenericPara = (FlowGenericParameterTreeImpl)tree;
						objectType += flowGenericPara.firstToken().text();	
					}
				}
				objectSymbol.setName(objectName);
			}
			if(childTree instanceof FlowObjectTypeTreeImpl){
				FlowObjectTypeTreeImpl flowObject = (FlowObjectTypeTreeImpl)childTree;
				Iterator<Tree> iterator = flowObject.childrenIterator();
				while(iterator.hasNext()){
					Tree tree = iterator.next();
					if(tree instanceof FlowPropertyDefinitionTreeImpl){
	    				FlowPropertyDefinitionTreeImpl flowProperty = (FlowPropertyDefinitionTreeImpl)tree;
	    				Iterator<Tree> itera = flowProperty.childrenIterator();
	    				ObjectSymbol symbol = new ObjectSymbol();
	    				String name = "";
	    				String type = "";
	    				while(itera.hasNext()){
	    					Tree iterTree = itera.next();
	    					if(iterTree instanceof FlowSimplePropertyDefinitionKeyTreeImpl){
	    						FlowSimplePropertyDefinitionKeyTreeImpl flowSimple = (FlowSimplePropertyDefinitionKeyTreeImpl)iterTree;
	    					    name += getFlowSimpleProperty(flowSimple);
	    						symbol.setName(name);
	    						symbol.setParent(objectSymbol);
	    						symbol.setPosition(getPosition(flowSimple.firstToken()));
	    						symbols.add(symbol);
	    					}
	    					if(iterTree instanceof FlowIndexerPropertyDefinitionKeyTreeImpl){
	    						FlowIndexerPropertyDefinitionKeyTreeImpl flowIndexer = (FlowIndexerPropertyDefinitionKeyTreeImpl)iterTree;
	    						name += getFlowIndexerProperty(flowIndexer);
	    						symbol.setName(name);
	    						symbol.setParent(objectSymbol);
	    						symbol.setPosition(getPosition(flowIndexer.identifier()));
	    						symbols.add(symbol);
	    					}
	    					if(iterTree instanceof FlowTypeAnnotationTreeImpl){
	    						FlowTypeAnnotationTreeImpl flowTypeA = (FlowTypeAnnotationTreeImpl)iterTree;
	    						type += insertSpaceAfterColon(getFlowTypeAnnotation(flowTypeA));
	    						name += type;
	    						symbol.setName(name);
	    					}
	    				}
	    			}
				}
			}
		}
	}
	
    private void processFlowSimpleProperty(FlowPropertyDefinitionTreeImpl flowProperty, FlowScriptSymbol parent, List<FlowScriptSymbol> symbols){
    	Iterator<Tree> itera = flowProperty.childrenIterator();
		ObjectSymbol symbol = new ObjectSymbol();
		String name = "";
		String type = "";
		while(itera.hasNext()){
			Tree iterTree = itera.next();

			if(iterTree instanceof FlowSimplePropertyDefinitionKeyTreeImpl){
				FlowSimplePropertyDefinitionKeyTreeImpl flowSimple = (FlowSimplePropertyDefinitionKeyTreeImpl)iterTree;
			    name += getFlowSimpleProperty(flowSimple);
				symbol.setName(name);
				symbol.setParent(parent);
				symbol.setPosition(getPosition(flowSimple.firstToken()));
				symbols.add(symbol);
			}
			if(iterTree instanceof FlowIndexerPropertyDefinitionKeyTreeImpl){
				FlowIndexerPropertyDefinitionKeyTreeImpl flowIndexer = (FlowIndexerPropertyDefinitionKeyTreeImpl)iterTree;
				name += getFlowIndexerProperty(flowIndexer);
				symbol.setName(name);
				symbol.setParent(parent);
				symbol.setPosition(getPosition(flowIndexer.identifier()));
				symbols.add(symbol);
			}
			if(iterTree instanceof FlowTypeAnnotationTreeImpl){
				FlowTypeAnnotationTreeImpl flowTypeA = (FlowTypeAnnotationTreeImpl)iterTree;
				type += insertSpaceAfterColon(getFlowTypeAnnotation(flowTypeA));
				name += type;
				symbol.setName(name);
			}
		}
    }

	private String getFlowSimpleProperty(FlowSimplePropertyDefinitionKeyTreeImpl flowSimple) {
		Iterator<Tree> childrenTree = flowSimple.childrenIterator();
		String name = "";
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree != null){
				name += childTree.toString();
			}
		}
		return name;
	}

	private void processNamedExportDecl(NamedExportDeclarationTreeImpl namedExportDeclaration, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
		Tree object = namedExportDeclaration.object();
		if (object instanceof ExportDefaultBindingImpl) {
			ExportDefaultBindingImpl exportDefaultBinding = (ExportDefaultBindingImpl) object;
			IdentifierTree identifier = exportDefaultBinding.exportedDefaultIdentifier();
			if (!identifier.name().equals("default")) {
				ObjectSymbol symbol = new ObjectSymbol();
				symbol.setName(getName(identifier.identifierToken()));
				symbol.setPosition(getPosition(identifier.identifierToken()));
				symbol.setModuleAccess(ModuleAccess.EXPORT);
				symbols.add(symbol);
			}
		} if (object instanceof FunctionDeclarationTree) {
			FlowScriptSymbol symbol = processFunctionTree((FunctionDeclarationTreeImpl)object, parent, symbols);
			symbol.setModuleAccess(ModuleAccess.EXPORT);
		} else if (object instanceof VariableStatementTree) {
			VariableDeclarationTree variable = (VariableDeclarationTreeImpl)((VariableStatementTree)object).declaration();
			for(FlowScriptSymbol symbol : processVariableDeclaration(variable, null, symbols)){
				symbol.setModuleAccess(ModuleAccess.EXPORT);
			}
		} else if (object instanceof ExportClauseTree) {
			ExportClauseTree exportClause = (ExportClauseTree) object;
			for (SpecifierTree specifier: exportClause.exports().specifiers()) {
				processSpecifierTree(specifier, symbols, ModuleAccess.EXPORT);
			}
		} else if (object instanceof ClassTreeImpl) {
			processClassTree((ClassTreeImpl)object, parent, symbols).setModuleAccess(ModuleAccess.EXPORT);
		}
	}

	private void processImportDeclarationTree(ImportDeclarationTreeImpl importDeclaration, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
		if (importDeclaration.importClause() instanceof ImportClauseTreeImpl) {
			ImportClauseTreeImpl importClause = (ImportClauseTreeImpl) importDeclaration.importClause();
			Iterator<Tree> childrenTree = importClause.childrenIterator();
			while(childrenTree.hasNext()){
				Tree tree = childrenTree.next();
				if(tree instanceof IdentifierTreeImpl){
				    IdentifierTreeImpl defaultImport = (IdentifierTreeImpl)tree;
				    if (defaultImport != null) {
				        SyntaxToken token = defaultImport.identifierToken();
				        ObjectSymbol symbol = new ObjectSymbol();
				        symbol.setName(getName(token));
		                symbol.setModuleAccess(ModuleAccess.IMPORT);
				        symbol.setPosition(getPosition(token));
				        symbols.add(symbol);
				    }
				}
				if(tree instanceof NamedImportExportClauseTreeImpl){
					NamedImportExportClauseTreeImpl specifierList = (NamedImportExportClauseTreeImpl)tree;
					for (SpecifierTree specifier: specifierList.specifiers()) {
						processSpecifierTree(specifier, symbols, ModuleAccess.IMPORT);
					}
				}
				if(tree instanceof NameSpaceImportTreeImpl){
					NameSpaceImportTreeImpl nameSpace = (NameSpaceImportTreeImpl)tree;
					processNameSpaceImportTree(nameSpace, symbols,ModuleAccess.IMPORT);
				}
			}
		}
	}

	private void processNameSpaceImportTree(NameSpaceImportTreeImpl nameSpace, List<FlowScriptSymbol> symbols,
			ModuleAccess moduleAccess) {
		SyntaxToken token = null;
	    IdentifierTree identifier = nameSpace.localName();  
        if (identifier != null && !getName(identifier).equals("default")) {
            token = identifier.identifierToken();
        } else {
            token = nameSpace.asToken();
        }
        if (token != null) {
            ObjectSymbol symbol = new ObjectSymbol();
            symbol.setName(getName(token));
            symbol.setModuleAccess(moduleAccess);
            symbol.setPosition(getPosition(token));
            symbols.add(symbol);
        }
	}
	
	private void processSpecifierTree(SpecifierTree specifier, List<FlowScriptSymbol> symbols, ModuleAccess moduleAccess) {
		SyntaxToken token = null;
	    IdentifierTree identifier = specifier.rightName();
        if (identifier != null && !getName(identifier).equals("default")) {
            token = identifier.identifierToken();
        } else {
            token = specifier.leftName().identifierToken();
        }
        if (token != null) {
            ObjectSymbol symbol = new ObjectSymbol();
            symbol.setName(getName(token));
            symbol.setModuleAccess(moduleAccess);
            symbol.setPosition(getPosition(token));
            symbols.add(symbol);
        }
	}
	
	private List<FlowScriptSymbol> processVariableDeclTree(VariableDeclarationTreeImpl variable, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols){
		Iterator<Tree> childrenTree = variable.childrenIterator();
		List<FlowScriptSymbol> varList = new ArrayList<>();
		while(childrenTree.hasNext()){
			Tree tree = childrenTree.next();
			if(tree instanceof IdentifierTreeImpl){
			} else if(tree instanceof InitializedBindingElementTreeImpl){
				InitializedBindingElementTreeImpl initialBinding = (InitializedBindingElementTreeImpl)tree;
			    varList.addAll(processInitializedBindingTree(initialBinding, parent, symbols));
			}
			else if(tree instanceof FlowTypedBindingElementTreeImpl){
				FlowTypedBindingElementTreeImpl flowTyped = (FlowTypedBindingElementTreeImpl)tree;
				processFlowTypedBinding(flowTyped, parent, symbols);
			}
		}
		return varList;
	}
	
	private void processFlowTypedBinding(FlowTypedBindingElementTreeImpl flowTyped, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
		String type = "";
		String name = "";
		ObjectSymbol symbol = new ObjectSymbol();
		Iterator<Tree> childrenTree = flowTyped.childrenIterator();
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof IdentifierTreeImpl){
				IdentifierTreeImpl identifier = (IdentifierTreeImpl)childTree;
				name = identifier.name();
				symbol.setName(name);
				symbol.setParent(parent);
				symbol.setPosition(getPosition(identifier));
				symbols.add(symbol);
			}
			else if(childTree instanceof FlowTypeAnnotationTreeImpl){
				FlowTypeAnnotationTreeImpl flowType = (FlowTypeAnnotationTreeImpl)childTree;
				type += insertSpaceAfterColon(getFlowTypeAnnotation(flowType));
				name += type;
				symbol.setName(name);
			}
		}
	}

	private List<FlowScriptSymbol> processInitializedBindingTree(InitializedBindingElementTreeImpl initialBinding,
			FlowScriptSymbol parent, List<FlowScriptSymbol> symbols) {
		Iterator<Tree> childrenTree = initialBinding.childrenIterator();
		ObjectSymbol object = null;
		String name = "";
		String type = "";
		List<FlowScriptSymbol> initial = new ArrayList<>();
		if(object == null){
			for(IdentifierTree identifier : initialBinding.bindingIdentifiers()){
				object = new ObjectSymbol();
				object.setParent(parent);
				object.setName(identifier.name());
				name = identifier.name();
				object.setPosition(getPosition(identifier));
				initial.add(object);
				symbols.add(object);
		    }
		}
		while(childrenTree.hasNext()){
			Tree tree = childrenTree.next();
			if(tree instanceof ObjectLiteralTreeImpl){
				ObjectLiteralTreeImpl objectLiteral = (ObjectLiteralTreeImpl)tree;
				if(object != null){
			    	object.setScope(getPosition(objectLiteral.openCurlyBraceToken(), objectLiteral.closeCurlyBraceToken()));
			    	for (Tree property: objectLiteral.properties()) {
						if (property instanceof PairPropertyTree) {
							PairPropertyTree pairProperty = (PairPropertyTree) property;
							SyntaxToken nameToken = getNameToken(pairProperty.key());
							String propertyName = getName(nameToken);
							/*
							if(!"".equals(pairProperty.value()) || !(null == pairProperty.value())){
							}*/
							if (nameToken != null) {
								FlowScriptSymbol propertySymbol = new ObjectSymbol();
								propertySymbol.setProperty(true);
								propertySymbol.setName(propertyName);
								propertySymbol.setPosition(getPosition(nameToken));
								propertySymbol.setParent(object);
								symbols.add(propertySymbol);
								initial.add(propertySymbol);
								assignNullSymbol(pairProperty.value(), object, propertySymbol, symbols);
							}
						} else if (property instanceof MethodDeclarationTree) {
							processMethodTree((MethodDeclarationTreeImpl) property, object, symbols);
						}else if(property instanceof AccessorMethodDeclarationTreeImpl){
							AccessorMethodDeclarationTreeImpl accessorMethod = (AccessorMethodDeclarationTreeImpl)property;
						    processAccessorMethodDeclTree(accessorMethod, object, symbols);
						}
					}
			    }
			}
			if(tree instanceof FlowTypedBindingElementTreeImpl){
				FlowTypedBindingElementTreeImpl flowTyped = (FlowTypedBindingElementTreeImpl)tree;
				type += ": ";
				Object[] objList = flowTyped.typeAnnotation().type().descendants().toArray();
//				int index = 0;
				for(Object obj : objList){
				    if(obj instanceof InternalSyntaxToken){
				    	InternalSyntaxToken token = (InternalSyntaxToken) obj;
				    	type += token.text();
				    	if("}".equals(token.text()) || ">".equals(token.text()) || "]".equals(token.text())){
				    		break;
				    	}
				    }
				    if(obj instanceof FlowPropertyDefinitionTreeImpl){
				    	FlowPropertyDefinitionTreeImpl flowProperty = (FlowPropertyDefinitionTreeImpl)obj;
				    	Iterator<Tree> iterTree = flowProperty.childrenIterator();
				    	while(iterTree.hasNext()){
				    		Tree iTree = iterTree.next();
				    		if(iTree instanceof FlowSimplePropertyDefinitionKeyTreeImpl){
				    			FlowSimplePropertyDefinitionKeyTreeImpl flowSimple = (FlowSimplePropertyDefinitionKeyTreeImpl)iTree;
				    		    type += flowSimple.firstToken().text();
				    			
				    		}
				    		if(iTree instanceof FlowTypeAnnotationTreeImpl){
				    			FlowTypeAnnotationTreeImpl flowTypeA = (FlowTypeAnnotationTreeImpl)iTree;
				    		    type += flowTypeA.firstToken().text();
				    		    type += flowTypeA.lastToken().text();
				    		}
				    	}
				    }
				    if(obj instanceof FlowSimpleTypeTreeImpl){
				    	FlowSimpleTypeTreeImpl simpleType = (FlowSimpleTypeTreeImpl)obj;
				    	type += simpleType.token().text();
				    } 
				}
				if(!"".equals(type)){
					name += type;
					object.setName(name);
				}
			}
			if(!"".equals(name) && tree instanceof CallExpressionTreeImpl){
				processExpressionTree((ExpressionTree)tree, parent, symbols);
			}
		}
		return initial;
	}

    private FlowScriptSymbol assignNullSymbol(ExpressionTree expression, FlowScriptSymbol parent, FlowScriptSymbol symbol,
    		List<FlowScriptSymbol> symbols){
		FlowScriptSymbol expressionSymbol= processExpressionTree(expression, parent, symbols);
		if(expressionSymbol != null){
			if(symbol.getModuleAccess() != ModuleAccess.NORMAL){
			    expressionSymbol.setModuleAccess(symbol.getModuleAccess());
			}
			expressionSymbol.setName(symbol.getName());
			expressionSymbol.setParent(symbol.getParent());
			expressionSymbol.setPosition(symbol.getPosition());
			expressionSymbol.setProperty(symbol.isProperty());	
			symbols.remove(symbol);
		    return expressionSymbol; 	
		}else {
    	    return symbol;
		}
    }

    private String insertSpaceAfterColon(String flowType) {
    	if (flowType.startsWith(":") && !flowType.startsWith(": "))
    		return ": " + flowType.substring(1);
    	else
    		return flowType;
    }
    
    /*
     * process GET and SET method
     */
	private void processAccessorMethodDeclTree(AccessorMethodDeclarationTreeImpl accessorMethod,
			FlowScriptSymbol parent, List<FlowScriptSymbol> symbols) {
		Iterator<Tree> iterator = accessorMethod.childrenIterator();
		String methodName = "";
		String parameters = "";
		MethodSymbol methodSymbol = new MethodSymbol();
		methodSymbol.setParent(parent);
		MethodAccess methodAccess = MethodAccess.NORMAL;
		while(iterator.hasNext()){
			Tree child = iterator.next();
			if(child instanceof InternalSyntaxToken){
				InternalSyntaxToken token = (InternalSyntaxToken)child;
				String text = token.text();
				if("set".equals(text)){
					methodAccess = MethodAccess.SET;
				}else if("get".equals(text)){
					methodAccess = MethodAccess.GET;
				}
			}
			if(child instanceof IdentifierTreeImpl){
				IdentifierTreeImpl identifier = (IdentifierTreeImpl)child;
				methodName += identifier.name();
				methodSymbol.setName(methodName);
				methodSymbol.setMethodAccess(methodAccess);
				methodSymbol.setProperty(true);
				methodSymbol.setPosition(getPosition(identifier));
			}
			if(child instanceof ParameterListTreeImpl){
				ParameterListTreeImpl parameter = (ParameterListTreeImpl)child;
				parameters+=getParameterList(parameter);
			}
			if(child instanceof FlowTypeAnnotationTreeImpl){
				FlowTypeAnnotationTreeImpl flowType = (FlowTypeAnnotationTreeImpl)child;
				String functionType = insertSpaceAfterColon(getFlowTypeAnnotation(flowType));
				if(!("".equals(functionType)) || !(null == functionType)){
					parameters += functionType;
				}
			}
			if(child instanceof BlockTreeImpl){
				methodSymbol.setParameters(parameters);
				BlockTreeImpl blockTree = (BlockTreeImpl)child;
				methodSymbol.setScope(getPosition(blockTree.openCurlyBraceToken(), blockTree.closeCurlyBraceToken()));
				symbols.add(methodSymbol);
				processBlockTree(blockTree, methodSymbol, symbols);
			}
		}
	}


	private List<FlowScriptSymbol> processBindingElement(BindingElementTree bindingElement,
			FlowScriptSymbol parent, List<FlowScriptSymbol> symbols) {
		List<FlowScriptSymbol> binded = new ArrayList<>();
		if(bindingElement.bindingIdentifiers().size() == 1){
			IdentifierTree identifier = bindingElement.bindingIdentifiers().get(0);
		    FlowScriptSymbol symbol = new ObjectSymbol();
		    symbol.setParent(parent);
		    symbol.setName(identifier.name());
		    symbol.setPosition(getPosition(identifier));
		    symbols.add(symbol);
		    if(bindingElement instanceof InitializedBindingElementTreeImpl){
		    	InitializedBindingElementTreeImpl initial = (InitializedBindingElementTreeImpl) bindingElement;
		    	binded.add(assignElement(initial.right(), parent, symbol, symbols));
		    }else{
		    	binded.add(symbol);
		    }
		}
		return binded;
	}

	private FlowScriptSymbol assignElement(ExpressionTree expressionTree, FlowScriptSymbol parent,
			FlowScriptSymbol symbol, List<FlowScriptSymbol> symbols) {
		FlowScriptSymbol expression = processExpressionTree(expressionTree, parent, symbols);
		if(expression != null){
			if(symbol.getModuleAccess() != ModuleAccess.NORMAL){
				expression.setModuleAccess(symbol.getModuleAccess());
			}
			expression.setName(symbol.getName());
			expression.setParent(symbol.getParent());
			expression.setPosition(symbol.getPosition());
			expression.setProperty(symbol.isProperty());
			symbols.remove(symbol);
			return expression;
		}else{
			return symbol;
		}
	}

	private FlowScriptSymbol processExpressionTree(ExpressionTree expression, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
		if (expression instanceof AssignmentExpressionTree) {
			return processAssignmentExpression((AssignmentExpressionTree)expression, parent, symbols);
		} else if (expression instanceof ObjectLiteralTree) {
			return processObjectLiteral((ObjectLiteralTree)expression, symbols, parent);
		} else if (expression instanceof ParenthesisedExpressionTree) {
			ParenthesisedExpressionTree parenthesisedExpression = (ParenthesisedExpressionTree) expression;
			return processExpressionTree(parenthesisedExpression.expression(), parent, symbols);
		} else if (expression instanceof NewExpressionTreeImpl) { // new SomeClass(...)
			NewExpressionTreeImpl newExpression = (NewExpressionTreeImpl) expression;
			if (newExpression.argumentClause() != null) {//vue 3
				for (Tree parameter: ( newExpression.argumentClause()).arguments()) {
					
					if(parameter instanceof ObjectLiteralTreeImpl){
						processObjectLiteral((ObjectLiteralTreeImpl)parameter, symbols, parent);
					}else if (parameter instanceof ExpressionTree) {
						// parameter may contain interesting structures, let's dig into it
						processExpressionTree((ExpressionTree)parameter, parent, symbols);
					} 				
				}
			}
			return null;
		} else if (expression instanceof CallExpressionTree) { // call a function
			CallExpressionTree callExpression = (CallExpressionTree) expression;
			//vue2
			// CommonJS require statement
			if (callExpression.callee() instanceof IdentifierTreeImpl) {  
				IdentifierTreeImpl callingFunction = (IdentifierTreeImpl) callExpression.callee();
				if (callingFunction.name().equals("require")) {
					ObjectSymbol symbol = new ObjectSymbol();
					symbol.setParent(parent);
					symbol.setModuleAccess(ModuleAccess.IMPORT);
					symbols.add(symbol);
					return symbol;
				} 
			} 
			// Vue.js component registration
			if (callExpression.callee() instanceof DotMemberExpressionTreeImpl 
					&& StringUtils.deleteWhitespace(callExpression.callee().toString()).equals("Vue.component") 
					&& ( callExpression.argumentClause()).arguments().size()>=2
					&& ( callExpression.argumentClause()).arguments().get(0) instanceof LiteralTree
					&& ((LiteralTree) ( callExpression.argumentClause()).arguments().get(0)).is(Kind.STRING_LITERAL)) {
				FlowScriptSymbol vueComponents = getVueComponents(symbols);
				LiteralTree vueComponent = (LiteralTree) ( callExpression.argumentClause()).arguments().get(0);
				/*
				 * sometimes we will extract null symbol.
				 * 
				 */
				FlowScriptSymbol symbol = new ObjectSymbol();
				symbol.setParent(vueComponents);
				symbol.setName(getName(vueComponent.token()));
				symbol.setPosition(getPosition(vueComponent.token()));
				symbol.setModuleAccess(ModuleAccess.EXPORT);
				symbols.add(symbol);
				if (( callExpression.argumentClause()).arguments().get(1) instanceof ExpressionTree) {
					assignSymbol((ExpressionTree)( callExpression.argumentClause()).arguments().get(1), parent, symbol, 
							symbols);
				} 
				return null;
			} 
			if (callExpression.callee() instanceof DotMemberExpressionTree 
					&& StringUtils.deleteWhitespace(callExpression.callee().toString()).equals("Vue.extend") 
					&& !( callExpression.argumentClause()).arguments().isEmpty()) {
				if (( callExpression.argumentClause()).arguments().get(0) instanceof ExpressionTree) {
					// parameter may contain interesting structures, let's dig into it
					processExpressionTree((ExpressionTree)( callExpression.argumentClause()).arguments().get(0), 
							parent, symbols);
				}
				return null;
			} 
			
			// callee may contain interesting structures, let's dig into it
			processExpressionTree(callExpression.callee(), parent, symbols);
			
			for (Tree parameter: callExpression.argumentClause().arguments()) {
				if (parameter instanceof ExpressionTree) {
					// parameter may contain interesting structures, let's dig into it
					processExpressionTree((ExpressionTree)parameter, parent, symbols);
				} 
			}
			return null;
		} else if (expression instanceof FunctionExpressionTreeImpl) { // an inline function declaration
			return processFunctionTree((FunctionTreeImpl) expression, parent, symbols);
		} else if (expression instanceof ArrowFunctionTreeImpl) {
			return processFunctionTree((FunctionTreeImpl) expression, parent, symbols);
		} else if (expression instanceof ClassTreeImpl) {
			return processClassTree((ClassTreeImpl)expression, parent, symbols);
		} else {
			List<IdentifierTree> identifierPath = getIdentifierPath(expression);
 			if (!identifierPath.isEmpty()) {
				ReferenceSymbol symbol = new ReferenceSymbol();
				List<String> referencedPath = new ArrayList<>();
				for (IdentifierTree identifier: identifierPath) {
					referencedPath.add(identifier.name());
				}
				symbol.setReferencedPath(referencedPath);
				symbol.setReferencedParent(parent);
				symbols.add(symbol);
				return symbol;
			} else {
				return null;
			}
		}
	}

	private List<IdentifierTree> getIdentifierPath(ExpressionTree expressionTree) {
		List<IdentifierTree> identifierPath = new ArrayList<>();
		if (expressionTree instanceof IdentifierTree) {
			IdentifierTree identifier = (IdentifierTree) expressionTree;
			identifierPath.add(identifier);
		} else if (expressionTree instanceof DotMemberExpressionTree) {
			DotMemberExpressionTree dotMemberExpressionTree = (DotMemberExpressionTree) expressionTree;
			identifierPath.addAll(getIdentifierPath(dotMemberExpressionTree.object()));
			if (!identifierPath.isEmpty()) {
				IdentifierTree identifier = dotMemberExpressionTree.property(); 
				identifierPath.add(identifier);
			}
		}
		return identifierPath;
	}

	private FlowScriptSymbol assignSymbol(ExpressionTree expressionTree, FlowScriptSymbol parent, FlowScriptSymbol symbol,
			List<FlowScriptSymbol> symbols) {
		FlowScriptSymbol expression = processExpressionTree(expressionTree, parent, symbols);
		if (expression != null) {
			if (symbol.getModuleAccess() != ModuleAccess.NORMAL) 
			expression.setModuleAccess(symbol.getModuleAccess());
			expression.setName(symbol.getName());
			expression.setParent(symbol.getParent());
			expression.setPosition(symbol.getPosition());
			expression.setProperty(symbol.isProperty());	
			// we will be using the expression symbol carrying more detailed information, so let's remove the original 
			// one
			symbols.remove(symbol);
			return expression;
		} else {
			return symbol;
		}
	}

	private FlowScriptSymbol getVueComponents(List<FlowScriptSymbol> symbols) {
		String name = "vueComponents";
		FlowScriptSymbol vueComponents = getChild(symbols, null, name);
		if (vueComponents == null) {
			vueComponents = new ObjectSymbol();
			vueComponents.setName(name);
			vueComponents.setSearchable(false);
			symbols.add(vueComponents);
		}
		return vueComponents;
	}

	private FlowScriptSymbol getChild(List<FlowScriptSymbol> symbols, FlowScriptSymbol parent, String... childPath) {
		Preconditions.checkArgument(childPath.length != 0);
		String childName = childPath[0];
		FlowScriptSymbol child = null;
		for (FlowScriptSymbol symbol: symbols) {
			if (symbol.getParent() == parent && childName.equals(symbol.getName())) {
				child = symbol;
				break;
			}
		}
		if (child != null && childPath.length > 1) {
			return getChild(symbols, child, Arrays.copyOfRange(childPath, 1, childPath.length));
		} else {
			return child;
		}
	}
	
	private FlowScriptSymbol processObjectLiteral(ObjectLiteralTree objectLiteral, List<FlowScriptSymbol> symbols,
			FlowScriptSymbol parent) {
		ObjectSymbol symbol = new ObjectSymbol();
		symbol.setParent(parent);
		symbol.setScope(getPosition(objectLiteral.openCurlyBraceToken(), objectLiteral.closeCurlyBraceToken()));
		symbols.add(symbol);
		for (Tree property: objectLiteral.properties()) {
			if (property instanceof PairPropertyTree) {
				PairPropertyTree pairProperty = (PairPropertyTree) property;
				SyntaxToken nameToken = getNameToken(pairProperty.key());
				String propertyName = getName(nameToken);
				/*
				if(!"".equals(pairProperty.value()) || !(null == pairProperty.value())){
				}*/
				if (nameToken != null) {
					FlowScriptSymbol propertySymbol = new ObjectSymbol();
					propertySymbol.setProperty(true);
					propertySymbol.setName(propertyName);
					propertySymbol.setPosition(getPosition(nameToken));
					propertySymbol.setParent(symbol);
					symbols.add(propertySymbol);
					assignNullSymbol(pairProperty.value(), symbol, propertySymbol, symbols);
//					assignSymbol(pairProperty.value(), object, propertySymbol, symbols);
				}
			} else if (property instanceof MethodDeclarationTree) {
				processMethodTree((MethodDeclarationTreeImpl) property, symbol, symbols);
			}
		}
		return symbol;
	}

	
	private SyntaxToken getNameToken(Tree nameTree) {
		if (nameTree instanceof IdentifierTree) {
			IdentifierTree identifier = (IdentifierTree) nameTree;
			return identifier.identifierToken();
		} else if (nameTree instanceof LiteralTree) {
			LiteralTree literal = (LiteralTree) nameTree;
			if (literal.is(Kind.STRING_LITERAL)) {
				return literal.token();
			}
		}
		return null;
	}

	private PlanarRange getPosition(IdentifierTree identifier) {
		return identifier != null ? getPosition(identifier.identifierToken()) : null;
	}

	private FlowScriptSymbol processClassTree(ClassTreeImpl classTree, FlowScriptSymbol parent, List<FlowScriptSymbol> symbols) {
		Iterator<Tree> childrenTree = classTree.childrenIterator();
		String className = "";
		ClassSymbol classSymbol = new ClassSymbol();
		classSymbol.setParent(parent);
		classSymbol.setScope(getPosition(classTree.openCurlyBraceToken(),classTree.closeCurlyBraceToken()));
		while(childrenTree.hasNext()){
			Tree child = childrenTree.next();
			if(child instanceof IdentifierTreeImpl){
				IdentifierTreeImpl identifier = (IdentifierTreeImpl)child;
				className = identifier.name();
				classSymbol.setName(className);
				classSymbol.setPosition(getPosition(identifier));
			}
			if(child instanceof FieldDeclarationTreeImpl){
				FieldDeclarationTreeImpl field = (FieldDeclarationTreeImpl)child;
				Iterator<Tree> tree = field.childrenIterator();
				ObjectSymbol objectSymbol = new ObjectSymbol();
				String objectName = "";
				String objectType = "";
				while(tree.hasNext()){
					Tree fieldTree = tree.next();
					if(fieldTree instanceof IdentifierTreeImpl){
						IdentifierTreeImpl identifier = (IdentifierTreeImpl)fieldTree;
						objectName = identifier.name();
						objectSymbol.setParent(classSymbol);
						objectSymbol.setPosition(getPosition(identifier));
						objectSymbol.setName(objectName);
					}
					if(fieldTree instanceof FlowTypeAnnotationTreeImpl){
						FlowTypeAnnotationTreeImpl flowType = (FlowTypeAnnotationTreeImpl)fieldTree;
						objectType += insertSpaceAfterColon(getFlowTypeAnnotation(flowType));
						objectName += objectType; 
						objectSymbol.setName(objectName);
					}
				}
				symbols.add(objectSymbol);
			}
			if(child instanceof MethodDeclarationTreeImpl){
				MethodDeclarationTreeImpl method = (MethodDeclarationTreeImpl)child;
				processMethodTree(method, classSymbol, symbols);
			}
			if(child instanceof AccessorMethodDeclarationTreeImpl){
				AccessorMethodDeclarationTreeImpl accessorMethod = (AccessorMethodDeclarationTreeImpl)child;
			    processAccessorMethodDeclTree(accessorMethod, classSymbol, symbols);
			}
			if(child instanceof FlowPropertyDefinitionTreeImpl){
				FlowPropertyDefinitionTreeImpl flowProperty = (FlowPropertyDefinitionTreeImpl)child;
				processFlowPropertyDefinitionTree(flowProperty, classSymbol, symbols);
			}
		}
		symbols.add(classSymbol);
		return classSymbol;
	}
	
	private void processMethodTree(MethodDeclarationTreeImpl method, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
		Iterator<Tree> iterator = method.childrenIterator();
		String parameters = "";
		MethodSymbol methodSymbol = new MethodSymbol();
		methodSymbol.setParent(parent);
		while(iterator.hasNext()){
			Tree child = iterator.next();
			if(child instanceof IdentifierTreeImpl){
				IdentifierTreeImpl identifier = (IdentifierTreeImpl)child;
				methodSymbol.setName(identifier.name());
				MethodAccess methodAccess = MethodAccess.NORMAL;
				methodSymbol.setMethodAccess(methodAccess);
				methodSymbol.setProperty(true);
				methodSymbol.setPosition(getPosition(identifier));
			}
			if(child instanceof ParameterListTreeImpl){
				ParameterListTreeImpl parameter = (ParameterListTreeImpl)child;
				parameters+=getParameterList(parameter);			
			}
			if(child instanceof FlowTypeAnnotationTreeImpl){
				FlowTypeAnnotationTreeImpl flowType = (FlowTypeAnnotationTreeImpl)child;
				String functionType = getFlowTypeAnnotation(flowType);
				if(!("".equals(functionType)) || !(null == functionType)){
					parameters += insertSpaceAfterColon(functionType);
				}
			}
			if(child instanceof BlockTreeImpl){
				methodSymbol.setParameters(parameters);
				BlockTreeImpl blockTree = (BlockTreeImpl)child;
				methodSymbol.setScope(getPosition(blockTree.openCurlyBraceToken(), blockTree.closeCurlyBraceToken()));
				processBlockTree(blockTree, methodSymbol, symbols);
			}
		}
		symbols.add(methodSymbol);
	}

	private FlowScriptSymbol processAssignmentExpression(AssignmentExpressionTree assignmentExpression, FlowScriptSymbol parent, List<FlowScriptSymbol> symbols) {
		if (assignmentExpression.is(Kind.ASSIGNMENT)) {
			List<IdentifierTree> identifierPath = getIdentifierPath(assignmentExpression.variable());
			if (!identifierPath.isEmpty()) {
				IdentifierTree identifier = identifierPath.get(0);
				FlowScriptSymbol symbol;
				/* 
				 * Find root symbol of a property path. Root symbol is the symbol representing first encountered 
				 * identifier in a identifier path. For instance root symbol of "person.name" is "person"
				 */
				boolean isThis = getName(identifier).equals("this");
				if (isThis) {
					// in case of the special "this" symbol, we always locate it inside current parent
					symbol = getChild(symbols, parent, getName(identifier));
				} else {
					symbol = getSymbolInHierarchy(symbols, parent, getName(identifier));
				}
				if (symbol == null) {
					symbol = new ObjectSymbol();
					symbol.setParent(isThis?parent:null);
					symbol.setName(getName(identifier));
					symbol.setPosition(getPosition(identifier));
					symbol.setProperty(isThis);
					symbol.setSearchable(!isThis);
					symbols.add(symbol);
//					assignNullSymbol(assignmentExpression.expression(), parent ,symbol, symbols);
				}
				/*
				 * Then we iterate over all subsequent identifiers to find the child symbol under root symbol
				 */
				for (int i=1; i<identifierPath.size(); i++) {
					identifier = identifierPath.get(i);
					FlowScriptSymbol child = getChild(symbols, symbol, getName(identifier));
					if (child == null) {
						child = new ObjectSymbol();
						child.setParent(symbol);
						child.setName(getName(identifier));
						child.setPosition(getPosition(identifier));
						child.setProperty(true);
						symbols.add(child);
					}
					symbol = child;
				}
				return assignNullSymbol(assignmentExpression.expression(), parent, symbol, symbols);
			} else {
				return null;
			}
		} else {
			return null;
		}
		
	}

	private String getName(IdentifierTree tree) {
		return tree!=null?getName(tree.identifierToken()):null;
	}

	private List<FlowScriptSymbol> processVariableDeclaration(VariableDeclarationTree declaration, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
		List<FlowScriptSymbol> declared = new ArrayList<>();
		for (BindingElementTree bindingElement: declaration.variables()) {
			declared.addAll(processBindingElement(bindingElement, parent, symbols));
		}
		return declared;
	}
	
	
	public FlowScriptSymbol processFunctionTree(FunctionTreeImpl functionTree, FlowScriptSymbol parent, List<FlowScriptSymbol> symbols){
		Iterator<Tree> childrenTree = null;
		if(functionTree instanceof FunctionExpressionTreeImpl){
			FunctionExpressionTreeImpl function = (FunctionExpressionTreeImpl)functionTree;	
		    childrenTree = function.childrenIterator();
		}else if(functionTree instanceof FunctionDeclarationTreeImpl){
			FunctionDeclarationTreeImpl function = (FunctionDeclarationTreeImpl)functionTree;
		    childrenTree = function.childrenIterator();
		}else if(functionTree instanceof ArrowFunctionTreeImpl){
			ArrowFunctionTreeImpl function = (ArrowFunctionTreeImpl)functionTree;
			childrenTree = function.childrenIterator();
		}
		String parameters = "";
		String arrowType = "";
		String functionName = "";
		FunctionSymbol functionSymbol = new FunctionSymbol();
		functionSymbol.setParent(parent);
		while(childrenTree.hasNext()){
			Tree child = childrenTree.next();
			if(child instanceof InternalSyntaxToken){
			    if("=>".equals(child.toString())){
			    	arrowType = child.toString();
			    }
			}
			if(child instanceof IdentifierTreeImpl){
				IdentifierTreeImpl identifier = (IdentifierTreeImpl)child;
				functionName = identifier.name();
				functionSymbol.setName(functionName);
				functionSymbol.setPosition(getPosition(identifier));
			}
			if(child instanceof FlowGenericParameterClauseTreeImpl){
				FlowGenericParameterClauseTreeImpl flowGeneric = (FlowGenericParameterClauseTreeImpl)child;
				Iterator<Tree> iterator = flowGeneric.childrenIterator();
				String type = "";
				while(iterator.hasNext()){
					Tree tree = iterator.next();
					if(tree instanceof InternalSyntaxToken){
						InternalSyntaxToken token = (InternalSyntaxToken)tree;
						type += token.text();
						if(">".equals(token.text())){
							functionName += type;
							break;
						}
					}
					if(tree instanceof FlowGenericParameterTreeImpl){
						FlowGenericParameterTreeImpl flowGenericPara = (FlowGenericParameterTreeImpl)tree;
						type += flowGenericPara.firstToken().text();
					}
				}
				functionSymbol.setName(functionName);
			}
			
			if(child instanceof ParameterListTreeImpl){
				ParameterListTreeImpl parameter = (ParameterListTreeImpl)child;
				parameters+=getParameterList(parameter);
			}
			if(child instanceof FlowTypeAnnotationTreeImpl){
				FlowTypeAnnotationTreeImpl flowType = (FlowTypeAnnotationTreeImpl)child;
				String functionType = getFlowTypeAnnotation(flowType);
				if(!("".equals(functionType)) || !(null == functionType)){
					parameters += insertSpaceAfterColon(functionType);
				}
			
			}
			if(child instanceof BlockTreeImpl){
				if(!"".equals(arrowType)){
					parameters += arrowType;
				}
				functionSymbol.setParameters(parameters);
				BlockTreeImpl blockTree = (BlockTreeImpl)child;
				functionSymbol.setScope(getPosition(blockTree.openCurlyBraceToken(), blockTree.closeCurlyBraceToken()));
				functionSymbol.setName(functionName);
				processBlockTree(blockTree, functionSymbol, symbols);
			} //there is one objectsymbol while process callExpressionTreeImpl 
		}
		symbols.add(functionSymbol);
		return functionSymbol;
	
	}	

	/*
	 * extract parameterList
	 */
	private String getParameterList(ParameterListTreeImpl parameter){
		List<IdentifierTree> list = parameter.parameterIdentifiers();
		Iterator<Tree> iterator = parameter.childrenIterator();
		String []para = new String[list.size()];
		String []type = new String[list.size()];
		StringBuilder paraBuilder = new StringBuilder();
		String parameters = "";
		int index = 0;
		for(IdentifierTree identifier : list){
			para[index] = "";
			type[index] = "";
			IdentifierTreeImpl paraName = (IdentifierTreeImpl)identifier;
			para[index++] = paraName.name();
		}
		index = 0;
		while(iterator.hasNext()){
			Tree childIter = iterator.next();
			if(childIter instanceof FlowTypedBindingElementTreeImpl){
				FlowTypedBindingElementTreeImpl flowType = (FlowTypedBindingElementTreeImpl)childIter;
//				FlowTypeAnnotationTreeImpl flowTypeA = (FlowTypeAnnotationTreeImpl)flowType.typeAnnotation();
				StringBuilder typeBuilder = new StringBuilder();
				Iterator<Tree> typeIterator = flowType.childrenIterator();
				while(typeIterator.hasNext()){
					Tree typeTree = typeIterator.next();
					if(typeTree instanceof FlowOptionalBindingElementTreeImpl){
						FlowOptionalBindingElementTreeImpl flowOptional = (FlowOptionalBindingElementTreeImpl)typeTree;
						para[index] += flowOptional.lastToken().text(); 
					}
					if(typeTree instanceof FlowTypeAnnotationTreeImpl){
						FlowTypeAnnotationTreeImpl flowTypeAnnotation = (FlowTypeAnnotationTreeImpl)typeTree;
						String temp = getFlowTypeAnnotation(flowTypeAnnotation);
						if(":".equals(temp.substring(0,1))){
							temp = temp.substring(1, temp.length());
						}
						type[index++] += temp;    
					}
					if(typeTree instanceof FlowUnionTypeTreeImpl){
						FlowUnionTypeTreeImpl flowUnion = (FlowUnionTypeTreeImpl)typeTree;
						for(FlowTypeTree flowTypeTree : flowUnion.subTypes()){
							typeBuilder.append(flowTypeTree.firstToken().text());
							typeBuilder.append("|");
						}
					}
				    if(typeTree instanceof FlowSimpleTypeTreeImpl){
				    	type[index++]=((FlowSimpleTypeTreeImpl)typeTree).toString();
				    }
				    if(typeTree instanceof FlowIntersectionTypeTreeImpl){
				    	FlowIntersectionTypeTreeImpl flowInter = (FlowIntersectionTypeTreeImpl)typeTree;
				    	for(FlowTypeTree flowTypeTree : flowInter.subTypes()){
				    		typeBuilder.append(flowTypeTree.firstToken().text());
				    		typeBuilder.append("&");
				    	}
				    }
				    if(typeTree instanceof FlowLiteralTypeTreeImpl){
				    	FlowLiteralTypeTreeImpl flowLiteral = (FlowLiteralTypeTreeImpl)typeTree;
				    	type[index++] = flowLiteral.firstToken().text();
				    }
				    if(typeTree instanceof FlowOptionalTypeTreeImpl){
				    	FlowOptionalTypeTreeImpl flowOptional = (FlowOptionalTypeTreeImpl)typeTree;
				    	type[index] = flowOptional.firstToken().text();
				    	type[index++] += flowOptional.type().toString(); 
				    	
				    }else if(typeTree instanceof FlowGenericParameterClauseTreeImpl){
			    		FlowGenericParameterClauseTreeImpl flowGeneric = (FlowGenericParameterClauseTreeImpl)typeTree;
			    		type[index++] = getFlowGenericParameter(flowGeneric);
			    	}
				}
				if(typeBuilder.length() > 0){
					type[index++] = typeBuilder.substring(0, typeBuilder.length()-1);
				}
			}
			if(childIter instanceof FlowOptionalBindingElementTreeImpl){
				FlowOptionalBindingElementTreeImpl flowOptional = (FlowOptionalBindingElementTreeImpl)childIter;
				para[index] += flowOptional.lastToken().text(); 
			}
			if(childIter instanceof InitializedBindingElementTreeImpl){
				InitializedBindingElementTreeImpl initial = (InitializedBindingElementTreeImpl)childIter;
				Iterator<Tree> initialTree = initial.childrenIterator();
				while(initialTree.hasNext()){
					Tree initialChild = initialTree.next();
					if(initialChild instanceof FlowTypedBindingElementTreeImpl){
						FlowTypedBindingElementTreeImpl flowTypedBinding = (FlowTypedBindingElementTreeImpl)initialChild;
						String tempType = getFlowTypeAnnotation((FlowTypeAnnotationTreeImpl) flowTypedBinding.typeAnnotation());
					    if(tempType.startsWith(":") && 1 < tempType.length()){
					    	type[index++] = tempType.substring(1, tempType.length());
					    }
					}
				}
			}
		}
		parameters += "(";
		for(int i = 0; i < list.size(); ++i){
			paraBuilder.append(para[i]);
			if(!"".equals(type[i])){
			    paraBuilder.append(": ");
			}
			paraBuilder.append(type[i]);
			paraBuilder.append(", ");
		}
		if(paraBuilder.length() > 0){
		    parameters += paraBuilder.substring(0, paraBuilder.length() - 2);
		}
		parameters = parameters + ")";
		return parameters;
	}
	
	/*
	 * gain Type Annotation
	 * */
	
	private String getFlowTypeAnnotation(FlowTypeAnnotationTreeImpl flowType){
		
		String type = "";
	    Iterator<Tree> childrenTree = flowType.childrenIterator();
	    while(childrenTree.hasNext()){
	    	Tree childTree = childrenTree.next();
	    	if(childTree instanceof FlowUnionTypeTreeImpl){
	    		FlowUnionTypeTreeImpl flowUnion = (FlowUnionTypeTreeImpl)childTree;
	    		type += getFlowUnionType(flowUnion);
	    	}else if(childTree instanceof FlowFunctionTypeTreeImpl){
				FlowFunctionTypeTreeImpl flowFunction = (FlowFunctionTypeTreeImpl)childTree;
				type += getFlowFunctionType(flowFunction);
			}else if(childTree instanceof FlowObjectTypeTreeImpl){
	    		FlowObjectTypeTreeImpl flowObject = (FlowObjectTypeTreeImpl)childTree;
				type += getFlowObjectType(flowObject);
	    	}else if(childTree instanceof FlowLiteralTypeTreeImpl){
	    		FlowLiteralTypeTreeImpl flowLiteral = (FlowLiteralTypeTreeImpl)childTree;
	    		type += getFlowLiteralType(flowLiteral);
	    	}else if(childTree instanceof FlowArrayTypeWithKeywordTreeImpl){
	    		FlowArrayTypeWithKeywordTreeImpl flowArrayType = (FlowArrayTypeWithKeywordTreeImpl)childTree;
	    		String temp = getFlowArrayType(flowArrayType);
	    		temp.replaceAll(" ", "");
	    		type += temp;
	    	}
	    	else if(childTree instanceof InternalSyntaxToken){
	    		InternalSyntaxToken token = (InternalSyntaxToken)childTree;
	    		type += token.text();
	    	}else if(childTree instanceof FlowGenericParameterClauseTreeImpl){
	    		FlowGenericParameterClauseTreeImpl flowGeneric = (FlowGenericParameterClauseTreeImpl)childTree;
	    		type += getFlowGenericParameter(flowGeneric);
	    	}else if(childTree instanceof FlowOptionalTypeTreeImpl){
	    		FlowOptionalTypeTreeImpl flowOptional = (FlowOptionalTypeTreeImpl)childTree;
	    		type += getFlowOptionalType(flowOptional);
	    	}else if(childTree instanceof FlowIntersectionTypeTreeImpl){
		    	FlowIntersectionTypeTreeImpl flowInter = (FlowIntersectionTypeTreeImpl)childTree;
		    	for(FlowTypeTree flowTypeTree : flowInter.subTypes()){
		    		type += flowTypeTree.firstToken().text();
		    		type += "&";
		    	}
		    	type = type.substring(0, type.length() - 1);
		    }
	    	else if(childTree != null){
	    		type += childTree.toString();
	    	}
	    }
	    if(type.endsWith(" ")){
	    	type = type.substring(0, type.length() - 1);
	    }
		return type;
	}
	
	/*
	 * gain  flow Array type
	 */
	private String getFlowArrayType(FlowArrayTypeWithKeywordTreeImpl flowArrayType) {
		String type = "";
		Iterator<Tree> childrenTree = flowArrayType.childrenIterator();
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof InternalSyntaxToken){
				InternalSyntaxToken token = (InternalSyntaxToken)childTree;
			    type += token.text();
			    if(">".equals(token.text())){
			    	break;
			    }
			}else if(childTree instanceof FlowObjectTypeTreeImpl){
				FlowObjectTypeTreeImpl flowObject = (FlowObjectTypeTreeImpl)childTree;
				type += getFlowObjectType(flowObject);
			}else if(childTree instanceof FlowUnionTypeTreeImpl){
				FlowUnionTypeTreeImpl flowUnion = (FlowUnionTypeTreeImpl)childTree;
				type += getFlowUnionType(flowUnion);
			}else if(childTree instanceof FlowGenericParameterClauseTreeImpl){
	    		FlowGenericParameterClauseTreeImpl flowGeneric = (FlowGenericParameterClauseTreeImpl)childTree;
	    		type += getFlowGenericParameter(flowGeneric);
	    	}else if(childTree instanceof FlowFunctionTypeTreeImpl){
	    		FlowFunctionTypeTreeImpl flowFunction = (FlowFunctionTypeTreeImpl)childTree;
	    		type += getFlowFunctionType(flowFunction);
	    	}else if(childTree instanceof FlowIntersectionTypeTreeImpl){
	    		FlowIntersectionTypeTreeImpl flowInter = (FlowIntersectionTypeTreeImpl)childTree;
		    	for(FlowTypeTree flowTypeTree : flowInter.subTypes()){
		    		type += flowTypeTree.firstToken().text();
		    		type += "&";
		    	}
		    	if(type.endsWith("&")){
		    	    type = type.substring(0, type.length() - 1);
		    	}
	    	}
			else if(childTree != null){
				type += childTree.toString();
			}
		}
		if(type.endsWith(" ")){
	    	type = type.substring(0, type.length() - 1);
	    }
		return type;
	}

	/*
	 * gain Union type
	 */
	private String getFlowUnionType(FlowUnionTypeTreeImpl flowUnion) {
		String type = "";
		StringBuilder typeBuilder = new StringBuilder();
	    for(FlowTypeTree flowTypeTree : flowUnion.subTypes()){
	    	if(flowTypeTree instanceof FlowArrayTypeWithKeywordTreeImpl){
	    		FlowArrayTypeWithKeywordTreeImpl flowArray = (FlowArrayTypeWithKeywordTreeImpl)flowTypeTree;
	    		typeBuilder.append(getFlowArrayType(flowArray));
	    	}else if(flowTypeTree instanceof FlowGenericParameterClauseTreeImpl){
	    		FlowGenericParameterClauseTreeImpl flowGeneric = (FlowGenericParameterClauseTreeImpl)flowTypeTree;
	    		type += getFlowGenericParameter(flowGeneric);
	    	}else if(flowTypeTree instanceof FlowObjectTypeTreeImpl){
				FlowObjectTypeTreeImpl flowObject = (FlowObjectTypeTreeImpl)flowTypeTree;
			    typeBuilder.append(getFlowObjectType(flowObject));
			}else if(flowTypeTree instanceof FlowFunctionTypeTreeImpl){
				FlowFunctionTypeTreeImpl flowFunction = (FlowFunctionTypeTreeImpl)flowTypeTree;
				typeBuilder.append(getFlowFunctionType(flowFunction));
			}
	    	else if(flowTypeTree != null){
			    typeBuilder.append(flowTypeTree.firstToken().text());
	    	}
			typeBuilder.append("|");
		}
	    if(typeBuilder.length() > 0){
	    	type += typeBuilder.substring(0, typeBuilder.length()-1);
	    }
	    if(type.endsWith(" ")){
	    	type = type.substring(0, type.length() - 1);
	    }
		return type;
	}

	/*
	 * gain flow Object type
	 */
	private String getFlowObjectType(FlowObjectTypeTreeImpl flowObject) {
		String type = "";
		Iterator<Tree> childrenTree = flowObject.childrenIterator();
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof InternalSyntaxToken){
				InternalSyntaxToken token = (InternalSyntaxToken)childTree;
				type += token.text();
				if("}".equals(token.text())){
					break;
				}
			}
			else if(childTree instanceof FlowPropertyDefinitionTreeImpl){
				FlowPropertyDefinitionTreeImpl flowProperty = (FlowPropertyDefinitionTreeImpl)childTree;
				type += getFlowProperty(flowProperty);
				
			}else if(childTree instanceof FlowIndexerPropertyDefinitionKeyTreeImpl){
				FlowIndexerPropertyDefinitionKeyTreeImpl flowIndexer = (FlowIndexerPropertyDefinitionKeyTreeImpl)childTree;
		        type += getFlowIndexerProperty(flowIndexer);
			}
		}
		if(type.endsWith(" ")){
	    	type = type.substring(0, type.length() - 1);
	    }
		return type;
	}

	private String getFlowProperty(FlowPropertyDefinitionTreeImpl flowProperty) {
		String type = "";
		Iterator<Tree> childrenTree = flowProperty.childrenIterator();
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof FlowSimplePropertyDefinitionKeyTreeImpl){
				FlowSimplePropertyDefinitionKeyTreeImpl flowSimple = (FlowSimplePropertyDefinitionKeyTreeImpl)childTree;
				type += getFlowSimpleProperty(flowSimple);
			}
			else if(childTree instanceof FlowTypeAnnotationTreeImpl){
				FlowTypeAnnotationTreeImpl flowType = (FlowTypeAnnotationTreeImpl)childTree;
				type += insertSpaceAfterColon(getFlowTypeAnnotation(flowType));
			}else if(childTree instanceof FlowMethodPropertyDefinitionKeyTreeImpl){
				FlowMethodPropertyDefinitionKeyTreeImpl flowMethod = (FlowMethodPropertyDefinitionKeyTreeImpl)childTree;
				type += getFlowMethodProperty(flowMethod);
			}
			else if(childTree instanceof FlowIndexerPropertyDefinitionKeyTreeImpl){
				FlowIndexerPropertyDefinitionKeyTreeImpl flowIndexer = (FlowIndexerPropertyDefinitionKeyTreeImpl)childTree;
		        type += getFlowIndexerProperty(flowIndexer);
			}
		}
		if(type.endsWith(" ")){
	    	type = type.substring(0, type.length() - 1);
	    }
		return type;
	}

	private String getFlowMethodProperty(FlowMethodPropertyDefinitionKeyTreeImpl flowMethod) {
		String type = "";
		Iterator<Tree> childrenTree = flowMethod.childrenIterator();
		while (childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof FlowFunctionTypeParameterClauseTreeImpl){
				FlowFunctionTypeParameterClauseTreeImpl flowFunction = (FlowFunctionTypeParameterClauseTreeImpl)childTree;
				type += getFlowFunctionTypeParameter(flowFunction);
			}else if(childTree != null){
				type += childTree.toString();
			}
		}
		if(type.endsWith(" ")){
	    	type = type.substring(0, type.length() - 1);
	    }
		return type;
	}

	private String getFlowLiteralType(FlowLiteralTypeTreeImpl flowLiteral) {
		String type = "";
		Iterator<Tree> childrenTree = flowLiteral.childrenIterator();
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree != null){
				type += childTree.toString();
			}
		}
		if(type.endsWith(" ")){
	    	type = type.substring(0, type.length() - 1);
	    }
		return type;
	}

	private String getFlowFunctionType(FlowFunctionTypeTreeImpl flowFunction) {
		String type = "";
		Iterator<Tree> childrenTree = flowFunction.childrenIterator();
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof InternalSyntaxToken){
				InternalSyntaxToken token = (InternalSyntaxToken)childTree;
				type += token.text();
			}else if(childTree instanceof FlowFunctionTypeParameterClauseTreeImpl){
				FlowFunctionTypeParameterClauseTreeImpl flowFunctionType = (FlowFunctionTypeParameterClauseTreeImpl)childTree;
				type += getFlowFunctionTypeParameter(flowFunctionType);
			}else if(childTree instanceof FlowOptionalTypeTreeImpl){
				FlowOptionalTypeTreeImpl flowOptional = (FlowOptionalTypeTreeImpl)childTree;
				type += getFlowOptionalType(flowOptional);
			}else if(childTree instanceof FlowSimpleTypeTreeImpl){
				FlowSimpleTypeTreeImpl flowSimple = (FlowSimpleTypeTreeImpl)childTree;
				type += flowSimple.toString();
			}else if(childTree instanceof FlowUnionTypeTreeImpl){
				FlowUnionTypeTreeImpl flowUnion = (FlowUnionTypeTreeImpl)childTree;
				type += getFlowUnionType(flowUnion);
			}else if(childTree instanceof FlowGenericParameterClauseTreeImpl){
				FlowGenericParameterClauseTreeImpl flowGeneric = (FlowGenericParameterClauseTreeImpl)childTree;
				type += getFlowGenericParameter(flowGeneric);
			}else if(childTree instanceof FlowFunctionTypeParameterClauseTreeImpl){
				FlowFunctionTypeParameterClauseTreeImpl flowFunctionType = (FlowFunctionTypeParameterClauseTreeImpl)childTree;
				type += getFlowFunctionTypeParameter(flowFunctionType);
			}else if(childTree instanceof FlowObjectTypeTreeImpl){
				FlowObjectTypeTreeImpl flowObject = (FlowObjectTypeTreeImpl)childTree;
				type += getFlowObjectType(flowObject);
			}else if(childTree instanceof FlowIndexerPropertyDefinitionKeyTreeImpl){
				FlowIndexerPropertyDefinitionKeyTreeImpl flowIndexer = (FlowIndexerPropertyDefinitionKeyTreeImpl)childTree;
		        type += getFlowIndexerProperty(flowIndexer);
			}
			else if(childTree != null){
				type += childTree.toString();
			}
		}
		if(type.endsWith(" ")){
	    	type = type.substring(0, type.length() - 1);
	    }
		return type;
	}

    private String getFlowGenericParameter(FlowGenericParameterClauseTreeImpl flowGeneric){
    	String type = "";
    	Iterator<Tree> iterator = flowGeneric.childrenIterator();
		while(iterator.hasNext()){
			Tree tree = iterator.next();
			if(tree instanceof InternalSyntaxToken){
				InternalSyntaxToken token = (InternalSyntaxToken)tree;
				type += token.text();
				if(">".equals(token.text())){
					break;
				}
			}
			else if(tree instanceof FlowGenericParameterTreeImpl){
				FlowGenericParameterTreeImpl flowGenericPara = (FlowGenericParameterTreeImpl)tree;
				type += flowGenericPara.firstToken().text();
				
			}else if(tree != null){
				type += tree.toString();
			}
		}
		if(type.endsWith(" ")){
	    	type = type.substring(0, type.length() - 1);
	    }
    	return type;
    }

	private String getFlowOptionalType(FlowOptionalTypeTreeImpl flowOptional) {
		String type = "";
		Iterator<Tree> childrenTree = flowOptional.childrenIterator();
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof FlowObjectTypeTreeImpl){
				FlowObjectTypeTreeImpl flowObject = (FlowObjectTypeTreeImpl)childTree;
			    type += getFlowObjectType(flowObject);
			}
			else if(childTree != null){
				type += childTree.toString();
			}
		}
		if(type.endsWith(" ")){
	    	type = type.substring(0, type.length() - 1);
	    }
		return type;
	}

	private String getFlowFunctionTypeParameter(FlowFunctionTypeParameterClauseTreeImpl flowFunctionType) {
		String type = "";
		StringBuilder paraBuilder = new StringBuilder();
		Iterator<Tree> paraIterator= flowFunctionType.childrenIterator();
		int size = flowFunctionType.parameters().size();
		if(size  == 0){
			return "()";
		}
		String[] paraName = new String[size];
		String[] paraType = new String[size];
		int index = 0; 
		for(int i = 0; i < size; ++i){
		    paraName[i] = "";
		    paraType[i] = "";
		}
		while(paraIterator.hasNext()){
			Tree paraTree = paraIterator.next();
			if(paraTree instanceof FlowFunctionTypeParameterTreeImpl){
				FlowFunctionTypeParameterTreeImpl parameter = (FlowFunctionTypeParameterTreeImpl)paraTree;
				Iterator<Tree> paraIter = parameter.childrenIterator();
				while(paraIter.hasNext()){
					Tree para = paraIter.next();
					if(para instanceof IdentifierTreeImpl){
						IdentifierTreeImpl identifier = (IdentifierTreeImpl)para;
						paraType[index] = "";
						paraName[index] = identifier.name();
					}
					else if(para instanceof FlowTypeAnnotationTreeImpl){
						FlowTypeAnnotationTreeImpl flowType = (FlowTypeAnnotationTreeImpl)para;
						paraType[index] += getFlowTypeAnnotation(flowType);
					}
					else if(para instanceof FlowSimpleTypeTreeImpl){
						paraName[index] = para.toString();
					}
				}
				index ++;
			}
			/*
			else if(paraTree != null){
			}*/
		}
		type += "(";
	    for(int i = 0; i < size; ++i){
	    	
	    	paraBuilder.append(paraName[i]);
	    	if(!"".equals(paraType[i])){
	    		paraBuilder.append(paraType[i]);
	    	}
	    	paraBuilder.append(", ");
	    }
	    if(paraBuilder.length() > 0){
	    	type += paraBuilder.substring(0, paraBuilder.length()-1);
	    }
	    type += ")";
		return type;
	}
	
	private String getFlowIndexerProperty(FlowIndexerPropertyDefinitionKeyTreeImpl flowIndexer){
		Iterator<Tree> childrenTree = flowIndexer.childrenIterator();
		String type = "";
		while(childrenTree.hasNext()){
			Tree childTree = childrenTree.next();
			if(childTree instanceof FlowUnionTypeTreeImpl){
				FlowUnionTypeTreeImpl flowUnion = (FlowUnionTypeTreeImpl)childTree;
				type += getFlowUnionType(flowUnion);
			}else if(childTree != null){
				type += childTree.toString();
			}
		}
		if(type.endsWith(" ")){
	    	type = type.substring(0, type.length() - 1);
	    }
		return type;
		
	}
	
	private PlanarRange getPosition(SyntaxToken from, SyntaxToken to) {
		return new PlanarRange(from.line()-1, from.column(), to.endLine()-1, to.endColumn());
	}

	private PlanarRange getPosition(IdentifierTreeImpl identifier) {
		return identifier != null ? getPosition(identifier.identifierToken()) : null;
	}

	private PlanarRange getPosition(SyntaxToken token) {
		return new PlanarRange(token.line() -1 , token.column(), token.endLine() -1, token.endColumn());
	}

	private void processBlockTree(BlockTreeImpl child, FlowScriptSymbol parent, List<FlowScriptSymbol> symbols) {
		for(StatementTree statement : child.statements()){
			processStatementTree(statement, parent, symbols);
		}
	}

	private void processStatementTree(StatementTree statement, FlowScriptSymbol parent,
			List<FlowScriptSymbol> symbols) {
		if (statement instanceof FunctionDeclarationTreeImpl) {
			processFunctionTree((FunctionDeclarationTreeImpl) statement, parent, symbols);
		} else if (statement instanceof VariableStatementTreeImpl) {
			VariableStatementTreeImpl variableStatement = (VariableStatementTreeImpl) statement;
			processVariableDeclaration(variableStatement.declaration(), parent, symbols);
		} else if (statement instanceof ExpressionStatementTreeImpl) {
			Tree expression = ((ExpressionStatementTreeImpl)statement).expression();
			if (expression instanceof ExpressionTree) {
				processExpressionTree((ExpressionTree) expression, parent, symbols);
			}
		} else if (statement instanceof BlockTreeImpl) {
			processBlockTree((BlockTreeImpl)statement, parent, symbols);
		} else if (statement instanceof ClassTreeImpl) {
			processClassTree((ClassTreeImpl) statement, parent, symbols);
		}else if(statement instanceof AssignmentExpressionTree){
			processAssignmentExpression((AssignmentExpressionTree)statement, parent, symbols);
		}
	}
	
	private String getName(SyntaxToken token) {
		return token != null ? removeQuotes(token.text()) : null;
    }
	
	private String removeQuotes(String name) {
		return StringUtils.stripEnd(StringUtils.stripStart(name, "'\""), "'\"");
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "js", "jsx") 
				&& !fileName.contains(".min.") 
				&& !fileName.contains("-min.")
				&& !fileName.contains("_min.");
	}

	@Override
	public int getVersion() {
		return 8;
	}
	
}
