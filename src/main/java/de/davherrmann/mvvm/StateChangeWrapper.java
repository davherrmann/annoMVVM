package de.davherrmann.mvvm;

public interface StateChangeWrapper {
	public StateChangeListener getStateChangeListener(final Object notified);
}
