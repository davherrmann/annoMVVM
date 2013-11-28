package de.davherrmann.mvvm;

public interface State<StateType> {
	public StateType get();
	public void set(StateType state);
	public boolean addStateChangeListener(StateChangeListener listener);
	public boolean removeStateChangeListener(StateChangeListener listener);
	public void notifyListener(StateChangeListener listener);
	public void notifyAllListeners();
	public Class<StateType> getStateType();
}