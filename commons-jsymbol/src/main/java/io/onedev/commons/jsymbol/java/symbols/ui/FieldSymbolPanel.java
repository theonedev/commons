package io.onedev.commons.jsymbol.java.symbols.ui;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.java.symbols.FieldSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.LinearRange;

public class FieldSymbolPanel extends Panel {

	private final FieldSymbol fieldSymbol;
	
	private final LinearRange highlight;
	
	public FieldSymbolPanel(String id, FieldSymbol fieldSymbol, LinearRange highlight) {
		super(id);
		this.fieldSymbol = fieldSymbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new HighlightableLabel("name", fieldSymbol.getName(), highlight));
		add(new Label("type", fieldSymbol.getType()).setVisible(fieldSymbol.getType()!=null));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new JavaSymbolResourceReference()));
	}

}
