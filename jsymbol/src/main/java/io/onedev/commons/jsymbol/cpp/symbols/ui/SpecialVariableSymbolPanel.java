package io.onedev.commons.jsymbol.cpp.symbols.ui;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.cpp.symbols.SpecialVariableSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.Range;

@SuppressWarnings("serial")
public class SpecialVariableSymbolPanel extends Panel {

	private final SpecialVariableSymbol specialVariableSymbol;
	
	private final Range highlight;
	
	public SpecialVariableSymbolPanel(String id, SpecialVariableSymbol specialVariableSymbol, Range highlight) {
		super(id);
		this.specialVariableSymbol = specialVariableSymbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new HighlightableLabel("name", specialVariableSymbol.getName(), highlight));
		
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CppSymbolResourceReference()));
	}
	
}
