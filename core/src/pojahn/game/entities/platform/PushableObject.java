package pojahn.game.entities.platform;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import pojahn.game.core.MobileEntity;
import pojahn.game.essentials.Hitbox;
import pojahn.game.events.Event;
import pojahn.lang.Obj;

import java.util.List;

import static pojahn.game.core.BaseLogic.rectanglesCollide;

public class PushableObject extends MobileEntity {

    public float mass, gravity, damping, fallSpeedLimit, deacceleration, pushStrength;
    private Vector2 vel;
    private boolean useGravity, mustStand;
    private final List<MobileEntity> pushers;
    private Event slamEvent;
    private Rectangle dummy;
    private Sound landingSound;
    private Music pushingSound;

    public PushableObject(final float x, final float y, final MobileEntity... pushers) {
        vel = new Vector2();
        move(x, y);
        this.pushers = Obj.requireNotEmpty(pushers);
        dummy = new Rectangle();
        useGravity = mustStand = true;
        mass = 1.0f;
        gravity = -500;
        damping = 0.0001f;
        fallSpeedLimit = -1200;

        pushStrength = 50;
        deacceleration = 200;
    }

    @Override
    public void init() {
        super.init();
        for (final MobileEntity mobile : pushers) {
            mobile.addObstacle(this);
            addObstacle(mobile);
        }
    }

    @Override
    public void logistics() {
        final float DELTA = getEngine().delta;

        if (useGravity) {
            if (!canDown()) {
                if (vel.y < 0) {
                    sounds.play(landingSound);
                    if (slamEvent != null)
                        slamEvent.eventHandling();
                }
                vel.y = 0;
            } else {
                drag();
                final float nextY = bounds.pos.y - vel.y * DELTA;

                if (!occupiedAt(x(), nextY))
                    move(x(), nextY);
                else
                    tryDown(10);
            }
        }

        if (pushStrength > 0.0f) {
            dummy.set(x() - 2, y(), width() + 4, height());

            for (final MobileEntity mobile : pushers) {
                if (!mustStand || !mobile.canDown()) {

                    if (rectanglesCollide(mobile.bounds.toRectangle(), dummy)) {
                        if (centerX() > mobile.centerX())
                            vel.x = -pushStrength;
                        else
                            vel.x = pushStrength;
                    }
                }
            }
        }

        if (vel.x != 0) {
            final float nextX = bounds.pos.x - vel.x * getEngine().delta;
            if (!occupiedAt(nextX, y())) {
                move(nextX, y());

                if (vel.x > 0) {
                    vel.x -= deacceleration * DELTA;
                    if (vel.x < 0)
                        vel.x = 0;
                } else if (vel.x < 0) {
                    vel.x += deacceleration * DELTA;
                    if (vel.x > 0)
                        vel.x = 0;
                }
            } else {
                vel.x = 0;
            }
        }

        if (pushingSound != null) {

            if (vel.x != 0 && !canDown()) {
                pushingSound.setVolume(sounds.calc());

                if (!pushingSound.isPlaying()) {
                    pushingSound.setLooping(true);
                    pushingSound.play();
                }
            } else if (pushingSound.isPlaying()) {
                pushingSound.pause();
            }
        }
    }

    public void useGravity(final boolean gravity) {
        useGravity = gravity;
    }

    public void mustStand(final boolean mustStand) {
        this.mustStand = mustStand;
    }

    public void setSlammingSound(final Sound sound) {
        landingSound = sound;
    }

    public void setPushingSound(final Music pushingSound) {
        this.pushingSound = pushingSound;
    }

    public boolean isFalling() {
        return vel.y < 0;
    }

    public void setSlamEvent(final Event slamEvent) {
        this.slamEvent = slamEvent;
    }

    @Deprecated
    @Override
    public void setHitbox(final Hitbox hitbox) {
        throw new UnsupportedOperationException("PushableObject is limited to rectangular hitbox.");
    }

    @Override
    public void dispose() {
        pushers.forEach(mobile -> mobile.removeObstacle(this));
    }

    private void drag() {
        final float force = mass * gravity;
        vel.y *= 1.0 - (damping * getEngine().delta);

        if (fallSpeedLimit < vel.y) {
            vel.y += (force / mass) * getEngine().delta;
        } else
            vel.y -= (force / mass) * getEngine().delta;
    }
}