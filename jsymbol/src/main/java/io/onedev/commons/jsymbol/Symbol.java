package io.onedev.commons.jsymbol;

import java.io.Serializable;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;

import io.onedev.commons.jsymbol.java.symbols.CompilationUnitSymbol;
import io.onedev.commons.utils.Range;

/**
 * Represents a symbol definition extracted from source file
 * 
 * @author robin
 *
 */
public abstract class Symbol implements Serializable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Get parent symbol containing definition of current symbol. For instance a Java method symbol will return the 
	 * enclosing class as its parent symbol
	 * 
	 * @return
	 * 			parent symbol or <tt>null</tt> for root symbol
	 */
	@Nullable
	public abstract Symbol getParent();
	
	/**
	 * Get name of this symbol. Return <tt>null</tt> if the symbol does not have a name. For instance an inline 
	 * JavaScript function may not have a name 
	 * 
	 * @return
	 * 			name of symbol, or <tt>null</tt> if this symbol does not have a name
	 */
	@Nullable
	public abstract String getName();

	/**
	 * @return
	 * 			whether or not the symbol should be displayed in outline
	 */
	public boolean isDisplayInOutline() {
		return true;
	}

	/**
	 * Get position of the symbol in source. The position will be highlighted if this symbol is selected in outline. 
	 * Symbols without positions will return <tt>null</tt> here. Some outline may contain faked symbols and these 
	 * faked symbols will not have positions. An example is the vueComponents symbol in JavaScript which we used to 
	 * group all discovered vue components
	 * 
	 * @return
	 * 			position of the symbol, or <tt>null</tt> if the symbol does not have a position
	 */
	@Nullable
	public abstract TokenPosition getPosition();

	/**
	 * Get scope of the symbol. If cursor moves into this scope, corresponding symbol will be highlighted in outline. 
	 * For instance the method symbol can use the method body range as scope
	 * 
	 * @return
	 * 			scope of the symbol, or <tt>null</tt> if the symbol does not have a scope
	 */
    @Nullable
    public abstract TokenPosition getScope();
    
    /**
     * Whether or not this symbol is local to its namespace. A local symbol can not be accessed outside of its 
     * namespace. For instance a Java private method can not be accessed outside of its defining class. This 
     * information will be used to calculate {@link #isLocalInHierarchy()} to determine if a symbol should be 
     * considered for matching when doing symbol cross-reference from another file
     * 
     * @return
     *          whether or not the symbol is local to its namespace
     */
    public abstract boolean isLocal();
    
	/**
	 * Whether or not this symbol is a primary symbol. Primary symbol will be searched 
	 * before non-primary symbols. For instance, a Java symbol search will match 
	 * against type symbols (class, enumeration, interface, annotation etc.) first 
	 * before matching other symbols such as fields and methods.
	 * 
	 * @return
	 * 			whether or not this symbol is primary
	 */
	public abstract boolean isPrimary();

	/**
	 * Whether or not the symbol should be indexed for search. Some symbols do not have too much sense for searching, 
	 * for instance Java {@link CompilationUnitSymbol} is used for namespace calcuation and outline display, and it is 
	 * not intended for searching. 
	 * 
	 * @return
	 * 			whether oro not the symbol should be indexed for search
	 */
	public boolean isSearchable() {
		return true;
	}
	
	/**
	 * Render icon representing thet symbol
	 * 
	 * @param componentId
	 * 			wicket component id corresponding to the symbol icon
	 * @return
	 * 			a wicket image representing icon of the symbol
	 */
	public abstract Image renderIcon(String componentId);
	
	/**
	 * Render the symbol in web UI
	 * 
	 * @param componentId
	 * 			wicket component id corresponding to this symbol
	 * @param highlight
	 * 			range of the name to be highlighted if not <tt>null</tt>. For instance, 
	 * 			when user searches for a symbol, this range will be used to highlight
	 * 			the matched search in the symbol name
	 * 			
	 * @return
	 * 			a wicket component to be displayed in web UI for the symbol
	 */
	public abstract Component render(String componentId, @Nullable Range highlight);
	
	/**
	 * Get the non-passthrough parent in outline
	 * 
	 * @return
	 * 			non-passthrough parent in outline, or <tt>null</tt> if the symbol does not have a non-passthrough 
	 * 			ancestor
	 */
	@Nullable
	public Symbol getOutlineParent() {
		Symbol parent = getParent();
		while (parent != null && !parent.isDisplayInOutline()) {
			parent = parent.getParent();
		}
		return parent;
	}
	
	/**
	 * Whether or not the symbol is local in the whole symbol hierarchy. A symbol local in hierarchy can not be 
	 * accessed outside of the containing file, and will not be considered for matching when doing symbol 
	 * cross-reference from another file
	 * 
	 * @return
	 * 			whether or not the symbol is local in the whole symbol hierarchy
	 */
    public boolean isLocalInHierarchy() {
    	Symbol current = this;
    	do {
    		if (current.isLocal())
    			return true;
    		current = current.getParent();
    	} while (current != null);
    	
    	return false;
    }
    
    /**
     * Get separator used to construct FQN of child symbols. For instance, if:
     * <ul>
     * <li> FQN of current symbol is &quot;namespace&quot;
     * <li> FQN separator is &quot;:&quot;
     * <li> name of child symbol is &quot;person&quot;
     * 
     * Then FQN of child symbol will be &quot;namespace:person&quot;
     * 
     * @return
     * 			FQN separator used to construct FQN of child symbols
     */
    public abstract String getFQNSeparator();
    
    protected String getFQNLeading() {
    	return "";
    }
    
    protected String getFQNElement() {
    	return getName();
    }

	/**
	 * Get fully qualified name of the symbol. When searching symbols, parent FQN of matched symbols will also be 
	 * displayed
	 * 
	 * @return
     *          fully qualified name of the symbol
	 */
	public final String getFQN() {
		String fqnElement = (getFQNElement()!=null?getFQNElement():"");
		if (getParent() != null) {
			return getParent().getFQN() + getParent().getFQNSeparator() + fqnElement;
		} else {
			return getFQNLeading() + fqnElement;
		}
	}
	
}
