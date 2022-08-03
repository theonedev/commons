package io.onedev.commons.utils.command;

import java.util.Stack;

public interface SecretMasker {

	static ThreadLocal<Stack<SecretMasker>> stack =  new ThreadLocal<Stack<SecretMasker>>() {

		@Override
		protected Stack<SecretMasker> initialValue() {
			return new Stack<SecretMasker>();
		}
	
	};
	
	String mask(String text);
	
	static void push(SecretMasker masker) {
		stack.get().push(masker);
	}
	
	static void pop() {
		stack.get().pop();
	}

	static SecretMasker get() {
		if (!stack.get().isEmpty())  
			return stack.get().peek();
		else 
			return null;
	}	
	
}
