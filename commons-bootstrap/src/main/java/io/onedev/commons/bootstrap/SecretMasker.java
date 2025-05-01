package io.onedev.commons.bootstrap;

import java.io.Serializable;
import java.util.Collection;
import java.util.Stack;

public interface SecretMasker extends Serializable {

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
	
	static SecretMasker create(Collection<String> secrets, String mask) {
		return new SecretMasker() {

			@Override
			public String mask(String value) {
				for (var secret: secrets) 
					value = value.replace(secret, mask);
				return value;
			}

		};
	}

}
