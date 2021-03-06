package pojahn.game.entities.particle;

import com.badlogic.gdx.math.Vector2;
import pojahn.game.core.BaseLogic;
import pojahn.game.entities.enemy.weapon.Projectile;

public class Shrapnel extends Particle {

    private final Projectile split;

    public Shrapnel(final Projectile shrapnel) {
        this.split = shrapnel;
    }

    @Override
    public Shrapnel getClone() {
        final Shrapnel s = new Shrapnel(split);
        copyData(s);
        if (cloneEvent != null)
            cloneEvent.handleClonded(s);

        return s;
    }

    @Override
    protected boolean completed() {
        return !isVisible() || getImage().hasEnded();
    }

    @Override
    protected void erupt() {
        final Vector2[] edgePoints = getEightDirection();

        for (final Vector2 edgePoint : edgePoints) {
            final Projectile proj = split.getClone();
            proj.center(this);
            proj.setTarget(edgePoint);
            getLevel().add(proj);
        }
    }

    private Vector2[] getEightDirection() {
        final int padding = 1;
        final float middleX = centerX();
        final float middleY = centerY();
        float x;
        float y;

        //NW Point
        x = middleX - padding;
        y = middleY - padding;
        final Vector2 p1 = BaseLogic.findEdgePoint(middleX, middleY, x, y, getLevel());

        //N Point
        x = middleX;
        y = middleY - padding;
        final Vector2 p2 = BaseLogic.findEdgePoint(middleX, middleY, x, y, getLevel());

        //NE Point
        x = middleX + padding;
        y = middleY - padding;
        final Vector2 p3 = BaseLogic.findEdgePoint(middleX, middleY, x, y, getLevel());

        //E Point
        x = middleX + padding;
        y = middleY;
        final Vector2 p4 = BaseLogic.findEdgePoint(middleX, middleY, x, y, getLevel());

        //SE Point
        x = middleX + padding;
        y = middleY + padding;
        final Vector2 p5 = BaseLogic.findEdgePoint(middleX, middleY, x, y, getLevel());

        //S Point
        x = middleX;
        y = middleY + padding;
        final Vector2 p6 = BaseLogic.findEdgePoint(middleX, middleY, x, y, getLevel());

        //SW Point
        x = middleX - padding;
        y = middleY + padding;
        final Vector2 p7 = BaseLogic.findEdgePoint(middleX, middleY, x, y, getLevel());

        //W Point
        x = middleX - padding;
        y = middleY;
        final Vector2 p8 = BaseLogic.findEdgePoint(middleX, middleY, x, y, getLevel());

        return new Vector2[]{p1, p2, p3, p4, p5, p6, p7, p8};
    }
}