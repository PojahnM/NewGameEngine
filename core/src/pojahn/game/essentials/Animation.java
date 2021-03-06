package pojahn.game.essentials;

import pojahn.game.events.Event;

import java.lang.reflect.Array;
import java.util.HashMap;

/**
 * This class is often used in game loops, where {@code getObject()} is called every frame, returning the next object in the array every time called(depends on the speed setting).<br>
 * Every instance of this class contains other than an object array, a counter and an index. The counter gets increased by one every time {@code getObject()} is called. Whenever the counter modulus(%) the speed is 0, the index is increased by one meaning the next element in the array is returned.<br>
 *
 * @param <T> Any type of object.
 * @author Pojahn Moradi
 */
public class Animation<T> {
    private int counter, speed, index, limit, holder;
    private boolean loop, stop, multiFaced, pingpong, returning, allowEvent;
    private HashMap<Integer, Event> events;
    T[] objs, tmpObjs;

//    @SafeVarargs
    public Animation(final T... objs) {
        this(1, objs);
    }

    /**
     * Constructs a {@code Frequency} object, with looping set to true and the limit set to the length of array.
     *
     * @param speed The speed.
     * @param objs  The objects array to use.
     */
//    @SafeVarargs
    public Animation(final int speed, final T... objs) {
        this.speed = speed;
        this.objs = objs;
        loop = true;
        limit = (objs != null) ? objs.length : 0;
        events = new HashMap<>();
    }

    /**
     * Returns the current object, without performing any updates or checks.
     *
     * @return The current object.
     */
    public T getCurrentObject() {
        return objs == null ? null : objs[index];
    }

    /**
     * Returns the current object, performing all types of checks and updates.
     *
     * @return The current object.
     */
    public T getObject() {
        if (objs == null)
            return null;

        final T obj = objs[index];

        if (stop)
            return obj;

        if (!allowEvent && holder != index)
            allowEvent = true;

        if (pingpong) {
            if (index >= limit - 1)
                returning = true;
            if (index == 0)
                returning = false;

            if (speed == 0 || ++counter % speed == 0) {
                if (returning)
                    index--;
                else
                    index++;
            }
        } else if (loop) {
            if (speed == 0 || ++counter % speed == 0)
                index++;

            if (index >= limit)
                index = 0;
        } else if (index < limit - 1 && (speed == 0 || ++counter % speed == 0)) {
            index++;
        }

        tryEvent(index);
        return obj;
    }

    private void tryEvent(final int index) {
        final Event event = events.get(index);
        if (event == null || !allowEvent)
            return;

        holder = index;
        allowEvent = false;
        event.eventHandling();
    }

    /**
     * The given {@code Event} will execute whenever {@code getObject()} return an object whom index(in the array) matches the events index.
     *
     * @param event The event.
     * @param index The index of the event.
     */
    public void addEvent(final Event event, final int index) {
        events.put(index, event);
    }

    /**
     * Pushes the index to the end. This means the next time {@code getObject()} is called, it will return array[limit](by default, this is the last element in the array).
     */
    public void pushToEnd() {
        index = limit - 1;
    }

    /**
     * The position to consider as the last element in the array.
     *
     * @param limit The limit.
     */
    public void setLimit(final int limit) {
        this.limit = limit;
    }

    /**
     * Setting this to true will change the limit to array.length / 8.<br>
     * This is required for multi faced {@code MovableObjects} to work properly.
     *
     * @param multi True if this is a multi faced unit.
     */
    public void setMultiFaced(final boolean multi) {
        if (multi)
            setLimit(objs.length / 8);
        this.multiFaced = multi;
    }

    /**
     * Checks if this unit multi faced.
     *
     * @return True if this unit is multifaced.
     */
    public boolean isMultiFaced() {
        return multiFaced;
    }

    /**
     * Return the current index.
     *
     * @return The index.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the index to the given position.
     *
     * @param index The index to use.
     */
    public void setIndex(final int index) {
        this.index = index;
    }

    /**
     * Returns the speed of this object.
     *
     * @return The speed.
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * Whether or not to loop. If looping is set to true, the index will reset when the limit has been reached. If false, the counter will stop updating once the limit is reached.
     *
     * @param loop True if this instance should loop.
     */
    public void setLoop(final boolean loop) {
        this.loop = loop;
    }

    /**
     * Overrides looping and decide whether or not this index should step back when reached the limit rather than reseting itself.
     *
     * @param pingpong True if this object should have "pingpong" behavior.
     */
    public void pingPong(final boolean pingpong) {
        this.pingpong = pingpong;
    }

    /**
     * Checks if this object is looping.
     *
     * @return True if this object is looping.
     */
    public boolean isLooping() {
        return loop;
    }

    /**
     * The array of objects to use. Resets the counter and index.
     *
     * @param objs The array to use.
     */
    public void setObjs(final T[] objs) {
        if (objs != null) {
            this.objs = objs;
            if (limit <= 0)
                limit = objs.length - 1;
        }
        index = counter = 0;
    }

    /**
     * Sets the speed and resets the counter and index.
     *
     * @param speed The speed to use.
     */
    public void setSpeed(final int speed) {
        this.speed = speed;
        index = counter = 0;
    }

    /**
     * Sets the index and counter to 0.
     */
    public void reset() {
        index = counter = 0;
    }

    /**
     * Whether or not to halt the counter, preventing it from updating.
     *
     * @param stop True to stop the counter from updating.
     */
    public void stop(final boolean stop) {
        this.stop = stop;
    }

    /**
     * True if this object is stopped.
     */
    public boolean isStopped() {
        return stop;
    }

    /**
     * Checks if the index has reached its limit.
     *
     * @return True if the limit has been reached.
     */
    public boolean hasEnded() {
        return index >= limit - 1;
    }

    /**
     * Returns the array that are being used.
     *
     * @return The array.
     */
    public T[] getArray() {
        return objs;
    }

    /**
     * Returns a clone of this object. Note that the array used is not cloned.
     *
     * @return The instance.
     */
    public Animation<T> getClone() {
        final Animation<T> freq = new Animation<T>(speed, objs);
        freq.limit = limit;
        freq.loop = loop;
        freq.stop = stop;
        freq.pingpong = pingpong;

        return freq;
    }

    /**
     * Returns a clone of this object, with the object array in reversed order.
     *
     * @param clazz The class of the object array.
     * @return The instance.
     */
    public Animation<T> getReversed(final Class<T> clazz) {
        @SuppressWarnings("unchecked") final T[] reversed = (T[]) Array.newInstance(clazz, objs.length);

        for (int i = 0; i < objs.length; i++)
            reversed[i] = objs[i];

        for (int left = 0, right = reversed.length - 1; left < right; left++, right--) {
            final T temp = reversed[left];
            reversed[left] = reversed[right];
            reversed[right] = temp;
        }

        final Animation<T> freq = getClone();
        freq.setObjs(reversed);

        return freq;
    }
}