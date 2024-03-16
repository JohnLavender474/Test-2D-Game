package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.enums.ProcessState
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.normalizedTrajectory
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.toGameRectangle
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
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
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.*
import kotlin.reflect.KClass

class SniperJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable, IDirectionRotatable {

    companion object {
        const val TAG = "SniperJoe"

        private const val DEFAULT_TYPE = "Orange"
        private const val SNOW_TYPE = "Snow"

        private val TIMES_TO_SHOOT = floatArrayOf(0.15f, 0.75f, 1.35f)

        private const val BULLET_SPEED = 7.5f
        private const val SNOWBALL_X = 10f
        private const val SNOWBALL_Y = 5f
        private const val SNOWBALL_GRAV = 0.15f
        private const val JUMP_IMPULSE = 15f

        private const val SHIELD_DUR = 1.75f
        private const val SHOOT_DUR = 1.5f
        private const val THROW_SHIELD_DUR = 0.5f
        private const val SHIELD_VEL = 10f

        private const val GROUND_GRAVITY = 0.015f
        private const val GRAVITY = 0.375f

        private val regions = ObjectMap<String, TextureRegion>()
        private val joeTypes = gdxArrayOf(DEFAULT_TYPE, SNOW_TYPE)
        private val regionKeys = gdxArrayOf(
            "JumpNoShield",
            "JumpWithShield",
            "ShootingNoShield",
            "ShootingWithShield",
            "StandNoShield",
            "StandShielded",
            "ThrowShield"
        )
    }

    enum class SniperJoeState {
        WAITING_SHIELDED, WAITING_NO_SHIELD, SHOOTING_WITH_SHIELD, SHOOTING_NO_SHIELD, THROWING_SHIELD
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(3), Fireball::class to dmgNeg(15), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 5
        }, ChargedShotExplosion::class to dmgNeg(3)
    )

    override var directionRotation: Direction
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    override var facing = Facing.RIGHT

    private lateinit var type: String
    private lateinit var state: SniperJoeState

    private var throwShieldTrigger: GameRectangle? = null

    private val shielded: Boolean
        get() = state == SniperJoeState.WAITING_SHIELDED
    private val hasShield: Boolean
        get() = state == SniperJoeState.WAITING_SHIELDED || state == SniperJoeState.SHOOTING_WITH_SHIELD

    private val waitTimer = Timer(SHIELD_DUR)
    private val shootTimer = Timer(SHOOT_DUR)
    private val throwShieldTimer = Timer(THROW_SHIELD_DUR)

    private var canThrowShield = false
    private var setToThrowShield = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            joeTypes.forEach { joeType ->
                regionKeys.forEach { regionKey ->
                    val region = atlas.findRegion("SniperJoe/$joeType/$regionKey")
                    regions.put("$joeType/$regionKey", region)
                }
            }
        }

        super.init()

        val shootRunnables = Array<TimeMarkedRunnable>()
        TIMES_TO_SHOOT.forEach { shootRunnables.add(TimeMarkedRunnable(it) { shoot() }) }
        shootTimer.setRunnables(shootRunnables)

        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn =
            if (spawnProps.containsKey(ConstKeys.BOUNDS)) spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
                .getBottomCenterPoint()
            else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.positionOnPoint(spawn, Position.BOTTOM_CENTER)

        if (spawnProps.containsKey(ConstKeys.TRIGGER)) {
            canThrowShield = true
            throwShieldTrigger =
                spawnProps.get(ConstKeys.TRIGGER, RectangleMapObject::class)!!.rectangle.toGameRectangle()
        } else {
            canThrowShield = false
            throwShieldTrigger = null
        }

        type = spawnProps.getOrDefault(ConstKeys.TYPE, DEFAULT_TYPE) as String
        state = SniperJoeState.WAITING_SHIELDED
        directionRotation = Direction.UP

        waitTimer.reset()
        shootTimer.setToEnd()
        throwShieldTimer.setToEnd()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)

        val shapes = Array<() -> IDrawableShape?>()

        // body fixture
        val bodyFixture = Fixture(GameRectangle().set(body), FixtureType.BODY)
        body.addFixture(bodyFixture)

        // feet fixture
        val feetFixture = Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM), FixtureType.FEET)
        feetFixture.offsetFromBodyCenter.y = -0.75f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.shape.color = Color.GREEN
        shapes.add { feetFixture.shape }

        // damager fixture
        val damagerFixture = Fixture(
            GameRectangle().setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM), FixtureType.DAMAGER
        )
        body.addFixture(damagerFixture)
        damagerFixture.shape.color = Color.RED
        shapes.add { damagerFixture.bodyRelativeShape }

        // damageable fixture
        val damageableFixture = Fixture(
            GameRectangle().setSize(0.8f * ConstVals.PPM, 1.35f * ConstVals.PPM), FixtureType.DAMAGEABLE
        )
        body.addFixture(damageableFixture)
        damageableFixture.shape.color = Color.PURPLE
        shapes.add { damageableFixture.bodyRelativeShape }

        // shield fixture
        val shieldFixture = Fixture(
            GameRectangle().setSize(0.4f * ConstVals.PPM, 0.9f * ConstVals.PPM), FixtureType.SHIELD
        )
        body.addFixture(shieldFixture)
        shieldFixture.shape.color = Color.BLUE
        shapes.add { shieldFixture.bodyRelativeShape }

        // trigger fixture
        val triggerFixture = Fixture(GameRectangle(), FixtureType.CONSUMER)
        triggerFixture.setConsumer { processState, fixture ->
            if (hasShield && processState == ProcessState.BEGIN && fixture.fixtureLabel == FixtureType.PLAYER) setToThrowShield =
                true
        }
        triggerFixture.attachedToBody = false
        body.addFixture(triggerFixture)
        triggerFixture.shape.color = Color.YELLOW
        shapes.add { triggerFixture.shape }

        // pre-process
        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            if (canThrowShield && throwShieldTrigger != null) {
                triggerFixture.active = true
                triggerFixture.shape = throwShieldTrigger!!
            } else triggerFixture.active = false

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) -GROUND_GRAVITY else -GRAVITY
            body.physics.gravity = (when (directionRotation) {
                Direction.UP -> Vector2(0f, gravity)
                Direction.DOWN -> Vector2(0f, -gravity)
                Direction.LEFT -> Vector2(-gravity, 0f)
                Direction.RIGHT -> Vector2(gravity, 0f)
            }).scl(ConstVals.PPM.toFloat())

            shieldFixture.active = shielded
            shieldFixture.offsetFromBodyCenter.x =
                0.35f * ConstVals.PPM * if (isDirectionRotatedUp() || isDirectionRotatedLeft()) facing.value
                else -facing.value
            shieldFixture.putProperty(ConstKeys.DIRECTION, directionRotation)

            if (shielded) damageableFixture.offsetFromBodyCenter.x =
                0.25f * ConstVals.PPM * if (isDirectionRotatedUp() || isDirectionRotatedLeft()) -facing.value
                else facing.value
            else damageableFixture.offsetFromBodyCenter.x = 0f
        })

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.35f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, "sniperjoe" to sprite)
        spritesComponent.putUpdateFunction("sniperjoe") { _, _sprite ->
            _sprite as GameSprite

            val flipX = facing == Facing.LEFT
            val flipY = directionRotation == Direction.DOWN
            _sprite.setFlip(flipX, flipY)

            val rotation = when (directionRotation) {
                Direction.UP, Direction.DOWN -> 0f

                Direction.LEFT -> 90f
                Direction.RIGHT -> 270f
            }
            sprite.setOriginCenter()
            _sprite.setRotation(rotation)

            val position = when (directionRotation) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)

            if (directionRotation == Direction.LEFT) _sprite.translateX(0.15f * ConstVals.PPM)
            else if (directionRotation == Direction.RIGHT) _sprite.translateX(-0.15f * ConstVals.PPM)
        }
        return spritesComponent
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = when (directionRotation) {
                Direction.UP, Direction.DOWN -> if (megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT
                Direction.LEFT, Direction.RIGHT -> if (megaman.body.y > body.y) Facing.RIGHT else Facing.LEFT
            }
            if (body.isSensing(BodySense.FEET_ON_GROUND) && megaman.body.x >= body.x && megaman.body.getMaxX() <= body.getMaxX()) jump()

            if (!isInGameCamBounds()) {
                state = if (hasShield) SniperJoeState.WAITING_SHIELDED else SniperJoeState.WAITING_NO_SHIELD
                waitTimer.reset()
                return@add
            }

            when (state) {
                SniperJoeState.WAITING_SHIELDED -> {
                    if (setToThrowShield) {
                        throwShield()
                        throwShieldTimer.reset()
                        state = SniperJoeState.THROWING_SHIELD
                        setToThrowShield = false
                    } else if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                        waitTimer.update(it)
                        if (waitTimer.isJustFinished()) {
                            shootTimer.reset()
                            state = SniperJoeState.SHOOTING_WITH_SHIELD
                        }
                    }
                }

                SniperJoeState.SHOOTING_WITH_SHIELD -> {
                    shootTimer.update(it)
                    if (shootTimer.isJustFinished()) {
                        waitTimer.reset()
                        state = SniperJoeState.WAITING_SHIELDED
                    }
                }

                SniperJoeState.THROWING_SHIELD -> {
                    throwShieldTimer.update(it)
                    if (throwShieldTimer.isJustFinished()) {
                        waitTimer.reset()
                        state = SniperJoeState.WAITING_NO_SHIELD
                    }
                }

                SniperJoeState.WAITING_NO_SHIELD -> {
                    waitTimer.update(it)
                    if (waitTimer.isJustFinished()) {
                        shootTimer.reset()
                        state = SniperJoeState.SHOOTING_NO_SHIELD
                    }
                }

                SniperJoeState.SHOOTING_NO_SHIELD -> {
                    shootTimer.update(it)
                    if (shootTimer.isJustFinished()) {
                        waitTimer.reset()
                        state = SniperJoeState.WAITING_NO_SHIELD
                    }
                }
            }
        }
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            val regionKey = when (state) {
                SniperJoeState.WAITING_SHIELDED -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "StandShielded"
                    else "JumpWithShield"
                }

                SniperJoeState.WAITING_NO_SHIELD -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "StandNoShield"
                    else "JumpNoShield"
                }

                SniperJoeState.SHOOTING_WITH_SHIELD -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "ShootingWithShield"
                    else "JumpWithShield"
                }

                SniperJoeState.SHOOTING_NO_SHIELD -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) "ShootingNoShield"
                    else "JumpNoShield"
                }

                SniperJoeState.THROWING_SHIELD -> "ThrowShield"
            }
            "$type/$regionKey"
        }

        val animations = ObjectMap<String, IAnimation>()
        joeTypes.forEach { joeType ->
            regionKeys.forEach { regionKey ->
                val region = regions.get("$joeType/$regionKey")
                animations.put("$joeType/$regionKey", Animation(region))
            }
        }

        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun throwShield() {
        val shield = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNIPER_JOE_SHIELD)!!
        game.gameEngine.spawn(
            shield, props(
                ConstKeys.POSITION to body.getCenter(), ConstKeys.TRAJECTORY to normalizedTrajectory(
                    body.getCenter(), megaman.body.getCenter(), SHIELD_VEL * ConstVals.PPM
                ), ConstKeys.OWNER to this
            )
        )
    }

    private fun jump() {
        body.physics.velocity.set(0f, JUMP_IMPULSE * ConstVals.PPM)
    }

    private fun shoot() {
        val spawn = (when (directionRotation) {
            Direction.UP -> Vector2(0.25f * facing.value, -0.15f)
            Direction.DOWN -> Vector2(0.25f * facing.value, 0.15f)
            Direction.LEFT -> Vector2(0.2f, 0.25f * facing.value)
            Direction.RIGHT -> Vector2(-0.2f, 0.25f * facing.value)
        }).scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        val trajectory = Vector2()

        val props = props(
            ConstKeys.OWNER to this,
            ConstKeys.POSITION to spawn,
            ConstKeys.TRAJECTORY to trajectory,
            ConstKeys.DIRECTION to directionRotation
        )

        val entity: IGameEntity = if (type == SNOW_TYPE) {
            trajectory.x = SNOWBALL_X * ConstVals.PPM * facing.value
            trajectory.y = SNOWBALL_Y * ConstVals.PPM

            props.put(ConstKeys.GRAVITY_ON, true)
            props.put(ConstKeys.GRAVITY, Vector2(0f, -SNOWBALL_GRAV * ConstVals.PPM))

            requestToPlaySound(SoundAsset.CHILL_SHOOT, false)

            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNOWBALL)!!
        } else {
            if (isDirectionRotatedVertically()) trajectory.set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)
            else trajectory.set(0f, BULLET_SPEED * ConstVals.PPM * facing.value)

            requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)

            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        }

        trajectory.scl(ConstVals.PPM.toFloat())
        game.gameEngine.spawn(entity, props)
    }
}
