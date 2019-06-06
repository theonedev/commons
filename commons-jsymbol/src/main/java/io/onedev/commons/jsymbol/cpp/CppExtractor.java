package io.onedev.commons.jsymbol.cpp;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTTypedefNameSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTArrayDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTArrayModifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTElaboratedTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTEnumerationSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTEnumerator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTEqualsInitializer;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFieldDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTLinkageSpecification;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamedTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamespaceDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTOperatorName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTParameterDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTPointer;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTProblem;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTProblemDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTReferenceOperator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateId;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateSpecialization;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTUsingDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTUsingDirective;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTVisibilityLabel;
import org.eclipse.core.runtime.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.cpp.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.CppSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.EnumSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.HeaderFileSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.MacroSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.MemberSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.NamespaceSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.SourceFileSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.SpecialFunctionSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.SpecialVariableSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.StructSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.TypedefSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.UnionSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.VariableSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.CppSymbol.Modifier;
/**
 * 
 * We relies on cdt to parse CPP header and source files, and then extract CPP symbols from the parse tree. Macro 
 * definition will be extracted at first.In addition,class,struct,union,namespace,enum and template will also be
 * extracted.What's more,some CPP14 characters are also be extracted,like atuo and constexpr.
 *
 * @Author: Yan
 */
@SuppressWarnings("deprecation")
public class CppExtractor extends AbstractSymbolExtractor<CppSymbol> {

	private static final Logger logger = LoggerFactory.getLogger(CppExtractor.class);
	
	@Override
	/*
	 * GPPLanguage is the most significant class and will parse source file
	 * GPPLanguage.OPTION_SKIP_FUNCTION_BODIES will skip function body;
	 * */
	public List<CppSymbol> extract(String fileName, String fileContent) {
		List<CppSymbol> symbols=new ArrayList<>();
		CppSymbol fileSymbol;
		if (fileName != null) {
			if (fileName.endsWith(".h") || fileName.endsWith(".hpp") || fileName.endsWith(".hxx"))
				fileSymbol = new HeaderFileSymbol(fileName);
			else
				fileSymbol = new SourceFileSymbol(fileName);
			symbols.add(fileSymbol);
		} else {
			fileSymbol = null;
		}
		if(null != fileContent){
		    Map<String, String> definedSymbols = new HashMap<String, String>();
		    String[] includePaths = new String[0];
		    IScannerInfo info = new ScannerInfo(definedSymbols, includePaths);
		    IParserLogService log = new DefaultLogService();
			int opts = GPPLanguage.OPTION_PARSE_INACTIVE_CODE;
		    opts += GPPLanguage.OPTION_SKIP_FUNCTION_BODIES;
			ICodeReaderFactory codeReaderFactory = null;
			IASTTranslationUnit translationUnit;
			try {
				CodeReader reader = new CodeReader("",fileContent.toCharArray());
				translationUnit = GPPLanguage.getDefault().getASTTranslationUnit(reader,info, codeReaderFactory, null, opts, log);
		        IASTPreprocessorMacroDefinition[] definition = translationUnit.getMacroDefinitions();
	            String macrodef = "";
	            MacroSymbol macro = null;
	            PlanarRange token = null;
	            boolean isLocal = findEnclosingHeader(fileSymbol) == null;
	            String temp = "";
	            int revise = 0,length = fileContent.length(), len = 0;
	            String[] strLine = fileContent.split("\\r?\\n");            
	            int[] strLineLength = new int[strLine.length];
	            for(int i = 0; i < strLine.length; ++i){
	            	strLineLength[i] = strLine[i].length();
	            	len += strLineLength[i];
	            	revise = len;
	            	while(len < length){
	            		temp = fileContent.substring(len,len+1);
	            		if("\r".equals(temp) || "\n".equals(temp)){
	            			++len;
	            		}
	            		else
	            			break;
	            	}
	            	revise = len - revise;
	            	strLineLength[i] += revise;
	            }
	            for(IASTPreprocessorMacroDefinition def : definition){
	       	        macrodef = def.toString();
	       	        isLocal = (hasStaticModifier(macrodef) || hasExternModifier(macrodef)) && isLocal;
	       	        macrodef = macrodef.substring(0,macrodef.indexOf("="));
	       	        token = getPosition(def.getName(),strLineLength);
	       	        if(macrodef.contains("(") && macrodef.contains(")")){
	       	        	SpecialFunctionSymbol function = new SpecialFunctionSymbol(fileSymbol,
	       	        			macrodef.substring(0,macrodef.indexOf("(")), isLocal, false,
	       	        			macrodef.substring(macrodef.indexOf("(")+1, macrodef.indexOf(")")), token, null, Modifier.MACRO);
	       	            symbols.add(function);
	       	        }
	       	        else{
	       	            macro = new MacroSymbol(fileSymbol, macrodef, isLocal, token, Modifier.NORMAL);
	       	            symbols.add(macro);
	       	        }
	            }
	            visitTree(translationUnit, 1 ,fileSymbol, symbols, 0, false, strLineLength, false);
			} catch (CoreException e) { 
				 // rethrow the exception as we can not continue with the parsing. Let caller handle it 
				throw new RuntimeException(e);
			}
		}
		return symbols;
	}
	private HeaderFileSymbol findEnclosingHeader(CppSymbol parentSymbol) {
	    while (parentSymbol != null && !(parentSymbol instanceof HeaderFileSymbol)) {
	        parentSymbol = parentSymbol.getParent();
	    }
	    return (HeaderFileSymbol) parentSymbol;
	}
    /*
     * visitTree method will distribute child node of the CPPASTTranslationUnit;
     * */   
    private void visitTree(IASTNode node, int index,CppSymbol fileSymbol, List<CppSymbol> symbols, int visibility, boolean isTemp, int []strLineLength, boolean isPrivate) {
	    IASTNode[] children = node.getChildren();
	   
	    for(IASTNode no:children){
	        if(no instanceof CPPASTFunctionDefinition)
	        {
	    	    visitFunction(no,fileSymbol,symbols,visibility,isTemp,strLineLength,isPrivate);
	        }
	        if(no instanceof CPPASTSimpleDeclaration && no.getChildren().length > 0){
	    	    IASTNode[] child = no.getChildren();
	    	    if(child[0] instanceof CPPASTFunctionDefinition){
	    		visitFunction(no,fileSymbol,symbols,visibility,isTemp,strLineLength,isPrivate);
	    	    }
	    	    else
	    	    {
	    		    if(child[0] instanceof CPPASTEnumerationSpecifier){
	    			    visitEnum(no,fileSymbol,symbols,visibility,isTemp,strLineLength,isPrivate);
	    		    }
	    		    if(child[0] instanceof CPPASTCompositeTypeSpecifier){
	    			    visitCompositeType(no,fileSymbol,symbols,visibility,isTemp,strLineLength,isPrivate);
	    		    }
	    		    if(judgeType(child[0])||child[0] instanceof CPPASTSimpleDeclaration){
	    			    if(child[0] instanceof CPPASTNamedTypeSpecifier){
	    			    	String nameType = no.getRawSignature();
	    			    	if(!(nameType.contains("(") && nameType.contains(")"))){
	    			    		visitGlobalVariable(no,fileSymbol,symbols,visibility,isTemp,strLineLength,isPrivate);
	    			    	}
	    			    }else{
	    		    	    visitGlobalVariable(no,fileSymbol,symbols,visibility,isTemp,strLineLength,isPrivate);
	    			    }
	    		    }
	    	    }
	         }
		     if(no instanceof CPPASTUsingDirective || no instanceof CPPASTUsingDeclaration){
		    	 visitGlobalVariable(no,fileSymbol,symbols,visibility,isTemp,strLineLength,isPrivate);
		     }
		     if(no instanceof CPPASTLinkageSpecification){
		    	 if(no.getChildren().length > 0){
		    		 visitTree(no, index, fileSymbol, symbols, visibility, isTemp, strLineLength,isPrivate);
		    	 }
		     }           
		     if(no instanceof CPPASTNamespaceDefinition){
		    	 visitNamespace(no,fileSymbol,symbols,visibility,isTemp,strLineLength,isPrivate);
		     }
		     if(no instanceof CPPASTTemplateDeclaration || no instanceof CPPASTTemplateSpecialization){
		    	 visitTemplateSymbol(no,fileSymbol,symbols,visibility,strLineLength,isPrivate);
		     }
			    
	         if(no instanceof CPPASTProblemDeclaration || no instanceof CPPASTProblem){
	        	 logger.trace("problem node:"+((no.getRawSignature().length()<20)?no.getRawSignature():no.getRawSignature().substring(0,20)));
	         }
		}
	    
      }
      /*
       * visitFunction method will parse function declaration in source file.
       * And some Special function declaration does not need type,such as constructor function.
       * */
      public void visitFunction(IASTNode nodeFunction,CppSymbol fileSymbol,List<CppSymbol> tempSymbols,int visibility, boolean isTemp, int []strLineLength, boolean isPrivate)
      {
       	  IASTNode[] node = nodeFunction.getChildren();
       	  String params = "", type = "", name = "", params1 = "";
       	  StringBuilder parameter = new StringBuilder();
       	  parameter.append("");
       	  PlanarRange token = null;
       	  PlanarRange scope = null;
       	  boolean variable = false;
       	  boolean definition = nodeFunction instanceof CPPASTFunctionDefinition;
       	  boolean varFun = false;
       	  boolean typedef = false;
       	  boolean isTypeNull = false;
       	  boolean isLocal = findEnclosingHeader(fileSymbol) == null;
       	  List<Modifier> modifiers = new ArrayList<>();
	      if(node.length > 0)
	      {
	    	  modifiers.add(getModifier(visibility));
	    	  visibility = getMethodType(nodeFunction,visibility);
	      }
	      scope = getScope(nodeFunction, strLineLength);
	      if(!definition){
	    	  scope = new PlanarRange(scope.getFromRow(), scope.getFromColumn(), scope.getToRow(), scope.getToColumn()-1);
	      }
       	  for(IASTNode ob : node)
       	  {
       		  if(judgeType(ob))
       		  {
       			  type = ob.getRawSignature();
       			  if("friend".equals(type)){
       				  visibility = 16;
       			  }
       			  if(type.contains("typedef "))
       			      typedef = true;
       			  String temp = removeStaAndExt(type);
				  while(!temp.equals(type) && !"".equals(temp)){
					  type = temp;
					  temp = removeStaAndExt(type);
				  }
				  type = temp;
       			  if(type.length() == 0)
       			  {
       				  isTypeNull = true;
       			  }
       			  if(type.equals("auto"))
       			  {
       				  isTypeNull = true;
       			  }
       			  isLocal = (isPrivate || hasStaticModifier(ob.getRawSignature()) || hasExternModifier(ob.getRawSignature())) && isLocal ;
       			  isPrivate = isPrivate ? !isPrivate : isPrivate;
       		  }
       		  if(ob instanceof CPPASTFunctionDeclarator)
       		  {
       			  IASTNode[] node1 = ((CPPASTFunctionDeclarator) ob).getChildren();
       			  for(IASTNode ob1 : node1)
       			  {
       				  if(ob1 instanceof CPPASTFunctionDeclarator)
       				  {
       					  name = getName(ob1);
       					  token = getPosition(getNode(ob1),strLineLength);
       					  params1 = getParameters(ob1);
       					  varFun = true;
       				  }
       				  if(isFunctionName(ob1)){
       					  name = ob1.getRawSignature();
       					  token = getPosition(ob1,strLineLength);
       				  }
       				  if(ob1 instanceof CPPASTReferenceOperator){
       					  type += ob1.getRawSignature();
       				  }
       				  if(ob1 instanceof CPPASTPointer)
       				  {
       					  type += ob1.getRawSignature();
       				  }
	        		  if(ob1 instanceof CPPASTDeclarator)
	        		  {
	        			  variable = true;
	        			  type += "(";
	        			  IASTNode[] node3 = ob1.getChildren();
	        			  for(IASTNode ob3 : node3)
	        			  {
	        				  if(ob3 instanceof CPPASTPointer)
	        				  {
	        					  type += ob3.getRawSignature();
	        				  }
	        				  if(isFunctionName(ob3))
	        				  {
	        					  name = ob3.getRawSignature();
	        					  token = getPosition(ob3,strLineLength);
	        				  }
	        				  if(ob3 instanceof CPPASTReferenceOperator)
	        				  {
	        					  type += ob3.getRawSignature();
	        				  }
	        			  }
	        			  type+= ")" ;
	        		  }
    				 
       				  if(ob1 instanceof CPPASTParameterDeclaration)
       				  {
       					  IASTNode[] node2 = ((CPPASTParameterDeclaration) ob1).getChildren();
       					  for(IASTNode ob2 : node2)
       					  {
       						  if(judgeType(ob2))
       						  {
       							  parameter.append(ob2.getRawSignature());
       						  }
       						  if(ob2 instanceof CPPASTDeclarator)
       						  {
       							  IASTNode[] node3 = ob2.getChildren();
       							  for(IASTNode ob3 : node3)
       							  {
       								  if(ob3 instanceof CPPASTPointer)
       								  {
       									  parameter.append(ob3.getRawSignature());
       								  }
       								  if(ob3 instanceof CPPASTEqualsInitializer)
       								  {
       									  parameter.append(ob3.getRawSignature());
       								  }
       								  if(ob3 instanceof CPPASTReferenceOperator)
       								  {
       									  parameter.append(ob3.getRawSignature());
       								  }
       							  }
       						  }
       						  if(ob2 instanceof CPPASTArrayDeclarator)
       						  {
       							  IASTNode[] node3 = ob2.getChildren();
       							  for(IASTNode ob3 : node3)
       							  {
       								  if(ob3 instanceof CPPASTArrayModifier)
       									  parameter.append(ob3.getRawSignature());
       							  }
       						  }
       					  }
       					  parameter.append(",");
       				  }
       			  }
       			  if(ob.getRawSignature().contains("..."))
       				  parameter.append("...,");
       			  if(parameter.toString().length()>0)
       				  params=parameter.toString().substring(0,parameter.toString().length()-1);
       		  }
       		  if(ob instanceof CPPASTProblemDeclaration || ob instanceof CPPASTProblem){
       			  logger.trace("problem node:"+((ob.getRawSignature().length()<20)?ob.getRawSignature():ob.getRawSignature().substring(0,20)));
       		  }
       	  }
       	  if(varFun)
       	  {
       		  String temp = params;
      		  params = "(";
      		  params += temp;
      		  params += ")";
       		  variable = false;
       		  type += params;
       		  params = params1; 
       	  }
       	  if(variable)
       	  {
       		  String temp = params;
       		  params = "(";
       		  params += temp;
       		  params += ")";
       		  type += params;
       		  
       		  if(!typedef)
       		  {
       			  VariableSymbol var = new VariableSymbol(fileSymbol, name, isLocal, type, token,modifiers,isTemp);
       			  tempSymbols.add(var);
       		  }
       		  else
       		  {
       			  TypedefSymbol ty = new TypedefSymbol(fileSymbol, name, isLocal, type, token, getModifier(visibility), isTemp);
       		      tempSymbols.add(ty);
       		  }
       	  }
       	  else
       	  {
       		  if(isTypeNull)
       		  {
       			  SpecialFunctionSymbol specialFunction = new SpecialFunctionSymbol(fileSymbol, name, isLocal, definition, params, token, scope, getModifier(visibility));
       			  tempSymbols.add(specialFunction);
       		  }
       		  else
       		  {
       			  
       			  FunctionSymbol function = new FunctionSymbol(fileSymbol, name, isLocal, definition, params, type, token, scope,getModifier(visibility),isTemp);
       			  tempSymbols.add(function);
       		  }
       	  }
    }
     /*
      * visitEnum method will parse enum declaration.
      * */
    public void visitEnum(IASTNode enumnode,CppSymbol fileSymbol, List<CppSymbol> tempSymbols,int visibility, boolean isTemp, int []strLineLength, boolean isPrivate){
    	IASTNode[] node = enumnode.getChildren();
    	List<CppSymbol> typeSymbols = new ArrayList<> ();
    	EnumSymbol symbol = null;
    	TypedefSymbol tsymbol = null; 
    	String name = "";
    	String temp = "";
    	MemberSymbol member = null;
    	PlanarRange token = null;
    	PlanarRange scope = null;
    	boolean isTypedef = false;
    	boolean isNameNull = false;
    	boolean isLocal = findEnclosingHeader(fileSymbol) == null;
    	String type = "";
    	List<Modifier> modifiers = new ArrayList<>();
    	Modifier modifier = getModifier(visibility);
     	modifiers.add(modifier);
    	for(IASTNode ob : node){
    		if(ob instanceof CPPASTEnumerationSpecifier){
    	    	scope = getScope(ob,strLineLength);//remove ;
            	String con = ob.getRawSignature();
            	con = con.indexOf("{")<0?con:con.substring(0,con.indexOf("{"));
                if(con.startsWith("typedef ") || con.contains(" typedef ")){
                	isTypedef = true;
            		type = removeStaAndExtInComp(con);
					temp = removeStaAndExtInComp(type);
					while(!temp.equals(type) && !"".equals(temp)){
						type = temp;
						temp = removeStaAndExtInComp(type);
					}
					type = temp;
                }
                if(2 == isStaOrFriFunction(ob.getRawSignature())){
                	modifier=getFriendModifier(visibility,ob.getRawSignature(),false);
                }
    	    	isNameNull = isNameNull(ob);
    			temp = ob.getRawSignature();
    			String tempStr = temp.indexOf("{")<0?temp:temp.substring(0,temp.indexOf("{"));
     			isLocal = (isPrivate || hasStaticModifier(tempStr) || hasExternModifier(tempStr)) && isLocal ;
     			isPrivate = isPrivate ? !isPrivate:isPrivate;
     			IASTNode[] node1 = ob.getChildren();
    			for(IASTNode ob1 : node1){
    				if(ob1 instanceof CPPASTName || ob1 instanceof CPPASTTemplateId){
    	    	    	if(isNameNull){
    	    	    		name = "(anonymous)";
    	    	    		int toCh = getAnonymousTextRange(ob.getRawSignature(),scope.getFromColumn());
    	    	    		token = new PlanarRange(scope.getFromRow(),scope.getFromColumn(),scope.getToRow(), toCh);
    	    	    	    symbol = new EnumSymbol(fileSymbol, name, isLocal, token, scope,modifier, isTemp);
    	    	    	    tempSymbols.add(symbol);
    	    	    	}
    	    	    	else{
    					    token = getPosition(ob1,strLineLength);
    					    type += ob1.getRawSignature();
    					    symbol = new EnumSymbol(fileSymbol, ob1.getRawSignature(), isLocal, token,scope,modifier, isTemp);
    				        tempSymbols.add(symbol);
    	    	    	}
    	    	    	if(isTypedef && 2 <= node.length){
    	    	    		token = getPosition(node[1],strLineLength);
    	    	    		tsymbol = new TypedefSymbol(fileSymbol, node[1].getRawSignature(), isLocal, type, token,getModifier(visibility), isTemp);
    	    	    	    typeSymbols.add(tsymbol);
    	    	    	}
    				}
    				if(ob1 instanceof CPPASTEnumerator && null != ob1){
    					token = getPosition(getNode(ob1),strLineLength);
    					name = getName(ob1);
    					if(isTypedef && !isSymbolNull(tsymbol)){
    					    member = new MemberSymbol(tsymbol, name, null, token,getModifier(visibility));
    					    typeSymbols.add(member);
    					}
    					if(!isSymbolNull(symbol)){
    						member = new MemberSymbol(symbol, name, null, token,getModifier(visibility));
    						tempSymbols.add(member);
    					}
    				}
    			}
    		}
    		if(ob instanceof CPPASTDeclarator){
    			if(!isNameNull && !isTypedef){
    				token = getPosition(ob,strLineLength);
    				VariableSymbol variable = new VariableSymbol(fileSymbol, ob.getRawSignature(), isLocal, type, token, modifiers, isLocal);
    			    tempSymbols.add(variable);
    			}
    		}
    		if(ob instanceof CPPASTProblemDeclaration || ob instanceof CPPASTProblem){
    			logger.trace("Problem node:"+((ob.getRawSignature().length()<20)?ob.getRawSignature():ob.getRawSignature().substring(0,20)));
    		}
    	}
		if(isTypedef && !typeSymbols.isEmpty()){
			tempSymbols.addAll(typeSymbols);
		}
    }
    /*
     * judgeType method is assisted.We will use it in some conditional judgments;
     * */
    public boolean judgeType(IASTNode ob){
    	if(ob instanceof CPPASTSimpleDeclSpecifier || 
    	   ob instanceof CPPASTElaboratedTypeSpecifier || 
    	   ob instanceof CASTTypedefNameSpecifier || 
           ob instanceof CPPASTName || 
           ob instanceof CPPASTTemplateId ||
    	   ob instanceof CPPASTNamedTypeSpecifier){
    		return true;
    	}
    	else
    		return false;
    }
    /*
     * judgeVariableType method is be used for parsing variable declaration.
     * And it also mainly used for conditional judgment.
     * */
    public boolean judgeVariableType(IASTNode ob){
    	if(ob instanceof CPPASTSimpleDeclSpecifier || 
    	    	   ob instanceof CPPASTElaboratedTypeSpecifier || 
    	    	   ob instanceof CASTTypedefNameSpecifier || 
    	           ob instanceof CPPASTName || 
    	    	   ob instanceof CPPASTNamedTypeSpecifier)
    	    	{
    	    		return true;
    	    	}
    	    	else
    	    		return false;
    	
    }
    /*
     * judgeTypeInComp method is be used for parsing Composition declaration.
     * And it also mainly used for conditional judgment.
     * */
    public boolean judgeTypeInComp(IASTNode ob){
    	if(ob instanceof CPPASTSimpleDeclSpecifier || 
    	   ob instanceof CPPASTElaboratedTypeSpecifier || 
    	   ob instanceof CASTTypedefNameSpecifier || 
    	   ob instanceof CPPASTName || 
    	   ob instanceof CPPASTNamedTypeSpecifier)
    	{
    		return true;
    	}
    	else
    		return false;
    }
    /*
     * visitGlobalVariable method is be used for parsing variable declaration.
     * And it  also used for special declaration,such as class cla; 
     * */
    public void visitGlobalVariable(IASTNode globalnode,CppSymbol fileSymbol,List<CppSymbol> tempSymbols,int visibility, boolean isTemp, int []strLineLength, boolean isPrivate){
    	IASTNode[] node = globalnode.getChildren();
    	VariableSymbol variable = null;
    	TypedefSymbol tsymbol = null;
    	SpecialVariableSymbol specialVariable = null;
    	String type = "";
    	String getType = "";
    	String name = "";
    	String temp = "";
    	boolean function = false;
    	boolean isFriend = false;
    	int backup = visibility;
    	boolean privateBackup = isPrivate;
    	PlanarRange token = null;
    	boolean isTypedef = false;
    	boolean isLocal = findEnclosingHeader(fileSymbol) == null;
    	Modifier modifier = Modifier.NORMAL;
    	List<Modifier> modifiers = new ArrayList<>();
    	
    	if(node.length > 0)
    	{  
    		modifier=getModifier(visibility);
    		modifiers.add(modifier);
    		visibility = getVariableType(node[0],visibility);
    		if(1 == (1 & isConOrStaVariable(node[0].getRawSignature())) || 4 == (4 & isConOrStaVariable(node[0].getRawSignature()))){
    			modifiers.add(Modifier.CONSTANT);
    		}
    		if(visibility == 20){
    			modifier=getFriendModifier(visibility,node[0].getRawSignature(),true);
    		    modifiers.add(modifier);
    			isFriend = true;
                if(Modifier.FRIENDVAR == modifier){
    		        isFriend=false;	
    		    }
    		}
    	}
    	for(IASTNode ob : node)
    	{
    		getType = "";
    		if(ob instanceof CPPASTFunctionDeclarator)
    		{
    			function = true;
    			visibility = backup;
    			isPrivate = privateBackup;
    			break;
    		}
    		if(globalnode instanceof CPPASTUsingDirective || globalnode instanceof CPPASTUsingDeclaration)
    		{
     			isLocal = (isPrivate || hasStaticModifier(globalnode.getRawSignature()) || hasExternModifier(globalnode.getRawSignature())) && isLocal ;
    			isPrivate = isPrivate ? !isPrivate : isPrivate;
     			visibility = getVariableType(globalnode,visibility);
    			name = getUsingName(globalnode);
    			token = getPosition(getUsingNameNode(globalnode),strLineLength);
    			specialVariable = new SpecialVariableSymbol(fileSymbol, name, isLocal, token, modifier);
    	    	tempSymbols.add(specialVariable);
    	    	break;
    		}
    		if(ob instanceof CPPASTFieldDeclarator){
    			type += ob.getRawSignature();
    		}
    		if(judgeVariableType(ob)){
     			isLocal = (isPrivate || hasStaticModifier(ob.getRawSignature()) || hasExternModifier(ob.getRawSignature())) && isLocal ;
    			isPrivate = isPrivate ? !isPrivate :isPrivate;
     			if(ob.getRawSignature().startsWith("typedef "))
    			{
    				isTypedef = true;
    			}
				type = ob.getRawSignature();
				temp = removeStaAndExt(type);
				while(!temp.equals(type) && !"".equals(temp)){
					type = temp;
					temp = removeStaAndExt(type);
				}
				type = temp;
				if("".equals(temp) && ob.getChildren().length > 0)
				{
					name = ob.getChildren()[0].getRawSignature();
					token = getPosition(ob.getChildren()[0],strLineLength);
					specialVariable = new SpecialVariableSymbol(fileSymbol, name, isLocal, token, modifier);
		    		tempSymbols.add(specialVariable);
				}
			}
    		else{
				if(ob instanceof CPPASTDeclarator){
					IASTNode[] node1 = ob.getChildren();
					if(node1.length > 1)
					{
						for(IASTNode ob1 : node1){
							if(ob1 instanceof CPPASTPointer){
								getType += ob1.getRawSignature();
							}
							if(ob1 instanceof CPPASTName){
								name = ob1.getRawSignature();
								token = getPosition(ob1, strLineLength);
							}
                            if(ob1 instanceof CPPASTReferenceOperator){
                            	type += ob1.getRawSignature();
                            }							
						}
					}
					else
					{
						if(1 == node1.length){
							name = node1[0].getRawSignature();
							token = getPosition(node1[0],strLineLength);
						}else{
							name = ob.getRawSignature();
							token = getPosition(ob,strLineLength);
						}
					}
				}
				if(ob instanceof CPPASTArrayDeclarator){
					IASTNode[] node2 = ob.getChildren();
					for(IASTNode ob2 : node2){
						if(ob2 instanceof CPPASTName){
							name = ob2.getRawSignature();
							token = getPosition(ob2, strLineLength);
						}
						if(ob2 instanceof CPPASTArrayModifier){
							getType += ob2.getRawSignature();
						}
					}
				}
				
				temp = "";
			    temp += type;
			    temp += getType;
			    if("".equals(name)){
			    	name = "(anonymous)";
			    }
			    if(null == token && 0 < globalnode.getChildren().length){
			    	token = getPosition(globalnode.getChildren()[0],strLineLength);
			    }
	    		boolean isSkip = false;
			    if(name.contains("(") && name.contains(")")&& !"(anonymous)".equals(name)){
	    		    isSkip = true;
			    }
			    if(!isSkip){
				    if(isTypedef)
				    {
				    	tsymbol = new TypedefSymbol(fileSymbol, name, isLocal, temp, token, getModifier(visibility), isTemp);
				    	tempSymbols.add(tsymbol);
				    }
				    else
				    {
				    	if(isFriend){
				    		specialVariable = new SpecialVariableSymbol(fileSymbol, name, isLocal, token, modifier);
				    		tempSymbols.add(specialVariable);
				    	}
				    	else{
				    		variable = new VariableSymbol(fileSymbol, name, isLocal, temp, token, modifiers,isTemp);
				    		tempSymbols.add(variable);
				    	}
				    }
			    }
    		}
    	    if(ob instanceof CPPASTProblemDeclaration || ob instanceof CPPASTProblem){
    	    	logger.trace("Problem node:"+((ob.getRawSignature().length()<20)?ob.getRawSignature():ob.getRawSignature().substring(0,20)));
    	    }
    	}
    	if(function)
    	{
    		visitFunction(globalnode,fileSymbol,tempSymbols,visibility,isTemp,strLineLength,isPrivate);
    	}
    }
    /*
     * removeStaAndExtInComp method is used for removing additional string of function'type,
     * such as static and extern and so on.
     * */
    public String removeStaAndExtInComp(String type){
    	if(!"".equals(type))
    	{
    		if(type.startsWith("static "))
	    	{
	    		type = type.substring(7);
	    		return type;
	    	}
	    	if(type.startsWith("extern "))
	    	{
	    		type = type.substring(7);
	    		return type;
	    	}
	    	if(type.startsWith("typedef "))
	    	{
	    		type = type.substring(8);
	    		return type;
	    	}
	    	if(type.startsWith("class "))
	    	{
	    		type = type.substring(6);
	    		type = "";
	    		return type;
	    	}
	    	if(type.startsWith("inline "))
	    	{
	    		type = type.substring(7);
	    	}
	    	if(type.startsWith("virtual "))
	    	{
	    		type = type.substring(8);
	    		return type;
	    	}
	    	if(type.startsWith("virtual") && type.length() == 7)
	    	{
	    		type = "";
	    		return type;
	    	}
	    	
	    	if(type.startsWith("struct{") || type.startsWith("struct "))
	    	{
	    		type = "struct ";
	    		return type;
	    	}
	    	if(type.startsWith("union{") || type.startsWith("union "))
	    	{
	    		type = "union ";
	    		return type;
	    	}
	    	if(type.startsWith("class{"))
	    	{
	    		type = "class ";
	    		return type;
	    	}
	    	if(type.startsWith("enum{") || type.startsWith("enum "))
	    	{
	    		type = "enum ";
	    		return type;
	    	}
	    	if(type.startsWith("friend "))
	    	{
	    		type = type.substring(7);
	    		return type;
	    	}
	    	if(type.startsWith("const static ") || type.startsWith("static const ")){
	    		String temp = "const ";
	    		type = type.substring(13);
	    		temp += type;
	    		type = temp;
	    	}
    	}
    	return type;
    }
    /*
     * removeStaAndExt is used for removing additional string of variable and
     * function type.
     * */
    public String removeStaAndExt(String type){
    	if(!"".equals(type))
    	{
    		if(type.startsWith("static "))
	    	{
	    		type = type.substring(7);
	    		return type;
	    	}
	    	if(type.startsWith("extern "))
	    	{
	    		type = type.substring(7);
	    		return type;
	    	}
	    	if(type.startsWith("typedef "))
	    	{
	    		type = type.substring(8);
	    		return type;
	    	}
	    	if(type.startsWith("class "))
	    	{
	    		type = type.substring(6);
	    		type = "";
	    		return type;
	    	}
	    	if(type.startsWith("inline "))
	    	{
	    		type = type.substring(7);
	    	}
	    	if(type.startsWith("virtual "))
	    	{
	    		type = type.substring(8);
	    		return type;
	    	}
	    	if(type.startsWith("virtual") && type.length() == 7)
	    	{
	    		type = "";
	    		return type;
	    	}
	    	if(type.startsWith("friend "))
	    	{
	    		type = type.substring(7);
	    		return type;
	    	}
	    	if("friend".equals(type)){
	    		type = "";
	    		return type;
	    	}
	    	if(type.startsWith("const static ") || type.startsWith("static const ")){
	    		String temp = "const ";
	    		type = type.substring(13);
	    		temp += type;
	    		type = temp;
	    	}
    	}
    	return type;
    }
    /*
     * visitCompositeType method will extract composition declaration,such as class,
     * struct and union declaration.
     * */
    public void visitCompositeType(IASTNode structnode,CppSymbol fileSymbol,List<CppSymbol> tempSymbols,int visibility, boolean isTemp, int []strLineLength, boolean isPrivate){
    	IASTNode[] node = structnode.getChildren();
    	StructSymbol struct = null;
    	VariableSymbol variable = null;
    	UnionSymbol union = null;
    	TypedefSymbol tsymbol = null;
    	ClassSymbol classSymbol = null;
    	CppSymbol symbol = null;
    	String type = "";
    	String name = "";
    	String tname = "";
    	String temp = "";
    	boolean isTypedef = false;
    	int tempVisibility = 0;
    	int judge = 0;
    	boolean isNameNull = false;
        PlanarRange token = null;
    	List<CppSymbol> typesymbols = new ArrayList<>();
    	boolean isLocal = findEnclosingHeader(fileSymbol) == null;
    	PlanarRange scope = null;
    	List<Modifier> modifiers = new ArrayList<>();
    	Modifier modifier = getModifier(visibility);
    	for(IASTNode ob : node){
    		if(ob instanceof CPPASTCompositeTypeSpecifier){
    	        scope = getScope(ob,strLineLength);
    			temp = ob.getRawSignature();
    			if(0 > temp.indexOf("{")){
    				break;
    			}
    			if(2 == isStaOrFriFunction(ob.getRawSignature())){
    				modifier = getFriendModifier(visibility,ob.getRawSignature(),false);
    			}
    			String tempStr = temp.indexOf("{")<0?temp:temp.substring(0,temp.indexOf("{"));
     			isLocal = (isPrivate || hasStaticModifier(tempStr) || hasExternModifier(tempStr)) && isLocal ;
    			isPrivate = isPrivate ? !isPrivate :isPrivate;
     			IASTNode[] node1 = ob.getChildren();
    			if(0 < node.length){
    		  	    isNameNull = isNameNull(node[0]);
    			}
    			String con = tempStr;
    			if(con.startsWith("struct ") || con.contains(" struct ")){
    				type = "struct ";
    				judge = 0;
    			}
    			if(con.startsWith("union ") || con.contains(" union ")){
    				type = "union ";
    				judge = 1;
    			}
    			if(con.startsWith("class ") || con.contains(" class ")){
    				type = "class ";
    				judge = 2;
    				tempVisibility = 1;
    				isPrivate = true;
    			}
    			if(con.startsWith("typedef ") || con.contains(" typedef ")){
    				isTypedef = true;
					type = removeStaAndExtInComp(con);
					if(type.startsWith("struct"))
						tempVisibility = 16;
					if(type.startsWith("union"))
						tempVisibility = 16;
					temp = removeStaAndExtInComp(type);
					while(!temp.equals(type) && !"".equals(temp)){
						type = temp;
						temp = removeStaAndExtInComp(type);
					}
					type = temp;
    			}
    			for(IASTNode ob1 : node1){
    				if(ob1 instanceof CPPASTVisibilityLabel){
    					tempVisibility = getVisibility(ob1.getRawSignature());
    					isPrivate = hasPrivateModifier(ob1.getRawSignature());
    				}
        			if(ob1 instanceof CPPASTName || ob1 instanceof CPPASTTemplateId){
        				if(isNameNull){
        					name = "(anonymous)";
        					int toCh = getAnonymousTextRange(ob.getRawSignature(),scope.getFromColumn());
        					token = new PlanarRange(scope.getFromRow(),scope.getFromColumn(),scope.getFromRow(),toCh);
        				}
        				else{
    					    name = ob1.getRawSignature();
    					    token = getPosition(ob1,strLineLength);
    					    type += name;
        				}
    					if(isTypedef && node.length >= 2){
    						tname = node[1].getRawSignature();
    						token = getPosition(node[1], strLineLength);
        					tsymbol = new TypedefSymbol(fileSymbol, tname, isLocal, type, token, modifier, isTemp);
        					typesymbols.add(tsymbol);
        				}
	    				if(judge == 0){
	    					tempVisibility = 3;
	    					struct = new StructSymbol(fileSymbol, name, isLocal, token, scope, modifier, isTemp);
	    					symbol = struct;
	    				}
	    				if(judge == 1){
	    					tempVisibility = 3;
	    					union = new UnionSymbol(fileSymbol, name, isLocal, token, scope, modifier, isTemp);
	    					symbol = union;
	    				}
	    				if(judge == 2){
	    					classSymbol = new ClassSymbol(fileSymbol, name, isLocal, token, scope, modifier, isTemp);	
	    					symbol = classSymbol;
	    				}
	    				tempSymbols.add(symbol);
    				}
    				if(ob1 instanceof CPPASTSimpleDeclaration){
    				//	token = getPosition(ob1,strLineLength);
    					IASTNode []node2 = ob1.getChildren();
    					for(IASTNode ob2 : node2){
    						if(ob2 instanceof CPPASTCompositeTypeSpecifier){
    	    					if(isTypedef && !isSymbolNull(tsymbol)){
    	    						visitCompositeType(ob1,tsymbol,typesymbols,tempVisibility,false,strLineLength,isPrivate);	
    	    					}
    	    					if(!isSymbolNull(symbol)){
    	    						visitCompositeType(ob1,symbol,tempSymbols,tempVisibility,false,strLineLength,isPrivate);
    	    					}
    						}
                            if(ob2 instanceof CPPASTFunctionDefinition){
    	    					if(isTypedef && !isSymbolNull(tsymbol)){
    	    						visitFunction(ob1,tsymbol,typesymbols,tempVisibility, false,strLineLength,isPrivate);	
    	    					}
    	    					if(!isSymbolNull(symbol)){
    	    						visitFunction(ob1,symbol,tempSymbols,tempVisibility,false,strLineLength,isPrivate);
    	    					}
                            }
                            if(ob2 instanceof CPPASTEnumerationSpecifier){
    	    					if(isTypedef && !isSymbolNull(tsymbol)){
    	    						visitEnum(ob1,tsymbol,typesymbols,tempVisibility,false,strLineLength,isPrivate);	
    	    					}
    	    					if(!isSymbolNull(symbol)){
    	    						visitEnum(ob1,symbol,tempSymbols,tempVisibility,false,strLineLength,isPrivate);
    	    					}
                            }
                			if(judgeType(ob2)){
    							if(isTypedef && !isSymbolNull(tsymbol)){
    	    						visitGlobalVariable(ob1,tsymbol,typesymbols,tempVisibility,false,strLineLength,isPrivate);
    	    					}
    	    					if(!isSymbolNull(symbol)){
    	    						visitGlobalVariable(ob1,symbol,tempSymbols,tempVisibility,false,strLineLength,isPrivate);
    	    					}
    	    				}
    					}
    				}
    				if(ob1 instanceof CPPASTFunctionDefinition){
    					if(isTypedef && !isSymbolNull(tsymbol)){
    						visitFunction(ob1,tsymbol,typesymbols,tempVisibility,false,strLineLength,isPrivate);	
    					}
    					if(!isSymbolNull(symbol)){
    					    visitFunction(ob1,symbol,tempSymbols,tempVisibility,false,strLineLength,isPrivate);
    					}
    				}
    				if(ob1 instanceof CPPASTLinkageSpecification){
    					if(isTypedef && !isSymbolNull(tsymbol)){
    						visitTree(ob1, 1, tsymbol, typesymbols, tempVisibility, false, strLineLength,isPrivate);
    					}
    					if(!isSymbolNull(symbol)){
                            visitTree(ob1 , 1, symbol, tempSymbols, tempVisibility, false, strLineLength,isPrivate);    	
    					}
    				}
    				if(ob1 instanceof CPPASTTemplateDeclaration){
    	    			if(isTypedef && !isSymbolNull(tsymbol)){
    						visitTemplateSymbol(ob1,tsymbol,typesymbols,tempVisibility,strLineLength,isPrivate);	
    					}
    					if(!isSymbolNull(symbol)){
    						visitTemplateSymbol(ob1,symbol,tempSymbols,tempVisibility,strLineLength,isPrivate);
    					}
    	    		}
    				if(ob1 instanceof CPPASTUsingDirective || ob1 instanceof CPPASTUsingDeclaration){
    					if(isTypedef && !isSymbolNull(tsymbol)){
    						visitGlobalVariable(ob1,tsymbol,tempSymbols,0,false,strLineLength,isPrivate);
    					}
    					if(!isSymbolNull(symbol)){
    					    visitGlobalVariable(ob1,symbol,tempSymbols,0,false,strLineLength,isPrivate);
    					}
    			    }
    			    
    			    if(ob1 instanceof CPPASTNamespaceDefinition){
    			    	if(isTypedef && !isSymbolNull(tsymbol)){
    			    		visitNamespace(ob1,tsymbol,tempSymbols,tempVisibility,false,strLineLength,isPrivate);
    					}
    			    	if(!isSymbolNull(symbol)){
    			    	    visitNamespace(ob1,symbol,tempSymbols,tempVisibility,false,strLineLength,isPrivate);
    			    	}
    			    }
    			}
    		}
    		if(ob instanceof CPPASTDeclarator){
    			temp = ob.getRawSignature();
    			token = getPosition(ob,strLineLength);//class{}a;
    			modifiers.add(getModifier(visibility));
    			if(!isTypedef && !isNameNull){
    				variable = new VariableSymbol(fileSymbol, temp, isLocal, type, token, modifiers,isTemp);
    				tempSymbols.add(variable);
    			}
    		}
    		if(ob instanceof CPPASTProblemDeclaration || ob instanceof CPPASTProblem){
    			logger.trace("Problem node:"+((ob.getRawSignature().length()<20)?ob.getRawSignature():ob.getRawSignature().substring(0,20)));
    		}
    	}
    	if(isTypedef){
    		for(CppSymbol symbol1 : typesymbols){
    			tempSymbols.add(symbol1);
    		}
    	}
    }
    /*
     * visitNamespace method will extract namespace declaration.
     * */
    public void visitNamespace(IASTNode nameNode, CppSymbol fileSymbol, List<CppSymbol> tempSymbols, int visibility, boolean isTemp, int []strLineLength, boolean isPrivate){
    	IASTNode[] node = nameNode.getChildren();
    	NamespaceSymbol nameSymbol = null;
    	String name = "";
    	String temp = "";
        PlanarRange token = null;
        PlanarRange scope = null;
        scope = getScope(nameNode,strLineLength);
        temp = nameNode.getRawSignature();
        boolean isLocal = findEnclosingHeader(fileSymbol) == null;
        String tempStr = temp.indexOf("{")<0?temp:temp.substring(0,temp.indexOf("{"));
	    isLocal = (isPrivate || hasStaticModifier(tempStr) || hasExternModifier(tempStr)) && isLocal ;
    	isPrivate = isPrivate ?!isPrivate:isPrivate;
    	Modifier modifier = getModifier(visibility);
    	if(2 == isStaOrFriFunction(nameNode.getRawSignature())){
    		modifier = getFriendModifier(visibility,nameNode.getRawSignature(),false);
     	}
	    for(IASTNode ob : node){
    		if(ob instanceof CPPASTName || ob instanceof CPPASTTemplateId){
    			name = ob.getRawSignature();
    			if(0 > temp.indexOf("{")){
    				break;
    			}
    			if("".equals(name)){
    				name = "(anonymous)";
    				int toCh = getAnonymousTextRange(nameNode.getRawSignature(),scope.getFromColumn());
    			    token = new PlanarRange(scope.getFromRow(),scope.getFromColumn(),scope.getFromRow(),toCh);
    			}else
    			{
    			    token = getPosition(ob,strLineLength);
    			}
    			nameSymbol = new NamespaceSymbol(fileSymbol, name, isLocal, token, scope, modifier, isTemp);
    			tempSymbols.add(nameSymbol);
    		}
    		if(ob instanceof CPPASTSimpleDeclaration && ob.getChildren().length > 0){
    			IASTNode[] child = ob.getChildren();
    			if(child[0] instanceof CPPASTFunctionDefinition){
    			    visitFunction(ob,nameSymbol,tempSymbols,0,false,strLineLength,isPrivate);
    			}
    			else
    			{
    			    if(child[0] instanceof CPPASTEnumerationSpecifier){ 
    			        visitEnum(ob,nameSymbol,tempSymbols,0,false,strLineLength,isPrivate);
    			    }
    			    if(child[0] instanceof CPPASTCompositeTypeSpecifier){
    			    	visitCompositeType(ob,nameSymbol,tempSymbols,0,false,strLineLength,isPrivate);
    			    }
    			    if(judgeType(child[0])){
    			    	visitGlobalVariable(ob,nameSymbol,tempSymbols,0,false,strLineLength,isPrivate);
    			    }
    			}
    		}
    		if(ob instanceof CPPASTFunctionDefinition){
    		    visitFunction(ob, nameSymbol, tempSymbols, 0,false,strLineLength,isPrivate);
    		}
    		if(ob instanceof CPPASTLinkageSpecification){
    			visitTree(ob, 1, nameSymbol, tempSymbols, 0, false, strLineLength,isPrivate);
    		}
    		if(ob instanceof CPPASTTemplateDeclaration){
    			visitTemplateSymbol(ob,nameSymbol,tempSymbols,0,strLineLength,isPrivate);
    		}
    		if(ob instanceof CPPASTUsingDirective || ob instanceof CPPASTUsingDeclaration){
    			visitGlobalVariable(ob,nameSymbol,tempSymbols,0,false,strLineLength,isPrivate);
    	    }
    	    if(ob instanceof CPPASTNamespaceDefinition){
    			visitNamespace(ob,nameSymbol,tempSymbols,0,false,strLineLength,isPrivate);
    	    }
    	    if(ob instanceof CPPASTProblemDeclaration || ob instanceof CPPASTProblem){
    	    	logger.trace("Problem node:"+((ob.getRawSignature().length()<20)?ob.getRawSignature():ob.getRawSignature().substring(0,20)));
    	    }
    	}
    }
    /*
     * visitTemplateSymbol method will extract template declarations.
     * */
    public void visitTemplateSymbol(IASTNode tempnode,CppSymbol fileSymbol,List<CppSymbol> tempSymbols, int visibility, int []strLineLength, boolean isPrivate){
    	IASTNode []node = tempnode.getChildren();
    	for(IASTNode ob : node){
    		if(ob instanceof CPPASTFunctionDefinition)
    			visitFunction(ob,fileSymbol,tempSymbols,visibility,true,strLineLength,isPrivate);
    		if(ob instanceof CPPASTSimpleDeclaration && ob.getChildren().length > 0){
    	    	IASTNode[] child = ob.getChildren();
    	    	if(child[0] instanceof CPPASTFunctionDefinition){
    	    		visitFunction(ob,fileSymbol,tempSymbols,visibility,true,strLineLength,isPrivate);
    	    	}
    	    	else
    	    	{
    	    		if(child[0] instanceof CPPASTEnumerationSpecifier){
    	    			visitEnum(ob,fileSymbol,tempSymbols,visibility,true,strLineLength,isPrivate);
    	    		}
    	    		if(child[0] instanceof CPPASTCompositeTypeSpecifier){
    	    			visitCompositeType(ob,fileSymbol,tempSymbols,visibility,true,strLineLength,isPrivate);
    	    		}
    	    		if(judgeType(child[0])){
    	    			visitGlobalVariable(ob,fileSymbol,tempSymbols,visibility,true,strLineLength,isPrivate);
    	    		}
    	    	}
    	    }
    		if(ob instanceof CPPASTNamespaceDefinition){
    	    	visitNamespace(ob,fileSymbol,tempSymbols,visibility,true,strLineLength,isPrivate);
    	    }
    		if(ob instanceof CPPASTLinkageSpecification){
    			visitTree(ob,1,fileSymbol,tempSymbols,0,true,strLineLength,isPrivate);
    		}
    		if(ob instanceof CPPASTUsingDirective || ob instanceof CPPASTUsingDeclaration){
    	    	visitGlobalVariable(ob,fileSymbol,tempSymbols,0,true,strLineLength,isPrivate);
    	    }
    		if(ob instanceof CPPASTProblemDeclaration || ob instanceof CPPASTProblem){
    			
    			logger.trace("Problem node:"+((ob.getRawSignature().length()<20)?ob.getRawSignature():ob.getRawSignature().substring(0,20)));
    		}
    	}    	
    }
    
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "h", "c", "cpp", "cc", "hpp", "hxx", "cxx");
	}
	/*
	 * getType method will gain Pointers.
	 * */
    public String getType(String type,IASTNode nodeFunction)
    {
    	IASTNode[] node = nodeFunction.getChildren();
    	type +="(";
    	for(IASTNode ob : node)
    	{
    		if(ob instanceof CPPASTPointer)
    		{
    			type += ob.getRawSignature();
    		}
    	}
    	type += ")";
    	return type;
    }
    /*
     * getName method will gain name of declaration.
     * */
    public String getName(IASTNode nodeFunction)
    {
    	IASTNode[] node = nodeFunction.getChildren();
    	for(IASTNode ob : node)
    	{
    		if(ob instanceof CPPASTName)
    		{
    			return ob.getRawSignature();
    		}
    	}
    	return "";
    }
    /*
     * getNode method will gain CPPASTName
     * */
    public IASTNode getNode(IASTNode nodeFunction){
    	IASTNode[] node = nodeFunction.getChildren();
    	for(IASTNode ob : node)
    	{
    		if(ob instanceof CPPASTName)
    		{
    			return ob;
    		}
    	}
    	return null;   	
    }
    /*
     * getParameters method will extract function declaration parameters.
     * */
    public String getParameters(IASTNode nodeFunction)
    {
    	String params = "";
    	StringBuilder parameter = new StringBuilder();
    	IASTNode[] node = nodeFunction.getChildren();
    	parameter.append("");
    	String temp = "";
    	for(IASTNode ob : node)
    	{
    		if(ob instanceof CPPASTParameterDeclaration)
    		{
    			parameter.append(getParameter(ob));
    			parameter.append(",");
    		}
    	}
    	temp = parameter.toString();
    	if(temp.length() > 0)
    		params = temp.substring(0,temp.length()-1);
    	return params;
    }
    
    /*
     * TextRange method will gain declaration position in source file.
     * */
    public PlanarRange getPosition(IASTNode node, int[] strLineLength){
    	if(null != node && !"".equals(node.getRawSignature())){// if string "" equals node.getRawSignature(),it will throw NPE
    	    int fromLine, toLine, fromCh, length;
    	    fromLine = node.getFileLocation().getStartingLineNumber();
    	    toLine = node.getFileLocation().getEndingLineNumber();  
    	    fromCh = node.getFileLocation().getNodeOffset();
    	    fromCh = fromCh - getLinesLength(fromLine - 1, strLineLength);
    	    String name = node.getRawSignature();
    	    length = name.contains("(")?(name.indexOf("(")+1):name.length();
    	    PlanarRange token = new PlanarRange(fromLine-1,fromCh,toLine-1,fromCh+length);
    	    return token;
    	}
    	else{
    		return null;
    	}
    }
    /*
     * getAnonymous method will gain anonymous symbol's toCh.
     * */
    public int getAnonymousTextRange(String str,int fromCh){
        int toCh = 0;
        while(!" ".equals(str.substring(toCh,toCh+1)) && !"\n".equals(str.substring(toCh,toCh+1)) && !"\r".equals(str.substring(toCh,toCh+1))){
            ++toCh;
        }
        return toCh + fromCh;
    }
    /*
     * getScope method will gain node's scope in source file.
     * */
    public PlanarRange getScope(IASTNode node, int[] strLineLength){
    	if(null != node && !"".equals(node.getRawSignature())){
    	    int fromLine, toLine, fromCh, toCh ;
    	    fromLine = node.getFileLocation().getStartingLineNumber();
    	    toLine = node.getFileLocation().getEndingLineNumber();
    	    fromCh = node.getFileLocation().getNodeOffset();
    	    toCh = fromCh + node.getFileLocation().getNodeLength();
    	    fromCh = fromCh - getLinesLength(fromLine - 1, strLineLength);
    	    toCh = toCh - getLinesLength(toLine - 1, strLineLength);
    	    PlanarRange token = new PlanarRange(fromLine-1,fromCh,toLine-1,toCh);
    	    return token;
    	}
    	else
    		return null;
    }
    /*
     * getLineLength method will gain string's length before line.
     * */
    public int getLinesLength(int line, int[] strLineLength){
    	if(0 >= line){
    		return 0;
    	}
    	else{
    		int length = 0;
    		for(int i = 0; i < line; ++i){
    			length += strLineLength[i];
    		}
    		return length;
    	}
    }
    /*
     * getParameter method use for gaining one parameter of function declaration
     * */
    public String getParameter(IASTNode nodeParameter)
    {
  	    IASTNode[] node = nodeParameter.getChildren();
   	    String params = "",type="";
   	    StringBuilder parameter = new StringBuilder();
   	    parameter.append("");
   	    boolean variable = false;
   	    for(IASTNode ob : node)
 	    {
 		    if(judgeType(ob))
 		    {
 			    type = ob.getRawSignature();
 		    }
 		    if(ob instanceof CPPASTDeclarator)
		    {
			    IASTNode[] node1 = ob.getChildren();
			    for(IASTNode ob1 : node1)
			    {
				    if(ob1 instanceof CPPASTPointer)
					    type += ob1.getRawSignature();
				    if(ob1 instanceof CPPASTReferenceOperator){
					    type += ob1.getRawSignature();
				    }
			    }
		    }
		    if(ob instanceof CPPASTArrayDeclarator)
		    {
			    IASTNode[] node2 = ob.getChildren();
			    for(IASTNode ob2 : node2)
			    {
				    if(ob2 instanceof CPPASTArrayModifier)
					    type += ob2.getRawSignature();
			    }
		    }
 		    if(ob instanceof CPPASTFunctionDeclarator)
 		    {
 			    IASTNode[] node1 = ((CPPASTFunctionDeclarator) ob).getChildren();
 			    for(IASTNode ob1 : node1)
 			    {
	      		    if(ob1 instanceof CPPASTDeclarator)
	      		    {
	      			    variable = true;
	      			    type += "(";
	      			    IASTNode[] node3 = ob1.getChildren();
	      			    for(IASTNode ob3 : node3)
	      			    {
	      				    if(ob3 instanceof CPPASTPointer)
	      				    {
	      					    type += ob3.getRawSignature();
	      				    }
	      				    if(ob3 instanceof CPPASTReferenceOperator){
	      					    type += ob3.getRawSignature();
	      				    }
	      			     }
	      			     type += ")";
	      		     }
					 if(ob1 instanceof CPPASTPointer)
					 {
						 type += ob1.getRawSignature();
					 }
	 				 if(ob1 instanceof CPPASTParameterDeclaration)
	 				 {
	 					 IASTNode[] node2 = ((CPPASTParameterDeclaration) ob1).getChildren();
	 					 for(IASTNode ob2 : node2)
	 					 {
	 						 if(judgeType(ob2))
	 						 {
	 							 parameter.append(ob2.getRawSignature());
	 						 }
	 						 if(ob2 instanceof CPPASTDeclarator)
	 						 {
	 							 IASTNode[] node3 = ob2.getChildren();
	 							 for(IASTNode ob3 : node3)
	 							 {
	 								 if(ob3 instanceof CPPASTPointer)
	 									 parameter.append(ob3.getRawSignature());
	 							 }
	 						 }
	 						 if(ob2 instanceof CPPASTArrayDeclarator)
	 						 {
	 							 IASTNode[] node3 = ob2.getChildren();
	 							 for(IASTNode ob3 : node3)
	 							 {
	 								 if(ob3 instanceof CPPASTArrayModifier)
	 									 parameter.append(ob3.getRawSignature());
	 							 }
	 						 }
	 					 }
	 					 parameter.append(",");
	 				 }
 			  }
 			  if(ob.getRawSignature().contains("..."))
 				  parameter.append("...,");
 			  if(parameter.toString().length()>0)
 				  params=parameter.toString().substring(0,parameter.toString().length()-1);
 		  }
 	   }
 	   if(variable)
 	   {
 		   String temp = params;
 		   params = "(";
 		   params += temp;
 		   params += ")";
 		   type += params;
 	   }
 	   params = type;
       return params;
    }
	@Override
	public int getVersion() {
		return 1;
	}
	/*
	 * getVisivility method will be used for judging visibility of class.
	 * */
	public int getVisibility(String visibility){
		int visual = 0;
		switch (visibility){
		case "private:":
			visual = 1;
			break;
		case "protected:":
			visual = 2;
			break;
		case "public:":
			visual = 3;
		}
		return visual;
	}
	/*
	 * getModifier method is used to extracting symbol's modifier,such as private,protected.
	 * */
	public CppSymbol.Modifier getModifier(int visibility){
		Modifier modifier = Modifier.NORMAL;
        switch(visibility){
        case 0:
        	modifier = Modifier.NORMAL;
        	break;
        case 1:
        	modifier = Modifier.PRIVATE;
        	break;
        case 2:
        	modifier = Modifier.PROTECTED;
            break;
        case 3:
        	modifier = Modifier.PUBLIC;
        	break;
        case 16:
        	modifier = Modifier.FRIENDFUNC;
        	break;
        }
        
        
		return modifier;
	}
	/*
	 * getFriendModifer method is used to extracting symbol's friend modifier.
	 * */
	public Modifier getFriendModifier(int visibility,String str,boolean isVariable){
		if(isVariable){
			return Modifier.FRIENDVAR;
		}else{
			if(str.contains(" class ")){
				return Modifier.FRIENDCLASS;
			}else if(str.contains(" struct ")){
				return Modifier.FRIENDSTRUCT;
			}else if(str.contains(" union ")){
				return Modifier.FRIENDUNION;
			}else if(str.contains(" enum ")){
				return Modifier.FRIENDENUM;
			}else if(str.contains(" typedef ")){
				return Modifier.FRIENDTYPEDEF;
			}	
		}
		return Modifier.FRIENDVAR;
	}
	/*
	 * getConstModifier method is used to extracting symbol's constant modifier.
	 * */
	public Modifier getConstModifier(int visibility,String str,boolean isVariable){
		if(isVariable){
			return Modifier.FRIENDVAR;
		}else{
			if(str.contains(" class ")){
				return Modifier.FRIENDCLASS;
			}else if(str.contains(" struct ")){
				return Modifier.FRIENDSTRUCT;
			}else if(str.contains(" union ")){
				return Modifier.FRIENDUNION;
			}else if(str.contains(" enum ")){
				return Modifier.FRIENDENUM;
			}else if(str.contains(" typedef ")){
				return Modifier.FRIENDTYPEDEF;
			}	
		}
		return Modifier.FRIENDVAR;
	}
//	public 
	/*
	 * getVariableType method is used for judging type of variable declarations.
	 * For example,if it returns 13 and visibility is 0,variable declaration is
	 * constant variable declaration.
	 * */
    public int getVariableType(IASTNode node,int visibility){
    	String head=node.getRawSignature();
    	int value = isConOrStaVariable(head);
    	if(value ==  3 || value == 6)
    	{
    		if(0 == visibility){
    			visibility += 15;
    		}
    		else if(16 == visibility || 20 == visibility){
    			visibility += 3;
    		}
    		else
    			visibility += 9;
    		return visibility;
    	}
    	if(value == 1 || value == 4)
    	{
    		if(0 == visibility)
    			visibility += 13;
    		else if(16 == visibility || 20 == visibility){
    			visibility += 1;
    		}else
    			visibility += 3;
    		
    		return visibility;
    	}
    	if(value == 2)
    	{
    		if(0 == visibility)
    			visibility += 14;
    		else if(16 == visibility || 20 == visibility){
    			visibility += 2;
    		}else
    			visibility +=6;
    		return visibility;
    	}
    	if(value == 8)
    	{
    		visibility = 20;
//    		return value;
    	}
    	if(value == 16)
    	{
    		visibility = 21;
    	}
    	return visibility;
    }
    /*
     * getMethodType method is used for judging type of function declaration.
     * For example,if it returns 14 and visibility is 0,function declaration is
     * static declaration.
     * */
    public int getMethodType(IASTNode funcNode,int visibility){
        IASTNode []node = funcNode.getChildren();
        boolean isStatic = false;
        boolean isConst = false;
        boolean isFriend = false;
    	String str = "";
    	if(16 == visibility)
    		visibility = 0;
        for(IASTNode ob : node){
    		if(ob instanceof CPPASTSimpleDeclSpecifier || ob instanceof CPPASTNamedTypeSpecifier){
    			str = ob.getRawSignature();
    			int value = isStaOrFriFunction(str);
    			if(value == 5)
    			{
    				isStatic = true;
    				isConst = true;
    			}
    			if(value == 1){
    				isStatic = true;
    			}
    			if(value == 2){
    				isFriend = true;
    			}
    			if(value == 4){
    				isConst = true;
    			}
    		}
    		if(ob instanceof CPPASTFunctionDeclarator){
    			str = ob.getRawSignature();
    			if(!isConst){ 
    				isConst = isConFunction(str);
    			}
    		}
    	}
        if(20 == visibility){
        	if(isConst)
        		visibility += 1;
        	if(isStatic)
        		visibility += 2;
        	return visibility;
        }
        if(visibility > 0){
        	if(isConst)
        		visibility += 3;
        	if(isStatic)
        		visibility += 6;
        	if(!isConst && !isStatic && isFriend)
        		visibility = 16;
        	return visibility;
        }
        else{
        	if(isConst)
        		visibility += 1;
        	if(isStatic)
        		visibility += 2;
        	if(!isConst && !isStatic && isFriend)
        		visibility = 16;
        	if(isConst || isStatic)
        		visibility += 12;
        }
    	return visibility;
    }
    /*
     * isNameNull method is used for judging declaration's name is Null or not.
     * */
    public boolean isNameNull(IASTNode node){
    	IASTNode[] no = node.getChildren();
    	for(IASTNode ob : no){
    		if(ob instanceof CPPASTName && 0 < ob.getRawSignature().length()){
    			return false;
    		}
    		if(ob instanceof CPPASTTemplateId && 0 < ob.getRawSignature().length()){
    			return false;
    		}
    	}
    	return true;
    }
    /*
     * getUsingName method is used for gaining using declaration's name that
     * we need and removing unnecessary information. 
     * */
    public String getUsingName(IASTNode usingnode){
    	IASTNode []node = usingnode.getChildren();
    	String name = "";
    	for(IASTNode ob : node){
    		if(ob instanceof CPPASTName){
    			name = ob.getRawSignature();
    			break;
    		}
    		if(ob instanceof CPPASTQualifiedName){
    			IASTNode []node1 = ob.getChildren();
    			for(IASTNode ob1 : node1){
    				if(ob1 instanceof CPPASTName){
    					name = ob1.getRawSignature();
    				}
    			}
    		}
    	}
    	return name;
    }
    /*
     *getUsingNameNode method will gain using declaration name node 
     * */
    public IASTNode getUsingNameNode(IASTNode usingnode){
    	IASTNode []node = usingnode.getChildren();
    	IASTNode namenode = null;
    	for(IASTNode ob : node){
    		if(ob instanceof CPPASTName){
    			namenode = ob;
    			break;
    		}//using utils::CpatureTime;
    		if(ob instanceof CPPASTQualifiedName){
    			IASTNode []node1 = ob.getChildren();
    			for(IASTNode ob1 : node1){
    				if(ob1 instanceof CPPASTName){
    					namenode = ob1;
    				}
    			}
    		}
    	}
    	return namenode;
    }
    /*
     * isConOrStaVariable method is used for judging type variable declaration.
     * if it returns 1,we will learn that the variable declaration is constant.
     * */
    public int isConOrStaVariable(String str){
    	String[] keyWords = {"const", "static", "constexpr", "friend", "using"};
    	String []spe = {" ","	"};
    	String temp = "";
    	int value = 0 ,index = 1;
    	for(int i = 0; i < 5; ++i){
    		for(int j = 0; j < 2; ++j){
    			temp = keyWords[i];
    			temp += spe[j];
    			if(str.startsWith(temp)){
    				value += index << i;
    			}
    			temp = spe[j];
    			temp += keyWords[i];
    			if(str.contains(temp + spe[0]) || str.contains(temp + spe[1])){
    				value += index << i;
    			}
    		}
    	}
    	return value;
    }
    /*
     *isStaOrFriFunction method is used for judging type of function declaration.
     *if it returns 1,we will know that the function declaration is static. 
     * */
    public int isStaOrFriFunction(String str){
    	String[] keyWords = {"static", "friend"};
    	String []spe = {" ","	"};
    	String temp = "";
    	int value = 0, index = 1;
    	for(int i = 0; i < 2; ++i){
    		for(int j = 0; j < 2; ++j){
    			temp = keyWords[i];
    			temp += spe[j];
    			if(str.startsWith(temp)){
    				value += index << i;
    			}
    			temp = spe[j];
    			temp += keyWords[i];
    			if(str.contains(temp + spe[0]) || str.contains(temp + spe[1])){
    				value += index << i;
    			}
    		}
    	}
    	return value;
    }
    /*
     * isConFunction method is used for judging type of function declaration.
     * if it returns true,we will learn that function declaration is constant.
     * */
    public boolean isConFunction(String str){
    	String[] keyWords = {"const", "constexpr"};
    	String []spe = {" ","	"};
    	String temp = "";
    	for(int i = 0; i < 2; ++i){
	    	for(int j = 0; j < 2; ++j){
	    		temp = ")";
	    		temp += keyWords[i];
	    		if(str.endsWith(temp) || str.endsWith(temp+spe[j])){
	    			return true;
	    		}
	    		temp = spe[j];
	    		temp += keyWords[i];
	    		if(str.endsWith(temp) || str.endsWith(temp + spe[0]) || str.endsWith(temp + spe[1])){
	    			return true;
	    		}
	    	}
    	}
    	return false;
    }
    /*
     * isFunctionName method judge node is whether or not name node
     * */
    public boolean isFunctionName(IASTNode node){
        if(node instanceof CPPASTOperatorName ||
           node instanceof CPPASTQualifiedName ||
           node instanceof CPPASTTemplateId ||
           node instanceof CPPASTName){
        	return true;
        }
    	return false;
    }
    /*
     * hasStaticModifier method will judge declaration is whether or not static
     */
    public boolean hasStaticModifier(String str){
    	String keyWords = "static";
    	String []spe = {" ","	"};
    	String temp = "";
	    for(int j = 0; j < 2; ++j){
	    	temp = keyWords;
	    	temp += spe[j];
	    	if(str.startsWith(temp)){
	    		return true;
	    	}
	    	temp = spe[j];
	    	temp += keyWords;
	    	if(str.contains(temp + spe[0]) || str.contains(temp + spe[1])){
	    		return true;
	    	}
	    }
    	return false;
    }
    /*
     * hasExternModifier method will judge declaration is whether or not extern 
     * */
    public boolean hasExternModifier(String str){
    	String keyWords = "extern";
    	String []spe = {" ","	"};
    	String temp = "";
	    for(int j = 0; j < 2; ++j){
	    	temp = keyWords;
	    	temp += spe[j];
	    	if(str.startsWith(temp)){
	    		return true;
	    	}
	    	temp = spe[j];
	    	temp += keyWords;
	    	if(str.contains(temp + spe[0]) || str.contains(temp + spe[1])){
	    		return true;
	    	}
	    }
    	return false;
    }
    /*
     * hasPrivateModifier mentod is used for juding symbol is private or not.
     * */
    public boolean hasPrivateModifier(String visibility){
    	if("private:".equals(visibility)){
    		return true;
    	}
    	return false;
    }
    
    /*
     * isSymbolNull method is used for judging symbol is Null or not.Or NullPointerException will occur.
     * */
    public boolean isSymbolNull(CppSymbol symbol){
    	return symbol == null;
    }
}