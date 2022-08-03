package io.onedev.commons.bootstrap;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MaskingPatternLayout extends PatternLayout {

	@Override
	public String doLayout(ILoggingEvent event) {
		String layout = super.doLayout(event);
		SensitiveMasker masker = SensitiveMasker.get();
		if (masker != null)
			return masker.mask(layout);
		else
			return layout;
	}

}
