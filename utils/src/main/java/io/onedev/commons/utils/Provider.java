package io.onedev.commons.utils;

import java.io.Serializable;

public interface Provider<T> extends Serializable {
    T get();
}