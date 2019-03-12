package io.onedev.commons.jsymbol.php.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.php.symbols.icon.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.Range;

public class NamespaceSymbol extends PhpSymbol {

	private static final long serialVersionUID = 1L;

	private final QualifiedName qualifiedName;
	
	public NamespaceSymbol(PhpSymbol parent, QualifiedName qualifiedName, TokenPosition position, TokenPosition scope) {
		super(parent, qualifiedName.getUnqualified(), position, scope);
		this.qualifiedName = qualifiedName;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, "namespace_obj.png"));
		icon.add(AttributeAppender.append("title", "Namespace"));
		return icon;
	}

	@Override
	protected String getFQNElement() {
		return qualifiedName.toString();
	}
	
	@Override
	public Component render(String componentId, Range highlight) {
		return new HighlightableLabel(componentId, qualifiedName, highlight);
	}
	
}
