package io.onedev.commons.jsymbol.cpp.symbols.ui;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.cpp.symbols.TypedefSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.Range;

@SuppressWarnings("serial")
public class TypedefSymbolPanel extends Panel {

	private final TypedefSymbol typedefSymbol;
	
	private final Range highlight;
	
	public TypedefSymbolPanel(String id, TypedefSymbol typedefSymbol, Range highlight) {
		super(id);
		this.typedefSymbol = typedefSymbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new HighlightableLabel("name", typedefSymbol.getName(), highlight));
		
		add(new Label("type", typedefSymbol.getType()).setVisible(typedefSymbol.getType()!=null));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CppSymbolResourceReference()));
	}
	
}
