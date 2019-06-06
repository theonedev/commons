package io.onedev.commons.jsymbol.java.symbols.ui;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.java.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.LinearRange;

@SuppressWarnings("serial")
public class TypeSymbolPanel extends Panel {

	private final TypeSymbol typeSymbol;
	
	private final LinearRange highlight;
	
	public TypeSymbolPanel(String id, TypeSymbol typeSymbol, LinearRange highlight) {
		super(id);
		this.typeSymbol = typeSymbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new HighlightableLabel("name", typeSymbol.getName(), highlight));

		if (typeSymbol.getTypeParams() != null) 
			add(new Label("typeParams", typeSymbol.getTypeParams()));
		else
			add(new WebMarkupContainer("typeParams").setVisible(false));
	}

}
