package pojahn.game.essentials;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public interface LaserBeam {

    /**
     * Register that a laser should be rendered between the given lines when {@code drawLasers} is called.
     */
    void fireAt(float srcX, float srcY, float destX, float destY, int active);

    /**
     * Renders all registered lasers.
     */
    void drawLasers(SpriteBatch batch);

    class Task {
        float srcX, srcY, destX, destY;
        int active;

        public Task(final float srcX, final float srcY, final float destX, final float destY, final int active) {
            this.srcX = srcX;
            this.srcY = srcY;
            this.destX = destX;
            this.destY = destY;
            this.active = active;
        }
    }
}
