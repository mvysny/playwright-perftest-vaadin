package com.vaadin.starter.skeleton;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.starter.skeleton.services.Services;

/**
 * The main view contains a button and a click listener.
 */
@Route("")
public class MainView extends VerticalLayout {

    public MainView() {
        // Use TextField for standard text input
        TextField textField = new TextField("Your name");
        textField.addClassName("bordered");
        textField.setId("nameField");

        // Button click listeners can be defined as lambda expressions
        Button button = new Button("Say hello",
                e -> Notification.show(Services.getGreetService().greet(textField.getValue())));
        button.setId("sayHelloButton");

        // Theme variants give you predefined extra styles for components.
        // Example: Primary button is more prominent look.
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // You can specify keyboard shortcuts for buttons.
        // Example: Pressing 'enter' in this view clicks the Button.
        button.addClickShortcut(Key.ENTER);

        add(textField, button);
    }
}
