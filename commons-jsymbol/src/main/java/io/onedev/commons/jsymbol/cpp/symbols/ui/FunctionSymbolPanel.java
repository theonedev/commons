package io.onedev.commons.jsymbol.cpp.symbols.ui;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.cpp.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.LinearRange;

@SuppressWarnings("serial")
public class FunctionSymbolPanel extends Panel {

	private final FunctionSymbol functionSymbol;
	
	private final LinearRange highlight;
	
	public FunctionSymbolPanel(String id, FunctionSymbol functionSymbol, LinearRange highlight) {
		super(id);
		this.functionSymbol = functionSymbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new HighlightableLabel("name", functionSymbol.getName(), highlight));
		
		if (functionSymbol.getParams() != null)
			add(new Label("params", functionSymbol.getParams()));
		else
			add(new WebMarkupContainer("params").setVisible(false));
		
		add(new WebMarkupContainer("definition").setVisible(functionSymbol.isDefinition()));
		
		add(new Label("type", functionSymbol.getType()).setVisible(functionSymbol.getType()!=null));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CppSymbolResourceReference()));
	}
	
}
