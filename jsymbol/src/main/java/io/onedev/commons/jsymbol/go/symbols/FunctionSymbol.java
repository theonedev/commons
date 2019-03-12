package io.onedev.commons.jsymbol.go.symbols;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.go.symbols.ui.FunctionSymbolPanel;
import io.onedev.commons.jsymbol.go.symbols.ui.icon.IconLocator;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

public class FunctionSymbol extends GolangSymbol {

	private static final long serialVersionUID = 1L;

	private final String type;
	
	private final String functionParams;
	
	private final String receiver;
	
	public FunctionSymbol(@Nullable GolangSymbol parent, String methodName, String functionParams, 
			@Nullable String returnType, @Nullable String receiver, TokenPosition position, 
			@Nullable TokenPosition scope) {
		super(parent, methodName, position, scope);

		this.type = returnType;
		this.functionParams = functionParams;
		this.receiver = receiver;
	}
	
	public String getType() {
		return type;
	}

	public String getFunctionParams() {
		return functionParams;
	}

	public String getReceiver() {
		return receiver;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new FunctionSymbolPanel(componentId, this, highlight);
	}

	@Override
	public Image renderIcon(String componentId) {
		String icon = Character.isUpperCase(getName().charAt(0))? "Method_purple_16x.png": "MethodPrivate_16x.png";
		return new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, icon));
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

}
