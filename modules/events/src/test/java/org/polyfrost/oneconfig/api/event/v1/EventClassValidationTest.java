package org.polyfrost.oneconfig.api.event.v1;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;
import org.polyfrost.oneconfig.api.event.v1.internal.EventClassValidator;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventClassValidationTest {
    @Test
    void creatingHandlerForParentEventTypeFailsAndSuggestsConcreteSubtypes() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> EventHandler.of(TickEvent.class, (Consumer<TickEvent>) event -> {
                }));
        assertTrue(e.getMessage().contains("TickEvent.Start"));
        assertTrue(e.getMessage().contains("TickEvent.End"));
    }

    @Test
    void creatingHandlerForConcreteEventTypeSucceeds() {
        assertDoesNotThrow(() -> EventHandler.of(TickEvent.Start.class, (Consumer<TickEvent.Start>) event -> {
        }).register().unregister());
    }

    @Test
    void registeringCustomHandlerForParentEventTypeFails() {
        EventHandler<TickEvent> handler = new EventHandler<TickEvent>() {
            @Override
            public boolean handle(TickEvent event) {
                return false;
            }

            @Override
            public Class<TickEvent> getEventClass() {
                return TickEvent.class;
            }
        };
        assertThrows(IllegalArgumentException.class, () -> EventManager.INSTANCE.register(handler));
    }

    @Test
    void subscribeMethodWithParentEventTypeFailsWithMethodNamed() {
        EventException e = assertThrows(
                EventException.class,
                () -> EventManager.INSTANCE.register(new BadSubscriber()));
        assertTrue(e.getMessage().contains("onTick"));
        assertTrue(e.getMessage().contains("TickEvent.Start"));
    }

    @Test
    void subscribeMethodWithNonEventParameterFails() {
        EventException e = assertThrows(
                EventException.class,
                () -> EventManager.INSTANCE.register(new NotAnEventSubscriber()));
        assertTrue(e.getMessage().contains("does not implement Event"));
    }

    @Test
    void subscribeMethodWithConcreteEventTypeSucceeds() {
        GoodSubscriber subscriber = new GoodSubscriber();
        assertDoesNotThrow(() -> {
            EventManager.INSTANCE.register(subscriber, true);
            EventManager.INSTANCE.unregister(subscriber);
        });
    }

    @Test
    void validatorAcceptsConcreteTypesAndRejectsAbstractOnes() {
        assertNull(EventClassValidator.describeProblem(TickEvent.End.class));
        assertTrue(EventClassValidator.describeProblem(TickEvent.class).contains("abstract"));
        assertTrue(EventClassValidator.describeProblem(Event.class).contains("abstract"));
        assertTrue(EventClassValidator.describeProblem(String.class).contains("does not implement Event"));
    }

    public static class BadSubscriber {
        @Subscribe
        public void onTick(TickEvent event) {
        }
    }

    public static class NotAnEventSubscriber {
        @Subscribe
        public void onSomething(String value) {
        }
    }

    public static class GoodSubscriber {
        @Subscribe
        public void onTick(TickEvent.End event) {
        }
    }
}
