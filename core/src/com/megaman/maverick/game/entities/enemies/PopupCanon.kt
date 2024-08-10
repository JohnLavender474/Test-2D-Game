package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.enums.Size
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class PopupCanon(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "PopupCanon"
        private const val SHOOT_X = 8f
        private const val SHOOT_Y = 2.5f
        private const val REST_DUR = 0.75f
        private const val TRANS_DUR = 0.6f
        private const val SHOOT_DUR = 0.25f
        private const val DEFAULT_BALL_GRAVITY_SCALAR = 1f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class PopupCanonState {
        REST, RISE, SHOOT, FALL
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        })
    override lateinit var facing: Facing

    private val loop = Loop(PopupCanonState.values().toGdxArray())
    private val timers = objectMapOf(
        "rest" to Timer(REST_DUR),
        "rise" to Timer(TRANS_DUR, gdxArrayOf(
            TimeMarkedRunnable(0f) { transState = Size.SMALL },
            TimeMarkedRunnable(0.25f) { transState = Size.MEDIUM },
            TimeMarkedRunnable(0.5f) { transState = Size.LARGE }
        )),
        "fall" to Timer(TRANS_DUR, gdxArrayOf(
            TimeMarkedRunnable(0f) { transState = Size.LARGE },
            TimeMarkedRunnable(0.25f) { transState = Size.MEDIUM },
            TimeMarkedRunnable(0.5f) { transState = Size.SMALL }
        )),
        "shoot" to Timer(SHOOT_DUR, gdxArrayOf(TimeMarkedRunnable(0.25f) { shoot() }))
    )
    private var ballGravityScalar = DEFAULT_BALL_GRAVITY_SCALAR
    private lateinit var transState: Size

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("rest", atlas.findRegion("$TAG/Down"))
            regions.put("trans", atlas.findRegion("$TAG/Rise"))
            regions.put("shoot", atlas.findRegion("$TAG/Up"))
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        loop.reset()
        timers.values().forEach { it.reset() }

        ballGravityScalar = spawnProps.getOrDefault(
            "${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}",
            DEFAULT_BALL_GRAVITY_SCALAR,
            Float::class
        )

        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        transState = Size.SMALL
    }

    private fun shoot() {
        val explodingBall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.EXPLODING_BALL)!!
        game.engine.spawn(
            explodingBall, props(
                ConstKeys.POSITION to body.getCenter().add(
                    0.25f * ConstVals.PPM * facing.value, 0.125f * ConstVals.PPM
                ),
                ConstKeys.IMPULSE to Vector2(SHOOT_X * facing.value, SHOOT_Y).scl(ConstVals.PPM.toFloat()),
                "${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}" to ballGravityScalar
            )
        )

        requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT

            val timerKey = when (loop.getCurrent()) {
                PopupCanonState.REST -> "rest"
                PopupCanonState.RISE -> "rise"
                PopupCanonState.FALL -> "fall"
                PopupCanonState.SHOOT -> "shoot"
            }
            val timer = timers.get(timerKey)
            timer.update(delta)
            if (timer.isFinished()) {
                timer.reset()
                loop.next()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.15f * ConstVals.PPM, 1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        // debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setWidth(1.15f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.GREEN
        // debugShapes.add { if (damageableFixture.active) damageableFixture.getShape() else null }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val damageable = loop.getCurrent() != PopupCanonState.REST
            damageableFixture.active = damageable

            (damagerFixture.rawShape as GameRectangle).height = (
                    when (transState) {
                        Size.LARGE -> 1.5f
                        Size.MEDIUM -> 1f
                        Size.SMALL -> 0.25f
                    }) * ConstVals.PPM
            damagerFixture.offsetFromBodyCenter.y = (when (transState) {
                Size.LARGE -> 0f
                Size.MEDIUM -> -0.25f
                Size.SMALL -> -0.525f
            }) * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.65f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = {
            when (loop.getCurrent()) {
                PopupCanonState.REST -> "rest"
                PopupCanonState.RISE -> "rise"
                PopupCanonState.SHOOT -> "shoot"
                PopupCanonState.FALL -> "fall"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "rest" to Animation(regions.get("rest")),
            "rise" to Animation(regions.get("trans"), 2, 3, 0.1f, false),
            "shoot" to Animation(regions.get("shoot")),
            "fall" to Animation(regions.get("trans"), 2, 3, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}