package io.onedev.commons.jsymbol.python.symbols.ui;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.python.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.LinearRange;

public class ClassSymbolPanel extends Panel {

	private final ClassSymbol symbol;
	
	private final LinearRange highlight;
	
	public ClassSymbolPanel(String id, ClassSymbol symbol, LinearRange highlight) {
		super(id);
		this.symbol = symbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
			
		add(new HighlightableLabel("name", symbol.getName(), highlight));
		add(new Label("params", symbol.getParameters()).setVisible(symbol.getParameters() != null));
	}

}
