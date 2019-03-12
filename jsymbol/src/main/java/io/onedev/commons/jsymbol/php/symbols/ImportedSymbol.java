package io.onedev.commons.jsymbol.php.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.php.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.Range;

public class ImportedSymbol extends PhpSymbol {

	private static final long serialVersionUID = 1L;
	
	public ImportedSymbol(PhpSymbol parent, String name, TokenPosition position, TokenPosition scope) {
		super(parent, name, position, scope);
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, "imp_obj.gif"));
		icon.add(AttributeAppender.append("title", "Imported object"));
		return icon;
	}

	@Override
	public Component render(String componentId, Range highlight) {
		return new HighlightableLabel(componentId, getName(), highlight);
	}

}
