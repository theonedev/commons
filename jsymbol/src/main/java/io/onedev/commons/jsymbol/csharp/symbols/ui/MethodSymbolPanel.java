package io.onedev.commons.jsymbol.csharp.symbols.ui;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.csharp.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.Range;

@SuppressWarnings("serial")
public class MethodSymbolPanel extends Panel {

	private final MethodSymbol methodSymbol;
	
	private final Range highlight;
	
	public MethodSymbolPanel(String id, MethodSymbol methodSymbol, Range highlight) {
		super(id);
		this.methodSymbol = methodSymbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		if (methodSymbol.getMethodPrefix() != null)
			add(new Label("prefix", methodSymbol.getMethodPrefix()));
		else
			add(new WebMarkupContainer("prefix").setVisible(false));
		add(new HighlightableLabel("name", methodSymbol.getQualifiedName(), highlight));
		
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
		response.render(CssHeaderItem.forReference(new CSharpSymbolResourceReference()));
	}
	
}
