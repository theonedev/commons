package io.onedev.commons.jsymbol.csharp.symbols.ui;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.csharp.symbols.FieldSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.Range;

@SuppressWarnings("serial")
public class FieldSymbolPanel extends Panel {

	private final FieldSymbol fieldSymbol;
	
	private final Range highlight;
	
	public FieldSymbolPanel(String id, FieldSymbol fieldSymbol, Range highlight) {
		super(id);
		this.fieldSymbol = fieldSymbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new HighlightableLabel("name", fieldSymbol.getQualifiedName(), highlight));
		add(new Label("indexParams", fieldSymbol.getIndexParams()).setVisible(fieldSymbol.getIndexParams() != null));
		add(new Label("type", fieldSymbol.getType()).setVisible(fieldSymbol.getType()!=null));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CSharpSymbolResourceReference()));
	}

}
