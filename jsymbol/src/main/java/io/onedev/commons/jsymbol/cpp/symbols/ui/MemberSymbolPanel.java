package io.onedev.commons.jsymbol.cpp.symbols.ui;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.commons.jsymbol.cpp.symbols.MemberSymbol;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.utils.Range;

@SuppressWarnings("serial")
public class MemberSymbolPanel extends Panel {

	private final MemberSymbol memberSymbol;
	
	private final Range highlight;
	
	public MemberSymbolPanel(String id, MemberSymbol memberSymbol, Range highlight) {
		super(id);
		this.memberSymbol = memberSymbol;
		this.highlight = highlight;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new HighlightableLabel("name", memberSymbol.getName(), highlight));
		
		add(new Label("type", memberSymbol.getType()).setVisible(memberSymbol.getType()!=null));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CppSymbolResourceReference()));
	}
	
}
