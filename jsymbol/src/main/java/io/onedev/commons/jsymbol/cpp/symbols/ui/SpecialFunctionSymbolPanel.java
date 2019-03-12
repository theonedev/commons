package io.onedev.commons.jsymbol.cpp.symbols.ui;


import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.cpp.symbols.SpecialFunctionSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.Range;

@SuppressWarnings("serial")
public class SpecialFunctionSymbolPanel extends Panel {

	private final SpecialFunctionSymbol specialFunction;
	
	private final Range highlight;
	
	public SpecialFunctionSymbolPanel(String id, SpecialFunctionSymbol specialFunction, Range highlight) {
		super(id);
		this.specialFunction = specialFunction;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new HighlightableLabel("name", specialFunction.getName(), highlight));
		
		if (specialFunction.getParams() != null)
			add(new Label("params", specialFunction.getParams()));
		else
			add(new WebMarkupContainer("params").setVisible(false));
		
		add(new WebMarkupContainer("definition").setVisible(specialFunction.isDefinition()));
		
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CppSymbolResourceReference()));
	}
	
}
