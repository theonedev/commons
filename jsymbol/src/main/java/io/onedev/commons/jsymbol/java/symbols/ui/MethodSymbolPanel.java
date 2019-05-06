package io.onedev.commons.jsymbol.java.symbols.ui;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.java.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.LinearRange;

@SuppressWarnings("serial")
public class MethodSymbolPanel extends Panel {

	private final MethodSymbol methodSymbol;
	
	private final LinearRange highlight;
	
	public MethodSymbolPanel(String id, MethodSymbol methodSymbol, LinearRange highlight) {
		super(id);
		this.methodSymbol = methodSymbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new HighlightableLabel("name", methodSymbol.getName(), highlight));
		
		if (methodSymbol.getMethodParams() != null)
			add(new Label("methodParams", methodSymbol.getMethodParams()));
		else
			add(new WebMarkupContainer("methodParams").setVisible(false));
		
		if (methodSymbol.getTypeParams() != null) 
			add(new Label("typeParams", methodSymbol.getTypeParams()));
		else
			add(new WebMarkupContainer("typeParams").setVisible(false));
		
		add(new Label("type", methodSymbol.getType()).setVisible(methodSymbol.getType()!=null));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new JavaSymbolResourceReference()));
	}
	
}
