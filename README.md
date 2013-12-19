annoMVVM [![Build Status](https://travis-ci.org/davherrmann/annoMVVM.png?branch=master)](https://travis-ci.org/davherrmann/annoMVVM)
========

Simple MVVM framework for Java applications

##Configuring annoMVVM for Vaadin
###StateChangeListener
In order for the ```ViewModelComposer``` to know how to synchronise a viewmodel's changed ```State``` with the bound field in the view, a ```StateChangeListener``` has to be provided. There can be different listeners for different types of fields, in the following an example for providing a listener for Vaadin's ```AbstractField``` and for a ```State``` of this framework:
```java
  viewModelComposer.addStateChangeWrapper(AbstractField.class,
  		new StateChangeWrapper() {
  			@Override
  			public StateChangeListener getStateChangeListener(
  					final Object notified) {
  				return new StateChangeListener() {
  					@SuppressWarnings("unchecked")
  					@Override
  					public void stateChange(Object value) {
  						//synchronise the state by invoking AbstractField.setValue
  						((AbstractField<Object>) notified)
  								.setValue(value);
  					}
  				};
  			}
  		});
  
  viewModelComposer.addStateChangeWrapper(State.class,
  		new StateChangeWrapper() {
  			@Override
  			public StateChangeListener getStateChangeListener(
  					final Object notified) {
  				return new StateChangeListener() {
  					@SuppressWarnings("unchecked")
  					@Override
  					public void stateChange(Object value) {
  						//synchronise the state by invoking State.set
  						((State<Object>) notified).set(value);
  					}
  				};
  			}
  		});
```

###SourceWrapper
When defining source fields in the ```@BindAction``` annotation, a ```SourceWrapper``` for the type of a source field has to be provided. It specifies how the data should be extracted from the source field.
```java
  viewModelComposer.addSourceWrapper(AbstractField.class,
  		new SourceWrapper<Object>() {
  			@SuppressWarnings("unchecked")
  			@Override
  			public Object get(Object source) {
  				return ((AbstractField<Object>) source).getValue();
  			}
  		});
```

###ActionHandler
When no source fields are defined in the ```@BindAction``` annotation, an ```ActionHandler``` has to be provided, which specifies what data should be passed on to the invoked method in the viewmodel.
```java
  viewModelComposer.addActionWrapper(ValueChangeNotifier.class,
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
  								actionHandler.handle(event.getProperty().getValue());
  							}
  						});
  			}
  		});
  
  viewModelComposer.addActionWrapper(Button.class, new ActionWrapper() {
  	@Override
  	public void addActionHandler(Object notifier,
  			final ActionHandler actionHandler) {
  		((Button) notifier).addClickListener(new ClickListener() {
  			private static final long serialVersionUID = 3154305342571215268L;
  
  			@Override
  			public void buttonClick(ClickEvent event) {
  				actionHandler.handle(event.getButton().getData());
  			}
  		});
  	}
  });
```

##Usage of the annotations
Annotations can be used for either passing events in the view to the viewmodel or synchronising a state in the viewmodel with the counterpart in the view.

###@BindAction
For binding an action in a view to a method in a viewmodel, an ```ActionHandler``` has to be declared in the viewmodel:
```java
public interface DoLogout extends ActionHandler {}
```
The declared handler can then be used in a method annotation in the viewmodel:
```java
@HandlesAction(DoLogin.class)
public void doLogin(String username, String password) {
	//do something with username and password
}
```
Now the method can be bound in the view:
```java
private TextField user = new TextField("User:");
private PasswordField password = new PasswordField("Password:");

@BindAction(value = DoLogin.class, source = { "user", "password" })
private Button loginButton;
```
Make sure the fields in the view are initialised properly - otherwise you might get a ```NullPointerException``` when binding the view to the viewmodel

Author: David Herrmann, 2013
