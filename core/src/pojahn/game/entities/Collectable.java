package pojahn.game.entities;

import com.badlogic.gdx.audio.Sound;
import pojahn.game.core.Entity;
import pojahn.game.core.MobileEntity;
import pojahn.game.events.Event;

public class Collectable extends Entity {

    @FunctionalInterface
    public interface CollectEvent {
        void eventHandling(Entity collector);

        static CollectEvent wrap(final Event event) {
            return collector -> event.eventHandling();
        }

        static CollectEvent freeze(final int frames) {
            return collector -> {
                final MobileEntity mobile = (MobileEntity) collector;
                mobile.freeze();
                mobile.getLevel().runOnceAfter(mobile::unfreeze, frames);
            };
        }

        static CollectEvent speed(final int frames, final float multiplier) {
            return collector -> {
                final MobileEntity mobile = (MobileEntity) collector;
                final float orgSpeed = mobile.getMoveSpeed();
                mobile.setMoveSpeed(orgSpeed * multiplier);
                mobile.getLevel().runOnceAfter(() -> mobile.setMoveSpeed(orgSpeed), frames);
            };
        }
    }

    private Entity[] collectors;
    private Entity subject;
    private Particle collectImage;
    private CollectEvent collectEvent;
    private Sound collectSound;
    private boolean collected, disposeCollected;

    public Collectable(final float x, final float y, final Entity... collectors) {
        this.collectors = collectors;
        disposeCollected = true;
        move(x, y);
    }

    public void setCollectSound(final Sound collectSound) {
        this.collectSound = collectSound;
    }

    public void setCollectEvent(final CollectEvent collectEvent) {
        this.collectEvent = collectEvent;
    }

    public void disposeCollected(final boolean disposeCollected) {
        this.disposeCollected = disposeCollected;
    }

    public void setCollectImage(final Particle collectImage) {
        this.collectImage = collectImage;
    }

    public Entity getSubject() {
        return subject;
    }

    @Override
    public void logistics() {
        if (!collected) {
            for (final Entity collector : collectors) {
                if (collidesWith(collector)) {
                    collected = true;
                    subject = collector;
                    if (collectEvent != null)
                        collectEvent.eventHandling(collector);
                    if (collectSound != null)
                        collectSound.play(sounds.calc());
                    if (collectImage != null)
                        getLevel().add(collectImage.getClone().center(this));
                    if (disposeCollected)
                        getLevel().discard(this);

                    break;
                }
            }
        }
    }
}
