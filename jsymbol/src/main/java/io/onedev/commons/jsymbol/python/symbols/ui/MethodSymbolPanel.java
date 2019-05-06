package io.onedev.commons.jsymbol.python.symbols.ui;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.python.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.LinearRange;

@SuppressWarnings("serial")
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
			
		add(new HighlightableLabel("name", symbol.getName(), highlight));
		add(new Label("params", symbol.getParameters()));
	}

}
