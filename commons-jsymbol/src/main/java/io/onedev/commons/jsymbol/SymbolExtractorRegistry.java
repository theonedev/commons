package io.onedev.commons.jsymbol;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.reflections.Reflections;

import com.google.common.base.Joiner;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SymbolExtractorRegistry {

	private static final List<SymbolExtractor<Symbol>> extractors;
	
	static {
		extractors = new ArrayList<>();
		Reflections reflections = new Reflections(SymbolExtractorRegistry.class.getPackage().getName());
		for (Class<? extends SymbolExtractor> extractorClass: reflections.getSubTypesOf(SymbolExtractor.class)) {
			if (!Modifier.isAbstract(extractorClass.getModifiers())) {
				try {
					extractors.add(extractorClass.getDeclaredConstructor().newInstance());
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException 
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new RuntimeException(e);
				}
			}
		}
		extractors.sort((o1, o2)->o1.getClass().getName().compareTo(o2.getClass().getName()));
	}
	
	/**
	 * Get symbol extractor of specified file name 
	 * 
	 * @return
	 * 			symbol extractor of specified file name, or <tt>null</tt> if not found
	 */
	@Nullable
	public static SymbolExtractor<Symbol> getExtractor(String fileName) {
		for (SymbolExtractor<Symbol> extractor: extractors) {
			if (extractor.accept(fileName))
				return extractor;
		}
		return null;
	}

	public static String getVersion() {
		List<String> versions = new ArrayList<>();
		
		for (SymbolExtractor<Symbol> extractor: extractors) 
			versions.add(extractor.getClass().getName() + ":" + extractor.getVersion());
		
		Collections.sort(versions);
		return Joiner.on(",").join(versions);
	}
	
}
