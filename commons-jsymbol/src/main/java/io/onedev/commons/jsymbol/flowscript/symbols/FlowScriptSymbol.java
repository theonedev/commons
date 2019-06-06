package io.onedev.commons.jsymbol.flowscript.symbols;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.utils.PlanarRange;

public abstract class FlowScriptSymbol extends Symbol {

	private static final long serialVersionUID = 1L;

	private FlowScriptSymbol parent;
	
	private String name;
	
	private PlanarRange position;
	
	private PlanarRange scope;
	
	private boolean searchable = true;
	
	private ModuleAccess moduleAccess = ModuleAccess.NORMAL;
	
	/**
	 * Whether or not the symbol is a property of its parent symbol. Property symbol can be referenced with property 
	 * path from parent symbol. Considering below code:
	 * <pre><code>
	 * function test() {
	 *   function inner() {
	 *   }
	 * }
	 * test.usage = "some usage guide";
	 * </code></pre>
	 * 
	 * The symbol <tt>usage</tt> is a property symbol, while <tt>inner</tt> is not
	 */
	private boolean property;
	
	@Override
    public FlowScriptSymbol getParent() {
		return parent;
	}

	public void setParent(FlowScriptSymbol parent) {
		this.parent = parent;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public PlanarRange getPosition() {
		return position;
	}

	public void setPosition(PlanarRange position) {
		this.position = position;
	}

	@Override
	public PlanarRange getScope() {
		return scope;
	}

	public void setScope(PlanarRange scope) {
		this.scope = scope;
	}

	@Override
	public boolean isSearchable() {
		return searchable;
	}

	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}

	public boolean isProperty() {
		return property;
	}

	public void setProperty(boolean property) {
		this.property = property;
	}

	public ModuleAccess getModuleAccess() {
		return moduleAccess;
	}

	public void setModuleAccess(ModuleAccess moduleAccess) {
		this.moduleAccess = moduleAccess;
	}

	@Override
	public boolean isLocal() {
		return moduleAccess != ModuleAccess.EXPORT 
				&& !isProperty() 
				&& (getParent() != null || moduleAccess == ModuleAccess.IMPORT);
	}

	@Override
	public boolean isPrimary() {
		return moduleAccess == ModuleAccess.EXPORT;
	}

	@Override
	public String getFQNSeparator() {
		return ".";
	}

}
