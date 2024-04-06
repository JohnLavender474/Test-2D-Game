package com.megaman.maverick.game.entities.bosses.sigmarat

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getRandomElements
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.getRandomBool
import com.engine.common.objects.Properties
import com.engine.common.objects.WeightedRandomSelector
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.getCenter
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.megaman.components.feet
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.SigmaRatElectricBall
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class SigmaRat(game: MegamanMaverickGame) : AbstractBoss(game) {

    companion object {
        const val TAG = "SigmaRat"

        private const val ATTACK_DELAY_MIN = 0.25f
        private const val ATTACK_DELAY_MAX = 1.25f

        private const val CLAW_ROTATION_SPEED = 5f

        private const val HEAD_POSITION = "head_position"

        private val ANGLES = gdxArrayOf(
            225f,
            232.5f,
            240f,
            247.5f,
            255f,
            262.5f,
            270f,
            277.5f,
            285f,
            292.5f,
            300f,
            307.5f,
            315f,
        )

        private const val ELECTRIC_BALLS_SPEED = 10f
        private const val ELECTRIC_BALL_SHOT_DELAY = 0.5f
        private const val INDEX_TO_TRY_CLAW_DURING_ELECTRIC_BALLS = 6

        private const val FIREBALL_DELAY = 0.5f
        private const val FIREBALL_SPEED = 7.5f
        private const val FIREBALL_CULL_TIME = 3f

        private var bodyRegion: TextureRegion? = null
        private var bodyDamagedRegion: TextureRegion? = null
        private var bodyTittyShootRegion: TextureRegion? = null
        private var bodyTittyShootDamagedRegion: TextureRegion? = null
    }

    enum class SigmaRatAttack {
        ELECTRIC_BALLS, FIRE_BLASTS, CLAW_SHOCK, CLAW_LAUNCH, /* TITTY_LASERS */
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(1), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        }, ChargedShotExplosion::class to dmgNeg(1)
    )

    private val weightedAttackSelector = WeightedRandomSelector(
        SigmaRatAttack.ELECTRIC_BALLS to 0.25f,
        SigmaRatAttack.FIRE_BLASTS to 0.25f,
        SigmaRatAttack.CLAW_SHOCK to 0.2f,
        SigmaRatAttack.CLAW_LAUNCH to 0.3f
    )

    private val attackTimer = Timer(ATTACK_DELAY_MAX)

    private val electricBalls = Queue<SigmaRatElectricBall>()
    private val electricShotDelayTimer = Timer(ELECTRIC_BALL_SHOT_DELAY)
    private val fireballs = Queue<Pair<Fireball, Float>>()
    private val fireballDelayTimer = Timer(FIREBALL_DELAY)

    private lateinit var headPosition: Vector2

    private lateinit var leftClawSpawn: Vector2
    private lateinit var rightClawSpawn: Vector2

    private var leftClaw: SigmaRatClaw? = null
    private var rightClaw: SigmaRatClaw? = null

    private var attackState: SigmaRatAttack? = null

    private var electricBallsClockwise = false

    override fun init() {
        if (bodyRegion == null || bodyDamagedRegion == null || bodyTittyShootRegion == null || bodyTittyShootDamagedRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            bodyRegion = atlas.findRegion("SigmaRat/Body")
            bodyDamagedRegion = atlas.findRegion("SigmaRat/BodyDamaged")
            bodyTittyShootRegion = atlas.findRegion("SigmaRat/BodyTittyShoot")
            bodyTittyShootDamagedRegion = atlas.findRegion("SigmaRat/BodyTittyShootDamaged")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        headPosition = spawnProps.get(HEAD_POSITION, RectangleMapObject::class)!!.rectangle.getCenter()

        leftClawSpawn = spawnProps.get(ConstKeys.LEFT, RectangleMapObject::class)!!.rectangle.getCenter()
        rightClawSpawn = spawnProps.get(ConstKeys.RIGHT, RectangleMapObject::class)!!.rectangle.getCenter()
        leftClaw = SigmaRatClaw(game as MegamanMaverickGame)
        rightClaw = SigmaRatClaw(game as MegamanMaverickGame)
        game.engine.spawn(
            leftClaw!! to props(
                ConstKeys.PARENT to this, ConstKeys.SPEED to CLAW_ROTATION_SPEED, ConstKeys.POSITION to leftClawSpawn
            ), rightClaw!! to props(
                ConstKeys.PARENT to this, ConstKeys.SPEED to -CLAW_ROTATION_SPEED, ConstKeys.POSITION to rightClawSpawn
            )
        )

        attackTimer.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        leftClaw?.kill()
        leftClaw = null
        rightClaw?.kill()
        rightClaw = null
        while (!electricBalls.isEmpty) electricBalls.removeFirst().kill()
    }

    override fun triggerDefeat() {
        super.triggerDefeat()
        val explosions = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION, 2)
        game.engine.spawn(
            explosions[0] to props(
                ConstKeys.POSITION to leftClaw!!.body.getCenter(),
                ConstKeys.SOUND to SoundAsset.EXPLOSION_1_SOUND
            ),
            explosions[1] to props(
                ConstKeys.POSITION to rightClaw!!.body.getCenter(),
                ConstKeys.SOUND to SoundAsset.EXPLOSION_1_SOUND
            )
        )
        leftClaw!!.kill()
        leftClaw = null
        rightClaw!!.kill()
        rightClaw = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            if (leftClaw?.dead == true) {
                leftClaw?.kill()
                leftClaw = null
            }
            if (rightClaw?.dead == true) {
                rightClaw?.kill()
                rightClaw = null
            }

            if (attackState == null) {
                attackTimer.update(delta)
                if (attackTimer.isFinished()) {
                    startAttack()
                    val newDuration = ATTACK_DELAY_MIN + (ATTACK_DELAY_MAX - ATTACK_DELAY_MIN) * getHealthRatio()
                    attackTimer.resetDuration(newDuration)
                }
            } else continueAttack(delta)
        }
    }

    private fun startAttack() {
        val attackState = weightedAttackSelector.getRandomItem()
        when (attackState) {
            SigmaRatAttack.ELECTRIC_BALLS -> {
                electricShotDelayTimer.reset()
                for (i in 0 until ANGLES.size) {
                    val electricBall =
                        EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SIGMA_RAT_ELECTRIC_BALL)!!
                    game.engine.spawn(electricBall, props(ConstKeys.POSITION to headPosition))
                    electricBalls.addLast(electricBall as SigmaRatElectricBall)
                }
                electricBallsClockwise = getRandomBool()
                requestToPlaySound(SoundAsset.LIFT_OFF_SOUND, false)
            }

            SigmaRatAttack.FIRE_BLASTS -> {
                fireballDelayTimer.reset()
                val angles = ANGLES.getRandomElements(3)
                for (angle in angles) {
                    val fireball =
                        EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIREBALL)!! as Fireball
                    game.engine.spawn(
                        fireball, props(
                            ConstKeys.POSITION to headPosition, ConstKeys.CULL_TIME to FIREBALL_CULL_TIME
                        )
                    )
                    fireballs.addLast(fireball to angle)
                }
            }

            SigmaRatAttack.CLAW_SHOCK -> {
                val claw = if (megaman.feet.getShape().overlaps(leftClaw!!.body)) leftClaw
                else if (megaman.feet.getShape().overlaps(rightClaw!!.body)) rightClaw
                else if (getRandomBool()) leftClaw else rightClaw

                if (claw!!.shocking) return
                claw.enterShockState()
            }

            SigmaRatAttack.CLAW_LAUNCH -> {
                val claw = if (megaman.feet.getShape().overlaps(leftClaw!!.body)) rightClaw
                else if (megaman.feet.getShape().overlaps(rightClaw!!.body)) leftClaw
                else if (getRandomBool()) leftClaw else rightClaw

                if (claw!!.launched) return
                claw.enterLaunchState()
            }
        }

        this.attackState = attackState
    }

    private fun continueAttack(delta: Float) {
        when (attackState) {
            SigmaRatAttack.ELECTRIC_BALLS -> {
                electricShotDelayTimer.update(delta)
                if (electricShotDelayTimer.isFinished()) {
                    electricShotDelayTimer.reset()

                    val electricBall = electricBalls.removeFirst()

                    var index = if (electricBallsClockwise) electricBalls.size
                    else ANGLES.size - electricBalls.size - 1
                    index = index.coerceIn(0, ANGLES.size - 1)
                    val angle = ANGLES[index]

                    val trajectory = Vector2(0f, ELECTRIC_BALLS_SPEED * ConstVals.PPM).setAngleDeg(angle)
                    electricBall.launch(trajectory)

                    if (index == INDEX_TO_TRY_CLAW_DURING_ELECTRIC_BALLS) {
                        val attack = weightedAttackSelector.getRandomItem()
                        when (attack) {
                            SigmaRatAttack.CLAW_SHOCK -> {
                                val claw = if (megaman.feet.getShape().overlaps(leftClaw!!.body)) leftClaw
                                else if (megaman.feet.getShape().overlaps(rightClaw!!.body)) rightClaw
                                else if (getRandomBool()) leftClaw else rightClaw
                                if (!claw!!.shocking) claw.enterShockState()
                            }

                            SigmaRatAttack.CLAW_LAUNCH -> {
                                val claw = if (megaman.feet.getShape().overlaps(leftClaw!!.body)) rightClaw
                                else if (megaman.feet.getShape().overlaps(rightClaw!!.body)) leftClaw
                                else if (getRandomBool()) leftClaw else rightClaw
                                if (!claw!!.launched) claw.enterLaunchState()
                            }

                            else -> {}
                        }
                    }

                    if (electricBalls.isEmpty) endAttack()
                }
            }

            SigmaRatAttack.FIRE_BLASTS -> {
                fireballDelayTimer.update(delta)
                if (fireballDelayTimer.isFinished()) {
                    val (fireball, angle) = fireballs.removeLast()
                    val trajectory = Vector2(0f, FIREBALL_SPEED * ConstVals.PPM).setAngleDeg(angle)
                    fireball.body.physics.velocity = trajectory
                    fireballDelayTimer.reset()
                }
                if (fireballs.isEmpty) endAttack()
            }

            SigmaRatAttack.CLAW_LAUNCH -> if (leftClaw?.launched != true && rightClaw?.launched != true) endAttack()

            SigmaRatAttack.CLAW_SHOCK -> if (leftClaw?.shocking != true && rightClaw?.shocking != true) endAttack()

            null -> {}
        }
    }

    private fun endAttack() {
        attackState = null
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(7.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        body.color = Color.YELLOW
        debugShapes.add { body }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.85f * ConstVals.PPM))
        damagerFixture.offsetFromBodyCenter.y = 3f * ConstVals.PPM
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.85f * ConstVals.PPM))
        damageableFixture.offsetFromBodyCenter.y = 3f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprite.setSize(10f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink || !ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when {
                damageBlink -> "BodyDamaged" /* when {
                    attackState == SigmaRatHeadAttack.TITTY_LASERS -> "BodyTittyShootDamaged"
                    else -> "BodyDamaged"
                }
                */

                /* attackState == SigmaRatHeadAttack.TITTY_LASERS -> "BodyTittyShoot" */
                else -> "Body"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "Body" to Animation(bodyRegion!!),
            "BodyTittyShoot" to Animation(bodyTittyShootRegion!!),
            "BodyDamaged" to Animation(bodyDamagedRegion!!, 1, 2, 0.1f, true),
            "BodyTittyShootDamaged" to Animation(bodyTittyShootDamagedRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}