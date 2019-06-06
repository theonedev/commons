package io.onedev.commons.jsyntax.brainfuck;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class BrainfuckTokenizer extends AbstractTokenizer<BrainfuckTokenizer.State> {

	String reserve = "><+-.,[]";
	
	static class State{
		Boolean commentLine;
		int left;
		int right;
		Boolean commentLoop;
		State(Boolean commentLine, int left, int right, Boolean commentLoop) {
			super();
			this.commentLine = commentLine;
			this.left = left;
			this.right = right;
			this.commentLoop = commentLoop;
		}
	}

	@Override
	public boolean accept(String fileName) {
		
		return acceptExtensions(fileName, "b", "bf");
	}

	@Override
	public State startState() {
		
		return new State(false,0,0,false);
	}

	@Override
	public String token(StringStream stream, State state) {
		
		if(stream.eatSpace()){
			return "";
		}
		if(stream.sol()){
			state.commentLine=false;
		}
		String ch=stream.next().toString();
		if(reserve.indexOf(ch)!=-1){
			if(state.commentLine==true){
				if(stream.eol()){
					state.commentLine=false;
				}
				return "comment";
			}
			if(ch.equals("]")|| ch.equals("[")){
				if(ch.equals("[")){
					state.left++;
				}else{
					state.right++;
				}
				return "bracket";
			}else if(ch.equals("+") || ch.equals("-")){
				return "keyword";
			}else if(ch.equals("<") || ch.equals(">")){
				return "atom";
			}else if(ch.equals(".") || ch.equals(",")){
				return "def";
			}else{
				return "";
			}
		}else{
			state.commentLine=true;
			if(stream.eol()){
				state.commentLine=false;
			}
			return "comment";
		}
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-brainfuck");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("brainfuck");
	}
}
