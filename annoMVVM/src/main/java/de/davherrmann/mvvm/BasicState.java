package de.davherrmann.mvvm;

import java.util.ArrayList;
import java.util.Collection;

public class BasicState<StateType> implements State<StateType> {
	private StateType value;
	private Class<StateType> stateType;
	private Collection<StateChangeListener> listeners = new ArrayList<StateChangeListener>();

	public BasicState(Class<StateType> stateType) {
		this.stateType = stateType;
	}

	@Override
	public StateType get() {
		return value;
	}

	@Override
	public void set(StateType value) {
		if (this.value != value) {
			this.value = value;
			notifyAllListeners();
		}
	}

	@Override
	public Class<StateType> getStateType() {
		return stateType;
	}

	@Override
	public boolean addStateChangeListener(StateChangeListener listener) {
		return listeners.add(listener);
	}

	@Override
	public boolean removeStateChangeListener(StateChangeListener listener) {
		return listeners.remove(listener);
	}

	@Override
	public void notifyListener(StateChangeListener listener) {
		listener.stateChange(get());
	}

	@Override
	public void notifyAllListeners() {
		for (StateChangeListener listener : listeners) {
			notifyListener(listener);
		}
	}
}
