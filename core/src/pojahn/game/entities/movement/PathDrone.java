package pojahn.game.entities.movement;

import com.badlogic.gdx.math.Vector2;
import pojahn.game.core.BaseLogic;
import pojahn.game.core.MobileEntity;
import pojahn.game.events.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PathDrone extends MobileEntity {

    private final List<Waypoint> waypoints;
    private boolean rock, skip;
    private int dataCounter, stillCounter;
    private boolean playEvent;

    public PathDrone(final float x, final float y) {
        move(x, y);
        waypoints = new ArrayList<>();
        dataCounter = stillCounter = 0;
    }

    public PathDrone getClone() {
        final PathDrone clone = new PathDrone(x(), y());
        copyData(clone);
        if (cloneEvent != null)
            cloneEvent.handleClonded(clone);

        return clone;
    }

    protected void copyData(final PathDrone clone) {
        super.copyData(clone);
        clone.waypoints.addAll(waypoints);
        clone.skip = skip;
        clone.rock = rock;
    }

    public void appendPath(final float x, final float y, final int frames, final boolean jump, final Event event) {
        waypoints.add(new Waypoint.StaticWaypoint(x, y, frames, jump, event));
    }

    public void appendPath(final Vector2 loc, final int frames, final boolean jump, final Event event) {
        waypoints.add(new Waypoint.StaticWaypoint(loc.x, loc.y, frames, jump, event));
    }

    public void appendPath(final Waypoint waypoint) {
        waypoints.add(waypoint);
    }

    public void appendPath(final Waypoint[] list) {
        waypoints.addAll(Arrays.asList(list));
    }

    public void appendPath(final float x, final float y) {
        appendPath(x, y, 0, false, null);
    }

    public void appendPath(final float x, final float y, final Event event) {
        waypoints.add(new Waypoint.StaticWaypoint(x, y, 0, false, event));
    }

    public void appendPath() {
        waypoints.add(new Waypoint.StaticWaypoint(x(), y(), 0, false, null));
    }

    public void appendReversed() {
        final List<Waypoint> reversed = new ArrayList<>(waypoints);
        reversed.remove(reversed.size() - 1);
        Collections.reverse(reversed);

        waypoints.addAll(reversed);
    }

    public void reverse() {
        Collections.reverse(waypoints);
    }

    public void clearData() {
        waypoints.clear();
        rollback();
    }

    private void rollback() {
        dataCounter = stillCounter = 0;
    }

    public void skipTo(final int index) {
        dataCounter = index;
        stillCounter = 0;
        final Waypoint wp = waypoints.get(index);
        move(wp.targetX(), wp.getTargetY());
    }

    @Override
    public void logistics() {
        if (!waypoints.isEmpty() && getMoveSpeed() > 0 && !isFrozen()) {
            if (dataCounter >= waypoints.size())
                dataCounter = 0;

            final Waypoint wp = waypoints.get(dataCounter);

            if (reached(wp)) {
                if (++stillCounter > wp.freezeFrames())
                    dataCounter++;

                forgetPast();

                bounds.pos.x = wp.targetX();
                bounds.pos.y = wp.getTargetY();

                final Event event = wp.getEvent();
                if (playEvent && event != null) {
                    event.eventHandling();
                    playEvent = false;
                }
            } else {
                playEvent = true;
                stillCounter = 0;

                if (wp.jump())
                    move(wp.targetX(), wp.getTargetY());
                else
                    moveTowards(wp.targetX(), wp.getTargetY());
            }
        }
    }

    @Override
    public void dumbMoveTowards(final float targetX, final float targetY, final float steps) {
        if (isFrozen())
            return;

        final Vector2 next = attemptTowards(targetX, targetY, steps);

        if (rock) {
            final boolean canNext = !occupiedAt(next.x, next.y);
            if (canNext)
                move(next.x, next.y);
            else if (skip)
                dataCounter++;
            else
                forgetPast();
        } else
            move(next.x, next.y);
    }

    @Override
    protected void smartMoveTowards(final float targetX, final float targetY, final float steps) {
        if (isFrozen())
            return;

        final Vector2 next = attemptTowards(targetX, targetY, steps);

        if (rock) {
            if (smartMove(next.x, next.y)) {

            } else if (skip)
                dataCounter++;
            else
                forgetPast();
        } else
            smartMove(next.x, next.y);
    }

    /**
     * A PathDrone that is rocky will respect walls and solid objects.
     *
     * @param rock True if this PathDrone respect walls.
     * @param skip Whether or not to skip the current waypoint and go for the next one when blocked.
     */
    public void setRock(final boolean rock, final boolean skip) {
        this.rock = rock;
        this.skip = skip;
    }

    private boolean reached(final Waypoint pd) {
        return getMoveSpeed() > BaseLogic.distance(pd.targetX(), pd.getTargetY(), x(), y());
    }
}