package pojahn.game.entities.object;

import pojahn.game.core.MobileEntity;
import pojahn.game.entities.main.GravityMan;
import pojahn.game.essentials.Direction;
import pojahn.lang.Obj;

import java.util.List;

public class Wind extends MobileEntity {

    private final float power, maxPower;
    private final Direction direction;
    private final List<GravityMan> targets;

    public Wind(final float x, final float y, final float power, final float maxPower, final Direction direction, final GravityMan... targets) {
        move(x, y);
        this.power = power;
        this.maxPower = maxPower;
        this.targets = Obj.requireNotEmpty(targets);
        this.direction = Obj.nonNull(direction);
    }

    @Override
    public MobileEntity getClone() {
        final Wind clone = new Wind(x(), y(), power, maxPower, direction, targets.toArray(new GravityMan[0]));
        copyData(clone);
        if (cloneEvent != null)
            cloneEvent.handleClonded(clone);

        return clone;
    }

    @Override
    public void logistics() {
        for (final GravityMan man : targets) {
            if (collidesWith(man)) {
                switch (direction) {
                    case N:
                        if (man.vel.y < maxPower)
                            man.vel.y += power;
                        break;
                    case NE:
                        if (man.vel.y < maxPower)
                            man.vel.y += power;
                        if (-man.vel.x < maxPower)
                            man.vel.x -= power;
                        break;
                    case E:
                        if (-man.vel.x < maxPower)
                            man.vel.x -= power;
                        break;
                    case SE:
                        if (-man.vel.y < maxPower)
                            man.vel.y -= power;
                        if (-man.vel.x < maxPower)
                            man.vel.x -= power;
                        break;
                    case S:
                        if (-man.vel.y < maxPower)
                            man.vel.y -= power;
                        break;
                    case SW:
                        if (-man.vel.y < maxPower)
                            man.vel.y -= power;
                        if (man.vel.x < maxPower)
                            man.vel.x += power;
                        break;
                    case W:
                        if (man.vel.x < maxPower)
                            man.vel.x += power;
                        break;
                    case NW:
                        if (man.vel.y < maxPower)
                            man.vel.y += power;
                        if (man.vel.x < maxPower)
                            man.vel.x += power;
                        break;
                }
            }
        }
    }
}
