package io.onedev.commons.jsymbol.python.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;

import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.python.symbols.ui.icons.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.utils.LinearRange;

public class ImportedSymbol extends PythonSymbol {

	private static final long serialVersionUID = 1L;

	private final QualifiedName qualifiedName;
	
	public ImportedSymbol(PythonSymbol parent, QualifiedName qualifiedName, PlanarRange position, PlanarRange scope) {
		super(parent, qualifiedName.getUnqualified(), position, scope);
		this.qualifiedName = qualifiedName;
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public boolean isPrimary() {
		return false;
	}

	public QualifiedName getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, 
				new PackageResourceReference(IconLocator.class, "imp_obj.gif"));
		icon.add(AttributeAppender.append("title", "imported object"));
		return icon;
	}

	@Override
	public Component render(String componentId, LinearRange highlight) {
		return new HighlightableLabel(componentId, qualifiedName, highlight);
	}

}
