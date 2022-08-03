package io.onedev.commons.bootstrap;

import java.util.Stack;

public interface SensitiveMasker {

	static ThreadLocal<Stack<SensitiveMasker>> stack =  new ThreadLocal<Stack<SensitiveMasker>>() {

		@Override
		protected Stack<SensitiveMasker> initialValue() {
			return new Stack<SensitiveMasker>();
		}
	
	};
	
	String mask(String text);
	
	static void push(SensitiveMasker masker) {
		stack.get().push(masker);
	}
	
	static void pop() {
		stack.get().pop();
	}

	static SensitiveMasker get() {
		if (!stack.get().isEmpty())  
			return stack.get().peek();
		else 
			return null;
	}	
	
}
