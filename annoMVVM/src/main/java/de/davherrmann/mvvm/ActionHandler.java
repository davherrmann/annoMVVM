package de.davherrmann.mvvm;

public interface ActionHandler {
	public <ActionDataType> void handle(ActionDataType actionData) throws UnsupportedOperationException;
}