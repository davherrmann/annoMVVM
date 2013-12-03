package de.davherrmann.mvvm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

import de.davherrmann.mvvm.annotations.AfterVMBinding;
import de.davherrmann.mvvm.annotations.BindAction;
import de.davherrmann.mvvm.annotations.BindState;
import de.davherrmann.mvvm.annotations.HandlesAction;
import de.davherrmann.mvvm.annotations.ProvidesState;

public class ViewModelComposer {
	private Map<Class<?>, StateChangeWrapper> stateChangeWrappers = new HashMap<>();
	private Map<Class<?>, ActionWrapper> actionWrappers = new HashMap<>();
	private Map<Object, Boolean> circularActionStateUpdatingFields = new WeakHashMap<>();

	public StateChangeWrapper addStateChangeWrapper(Class<?> boundObjectType,
			StateChangeWrapper stateChangeWrapper) {
		return stateChangeWrappers.put(boundObjectType, stateChangeWrapper);
	}

	public ActionWrapper addActionWrapper(Class<?> boundObjectType,
			ActionWrapper actionWrapper) {
		return actionWrappers.put(boundObjectType, actionWrapper);
	}

	public void bind(Object view, Object... viewModels)
			throws IllegalAccessException, NoSuchElementException,
			UnsupportedOperationException {
		bindStates(view, viewModels);
		bindActionHandlers(view, viewModels);
		callAfterVMBinding(view);
		for (Object viewModel : viewModels) {
			callAfterVMBinding(viewModel);
		}
	}

	public void bindState(Object fieldInstance,
			Class<? extends State<?>> stateType, Object... viewModels)
			throws IllegalAccessException, NoSuchElementException,
			UnsupportedOperationException {

		StateChangeWrapper stateChangeWrapper = null;
		for (Class<?> key : stateChangeWrappers.keySet()) {
			if (key.isAssignableFrom(fieldInstance.getClass())) {
				stateChangeWrapper = stateChangeWrappers.get(key);
				break;
			}
		}

		bindState(fieldInstance, stateType, stateChangeWrapper, viewModels);
	}

	public void bindState(Object fieldInstance,
			Class<? extends State<?>> stateType,
			StateChangeWrapper stateChangeWrapper, Object... viewModels)
			throws IllegalAccessException, NoSuchElementException,
			UnsupportedOperationException {
		State<?> state = null;
		for (Object viewModel : viewModels) {
			state = findStateInstance(viewModel, stateType);
			if (state != null) {
				break;
			}
		}
		if (state == null) {
			throw new NoSuchElementException("Provider for State " + stateType
					+ " not found: Check annotation, instantiation and type.");
		}

		if (stateChangeWrapper == null) {
			throw new NoSuchElementException("No "
					+ StateChangeWrapper.class.getSimpleName() + " for "
					+ fieldInstance.getClass() + " found.");
		}

		circularActionStateUpdatingFields.put(fieldInstance, false);

		StateChangeListener stateChangeListener = stateChangeWrapper
				.getStateChangeListener(fieldInstance);

		state.addStateChangeListener(stateChangeListener);
		state.notifyListener(stateChangeListener);
	}

	private void bindStates(Object view, Object... viewModels)
			throws IllegalAccessException, NoSuchElementException,
			UnsupportedOperationException {

		for (final Field field : view.getClass().getDeclaredFields()) {
			BindState bindStateAnnotation = field
					.getAnnotation(BindState.class);
			if (bindStateAnnotation == null) {
				continue;
			}

			if (!Modifier.isPublic(field.getModifiers())) {
				field.setAccessible(true);
			}

			final Object fieldInstance = field.get(view);

			bindState(fieldInstance, bindStateAnnotation.value(), viewModels);
		}
	}

	private State<?> findStateInstance(Object viewModel,
			Class<? extends State<?>> stateType) throws IllegalAccessException,
			UnsupportedOperationException {
		checkForCorrectAnnotations(viewModel);
		for (Field field : viewModel.getClass().getDeclaredFields()) {
			ProvidesState providesStateAnnotation = field
					.getAnnotation(ProvidesState.class);
			if (providesStateAnnotation == null
					|| providesStateAnnotation.value() != stateType) {
				continue;
			}

			if (!State.class.isAssignableFrom(field.getType())) {
				continue;
			}

			if (!Modifier.isPublic(field.getModifiers())) {
				field.setAccessible(true);
			}

			try {
				return (State<?>) field.get(viewModel);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				continue;
			}
		}
		return null;
	}

	public void bindActionHandler(final Object fieldInstance,
			Class<? extends ActionHandler> actionHandlerType,
			Object... viewModels) throws IllegalAccessException,
			UnsupportedOperationException {

		ActionWrapper actionWrapper = null;
		for (Class<?> key : actionWrappers.keySet()) {
			if (key.isAssignableFrom(fieldInstance.getClass())) {
				actionWrapper = actionWrappers.get(key);
				break;
			}
		}
		bindActionHandler(fieldInstance, actionHandlerType, actionWrapper,
				viewModels);
	}

	public void bindActionHandler(final Object fieldInstance,
			final Class<? extends ActionHandler> actionHandlerType,
			ActionWrapper actionWrapper, Object... viewModels)
			throws IllegalAccessException, UnsupportedOperationException {

		Method actionHandler = null;
		Object viewModel = null;
		for (Object viewModelObject : viewModels) {
			actionHandler = findActionHandlerInstance(viewModelObject,
					actionHandlerType);
			viewModel = viewModelObject;
			if (actionHandler != null) {
				break;
			}
		}

		if (actionHandler == null || viewModel == null) {
			throw new NoSuchElementException("Handler for Action "
					+ actionHandlerType
					+ " not found: Check annotation, instantiation and type.");
		}

		if (actionWrapper == null) {
			throw new NoSuchElementException("No "
					+ ActionWrapper.class.getSimpleName() + " for "
					+ fieldInstance.getClass() + " found.");
		}

		final Method actionHandlerTarget = actionHandler;
		final Object viewModelTarget = viewModel;

		actionWrapper.addActionHandler(fieldInstance, new ActionHandler() {
			@Override
			public <ActionDataType> void handle(ActionDataType actionData)
					throws UnsupportedOperationException {
				if (circularActionStateUpdatingFields
						.containsKey(fieldInstance)
						&& circularActionStateUpdatingFields.get(fieldInstance)) {
					return;
				}
				if (circularActionStateUpdatingFields
						.containsKey(fieldInstance)) {
					circularActionStateUpdatingFields.put(fieldInstance, true);
				}
				try {
					actionHandlerTarget.invoke(viewModelTarget, actionData);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | ClassCastException e) {
					throw new UnsupportedOperationException("Invoking "
							+ ActionHandler.class.getSimpleName() + " "
							+ actionHandlerTarget.getName() + " in "
							+ viewModelTarget.getClass().getSimpleName()
							+ " for " + actionHandlerType.getName()
							+ " event of "
							+ fieldInstance.getClass().getSimpleName()
							+ " failed: check amount and type of parameters.");
				} finally {
					circularActionStateUpdatingFields.put(fieldInstance, false);
				}
			}
		});
	}

	private void bindActionHandlers(Object view, Object... viewModels)
			throws IllegalAccessException, UnsupportedOperationException {
		for (Field field : view.getClass().getDeclaredFields()) {
			BindAction bindActionAnnotation = field
					.getAnnotation(BindAction.class);
			if (bindActionAnnotation == null) {
				continue;
			}

			if (!Modifier.isPublic(field.getModifiers())) {
				field.setAccessible(true);
			}

			final Object fieldInstance = field.get(view);

			bindActionHandler(fieldInstance, bindActionAnnotation.value(),
					viewModels);
		}
	}

	private Method findActionHandlerInstance(Object viewModel,
			Class<? extends ActionHandler> actionHandlerType)
			throws IllegalAccessException, UnsupportedOperationException {
		checkForCorrectAnnotations(viewModel);
		for (Method method : viewModel.getClass().getDeclaredMethods()) {
			HandlesAction handlesActionAnnotation = method
					.getAnnotation(HandlesAction.class);
			if (handlesActionAnnotation == null
					|| handlesActionAnnotation.value() != actionHandlerType) {
				continue;
			}

			if (!Modifier.isPublic(method.getModifiers())) {
				method.setAccessible(true);
			}

			return method;
		}
		return null;
	}

	private void checkForCorrectAnnotations(Object object)
			throws UnsupportedOperationException {
		Map<Annotation, Object> annotationValueMapping = new HashMap<>();
		for (Field field : object.getClass().getDeclaredFields()) {
			ProvidesState providesStateAnnotation = field
					.getAnnotation(ProvidesState.class);
			if (providesStateAnnotation != null) {
				if (annotationValueMapping.put(providesStateAnnotation,
						providesStateAnnotation.value()) != null) {
					throw new UnsupportedOperationException("More than one @"
							+ ProvidesState.class.getSimpleName() + " for "
							+ providesStateAnnotation.value().getName()
							+ " used in " + object.getClass().getName());
				}
			}
			HandlesAction handlesActionAnnotation = field
					.getAnnotation(HandlesAction.class);
			if (handlesActionAnnotation != null) {
				if (annotationValueMapping.put(handlesActionAnnotation,
						handlesActionAnnotation.value()) != null) {
					throw new UnsupportedOperationException("More than one @"
							+ HandlesAction.class.getSimpleName() + " for "
							+ handlesActionAnnotation.value().getName()
							+ " used in " + object.getClass().getName());
				}
			}
		}
	}

	private void callAfterVMBinding(Object boundObject)
			throws IllegalAccessException {
		for (Method method : boundObject.getClass().getDeclaredMethods()) {
			method.setAccessible(true);
			if (method.getAnnotation(AfterVMBinding.class) != null) {
				try {
					method.invoke(boundObject);
				} catch (IllegalArgumentException | InvocationTargetException e) {
					continue;
				}
			}
		}
	}
}
