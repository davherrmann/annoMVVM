package de.davherrmann.mvvm;

public interface SourceWrapper<SourceType> {
	public SourceType get(final Object source);
}
