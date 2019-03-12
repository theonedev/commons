package io.onedev.commons.jsymbol.util;

import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.ResourceReference;

public class NoAntiCacheImage extends Image {

	public NoAntiCacheImage(String id, ResourceReference resourceReference, ResourceReference... resourceReferences) {
		super(id, resourceReference, resourceReferences);
	}

	private static final long serialVersionUID = 1L;

	@Override
	protected boolean shouldAddAntiCacheParameter() {
		return false;
	}

}
