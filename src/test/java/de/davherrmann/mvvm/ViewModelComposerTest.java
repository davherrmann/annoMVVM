package de.davherrmann.mvvm;

import static org.junit.Assert.*;

import org.junit.Test;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Property.ValueChangeNotifier;
import com.vaadin.ui.AbstractField;

public class ViewModelComposerTest {
	
	private ViewModelComposer viewModelComposer = new ViewModelComposer();
	
	@Test
	public void testAddStateChangeWrapper() {
		assertNull(viewModelComposer.addStateChangeWrapper(AbstractField.class,
				new StateChangeWrapper() {
					@Override
					public StateChangeListener getStateChangeListener(
							final Object notified) {
						return new StateChangeListener() {
							@SuppressWarnings("unchecked")
							@Override
							public void stateChange(Object value) {
								((AbstractField<Object>) notified)
										.setValue(value);
							}
						};
					}
				}));
	}

	@Test
	public void testAddActionWrapper() {
		assertNull(viewModelComposer.addActionWrapper(ValueChangeNotifier.class,
				new ActionWrapper() {
			@Override
			public void addActionHandler(Object notifier,
					final ActionHandler actionHandler) {
				((ValueChangeNotifier) notifier)
						.addValueChangeListener(new ValueChangeListener() {
							private static final long serialVersionUID = -854400079672018869L;

							@Override
							public void valueChange(
									ValueChangeEvent event) {
								actionHandler.handle(event
										.getProperty().getValue());
							}
						});
			}
		}));
	}

	@Test
	public void testBind() {
		assertNotNull("Not yet implemented");
	}

}
