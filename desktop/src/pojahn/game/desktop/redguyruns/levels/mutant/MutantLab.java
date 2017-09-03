package pojahn.game.desktop.redguyruns.levels.mutant;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import pojahn.game.core.Collisions;
import pojahn.game.core.Entity;
import pojahn.game.desktop.redguyruns.util.GFX;
import pojahn.game.entities.BigImage;
import pojahn.game.entities.Bullet;
import pojahn.game.entities.EvilDog;
import pojahn.game.entities.LaserDrone;
import pojahn.game.entities.OneWay;
import pojahn.game.entities.Particle;
import pojahn.game.entities.PathDrone;
import pojahn.game.entities.Shuttle;
import pojahn.game.entities.SolidPlatform;
import pojahn.game.entities.Weapon;
import pojahn.game.entities.mains.GravityMan;
import pojahn.game.essentials.Animation;
import pojahn.game.essentials.CameraEffects;
import pojahn.game.essentials.Direction;
import pojahn.game.essentials.EntityBuilder;
import pojahn.game.essentials.Factory;
import pojahn.game.essentials.GameState;
import pojahn.game.essentials.Hitbox;
import pojahn.game.essentials.Image2D;
import pojahn.game.essentials.ResourceManager;
import pojahn.game.essentials.Utils;
import pojahn.game.essentials.Vitality;
import pojahn.game.essentials.stages.PixelBasedLevel;
import pojahn.game.events.Event;
import pojahn.game.desktop.redguyruns.util.ResourceUtil;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static pojahn.game.core.Collisions.distance;

public class MutantLab extends PixelBasedLevel {

    private ResourceManager res;
    private GravityMan play;
    private Bullet proj;
    private Particle gunfire;
    private Vector2[] wp1, wp2, wp3;
    private Music standardMusic, stressMusic, labMusic;
    private boolean buttonDown, metAttacking, stunned;
    private int smashCounter;

    @Override
    public void init(Serializable meta) throws Exception {
        res = new ResourceManager();
        res.loadContentFromDirectory(Gdx.files.internal("res/data"));
        res.loadContentFromDirectory(Gdx.files.internal("res/general"));
        res.loadContentFromDirectory(Gdx.files.internal("res/mutantlabb"));
        getEngine().timeFont = res.getFont("sansserif32.fnt");

        createMap(res.getPixmap("pixmap.png"));
        Stream.of(res.getAnimation("main")).forEach(Image2D::createPixelData);
        Stream.of(res.getAnimation("stun")).forEach(Image2D::createPixelData);
        Stream.of(res.getAnimation("alien")).forEach(Image2D::createPixelData);
        Stream.of(res.getAnimation("alienattack")).forEach(Image2D::createPixelData);
        Stream.of(res.getAnimation("bug")).forEach(Image2D::createPixelData);

        res.loadSound(Gdx.files.internal("res/cave/exp3.wav"));
        res.loadAnimation(Gdx.files.internal("res/clubber/trailer"));

        standardMusic = res.getMusic("standardMusic.ogg");
        standardMusic.setOnCompletionListener(music -> {
            standardMusic.play();
            standardMusic.setPosition(2.28f);
        });
        stressMusic = res.getMusic("stressMusic.ogg");
        stressMusic.setOnCompletionListener(music -> {
            stressMusic.play();
            stressMusic.setPosition(7.62f);
        });

        labMusic = res.getMusic("labbmusic.ogg");

        getCheckpointHandler().setReachEvent(() -> GFX.renderCheckpoint(res, this));
        getCheckpointHandler().appendCheckpoint(6023, 1324 - 2, 6024, 1248, 110, 110);
    }

    @Override
    public void build() {
        /*
         * Init
         */
        buttonDown = metAttacking = stunned = false;

        labMusic.setVolume(0);
        labMusic.play();

        if (stressMusic.isPlaying())
            stressMusic.stop();
        if (!standardMusic.isPlaying())
            standardMusic.play();

        /*
         * Main Character
         */
        play = ResourceUtil.getGravityMan(res);
        play.move(513, 460 - 1);
        play.move(3112, 1473);
        play.touch(1);
        add(play);

        /*
         * Background & Foreground
         */
        add(new EntityBuilder().image(res.getImage("foreground.png")).zIndex(100).build());
        add(new EntityBuilder().image(res.getImage("background.png")).zIndex(-100).build(BigImage.class, BigImage.RenderStrategy.PARALLAX_REPEAT));

        /*
         * Room Background
		 */
        Entity room = new Entity();
        room.setImage(res.getImage("roombg.png"));
        room.zIndex(-50);
        room.move(1836, 1248);
        add(room);

        Entity musicHandle = Utils.wrap(Factory.roomMusic(
                new Rectangle(room.x() - 200, room.y(), room.width() + 200, room.height()),
                labMusic,
                standardMusic,
                .01f,
                1,
                play));
        add(musicHandle);

		/*
		 * Doors
		 */
        SolidPlatform goalDoor = getDoor(1160, 1540, true);
        SolidPlatform locked1 = getDoor(3176, 1540, true);
        SolidPlatform locked2 = getDoor(5864, 1348, true);
        SolidPlatform trapdoor1 = getDoor(5832, 1256 - res.getImage("door1.png").getHeight(), false);
        SolidPlatform trapdoor2 = getDoor(3264, 1360, false);

        add(goalDoor);
        add(locked1);
        add(locked2);
        add(trapdoor1);
        add(trapdoor2);

		/*
		 * Room stuff
		 */
        Entity glass1 = new Entity();
        glass1.setImage(res.getImage("glass.png"));
        glass1.zIndex(-5);
        glass1.tint.a = .8f;
        glass1.move(1967, 1328);

        Entity glass2 = glass1.getClone().move(2210, 1328);
        Entity glass3 = glass1.getClone().move(2452, 1328);

        Entity bubble1 = new Entity();
        bubble1.zIndex(-4);
        bubble1.setImage(3, res.getAnimation("bubbles"));
        bubble1.move(1967, 1344);

        Entity bubble2 = bubble1.getClone().move(2210, 1344);
        Entity bubble3 = bubble1.getClone().move(2452, 1344);

        add(glass1);
        add(glass2);
        add(glass3);
        add(bubble1);
        addAfter(bubble2, 20);
        addAfter(bubble3, 40);

		/*
		 * Crack Event
		 */
        if (getCheckpointHandler().getLatestCheckpoint() == null) {
            runOnceWhen(() -> {
                glass3.setImage(res.getImage("glasscrack.png"));
                res.getSound("glasscracking.wav").play();
            }, () -> play.x() > 2813 && !buttonDown);
        }

		/*
		 * Metroids
		 */
        EvilDog met1 = getMetroid(1976, 1368);
        EvilDog met2 = getMetroid(2219, 1368);
        EvilDog met3 = getMetroid(2461, 1368);
        met1.drag = .4f;
        met2.drag = .3f;
        add(met1);
        add(met2);
        add(met3);

		/*
		 * Goal Button
		 */
        SolidPlatform gbutton = new SolidPlatform(6574, 1328, play);
        gbutton.setImage(res.getImage("goalbutton.png"));
        gbutton.freeze();
        gbutton.setMoveSpeed(1.5f);
        gbutton.appendPath(gbutton.x(), gbutton.y() + gbutton.halfHeight(), 0, false, () -> {
            buttonDown = true;
            play.freeze();
            removeFocusObject(play);

            PathDrone camera = new PathDrone(play.x(), play.y());
            camera.appendPath(camera.x(), camera.y(), 60, true, null);
            camera.appendPath(1160, 1540, 60, true, () -> {
                camera.freeze();

                addAfter(goalDoor::unfreeze, 60);
                runOnceAfter(() -> {
                    removeFocusObject(camera);
                    addFocusObject(play);
                    play.unfreeze();
                    res.getSound("achievement.wav").play();
                }, 140);
            });

            add(camera);
            addFocusObject(camera);
        });
        Entity dummy = new Entity();
        dummy.move(gbutton.x(), gbutton.y() - 1);
        dummy.bounds.size.width = gbutton.width();
        dummy.addEvent(() -> {
            if (dummy.collidesWith(play)) {
                gbutton.unfreeze();
                discard(dummy);
            }
        });
        add(dummy);
        add(gbutton);

		/*
		 * Metroid Release
		 */
        runOnceWhen(() -> {
            runOnceAfter(() -> res.getSound("sireen.wav").play(), 30);

            res.getSound("glassbreak.wav").play();
            res.getMusic("stressMusic.ogg").setLooping(true);
            res.getMusic("stressMusic.ogg").play();
            res.getMusic("standardMusic.ogg").pause();

            discard(bubble1);
            discard(bubble2);
            discard(bubble3);
            discard(musicHandle);

            Image2D glassDestroyed = res.getImage("glassdestroyed.png");
            glass1.setImage(glassDestroyed);
            glass2.setImage(glassDestroyed);
            glass3.setImage(glassDestroyed);

            locked1.unfreeze();
            locked2.unfreeze();
            trapdoor1.unfreeze();
            trapdoor2.unfreeze();
            met1.unfreeze();
            met2.unfreeze();
            met3.unfreeze();
        }, () -> buttonDown && play.x() < 6064);

		/*
		 * Alien
		 */
        Animation<Image2D> smash = new Animation<>(4, res.getAnimation("alienattack"));
        smash.setLoop(false);

        PathDrone monster = new PathDrone(1536, 2273);
        monster.setFacings(2);
        monster.addEvent(monster::face);
        monster.setImage(8, res.getAnimation("alien"));
        monster.setMoveSpeed(1);
        monster.setHitbox(Hitbox.PIXEL);
        monster.sounds.maxVolume = 1;
        monster.sounds.maxDistance = 500;
        monster.sounds.power = 40;
        monster.sounds.useFalloff = true;
        monster.appendPath();
        monster.appendPath(1885, monster.y());
        monster.addEvent(Factory.hitMain(monster, play, -1));
        monster.addEvent(() -> {
            if (++smashCounter % 110 == 0) {
                smash.reset();
                monster.setImage(smash);
                monster.setMoveSpeed(0);

                double volume = monster.sounds.calc(play);
                if (volume > 0.0)
                    getRandomMonster().play((float) volume);
            }
        });
        add(monster);

        smash.addEvent(() -> {
            final double dist = monster.dist(play);

            if (dist < 300 && !stunned && play.isAlive()) {
                stunned = true;
                play.setImage(1, res.getAnimation("stun"));
                play.vel.y = 0;

                temp(() -> play.vel.x = 0, 60);
                runOnceAfter(() -> {
                    stunned = false;
                    play.setImage(4, res.getAnimation("main"));
                }, 60);
            }

            if (dist < 600) {
                res.getSound("slam.wav").play(monster.sounds.calc(play));
                temp(CameraEffects.vibration(2.5f), 40);
            }

        }, 4);
        smash.addEvent(() -> {
            monster.setImage(8, res.getAnimation("alien"));
            monster.setMoveSpeed(1);
        }, res.getAnimation("alienattack").length - 1);

		/*
		 * One Ways
		 */
        OneWay ow1 = new OneWay(1618, 2187, Direction.N, play);
        ow1.setImage(res.getImage("ow.png"));
        add(ow1);
        add(ow1.getClone().move(1751, ow1.y()));
        add(ow1.getClone().move(1883, ow1.y()));

		/*
		 * Health
		 */
        Entity hp = new Entity();
        hp.move(3431, 1896);
        hp.setImage(4, res.getAnimation("health"));
        hp.addEvent(() -> {
            if (hp.collidesWith(play)) {
                play.touch(1);
                discard(hp);
                res.getSound("health.wav").play();
            }
        });
        add(hp);

		/*
		 * Bugs
		 */
        Entity b = new Entity();
        b.setImage(6, res.getAnimation("bug"));
        b.setHitbox(Hitbox.PIXEL);
        b.setCloneEvent((clone) -> {
            clone.addEvent(Factory.hitMain(clone, play, -1));
            clone.addEvent(Factory.spazz(clone, .7f, 7));
        });

        add(b.getClone().move(200, 851));
        add(b.getClone().move(240, 851));
        add(b.getClone().move(280, 851));
        add(b.getClone().move(320, 851));
        add(b.getClone().move(360, 851));
        add(b.getClone().move(400, 851));
        add(b.getClone().move(380, 1100));
        add(b.getClone().move(420, 1100));
        add(b.getClone().move(460, 1100));
        add(b.getClone().move(500, 1100));
        add(b.getClone().move(540, 1100));
        add(b.getClone().move(620, 1100));

		/*
		 * Turret & Projectile
		 */
        gunfire = new Particle();
        gunfire.setIntroSound(res.getSound("firesound.wav"));
        gunfire.sounds.useFalloff = true;
        gunfire.sounds.maxDistance = 1300;

        final Particle p = new Particle();
        p.zIndex(4);
        p.setImage(2, res.getAnimation("trailer"));

        final Particle imp = new Particle();
        imp.setImage(2, res.getAnimation("impact"));
        imp.zIndex(200);
        imp.setIntroSound(res.getSound("exp3.wav"));
        imp.sounds.useFalloff = true;
        imp.sounds.maxVolume = .7f;

        proj = new Bullet(0, 0, play);
        proj.zIndex(5);
        proj.setImage(res.getImage("bullet.png"));
        proj.setTrailer(p);
        proj.setImpact(imp);

        int padY = 150;
        int padX = 100;

        Weapon wep1 = getWeapon();
        wep1.move(3552, 1253);
        wep1.appendPath(3552 + 32, 1213 - 32 - padY, 0, false, wep1::freeze);
        wep1.appendPath(3976 + padX, 768, 0, false, wep1::freeze);

        Weapon wep2 = getWeapon();
        wep2.move(3552 + 32, 1253);
        wep2.appendPath(3552 + 32, 1213 - padY, 0, false, wep2::freeze);
        wep2.appendPath(3976 - 32 + padX, 768, 0, false, wep2::freeze);

        Weapon wep3 = getWeapon();
        wep3.move(3552 + (32 * 2), 1253);
        wep3.appendPath(3552 + 32, 1213 + 32 - padY, 0, false, wep3::freeze);
        wep3.appendPath(3976 - (32 * 2) + padX, 768, 0, false, wep3::freeze);

        Weapon wep4 = getWeapon();
        wep4.move(3552, 1253 + 32);
        wep4.appendPath(3584, 1325 - 32 - padY, 0, false, wep4::freeze);
        wep4.appendPath(3976 - (32 * 3) + padX, 768, 0, false, wep4::freeze);

        Weapon wep5 = getWeapon();
        wep5.move(3552 + 32, 1253 + 32);
        wep5.appendPath(3584, 1325 - padY, 0, false, wep5::freeze);
        wep5.appendPath(3976 - (32 * 4) + padX, 768, 0, false, wep5::freeze);

        Weapon wep6 = getWeapon();
        wep6.move(3552 + (32 * 2), 1253 + 32);
        wep6.appendPath(3584, 1325 + 32 - padY, 0, false, wep6::freeze);
        wep6.appendPath(3976 - (32 * 5) + padX, 768, 0, false, wep6::freeze);

        Rectangle area1 = new Rectangle(3450, 1182, 300, 220);
        Rectangle area2 = new Rectangle(3441, 779, 362, 300);

        runOnceWhen(()->{
            Stream.of(wep1, wep2, wep3, wep4, wep5, wep6).forEach(PathDrone::unfreeze);
        }, ()-> Collisions.rectanglesCollide(play.bounds.toRectangle(), area1));

        runOnceWhen(()->{
            Stream.of(wep1, wep2, wep3, wep4, wep5, wep6).forEach(PathDrone::unfreeze);
        }, ()-> Collisions.rectanglesCollide(play.bounds.toRectangle(), area2));

        add(wep1);
        add(wep2);
        add(wep3);
        add(wep4);
        add(wep5);
        add(wep6);

        /*
         * Doors in Weapon area
         */
        if (!getCheckpointHandler().available()) {
            SolidPlatform door = new SolidPlatform(4290, 1013, play);
            door.setImage(res.getImage("door3.png"));
            door.appendPath(door.x(), door.y() + door.height() + 1);
            door.freeze();

            Entity key = new EntityBuilder()
                    .move(4272, 776).image(res.getImage("key.png")).build();
            key.ifCollides(play).thenRunOnce(()->{
                door.unfreeze();
                key.die();
            });

            add(door);
            add(key);
        }

		/*
		 * Laser Monsters
		 */
        if (!getEngine().isReplaying()) {
            wp1 = getLaserMonsterWP();
            wp2 = getLaserMonsterWP();
            wp3 = getLaserMonsterWP();
        }
		List<Entity> monsters = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            Vector2[] waypoints;
            if (i == 0) waypoints = wp1;
            else if (i == 1) waypoints = wp2;
            else waypoints = wp3;

            Shuttle sh = new Shuttle(4687, 772);
            sh.setImage(4, res.getAnimation("laseralien"));
            sh.appendPath(waypoints);
            sh.zIndex(200);
            sh.thrust = 100;

            LaserDrone ld = new LaserDrone(4687, 772, 50, 10, 60, play);
            ld.addEvent(Factory.follow(sh, ld, sh.halfWidth(), sh.halfHeight()));
            ld.setLaserTint(Color.valueOf("00f428FF"));
            ld.setFiringSound(res.getSound("laserfire.wav"));
            ld.setChargeBeam(ResourceUtil.getChargingLaser(res));
            ld.setFiringBeam(ResourceUtil.getFiringLaser(res));
            ld.sounds.useFalloff = true;
            ld.sounds.power = 40;
            ld.sounds.maxVolume = 1;
            ld.sounds.maxDistance = 1600;
            ld.addEvent(new Event() {
                int counter, delay = MathUtils.random(120, 200);

                @Override
                public void eventHandling() {
                    if (++counter % delay == 0)
                        getRandomAlienLaser().play(ld.sounds.calc(play));

                    if (buttonDown)
                        discard(ld);
                }
            });

            add(sh);
            add(ld);
            monsters.add(ld);
        }

		/*
		 * Goal
		 */
        Entity goal = new Entity();
        goal.setImage(res.getImage("door.png"));
        goal.move(1929, 3006);
        goal.zIndex(-1);
        goal.addEvent(new Event() {
            @Override
            public void eventHandling() {
                if (goal.collidesWith(play)) {
                    goal.removeEvent(this);
                    stressMusic.stop();
                    standardMusic.play();
                    met1.freeze();
                    met2.freeze();
                    met3.freeze();
                    play.setState(Vitality.COMPLETED);
                }
            }
        });
        add(goal);

		/*
		 * Finalize
		 */
        play.setActionEvent((hitter) -> {
            if (hitter.isCloneOf(proj) || monsters.stream().map(hitter::isCloneOf).findAny().orElse(false))
                play.touch(-1);
        });
    }

    private Weapon getWeapon() {
        Animation<Image2D> firingImage = new Animation<>(4, res.getAnimation("weapon"));
        firingImage.setLoop(false);

        Weapon turr = new Weapon(3456, 768, 1, 1, 90, play);
        turr.zIndex(1);
        turr.setProjectile(proj);
        turr.setImage(res.getAnimation("weapon")[6]);
//        turr.setFiringImage(firingImage);
//        turr.setFiringOffsets(turr.halfWidth(), turr.halfHeight());
        turr.setFiringParticle(gunfire);
        turr.setMoveSpeed(4);
        turr.freeze();

        return turr;
    }

    private EvilDog getMetroid(float x, float y) {
        Entity dummy = new Entity();
        dummy.bounds.size.width = play.width();
        dummy.bounds.size.height = play.height();
        dummy.bounds.pos.set(play.bounds.pos);

        EvilDog m = new EvilDog(x, y, -1, dummy) {
            boolean attacking, recover;
            int attackingCounter, recoveryCounter;

            @Override
            public void logistics() {
                if (isFrozen() || getEngine().getGameState() != GameState.ACTIVE)
                    return;

                float targetX = play.x() - 28;
                float targetY = play.y() - 63;

                if (!recover && !metAttacking && (attacking || distance(x(), y(), targetX, targetY) < 25)) {
                    attacking = metAttacking = true;
                    getImage().stop(false);
                    play.touch(-1);
                    playRandomMetroid();
                }

                if (attacking) {
                    if (++attackingCounter % 95 == 0) {
                        attacking = metAttacking = false;
                        getImage().reset();
                        getImage().stop(true);
                        recover = true;
                        vx = vy = 0;
                    } else
                        move(targetX, targetY);
                }

                if (recover && ++recoveryCounter % 120 == 0)
                    recover = false;

                if (!attacking) {
                    dummy.move(targetX, targetY);
                    super.logistics();
                }
            }

            @Override
            public void unfreeze() {
                zIndex(150);
                super.unfreeze();
            }
        };
        m.setImage(5, res.getAnimation("metroid"));
        m.getImage().stop(true);
        m.zIndex(-6);
        m.move(x, y);
        m.freeze();
        m.thrust = 200;

        return m;
    }

    SolidPlatform getDoor(float x, float y, boolean horizontal) {
        SolidPlatform solp;
        Image2D lamp1 = res.getImage("lamp1.png");
        Image2D lamp2 = res.getImage("lamp2.png");
        Image2D lamp3 = res.getImage("lamp3.png");
        Image2D lamp4 = res.getImage("lamp4.png");

        if (horizontal) {
            solp = new SolidPlatform(0, 0, play) {
                int counter;
                float posX = 1;
                float posY = 4;
                boolean goingBack;

                @Override
                public void render(SpriteBatch batch) {
                    if (++counter % 5 == 0) {
                        if (goingBack) {
                            posX -= 8;
                            if (posX < 1)
                                goingBack = false;
                        } else {
                            posX += 8;
                            if (posX > 65) {
                                goingBack = true;
                                posX -= 8;
                            }
                        }
                    }

                    super.render(batch);
                    batch.draw(goingBack ? lamp4 : lamp3, posX + x(), posY + y());
                }
            };
        } else {
            solp = new SolidPlatform(0, 0, play) {
                int counter;
                float posX = 7;
                float posY = 1;
                boolean goingBack;

                @Override
                public void render(SpriteBatch batch) {
                    if (++counter % 5 == 0) {
                        if (goingBack) {
                            posY -= 8;
                            if (posY < 7)
                                goingBack = false;
                        } else {
                            posY += 8;
                            if (posY > 65) {
                                goingBack = true;
                                posY -= 8;
                            }
                        }
                    }

                    super.render(batch);
                    batch.draw(goingBack ? lamp2 : lamp1, posX + x(), posY + y());
                }
            };
        }

        solp.setMoveSpeed(2);
        solp.freeze();
        solp.move(x, y);
        solp.setImage(horizontal ? res.getImage("door2.png") : res.getImage("door1.png"));
        solp.zIndex(1);
        if (horizontal)
            solp.appendPath(solp.x() - solp.width(), solp.y());
        else
            solp.appendPath(solp.x(), solp.y() + solp.height());

        return solp;
    }

    private void playRandomMetroid() {
        int value = MathUtils.random(1, 4);

        switch (value) {
            case 1:
                res.getSound("metroid01.wav").play();
                break;
            case 2:
                res.getSound("metroid02.wav").play();
                break;
            case 3:
                res.getSound("metroid03.wav").play();
                break;
            case 4:
                res.getSound("metroid04.wav").play();
                break;
        }
    }

    private Sound getRandomMonster() {
        int value = MathUtils.random(1, 3);

        switch (value) {
            case 1:
                return res.getSound("monster1.wav");
            case 2:
                return res.getSound("monster2.wav");
            case 3:
                return res.getSound("monster3.wav");
        }

        return null;
    }

    private Sound getRandomAlienLaser() {
        int value = MathUtils.random(1, 3);

        switch (value) {
            case 1:
                return res.getSound("lasermonster1.wav");
            case 2:
                return res.getSound("lasermonster2.wav");
            case 3:
                return res.getSound("lasermonster3.wav");
        }

        return null;
    }

    private Vector2[] getLaserMonsterWP() {
        int padding = 100;
        PathDrone.Waypoint[] pd = randomWallPoints(4704 + padding, 5597 - padding, 768 + padding, 1152 - padding);
        Vector2[] wps = new Vector2[pd.length];
        for (int j = 0; j < wps.length; j++)
            wps[j] = new Vector2(pd[j].targetX, pd[j].targetY);

        return wps;
    }

    @Override
    public void dispose() {
        res.disposeAll();
    }

    @Override
    public Music getStageMusic() { //TODO: Room music?
        return metAttacking ? stressMusic : standardMusic;
    }

    @Override
    public String getLevelName() {
        return "Mutant Lab";
    }

    private PathDrone.Waypoint[] randomWallPoints(int minX, int maxX, int minY, int maxY) {
        int last = -1;
        int quantity = new Random().nextInt(100) + 100;
        List<PathDrone.Waypoint> pdlist = new ArrayList<>(quantity);
        Random r = new Random();

        for (int i = 0; i < quantity; i++) {
            int dir = r.nextInt(4);
            if (dir != last) {
                last = dir;

                Point2D.Float point = getDirection(dir, minX, maxX, minY, maxY);
                pdlist.add(new PathDrone.Waypoint(point.x, point.y, 0, false, null));
            } else
                i--;
        }

        return pdlist.toArray(new PathDrone.Waypoint[pdlist.size()]);
    }

    private Point2D.Float getDirection(int dir, int minX, int maxX, int minY, int maxY) {
        Point2D.Float point = new Point2D.Float();
        final int UP = 0, DOWN = 1, LEFT = 2, RIGHT = 3;

        Random r = new Random();

        switch (dir) {
            case UP:
                point.x = r.nextInt(maxX - minX) + minX;
                point.y = minY;
                break;
            case DOWN:
                point.x = r.nextInt(maxX - minX) + minX;
                point.y = maxY;
                break;
            case LEFT:
                point.x = minX;
                point.y = r.nextInt(maxY - minY) + minY;
                break;
            case RIGHT:
                point.x = maxX;
                point.y = r.nextInt(maxY - minY) + minY;
                break;
        }

        return point;
    }
}
