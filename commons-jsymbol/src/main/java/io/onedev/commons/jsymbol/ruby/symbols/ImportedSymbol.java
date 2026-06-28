package io.onedev.commons.jsymbol.ruby.symbols;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.ruby.symbols.ui.icons.IconLocator;
import io.onedev.commons.jsymbol.util.HighlightableLabel;
import io.onedev.commons.jsymbol.util.NoAntiCacheImage;
import io.onedev.commons.utils.LinearRange;
import io.onedev.commons.utils.PlanarRange;

public class ImportedSymbol extends RubySymbol {

	private static final long serialVersionUID = 1L;

	private final String imported;

	public ImportedSymbol(@Nullable RubySymbol parent, String imported, PlanarRange position, PlanarRange scope) {
		super(parent, imported, position, scope);
		this.imported = imported;
	}

	public String getImported() {
		return imported;
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
	public boolean isSearchable() {
		return false;
	}

	@Override
	public Image renderIcon(String componentId) {
		Image icon = new NoAntiCacheImage(componentId, new PackageResourceReference(IconLocator.class, "imp_obj.gif"));
		icon.add(AttributeAppender.append("title", "require"));
		return icon;
	}

	@Override
	public Component render(String componentId, @Nullable LinearRange highlight) {
		return new HighlightableLabel(componentId, imported, highlight);
	}

}
