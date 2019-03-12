package io.onedev.commons.jsymbol.go.symbols.ui;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.go.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.Range;

@SuppressWarnings("serial")
public class FunctionSymbolPanel extends Panel {

	private final FunctionSymbol functionSymbol;
	
	private final Range highlight;
	
	public FunctionSymbolPanel(String id, FunctionSymbol functionSymbol, Range highlight) {
		super(id);
		this.functionSymbol = functionSymbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new HighlightableLabel("name", functionSymbol.getName(), highlight));
		add(new Label("functionParams", functionSymbol.getFunctionParams()));
		add(new Label("type", functionSymbol.getType()).setVisible(functionSymbol.getType()!=null));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new GolangSymbolResourceReference()));
	}
	
}
