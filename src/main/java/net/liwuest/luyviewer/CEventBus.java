package net.liwuest.luyviewer;

import java.util.*;
import java.util.function.Consumer;

public final class CEventBus {
  public interface AbstractEvent {}

  private final static Map<Class<? extends AbstractEvent>, Set<Consumer<? extends AbstractEvent>>> EventListeners = new TreeMap<>();

  public static synchronized <T extends AbstractEvent> void subscribe(Consumer<T> Listener, Class<T> EventClass) {
    if ((null == Listener) || (null == EventClass)) return;
    EventListeners.putIfAbsent(EventClass, new LinkedHashSet<>());
    EventListeners.get(EventClass).add(Listener);
  }

  public static synchronized <T extends AbstractEvent> void unsubscribe(Consumer<T> Listener) {
    if (null == Listener) return;
    EventListeners.values().forEach(listeners -> listeners.remove(Listener));
  }

  public static synchronized <T extends AbstractEvent> void unsubscribe(Consumer<T> Listener, Class<T> EventClass) {
    if ((null == Listener) || (null == EventClass)) return;
    EventListeners.getOrDefault(EventClass, new LinkedHashSet<>()).remove(Listener);
  }

  public static synchronized <T extends AbstractEvent> void publish(T Event) {
    if (null == Event) return;
    EventListeners.getOrDefault(Event.getClass(), new LinkedHashSet<>()).forEach(c -> ((Consumer<T>)c).accept(Event));
  }
}
