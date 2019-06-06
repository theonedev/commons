package io.onedev.commons.jsyntax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Resources;

import io.onedev.commons.jsyntax.TokenUtils;
import io.onedev.commons.jsyntax.Tokenized;
import io.onedev.commons.jsyntax.Tokenizer;

public abstract class AbstractTokenizerTest {

	protected void verify(Tokenizer tokenizer, String[] loadModes, String fileName) {
		verify(tokenizer, loadModes, fileName, null);
	}
	
	protected void verify(Tokenizer tokenizer, String[] loadModes, String fileName, @Nullable String mimeType) {
		try {
			// disable nashorn assertion on duplicate code
			getClass().getClassLoader().getParent().setDefaultAssertionStatus(false);
			
	        ScriptEngineManager factory = new ScriptEngineManager();
			ScriptEngine engine = factory.getEngineByName("JavaScript");
			
			String script = CodeMirrorResource.readAsString("addon/runmode/runmode-standalone.js");
			script = StringUtils.replace(script, "window.CodeMirror", "CodeMirror");
			engine.eval(script);
			
			engine.eval(CodeMirrorResource.readAsString("mode/meta.js"));
			
			engine.eval(CodeMirrorResource.readAsString("addon/mode/simple.js"));
			engine.eval(CodeMirrorResource.readAsString("addon/mode/multiplex.js"));
			engine.eval(CodeMirrorResource.readAsString("addon/mode/overlay.js"));
			
			for (String mode: loadModes) {
				script = CodeMirrorResource.readAsString("mode/" + mode);
				engine.eval(StringUtils.replace(script, "window.CodeMirror", "CodeMirror"));
			}
			
			String fileContent = Resources.toString(Resources.getResource(getClass(), fileName), Charsets.UTF_8);
			if (fileName.contains("/"))
				fileName = StringUtils.substringAfterLast(fileName, "/");
			if (fileName.endsWith(".txt"))
				fileName = fileName.substring(0, fileName.length()-4); 
			engine.put("fileName", fileName);
			engine.put("fileContent", fileContent);
			if (mimeType != null)
				engine.put("mimeType", mimeType);
			else
				engine.put("mimeType", "");

	        List<String> lines = new ArrayList<>();
	        for (String line: Splitter.on('\n').splitToList(fileContent))
	        	lines.add(StringUtils.stripEnd(line, "\r"));
	        
			Callback callback = new Callback(lines);
			engine.put("callback", callback);
			
	        engine.eval(""
	        		+ "if (mimeType.length == 0) "
	        		+ "  mimeType = CodeMirror.findModeByFileName(fileName).mime;"
	        		+ "var runModeCallback = function(text, style, lineIndex, beginPos) {"
	        		+ "  if (!style)"
	        		+ "    style = \"\";"
	        		+ "  if (text == \"\\n\")"
	        		+ "    callback.newLine();"
	        		+ "  else "
	        		+ "    callback.token(style, beginPos, beginPos+text.length);"
	        		+ "};"
	        		+ "CodeMirror.runMode(fileContent, mimeType, runModeCallback);");
	        
	        String expected = toString(callback.getTokenizedLines());
	        String actual = toString(tokenizer.tokenize(lines));
	        
	        Assert.assertEquals(expected, actual);
	    } catch (IOException | ScriptException e) {
			throw new RuntimeException(e);
		} 
	}
	
	public static class Callback {

		private final List<String> lines;
		
		private int index;
		
		private List<Tokenized> tokenizedLines = new ArrayList<>();
	
		private List<Long> currentTokens = new ArrayList<>();
		
		public Callback(List<String> lines) {
			this.lines = lines;
		}
		
		public void newLine() {
			tokenizedLines.add(new Tokenized(lines.get(index++), TokenUtils.toArray(currentTokens)));
			currentTokens.clear();
		}
		
		public void token(String style, int beginPos, int endPos) {
			long token = TokenUtils.getToken(beginPos, endPos, style);
			List<Long> splitted = TokenUtils.splitWhitespace(lines.get(index), token);
			if (splitted != null)
				currentTokens.addAll(splitted);
			else
				currentTokens.add(token);
		}
		
		public List<Tokenized> getTokenizedLines() {
			List<Tokenized> copy = new ArrayList<>(tokenizedLines);
			if (!currentTokens.isEmpty())
				copy.add(new Tokenized(lines.get(index), TokenUtils.toArray(currentTokens)));
			return copy;
		}
	}
	
	private String toString(List<Tokenized> tokenizedLines) {
		StringBuilder builder = new StringBuilder();
        for (int i=0; i<tokenizedLines.size(); i++) {
        	for (long token: tokenizedLines.get(i).getTokens()) 
        		builder.append(TokenUtils.toString(tokenizedLines.get(i).getText(), token));
    		builder.append("\n");
        }
		return StringUtils.stripEnd(builder.toString(), "\n");
	}
}
