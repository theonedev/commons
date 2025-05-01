package io.onedev.commons.jsymbol.flowscript.symbols.ui;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.flowscript.symbols.MethodAccess;
import io.onedev.commons.jsymbol.flowscript.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.LinearRange;

public class MethodSymbolPanel extends Panel {

	private final MethodSymbol symbol;
	
	private final LinearRange highlight;
	
	public MethodSymbolPanel(String id, MethodSymbol symbol, LinearRange highlight) {
		super(id);
		this.symbol = symbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		if (symbol.getMethodAccess() == MethodAccess.GET)
			add(new Label("accessor", "get"));
		else if (symbol.getMethodAccess() == MethodAccess.SET)
			add(new Label("accessor", "set"));
		else
			add(new WebMarkupContainer("accessor").setVisible(false));
		
		/*
		 * highlight only applies to indexed/searchable name
		 */
		add(new HighlightableLabel("name", symbol.getName(), highlight));
		
		add(new Label("params", symbol.getParameters()));
	}

}
