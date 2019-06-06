package io.onedev.commons.jsymbol.go.symbols.ui;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.go.symbols.VariableSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.LinearRange;

@SuppressWarnings("serial")
public class VariableSymbolPanel extends Panel {

	private final VariableSymbol variableSymbol;
	
	private final LinearRange highlight;
	
	public VariableSymbolPanel(String id, VariableSymbol variableSymbol, LinearRange highlight) {
		super(id);
		this.variableSymbol = variableSymbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new HighlightableLabel("name", variableSymbol.getName(), highlight));
		add(new Label("type", variableSymbol.getType()).setVisible(variableSymbol.getType()!=null));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new GolangSymbolResourceReference()));
	}
	
}
