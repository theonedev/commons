package io.onedev.commons.jsymbol;

import java.util.List;

/**
 * Extracts symbols from source code to serve below purposes:
 * <ul>
 * <li> Index symbol definition names for definition search
 * <li> Display outlines of source files
 * 
 * So local structures such as local variables inside a method should not be extracted, 
 * otherwise it will clutter search result of symbol definitions with same name, and 
 * also clutter the outline   
 * 
 * @author robin
 *
 * @param <T>
 */
public interface SymbolExtractor<T extends Symbol> {
	
	/**
	 * Extract symbols from specified content
	 * 
	 * @param fileName
	 * 			name of file to extract symbols from 
	 * @param fileContent
	 * 			content of file to extract symbols from
	 * @return
	 * 			list of extracted symbols
	 * 
	 * @throws ExtractException
	 */
	List<T> extract(String fileName, String fileContent);
	
	List<T> extract(String fileName, List<String> fileContent);
	
	boolean accept(String fileName);
	
	int getVersion();
	
}