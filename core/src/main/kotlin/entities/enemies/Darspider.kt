package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.ObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class Darspider(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDirectional, IFaceable {

    companion object {
        const val TAG = "Darspider"
        private const val STILL_DUR = 0.35f
        private const val CRAWL_SPEED = 6f
        private const val JUMP_IMPULSE = 12f
        private const val GROUND_GRAVITY = -0.001f
        private const val GRAVITY = -0.2f
        private const val LEFT_FOOT = "${ConstKeys.LEFT}_${ConstKeys.FOOT}"
        private const val RIGHT_FOOT = "${ConstKeys.RIGHT}_${ConstKeys.FOOT}"
        private const val CULL_TIME = 1.5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 20
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 15 else 10
        }
    )
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override lateinit var facing: Facing

    private val stillTimer = Timer(STILL_DUR)
    private var onCeiling = true
    private var minXOnCeiling = 0f
    private var maxXOnCeiling = 0f
    private var minXOffCeiling = 0f
    private var maxXOffCeiling = 0f

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("crawl", atlas.findRegion("$TAG/crawl"))
            regions.put("frozen", atlas.findRegion("$TAG/frozen"))
            regions.put("jump", atlas.findRegion("$TAG/jump"))
            regions.put("still", atlas.findRegion("$TAG/still"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        super.onSpawn(spawnProps)

        onCeiling = spawnProps.getOrDefault(ConstKeys.ON, true, Boolean::class)
        direction =
            Direction.valueOf(
                spawnProps.getOrDefault(ConstKeys.DIRECTION, if (onCeiling) "down" else "up", String::class).uppercase()
            )
        facing = when (direction) {
            Direction.DOWN -> if (megaman().body.getX() < body.getX()) Facing.RIGHT else Facing.LEFT
            else -> if (megaman().body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
        }

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
            .getPositionPoint(DirectionPositionMapper.getPosition(direction).opposite())
        body.positionOnPoint(spawn, DirectionPositionMapper.getPosition(direction).opposite())

        minXOnCeiling =
            spawn.x - spawnProps.getOrDefault("${ConstKeys.ON}_${ConstKeys.MIN}", 0f, Float::class) * ConstVals.PPM
        maxXOnCeiling =
            spawn.x + spawnProps.getOrDefault("${ConstKeys.ON}_${ConstKeys.MAX}", 0f, Float::class) * ConstVals.PPM
        minXOffCeiling =
            spawn.x - spawnProps.getOrDefault("${ConstKeys.OFF}_${ConstKeys.MIN}", 0f, Float::class) * ConstVals.PPM
        maxXOffCeiling =
            spawn.x + spawnProps.getOrDefault("${ConstKeys.OFF}_${ConstKeys.MAX}", 0f, Float::class) * ConstVals.PPM

        stillTimer.reset()
    }

    private fun drop() {
        body.physics.velocity.x = 0f
        body.physics.velocity.y = -JUMP_IMPULSE * ConstVals.PPM
        direction = Direction.UP
        onCeiling = false
    }

    private fun jump() {
        body.physics.velocity.x = 0f
        body.physics.velocity.y = JUMP_IMPULSE * ConstVals.PPM
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!stillTimer.isFinished()) {
                body.physics.velocity.x = 0f
                stillTimer.update(delta)
                return@add
            }

            body.physics.velocity.x = if (body.isSensing(BodySense.FEET_ON_GROUND))
                CRAWL_SPEED * ConstVals.PPM * facing.value else 0f

            if (onCeiling &&
                ((isFacing(Facing.RIGHT) && body.getCenter().x >= maxXOnCeiling) ||
                    (isFacing(Facing.LEFT) && body.getCenter().x <= minXOnCeiling))
            ) {
                swapFacing()
                stillTimer.reset()
            } else if (!onCeiling &&
                ((isFacing(Facing.LEFT) && body.getCenter().x <= minXOffCeiling) ||
                    (isFacing(Facing.RIGHT) && body.getCenter().x >= maxXOffCeiling))
            ) {
                swapFacing()
                stillTimer.reset()
            }

            if (onCeiling) {
                if (megaman().body.getMaxX() > body.getX() && megaman().body.getX() < body.getMaxX()) drop()
            } else if (body.isSensing(BodySense.FEET_ON_GROUND) &&
                megaman().body.getMaxX() >= body.getX() &&
                megaman().body.getX() <= body.getMaxX() &&
                megaman().body.getY() >= body.getMaxY()
            ) jump()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.85f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.putProperty(LEFT_FOOT, false)
        body.putProperty(RIGHT_FOOT, false)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -0.375f * ConstVals.PPM
        feetFixture.setHitByBlockReceiver {
            facing = if (megaman().body.getX() < body.getX()) Facing.RIGHT else Facing.LEFT
            if (direction == Direction.UP) swapFacing()
        }
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture}

        val leftSideFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftSideFixture.offsetFromBodyAttachment.x = -0.375f * ConstVals.PPM
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftSideFixture)
        debugShapes.add { leftSideFixture}

        val rightSideFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightSideFixture.offsetFromBodyAttachment.x = 0.375f * ConstVals.PPM
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightSideFixture)
        debugShapes.add { rightSideFixture}

        val leftFootFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFootFixture.setConsumer { _, fixture ->
            if (fixture.getType() == FixtureType.BLOCK)
                body.putProperty("${ConstKeys.LEFT}_${ConstKeys.FOOT}", true)
        }
        leftFootFixture.offsetFromBodyAttachment =
            ObjectPools.get(Vector2::class).set(-0.375f * ConstVals.PPM, -0.375f * ConstVals.PPM)
        body.addFixture(leftFootFixture)
        debugShapes.add { leftFootFixture}

        val rightFootFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFootFixture.setConsumer { _, fixture ->
            if (fixture.getType() == FixtureType.BLOCK)
                body.putProperty("${ConstKeys.RIGHT}_${ConstKeys.FOOT}", true)
        }
        rightFootFixture.offsetFromBodyAttachment.x = 0.375f * ConstVals.PPM
        rightFootFixture.offsetFromBodyAttachment.y = -0.375f * ConstVals.PPM
        body.addFixture(rightFootFixture)
        debugShapes.add { rightFootFixture}

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.putProperty(LEFT_FOOT, false)
            body.putProperty(RIGHT_FOOT, false)
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity = when (direction) {
                Direction.UP -> Vector2(0f, gravity)
                Direction.DOWN -> Vector2(0f, -gravity)
                Direction.LEFT -> Vector2(-gravity, 0f)
                Direction.RIGHT -> Vector2(gravity, 0f)
            }.scl(ConstVals.PPM.toFloat())
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            val position = DirectionPositionMapper.getPosition(direction).opposite()
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (!stillTimer.isFinished()) "still"
            else if (!body.isSensing(BodySense.FEET_ON_GROUND)) "jump"
            else "crawl"
        }
        val animation = objectMapOf<String, IAnimation>(
            "still" pairTo Animation(regions["still"]),
            "jump" pairTo Animation(regions["jump"]),
            "crawl" pairTo Animation(regions["crawl"], 2, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animation)
        return AnimationsComponent(this, animator)
    }
}
