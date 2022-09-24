package io.onedev.commons.utils.command;

public class PtyMode {

	private ResizeSupport resizeSupport;
	
	public ResizeSupport getResizeSupport() {
		return resizeSupport;
	}

	public void setResizeSupport(ResizeSupport resizeSupport) {
		this.resizeSupport = resizeSupport;
	}

	public static interface ResizeSupport {
		
		void resize(int rows, int cols);
		
	}
	
}
