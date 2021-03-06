package pojahn.game.entities.main;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import pojahn.game.core.Level;
import pojahn.game.core.PlayableEntity;
import pojahn.game.essentials.Keystrokes;

public class GravityMan extends PlayableEntity {

    public Vector2 vel, tVel, slidingTVel;
    public float accX, mass, gravity, damping, wallGravity, wallDamping, jumpStrength, wallJumpHorizontalStrength;
    private int jumpKeyDownCounter, shortJumpFrames;
    private boolean isWallSliding, allowWallSlide, allowWallJump, allowSlopeWalk;
    private Sound jumpSound, landingSound;
    private Keystrokes prevStrokes, currStrokes;

    public GravityMan() {
        vel = new Vector2();
        tVel = new Vector2(260, -800);
        slidingTVel = new Vector2(0, -100);

        jumpKeyDownCounter = Integer.MAX_VALUE;
        shortJumpFrames = 6;

        accX = 650;

        mass = 1.0f;
        gravity = -500;
        damping = 0.0001f;
        jumpStrength = 125;

        wallGravity = -90;
        wallDamping = 1.1f;
        wallJumpHorizontalStrength = 150;

        allowWallSlide = allowWallJump = allowSlopeWalk = true;
        prevStrokes = Keystrokes.AFK;
    }

    public GravityMan getClone() {
        final GravityMan clone = new GravityMan();
        copyData(clone);
        if (cloneEvent != null)
            cloneEvent.handleClonded(clone);

        return clone;
    }

    @Override
    public void logistics() {
        currStrokes = getKeysDown();

        run();
        wallSlide();
        jump();

        prevStrokes = currStrokes;
    }

    @Override
    public void render(final SpriteBatch batch) {
        getImage().stop(vel.x == 0);
        super.render(batch);
    }

    public void setJumpSound(final Sound sound) {
        this.jumpSound = sound;
    }

    public void setLandingSound(final Sound landingSound) {
        this.landingSound = landingSound;
    }

    public void setAllowSlopeWalk(final boolean allowSlopeWalk) {
        this.allowSlopeWalk = allowSlopeWalk;
    }

    protected void wallSlide() {
        isWallSliding = isWallSliding();
    }

    protected void jump() {
        if (!isFrozen()) {
            final boolean canDown = canDown();

            if (jumpJustPressed()) {
                if (vel.y == 0 && !canDown) {
                    jumpKeyDownCounter = 0;
                } else if (isWallSliding) {
                    jumpKeyDownCounter = 0;
                    vel.y = 0;
                }

                if (!canLeft() && (canDown || currStrokes.left))
                    vel.x = -wallJumpHorizontalStrength;
                else if (!canRight() && (canDown || currStrokes.right))
                    vel.x = wallJumpHorizontalStrength;
            }

            if (jumpKeyDownCounter <= shortJumpFrames && currStrokes.jump) {
                if (++jumpKeyDownCounter == 1 && jumpSound != null)
                    sounds.play(jumpSound);

                vel.y += jumpStrength / jumpKeyDownCounter;
            }
            if (jumpKeyDownCounter > 1 && jumpReleased()) {
                jumpKeyDownCounter = Integer.MAX_VALUE;
            }
        }

        if (canDown() || launching())
            drag();

        final float futureY = getFutureY();
        if (!occupiedAt(x(), futureY))
            applyYForces();
        else {
            if (landed()) {
                land();
                if (landingSound != null)
                    sounds.play(landingSound);
            }

            vel.y = 0;
        }
    }

    protected void run() {
        final boolean left = !isFrozen() && currStrokes.left;
        final boolean right = !isFrozen() && currStrokes.right;
        final boolean moving = left || right;

        if (left) {
            moveLeft();
        } else if (right) {
            moveRight();
        }

        if (!moving) {
            if (vel.x > 0) {
                moveRight();
                if (vel.x < 0)
                    vel.x = 0;
            } else if (vel.x < 0) {
                moveLeft();
                if (vel.x > 0)
                    vel.x = 0;
            }
        }

        if (vel.x != 0) {
            final float futureX = getFutureX();

            if (vel.x > 0)
                runLeft(futureX);
            else if (vel.x < 0)
                runRight(futureX);
        }
    }

    protected boolean landed() {
        return vel.y < 0;
    }

    protected boolean launching() {
        return vel.y > 0;
    }

    protected void land() {
        tryDown(10);
    }


    protected void runLeft(final float targetX) {
        for (float next = bounds.pos.x; next >= targetX; next -= 0.2f) {
            if (!occupiedAt(next, y())) {
                bounds.pos.x = next;
                if (!occupiedAt(x(), y() + 1) && occupiedAt(x(), y() + 2))
                    bounds.pos.y++;
            } else if (allowSlopeWalk && canSlopeLeft(next)) {
                move(next, bounds.pos.y - 1);
                tryDown(1);
            } else {
                vel.x = 0;
                return;
            }
        }
    }

    protected void runRight(final float targetX) {
        for (float next = x(); next <= targetX; next += 0.2f) {
            if (!occupiedAt(next, y())) {
                bounds.pos.x = next;
                if (!occupiedAt(x(), y() + 1) && occupiedAt(x(), y() + 2))
                    bounds.pos.y++;
            } else if (allowSlopeWalk && canSlopeRight(next)) {
                move(next, y() - 1);
                tryDown(1);
            } else {
                vel.x = 0;
                break;
            }
        }
    }

    protected boolean canSlopeLeft(final float targetX) {
        final int y = (int) y() - 1;
        final int tar = (int) targetX;
        final Level l = getLevel();

        for (int i = 0; i < height(); i++)
            if (l.isSolid(tar, y + i))
                return false;

        return !obstacleCollision(targetX, bounds.pos.y);
    }

    protected boolean canSlopeRight(final float targetX) {
        final int y = (int) y() - 1;
        final int tar = (int) (targetX + width());
        final Level l = getLevel();

        for (int i = 0; i < height(); i++)
            if (l.isSolid(tar, y + i))
                return false;

        return !obstacleCollision(targetX, y());
    }

    protected boolean isWallSliding() {
        return !(!allowWallSlide || currStrokes.down) && canDown() &&
                (((currStrokes.left || isWallSliding) && !canLeft()) || ((currStrokes.right || isWallSliding) && !canRight()));
    }

    protected void moveLeft() {
        if (vel.x < thermVx())
            vel.x += accX * getDelta();
    }

    protected void moveRight() {
        if (-vel.x < thermVx())
            vel.x -= accX * getDelta();
    }

    protected void drag() {
        final float force = mass * getGravity();
        vel.y *= 1.0 - (getDamping() * getDelta());

        if (thermVy() < vel.y) {
            vel.y += (force / mass) * getDelta();
        } else
            vel.y -= (force / mass) * getDelta();
    }

    protected void applyYForces() {
        bounds.pos.y -= vel.y * getDelta();
    }

    protected float getFutureX() {
        return bounds.pos.x - vel.x * getDelta();
    }

    protected float getFutureY() {
        return bounds.pos.y - vel.y * getDelta();
    }

    protected float thermVx() {
        return isWallSliding && allowWallSlide ? slidingTVel.x : tVel.x;
    }

    protected float thermVy() {
        return isWallSliding && allowWallSlide ? slidingTVel.y : tVel.y;
    }

    protected float getGravity() {
        return isWallSliding && allowWallSlide ? wallGravity : gravity;
    }

    protected float getDamping() {
        return isWallSliding && allowWallSlide ? wallDamping : damping;
    }

    protected float getDelta() {
        return getEngine().delta;
    }

    private boolean jumpJustPressed() {
        return !prevStrokes.jump && currStrokes.jump;
    }

    private boolean jumpReleased() {
        return prevStrokes.jump && !currStrokes.jump;
    }

    protected void copyData(final GravityMan clone) {
        super.copyData(clone);
        clone.vel.set(vel);
        clone.tVel.set(tVel);
        clone.slidingTVel.set(slidingTVel);
        clone.allowWallJump = allowWallJump;
        clone.allowWallSlide = allowWallSlide;
        clone.accX = accX;
        clone.mass = mass;
        clone.gravity = gravity;
        clone.damping = damping;
        clone.wallGravity = wallGravity;
        clone.wallDamping = wallDamping;
        clone.jumpStrength = jumpStrength;
        clone.wallJumpHorizontalStrength = wallJumpHorizontalStrength;
        clone.jumpSound = jumpSound;
    }
}
