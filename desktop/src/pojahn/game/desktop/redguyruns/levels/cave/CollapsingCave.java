package pojahn.game.desktop.redguyruns.levels.cave;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import pojahn.game.core.BaseLogic;
import pojahn.game.core.Entity;
import pojahn.game.core.MobileEntity;
import pojahn.game.core.PlayableEntity;
import pojahn.game.desktop.redguyruns.util.ResourceUtil;
import pojahn.game.entities.TmxEntity;
import pojahn.game.entities.image.RepeatingParallaxImage;
import pojahn.game.entities.image.StaticImage;
import pojahn.game.entities.movement.PathDrone;
import pojahn.game.entities.platform.SolidPlatform;
import pojahn.game.essentials.CameraEffects;
import pojahn.game.essentials.EntityBuilder;
import pojahn.game.essentials.Factory;
import pojahn.game.essentials.Hitbox;
import pojahn.game.essentials.Image2D;
import pojahn.game.essentials.ResourceManager;
import pojahn.game.essentials.Utils;
import pojahn.game.essentials.Vitality;
import pojahn.game.essentials.geom.Dimension;
import pojahn.game.essentials.stages.TileBasedLevel;
import pojahn.lang.PingPongFloat;

import java.io.Serializable;
import java.util.Random;
import java.util.stream.Stream;

public class CollapsingCave extends TileBasedLevel {

    private Random r = new Random();
    private ResourceManager resources;
    private Music music, collapsing, drilling;
    private Sound exp1, exp2, exp3, exp4;
    private ParticleEffect ps;
    private PlayableEntity play;
    private SolidPlatform crusher;
    private PathDrone drill;
    private MobileEntity camera;
    private Entity item1, flag, dust;
    private int soundCounter;
    private boolean coll1, coll2, coll3, coll4, done, drugEffect;

    @Override
    public void load(final Serializable meta) throws Exception {
        resources = new ResourceManager();
        resources.loadContentFromDirectory(Gdx.files.internal("res/data"));
        resources.loadContentFromDirectory(Gdx.files.internal("res/general"));
        resources.loadContentFromDirectory(Gdx.files.internal("res/cave"));

        ps = new ParticleEffect();
        ps.load(new FileHandle("res/cave/muzzle_skip.p"), new FileHandle("res/cave"));
        ps.flipY();
        ps.getEmitters().get(0).setContinuous(true);
        ps.start();
        resources.addAsset("particle", ps);

        final Dimension screenSize = getEngine().getScreenSize();
        final Pixmap pix = new Pixmap((int) screenSize.getWidth(), (int) screenSize.getHeight(), Pixmap.Format.RGBA8888);
        pix.setColor(Color.BLACK);
        pix.fill();
        final Image2D black = new Image2D(pix);
        pix.dispose();
        resources.addAsset("darkness", black);

        Stream.of(resources.getAnimation("main")).forEach(Image2D::createPixelData);
        Stream.of(resources.getAnimation("drill")).forEach(Image2D::createPixelData);
        Stream.of(resources.getAnimation("bottompart")).forEach(Image2D::createPixelData);

        getEngine().timeFont = resources.getFont("sansserif32.fnt");
        resources.remove("pixmap.png");

        collapsing = resources.getMusic("collapsing_music.wav");
        drilling = resources.getMusic("drilling_music.wav");
        exp1 = resources.getSound("exp1.wav");
        exp2 = resources.getSound("exp2.wav");
        exp3 = resources.getSound("exp3.wav");
        exp4 = resources.getSound("exp4.wav");
        music = resources.getMusic("music.ogg");

        Utils.playMusic(music, 0, .7f);
    }

    @Override
    public TiledMap getTileMap() {
        return resources.getTiledMap("map.tmx");
    }

    @Override
    public void build() {
        /*
         * Level Stuff
         */
        drugEffect = false;
        coll1 = coll2 = coll3 = coll4 = done = false;
        collapsing.stop();
        drilling.setVolume(0);
        drilling.setLooping(true);
        drilling.play();
        add(this::extra);

        final Entity vShake = CameraEffects.verticalMovement(2, 2);
        final Entity hShake = CameraEffects.horizontalMovement(2, 2);

        runOnceWhen(() -> {
            add(vShake);
            add(hShake);
        }, () -> drugEffect);

        /*
         * Main Character
         */
        play = ResourceUtil.getGravityMan(resources);
        play.move(4 * 24, (56 * 24));
        play.freeze();
        add(play);

        /*
         * Background and foreground
         */
        add(getWorldImage());
        add(new EntityBuilder().image(resources.getImage("background.png")).zIndex(-5).build(RepeatingParallaxImage.class));

        /*
         * Darkness
         */
        final PingPongFloat ppFloat = new PingPongFloat(.1f, .5f, .001f);
        final StaticImage darkness = new StaticImage();
        darkness.setImage(resources.getImage("darkness"));
        darkness.zIndex(1000);
        darkness.tint.a = ppFloat.get();
        darkness.addEvent(() -> darkness.tint.a = ppFloat.get());
        add(darkness);

        /*
         * Collapsing roof
		 */
        final TmxEntity tmxCrusher = new TmxEntity(resources.getTiledMap("crusher.tmx"));
        tmxCrusher.zIndex(99);

        crusher = new SolidPlatform(0, -1406, play);
        crusher.bounds.size.set(tmxCrusher.width(), tmxCrusher.height());
        crusher.appendPath(0, -119, Integer.MAX_VALUE, false, () -> {
            drugEffect = false;
            collapsing.stop();
            resources.getSound("slam.wav").play();
            discard(vShake);
            discard(hShake);
        });
        crusher.setMoveSpeed(0);

        tmxCrusher.addEvent(Factory.follow(crusher, tmxCrusher));

        /*
		 * Camera
		 */
        camera = new MobileEntity();
        camera.move(547, 215);
        camera.setMoveSpeed(7);
        camera.freeze();

        addSoundListener(camera);
        addFocusObject(camera);
        runOnceAfter(camera::unfreeze, 85);

        /*
		 * Drill
		 */
        drill = new PathDrone(547, 215);
        drill.setImage(1, resources.getAnimation("drill"));
        drill.setMoveSpeed(2);
        drill.zIndex(10);
        drill.setHitbox(Hitbox.PIXEL);
        drill.sounds.useFalloff = true;
        drill.sounds.maxDistance = 2000;
        drill.appendPath(547, 60, 0, false, () -> {
            drill.setMoveSpeed(.1f);
            add(dust);
        });
        drill.appendPath(547, -100, Integer.MAX_VALUE, false, () -> {
            crusher.setMoveSpeed(2.1f);
            discard(dust);
            collapsing.setLooping(true);
            collapsing.play();
            drilling.stop();
            drugEffect = true;
        });
        drill.addEvent(Factory.hitMain(drill, play, -1));
        drill.addEvent(()-> {
            drilling.setVolume(drill.sounds.calc());
        });

        final PathDrone middlePart = new PathDrone(0, 0);
        middlePart.addEvent(Factory.follow(drill, middlePart, 8, 100));
        middlePart.setImage(resources.getImage("middlepart.png"));

        final PathDrone bottomPart = new PathDrone(0, 0);
        bottomPart.addEvent(Factory.follow(drill, bottomPart, 17, 160));
        bottomPart.setImage(1, resources.getAnimation("bottompart"));
        bottomPart.setHitbox(Hitbox.PIXEL);

        play.addObstacle(middlePart);
        play.addObstacle(bottomPart);
//        addFocusObject(drill);

        add(drill);
        add(middlePart);
        add(bottomPart);
        add(crusher);
        add(tmxCrusher);


        /*
		 * Collectable Items
		 */
        item1 = new Entity();
        item1.move(781, 1417);
        item1.setImage(6, resources.getAnimation("collect"));
        final Entity item2 = item1.getClone().move(901, 708);
        final Entity item3 = item1.getClone().move(195, 263);
        final Entity item4 = item1.getClone().move(1130, 229);

        item2.ifCollides(play).thenRunOnce(() -> {
            discard(item2);
            coll2 = true;
            resources.getSound("collect3.wav").play();
        });

        item3.ifCollides(play).thenRunOnce(() -> {
            discard(item3);
            coll3 = true;
            resources.getSound("collect3.wav").play();
        });

        item4.ifCollides(play).thenRunOnce(() -> {
            discard(item4);
            coll4 = true;
            resources.getSound("collect3.wav").play();
        });

        add(item1);
        add(item2);
        add(item3);
        add(item4);

        /*
		 * Particle Object
		 */
        dust = new Entity() {
            {
                setVisible(true);
            }
            @Override
            public void render(final SpriteBatch batch) {
                ps.setPosition(drill.centerX(), drill.centerY());
                ps.draw(batch, Gdx.graphics.getDeltaTime());
            }
        };
        dust.zIndex(98);

        /*
		 * Flag
		 */
        flag = new Entity();
        flag.move(1011, 1327);
        flag.setImage(4, resources.getAnimation("flag"));
        flag.zIndex(-1);
        add(flag);
        add(new EntityBuilder().image(resources.getImage("pole.png")).move(1006, 1327).zIndex(-1).build());
    }

    @Override
    public void dispose() {
        resources.disposeAll();
    }

    @Override
    public Music getStageMusic() {
        return music;
    }

    @Override
    public String getLevelName() {
        return "Collapsing Cave";
    }

    private void extra() {
        if (drill.getMoveSpeed() == .1f && crusher.getMoveSpeed() == 0 && ++soundCounter % 6 == 0) {

            switch (r.nextInt(4)) {
                case 0:
                    drill.sounds.play(exp1);
                    break;
                case 1:
                    drill.sounds.play(exp2);
                    break;
                case 2:
                    drill.sounds.play(exp3);
                    break;
                case 3:
                    drill.sounds.play(exp4);
                    break;
            }
        }

        if (!coll1 && item1.collidesWith(play)) {
            discard(item1);
            coll1 = true;
            resources.getSound("collect3.wav").play();
        }

        if (coll1 && coll2 && coll3 && coll4 && flag.collidesWith(play))
            play.setState(Vitality.COMPLETED);

        if (!done && !camera.isFrozen()) {
            camera.moveTowards(play.x(), play.y());
            if (BaseLogic.distance(camera, play) < 200) {
                done = true;
                removeFocusObject(camera);
                removeSoundListener(camera);
                addFocusObject(play);
                play.unfreeze();
            }
        }
    }
}
