package io.onedev.commons.jsyntax.forth;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class ForthTokenizer extends AbstractTokenizer<ForthTokenizer.State>{

	static final List<String> coreWordList = toWordList("INVERT AND OR XOR"+ 
			" 2* 2/ LSHIFT RSHIFT" + 
			" 0= = 0< < > U< MIN MAX" + 
			" 2DROP 2DUP 2OVER 2SWAP ?DUP DEPTH DROP DUP OVER ROT SWAP" + 
			" >R R> R@" + 
			" + - 1+ 1- ABS NEGATE" + 
			" S>D * M* UM*" + 
			" FM/MOD SM/REM UM/MOD */ */MOD / /MOD MOD" + 
			" HERE , @ ! CELL+ CELLS C, C@ C! CHARS 2@ 2!" + 
			" ALIGN ALIGNED +! ALLOT" + 
			" CHAR [CHAR] [ ] BL" + 
			" FIND EXECUTE IMMEDIATE COUNT LITERAL STATE" + 
			" ; DOES> >BODY" + 
			" EVALUATE" + 
			" SOURCE >IN" + 
			" <# # #S #> HOLD SIGN BASE >NUMBER HEX DECIMAL" + 
			" FILL MOVE" + 
			" . CR EMIT SPACE SPACES TYPE U. .R U.R" + 
			" ACCEPT" + 
			" TRUE FALSE" + 
			" <> U> 0<> 0>" + 
			" NIP TUCK ROLL PICK" + 
			" 2>R 2R@ 2R>" + 
			" WITHIN UNUSED MARKER" + 
			" I J" + 
			" TO" + 
			" COMPILE, [COMPILE]" + 
			" SAVE-INPUT RESTORE-INPUT" + 
			" PAD ERASE" + 
			" 2LITERAL DNEGATE" + 
			" D- D+ D0< D0= D2* D2/ D< D= DMAX DMIN D>S DABS" + 
			" M+ M*/ D. D.R 2ROT DU<" + 
			" CATCH THROW" + 
			" FREE RESIZE ALLOCATE" + 
			" CS-PICK CS-ROLL" + 
			" GET-CURRENT SET-CURRENT FORTH-WORDLIST GET-ORDER SET-ORDER" + 
			" PREVIOUS SEARCH-WORDLIST WORDLIST FIND ALSO ONLY FORTH DEFINITIONS ORDER" + 
			" -TRAILING /STRING SEARCH COMPARE CMOVE CMOVE> BLANK SLITERAL");
	
	static final List<String> immediateWordList = toWordList("IF ELSE THEN BEGIN WHILE REPEAT UNTIL RECURSE [IF] [ELSE] [THEN] ?DO DO LOOP +LOOP UNLOOP LEAVE EXIT AGAIN CASE OF ENDOF ENDCASE");
	
	
	class State{
		String state;
		int base;
		List<String> wordList ;
		List<String> coreWordList;
		List<String> immediateWordList;
		
		public State(String state,int base,List<String> wordList,List<String> coreWordList,List<String> immediateWordList)
		{
			this.state = state;
			this.base = base;
			this.wordList = wordList;
			this.coreWordList = coreWordList;
			this.immediateWordList = immediateWordList;
		}
	}
	
	
	
	static List<String> toWordList(String words){
		List<String> ret = new ArrayList<String>(); 
	    String[] strArr = words.split(" ");
//	    		.forEach(function (e) {
//	        ret.push({
//	            name : e });
//	        }
//	        );
	    for(int i=0;i<strArr.length;i++) {
	    	ret.add(strArr[i]);
	    }
	    
	    return ret;
	}

	
	String searchWordList(List<String> wordList, String word){
	    int i;
	    for (i = wordList.size() - 1;i >= 0;i --){
	        if (wordList.get(i).equals(word.toUpperCase()) ) {
	            return wordList.get(i);
	        }
	    }
	    return "";
//	    return null;
	}
	
	
	@Override
	public boolean accept(String fileName) {		
		return acceptExtensions(fileName,"fth");
	}

	@Override
	public State startState() {
		return new State("",10,new ArrayList<String>() ,coreWordList,immediateWordList);
//	        state : "", base : 10, coreWordList : coreWordList, immediateWordList : immediateWordList, wordList :[] };

	}

	
	static final Pattern pattern[] = new Pattern[10];
	static {
	    pattern[0] = Pattern.compile("^(\\]|:NONAME)(\\s|$)",Pattern.CASE_INSENSITIVE);
	    pattern[1] = Pattern.compile("^(\\:)\\s+(\\S+)(\\s|$)+");
	    pattern[2] = Pattern.compile("^(VARIABLE|2VARIABLE|CONSTANT|2CONSTANT|CREATE|POSTPONE|VALUE|WORD)\\s+(\\S+)(\\s|$)+",Pattern.CASE_INSENSITIVE);
	    pattern[3] = Pattern.compile("^(\\'|\\[\\'\\])\\s+(\\S+)(\\s|$)+");
	    pattern[4] = Pattern.compile("^(\\;|\\[)(\\s)");
	    pattern[5] = Pattern.compile("^(\\;|\\[)($)");
	    pattern[6] = Pattern.compile("^(POSTPONE)\\s+\\S+(\\s|$)+");
	    pattern[7] = Pattern.compile("^(\\S+)(\\s+|$)");
	    pattern[8] = Pattern.compile("[^)]");
	    pattern[9] = Pattern.compile("[^\"]");
	}
	

	@Override
	public String token(StringStream stream, State stt) {
		// TODO Auto-generated method stub
		List<String> mat;
	    if (stream.eatSpace()) {
	        return "" ;
	    }
	    if (stt.state.equals("")) {	    	// interpretation
	        if (!stream.match(pattern[0]).isEmpty()) {
	            stt.state = " compilation";
	            return "builtin compilation";
	        }
	        mat = stream.match(pattern[1]);
	        if (!mat.isEmpty()) {
//	            stt.wordList.push({
//	                name : mat[2].toUpperCase() });
	        	
	        	stt.wordList.add(mat.get(2).toUpperCase());	        	
	            stt.state = " compilation";
	            return "def" + stt.state;
	        }
	        mat = stream.match(pattern[2]);
	        if (!mat.isEmpty()) {
//	            stt.wordList.push({
//	            name : mat[2].toUpperCase() });
	        	stt.wordList.add(mat.get(2).toUpperCase());	        	
	            return "def" + stt.state;
	        }
	        mat = stream.match(pattern[3]);
	        if (!mat.isEmpty()) {
	            return "builtin" + stt.state;
	        }
	    }
	    else {  // compilation
	    	// ; [
	    	if (!stream.match(pattern[4]).isEmpty()) {
	    		stt.state = "";
	            stream.backUp(1);
	            return "builtin compilation";
	        }
	        if (!stream.match(pattern[5]).isEmpty()) {
	            stt.state = "";
	            return "builtin compilation";
	        }
	        if (!stream.match(pattern[6]).isEmpty()) {
	            return "builtin";
	        }
	    }
	    
	    // dynamic wordlist
	    mat = stream.match(pattern[7]);
	    if (!mat.isEmpty()) {
	    	if (!searchWordList(stt.wordList, mat.get(1)).equals("") ) {
//	    	if (searchWordList(stt.wordList, mat.get(1))!=null) {
	    		return "variable" + stt.state;
	    	}
	                
	    	// comments
	    	if (mat.get(1).equals("\\")) {
	    		stream.skipToEnd();
	    		return "comment" + stt.state;
	    	}
	    	// core words
	    	if (!searchWordList(stt.coreWordList, mat.get(1).toUpperCase()).equals("")) {
//	    	if (searchWordList(stt.coreWordList, mat.get(1).toUpperCase())!=null) {
	    		return "builtin" + stt.state;
	    	}
	    	if (!searchWordList(stt.immediateWordList, mat.get(1)).equals("")) {
//	    	if (searchWordList(stt.immediateWordList, mat.get(1))!=null) {
	    		return "keyword" + stt.state;
	    	}

	    	if (mat.get(1).equals("(")) {
//	    		stream.eatWhile(function (s) {
//	        	return s != = ")";} );

	    	    stream.eatWhile(pattern[8]);	    		
	    		stream.eat(")");
	    		return "comment" + stt.state;
	    	}
	                   
	                
	    	// // strings
	        if (mat.get(1).equals(".(")) {
//	        	stream.eatWhile(function (s) {
//	                        return s != = ")";
//	                    }
//	                    );
	        	stream.eatWhile(pattern[8]);
	            stream.eat(")");
	            return "string" + stt.state;
	        }
	        if (mat.get(1).equals("S\"") || mat.get(1).equals(".\"") || mat.get(1).equals("C\"")) {
//	        	stream.eatWhile(function (s) {
//	        	return s != = """;
//	                    }
//	                    );
	        	stream.eatWhile(pattern[9]);
	        	stream.eat("\"");
	            return "string" + stt.state;
	        }
	        // numbers
//	        if (mat[1] - 0 xfffffffff)  68719476735{
//	        	return 'number' + stt.state;
//	        }

	        try {
		        if ((Double.parseDouble(mat.get(1))- Double.parseDouble("68719476735"))!=0) {
		        	return "number" + stt.state;
		        }
	        }
	        catch(NumberFormatException e) {
	        }

	        return "atom" + stt.state;
	    }
	    return "";
	}


	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-forth");
	}


	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("forth");
	}
	    

}
