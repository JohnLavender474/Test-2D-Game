package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.state.StateMachineBuilder
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.hazards.DeadlyLeaf
import com.megaman.maverick.game.entities.megaman.components.damageableFixture
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.GroundPebble
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.misc.StunType
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs
import kotlin.reflect.KClass

// TODO:
//  - if Timber Woman is within Y distance of ground, do not allow jump spin
class TimberWoman(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IDrawableShapesEntity, IFaceable {

    companion object {
        const val TAG = "TimberWoman"

        private const val LEAF_SPAWN = "leaf_spawn"

        private const val BODY_WIDTH = 1.5f
        private const val BODY_HEIGHT = 1.75f

        private const val RUN_IMPULSE_X = 25f
        private const val MAX_RUN_SPEED = 10f

        private const val VEL_CLAMP_X = 50f
        private const val VEL_CLAMP_Y = 25f

        private const val SPRITE_SIZE = 3.5f

        private const val INIT_DUR = 1f
        private const val STAND_DUR = 1.8f
        private const val MAX_RUN_DUR = 2f
        private const val WALL_SLIDE_DUR = 0.75f
        private const val STAND_SWING_DUR = 1f
        private const val STAND_POUND_DUR = 2f
        private const val MAX_JUMP_SPIN_DUR = 1f

        // When Timber Woman transitions to the stand state, she might be sliding on the floor due to
        // velocity applied in a previous state. When this amount of time has passed in the state state,
        // her X velocity should be set to zero to prevent further floor sliding.
        private const val STAND_STILL_DELAY = 0.1f
        private const val STAND_STILL_DELAY_KEY = "stand_still_delay"

        private const val MAX_CYCLES_WITHOUT_JUMP = 3

        private const val STAND_POUND_CHANCE = 25
        private const val JUMP_CHANCE = 50
        private const val RUN_CHANCE = 65

        private const val STAND_SWING_GROUND_BURST_TIME = 0.35f
        private val STAND_POUND_GROUND_BURST_TIMES = gdxArrayOf(0.35f, 0.95f, 1.55f)

        private val GROUND_PEBBLE_IMPULSES = gdxArrayOf(
            Vector2(-18f, 5f),
            Vector2(-15f, 10f),
            Vector2(-9f, 18f),
            Vector2(-3f, 26f),
            Vector2(0f, 30f),
            Vector2(3f, 26f),
            Vector2(9f, 18f),
            Vector2(15f, 10f),
            Vector2(18f, 5f),
        )
        private const val GROUND_PEBBLES_AXE_SWING_OFFSET_X = 2f
        private const val GROUND_PEBBLES_OFFSET_Y = 0.35f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f
        private const val WALL_SLIDE_GRAVITY = -0.075f

        private const val STAND_FRICTION_X = 7.5f
        private const val RUN_FRICTION_X = 2.75f
        private const val JUMP_FRICTION_X = 1.15f
        private const val DEFAULT_FRICTION_Y = 1f
        private const val WALLSLIDE_FRICTION_Y = 10f

        private const val JUMP_MAX_IMPULSE_X = 10f
        private const val JUMP_IMPULSE_Y = 16f

        private const val WALL_JUMP_IMPULSE_X = 5f

        private const val JUMP_SPIN_RADIUS = 1.5f
        private const val JUMP_SPIN_SCANNER_RADIUS = 2.5f

        private const val ROOM_SHAKE_DUR = 0.5f
        private const val ROOM_SHAKE_INTERVAL = 0.1f
        private const val ROOM_SHAKE_X = 0f
        private const val ROOM_SHAKE_Y = 0.003125f

        private const val AXE_SWING_DAMAGER_WIDTH_1 = 1.25f
        private const val AXE_SWING_DAMAGER_HEIGHT_1 = 2f
        private const val AXE_SWING_DAMAGER_ANIM_INDEX_1 = 2

        private const val AXE_SWING_DAMAGER_WIDTH_2 = 1.75f
        private const val AXE_SWING_DAMAGER_HEIGHT_2 = 0.5f
        private const val AXE_SWING_DAMAGER_ANIM_INDEX_MAX_2 = 5

        private const val AXE_WALLSLIDE_REGION = "axe_wallslide"

        private const val AXE_SWING_REGION_1 = "axe_swing1"
        private const val AXE_SWING_1_INDEX = 2

        private const val AXE_SWING_REGION_2 = "axe_swing2"
        private val AXE_SWING_2_INDICES = objectSetOf(3, 4, 5)

        private const val AXE_POUND_REGION_1 = "axe_pound1"
        private const val AXE_POUND_1_INDEX = 3

        private const val AXE_POUND_REGION_2 = "axe_pound2"
        private const val AXE_POUND_2_INDEX = 4

        private const val LEAVES_PER_POUND = 2

        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = ObjectMap<String, AnimationDef>()
    }

    private enum class TimberWomanState {
        INIT, STAND, STAND_SWING, STAND_POUND, WALLSLIDE, RUN, JUMP_UP, JUMP_DOWN, JUMP_SPIN
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(1),
        Fireball::class pairTo dmgNeg(2),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 2 else 1
        }
    )
    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<TimberWomanState>
    private val currentState: TimberWomanState
        get() = stateMachine.getCurrent()
    private var previousNonStandState: TimberWomanState? = null
    private val stateTimers = OrderedMap<TimberWomanState, Timer>()

    private val otherTimers = OrderedMap<String, Timer>()

    // If Megaman overlaps this circle while Timber Woman is jumping, then she will perform a jump spin.
    private val jumpSpinScannerCircle = GameCircle().setRadius(JUMP_SPIN_SCANNER_RADIUS * ConstVals.PPM)

    // The bounds from which the deadly falling leaves are spawned.
    private val leafSpawnBounds = GameRectangle()

    // When Timber Woman is standing and about to swing her axe, the walls are checked so that the axe does not overlap
    // the walls. When Timber Woman is facing one way and her axe would overlap one of the walls when swung, she is
    // turned to face the opposite direction to prevent the overlap.
    private val walls = Array<GameRectangle>()

    private var cyclesSinceLastJump = 0

    // Used to collect entities that should be destroyed when Timber Woman is destroyed.
    private val entitySet = OrderedSet<MegaGameEntity>()

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)

            val keys = gdxArrayOf(
                ConstKeys.DEFEATED,
                AXE_WALLSLIDE_REGION,
                AXE_SWING_REGION_1,
                AXE_SWING_REGION_2,
                AXE_POUND_REGION_1,
                AXE_POUND_REGION_2
            )
            TimberWomanState.entries.forEach { keys.add(it.name.lowercase()) }

            keys.forEach { key -> regions.put(key, atlas.findRegion("${TAG}/${key}")) }
        }

        if (animDefs.isEmpty) animDefs.putAll(
            TimberWomanState.INIT.name.lowercase() pairTo AnimationDef(7, 1, 0.2f, true), // TODO: replace init def
            TimberWomanState.STAND.name.lowercase() pairTo AnimationDef(7, 1, 0.2f, true),
            TimberWomanState.STAND_SWING.name.lowercase() pairTo AnimationDef(
                2,
                4,
                gdxArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.25f, 0.1f, 0.1f),
                false
            ),
            TimberWomanState.STAND_POUND.name.lowercase() pairTo AnimationDef(3, 2, 0.1f, true),
            TimberWomanState.JUMP_SPIN.name.lowercase() pairTo AnimationDef(4, 2, 0.025f, true),
            TimberWomanState.JUMP_UP.name.lowercase() pairTo AnimationDef(2, 1, 0.1f, true),
            TimberWomanState.JUMP_DOWN.name.lowercase() pairTo AnimationDef(2, 1, 0.1f, true),
            TimberWomanState.RUN.name.lowercase() pairTo AnimationDef(2, 2, 0.1f, true),
            TimberWomanState.WALLSLIDE.name.lowercase() pairTo AnimationDef(),
            ConstKeys.DEFEATED pairTo AnimationDef()
        )

        if (stateTimers.isEmpty) stateTimers.putAll(
            TimberWomanState.INIT pairTo Timer(INIT_DUR),
            TimberWomanState.STAND pairTo Timer(STAND_DUR),
            TimberWomanState.RUN pairTo Timer(MAX_RUN_DUR),
            TimberWomanState.WALLSLIDE pairTo Timer(WALL_SLIDE_DUR),
            TimberWomanState.STAND_SWING pairTo Timer(STAND_SWING_DUR)
                .setRunnables(TimeMarkedRunnable(STAND_SWING_GROUND_BURST_TIME) { groundPound() }),
            TimberWomanState.STAND_POUND pairTo Timer(STAND_POUND_DUR)
                .also { timer ->
                    val runnables = Array<TimeMarkedRunnable>()
                    STAND_POUND_GROUND_BURST_TIMES.forEach { time ->
                        runnables.add(TimeMarkedRunnable(time) { groundPound() })
                    }
                    timer.setRunnables(runnables)
                },
            TimberWomanState.JUMP_SPIN pairTo Timer(MAX_JUMP_SPIN_DUR)
        )

        if (otherTimers.isEmpty) otherTimers.putAll(STAND_STILL_DELAY_KEY pairTo Timer(STAND_STILL_DELAY))

        stateMachine = buildStateMachine()

        super.init()

        addComponent(defineAnimationsComponent())

        jumpSpinScannerCircle.drawingColor = Color.WHITE
        addDebugShapeSupplier { jumpSpinScannerCircle }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        stateMachine.reset()

        stateTimers.values().forEach { it.reset() }
        otherTimers.values().forEach { it.reset() }

        spawnProps.forEach { key, value ->
            when {
                key.toString().contains(LEAF_SPAWN) -> {
                    val bounds = (value as RectangleMapObject).rectangle
                    leafSpawnBounds.set(bounds)
                }

                key.toString().contains(ConstKeys.WALL) -> {
                    val bounds = (value as RectangleMapObject).rectangle.toGameRectangle(false)
                    walls.add(bounds)
                }
            }
        }

        updateFacing()

        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)

        cyclesSinceLastJump = 0
    }

    override fun isReady(delta: Float) = stateTimers[TimberWomanState.INIT].isFinished()

    override fun onReady() {
        super.onReady()
        body.physics.gravityOn = true
    }

    override fun onDestroy() {
        super.onDestroy()

        walls.clear()

        // Destroy all ground pebbles and deadly leaves when Timber Woman is destroyed
        val entitiesToDestroy = MegaGameEntities.getEntitiesOfTags(entitySet, GroundPebble.TAG, DeadlyLeaf.TAG)
        entitiesToDestroy.forEach { entity -> entity.destroy() }
        entitiesToDestroy.clear()
    }

    private fun buildStateMachine(): StateMachine<TimberWomanState> {
        val builder = StateMachineBuilder<TimberWomanState>()
        TimberWomanState.entries.forEach { builder.state(it.name, it) }
        builder.setOnChangeState(this::onChangeState)
        builder.initialState(TimberWomanState.INIT.name)
            // from init state
            .transition(TimberWomanState.INIT.name, TimberWomanState.STAND.name) { ready }

            // from stand state
            .transition(TimberWomanState.STAND.name, TimberWomanState.JUMP_UP.name) {
                when {
                    cyclesSinceLastJump <= 1 -> false
                    cyclesSinceLastJump >= MAX_CYCLES_WITHOUT_JUMP -> true
                    else -> getRandom(0, 100) <= JUMP_CHANCE
                }
            }
            .transition(TimberWomanState.STAND.name, TimberWomanState.STAND_POUND.name) {
                getRandom(0, 100) <= STAND_POUND_CHANCE
            }
            .transition(TimberWomanState.STAND.name, TimberWomanState.RUN.name) standToRun@{
                if (previousNonStandState == TimberWomanState.RUN) return@standToRun false

                if ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                    (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) ||
                    megaman.body.getY() > body.getMaxY()
                ) false

                return@standToRun getRandom(0, 100) <= RUN_CHANCE
            }
            .transition(TimberWomanState.STAND.name, TimberWomanState.STAND_SWING.name) { true }

            // from run state
            .transition(TimberWomanState.RUN.name, TimberWomanState.STAND.name) { true }

            // from stand swing state
            .transition(TimberWomanState.STAND_SWING.name, TimberWomanState.STAND.name) { true }
            .transition(TimberWomanState.STAND_POUND.name, TimberWomanState.STAND.name) { true }

            // from jump up state
            .transition(TimberWomanState.JUMP_UP.name, TimberWomanState.WALLSLIDE.name) { shouldStartWallSliding() }
            .transition(TimberWomanState.JUMP_UP.name, TimberWomanState.JUMP_SPIN.name) { shouldJumpSpin() }
            .transition(TimberWomanState.JUMP_UP.name, TimberWomanState.JUMP_DOWN.name) { true }

            // from jump down state
            .transition(TimberWomanState.JUMP_DOWN.name, TimberWomanState.WALLSLIDE.name) { shouldStartWallSliding() }
            .transition(TimberWomanState.JUMP_DOWN.name, TimberWomanState.JUMP_SPIN.name) { shouldJumpSpin() }
            .transition(TimberWomanState.JUMP_DOWN.name, TimberWomanState.STAND.name) { true }

            // from wall slide state
            .transition(TimberWomanState.WALLSLIDE.name, TimberWomanState.JUMP_UP.name) {
                !body.isSensing(BodySense.FEET_ON_GROUND)
            }
            .transition(TimberWomanState.WALLSLIDE.name, TimberWomanState.STAND.name) { true }

            // from jump spin state
            .transition(TimberWomanState.JUMP_SPIN.name, TimberWomanState.JUMP_UP.name) {
                !body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y > 0f
            }
            .transition(TimberWomanState.JUMP_SPIN.name, TimberWomanState.JUMP_DOWN.name) {
                !body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f
            }
            .transition(TimberWomanState.JUMP_SPIN.name, TimberWomanState.STAND.name) { true }
        return builder.build()
    }

    private fun onChangeState(current: TimberWomanState, previous: TimberWomanState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        stateTimers.values().forEach { it.reset() }
        otherTimers.values().forEach { it.reset() }

        if (previous != TimberWomanState.STAND) previousNonStandState = previous

        when (previous) {
            TimberWomanState.JUMP_DOWN -> {
                GameLogger.debug(TAG, "onChangeState(): apply friction x when previous=$previous")

                body.physics.applyFrictionX = true
            }

            else -> GameLogger.debug(TAG, "onChangeState(): no action when previous=$previous")
        }

        when (current) {
            TimberWomanState.STAND -> {
                val temp = cyclesSinceLastJump
                cyclesSinceLastJump++

                GameLogger.debug(
                    TAG, "onChangeState(): increment cycles since last jump from $temp to " +
                        "$cyclesSinceLastJump when current=$current"
                )
            }

            TimberWomanState.STAND_SWING -> {
                val oldFacing = facing

                val maxSwingX = when (facing) {
                    Facing.LEFT -> body.getX() - AXE_SWING_DAMAGER_WIDTH_2 * ConstVals.PPM
                    Facing.RIGHT -> body.getMaxX() + AXE_SWING_DAMAGER_WIDTH_2 * ConstVals.PPM
                }
                val maxSwingPoint = GameObjectPools.fetch(Vector2::class).set(maxSwingX, body.getCenter().y)
                if (walls.any { it.contains(maxSwingPoint) }) facing = facing.opposite()

                GameLogger.debug(TAG, "onChangeState(): adjust facing from $oldFacing to $facing when current=$current")
            }

            TimberWomanState.JUMP_UP -> {
                body.physics.applyFrictionX = false

                jump(megaman.body.getCenter())
                if (previous == TimberWomanState.WALLSLIDE) {
                    var impulseX = WALL_JUMP_IMPULSE_X * ConstVals.PPM
                    if (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) impulseX *= -1f

                    body.physics.velocity.x = impulseX

                    GameLogger.debug(TAG, "onChangeState(): set impulseX=$impulseX for wall jump")
                }

                cyclesSinceLastJump = 0

                GameLogger.debug(
                    TAG, "onChangeState(): turn off x friction, jump=${body.physics.velocity}, and set cycles since " +
                        "last jump to 0 when current=$current"
                )
            }

            TimberWomanState.JUMP_SPIN -> {
                requestToPlaySound(SoundAsset.BRUSH_SOUND, false)

                GameLogger.debug(TAG, "onChangeState(): play brush sound when current=$current")
            }

            else -> GameLogger.debug(TAG, "onChangeState(): no action when current=$current")
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (betweenReadyAndEndBossSpawnEvent) return@add

            if (defeated) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                explodeOnDefeat(delta)
                return@add
            }

            jumpSpinScannerCircle.setCenter(body.getCenter())

            when (currentState) {
                TimberWomanState.INIT, TimberWomanState.STAND, TimberWomanState.STAND_SWING -> {
                    updateFacing()

                    if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                        // Prevents Timber Woman from excessively sliding when she lands on the ground and still has
                        // x velocity being applied.
                        val standStillDelay = otherTimers[STAND_STILL_DELAY_KEY]
                        standStillDelay.update(delta)
                        if (standStillDelay.isJustFinished()) body.physics.velocity.x = 0f

                        if (updateTimerFor(currentState, delta)) stateMachine.next()
                    }
                }

                TimberWomanState.STAND_POUND ->
                    if (body.isSensing(BodySense.FEET_ON_GROUND) && updateTimerFor(currentState, delta))
                        stateMachine.next()

                TimberWomanState.RUN -> {
                    updateFacing()

                    // When Timber Woman changes facing direction while running, set her velocity to zero
                    if ((isFacing(Facing.LEFT) && body.physics.velocity.x > 0f) ||
                        (isFacing(Facing.RIGHT) && body.physics.velocity.x < 0f)
                    ) body.physics.velocity.x = 0f

                    if (abs(body.physics.velocity.x) < MAX_RUN_SPEED * ConstVals.PPM)
                        body.physics.velocity.x += RUN_IMPULSE_X * ConstVals.PPM * facing.value * delta

                    body.physics.velocity.x = body.physics.velocity.x.coerceIn(
                        -MAX_RUN_SPEED * ConstVals.PPM, MAX_RUN_SPEED * ConstVals.PPM
                    )

                    if (shouldStopRunning()) {
                        stateMachine.next()
                        return@add
                    }

                    if (body.isSensing(BodySense.FEET_ON_GROUND) && updateTimerFor(currentState, delta))
                        stateMachine.next()
                }

                TimberWomanState.WALLSLIDE -> {
                    if (shouldStopWallSliding()) {
                        stateMachine.next()
                        return@add
                    }

                    if (updateTimerFor(currentState, delta)) stateMachine.next()
                }

                TimberWomanState.JUMP_SPIN -> {
                    if (shouldStopJumpSpinning()) {
                        stateMachine.next()
                        return@add
                    }

                    if (updateTimerFor(currentState, delta)) stateMachine.next()
                }

                TimberWomanState.JUMP_UP -> {
                    updateFacing()

                    if (shouldJumpSpin() || shouldStopJumpingUp()) stateMachine.next()
                }

                TimberWomanState.JUMP_DOWN -> {
                    updateFacing()

                    if (shouldJumpSpin() || shouldStopJumpingDown()) stateMachine.next()
                }
            }
        }
    }

    private fun updateTimerFor(state: TimberWomanState, delta: Float): Boolean {
        if (!stateTimers.containsKey(state)) {
            GameLogger.error(TAG, "updateTimerFor(): no timer for state=$state")
            return false
        }

        val timer = stateTimers[state]
        timer.update(delta)

        return timer.isFinished()
    }

    private fun shouldStopJumpingUp() =
        body.physics.velocity.y <= 0f || body.isSensing(BodySense.FEET_ON_GROUND) || shouldStartWallSliding()

    private fun shouldStopJumpingDown() = body.isSensing(BodySense.FEET_ON_GROUND) || shouldStartWallSliding()

    private fun shouldStopJumpSpinning() = shouldStopJumpingDown()

    private fun shouldStartWallSliding() = !body.isSensing(BodySense.FEET_ON_GROUND) &&
        ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
            (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)))

    private fun shouldJumpSpin() = !body.isSensing(BodySense.FEET_ON_GROUND) &&
        !megaman.invincible && jumpSpinScannerCircle.overlaps(megaman.damageableFixture.getShape())

    private fun shouldStopWallSliding() =
        !body.isSensingAny(BodySense.SIDE_TOUCHING_BLOCK_LEFT, BodySense.SIDE_TOUCHING_BLOCK_RIGHT) ||
            body.isSensing(BodySense.FEET_ON_GROUND)

    private fun shouldStopRunning() = body.getBounds().contains(megaman.body.getCenter()) ||
        (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
        (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))

    private fun updateFacing() {
        when {
            megaman.body.getMaxX() < body.getX() -> facing = Facing.LEFT
            megaman.body.getX() > body.getMaxX() -> facing = Facing.RIGHT
        }
    }

    private fun spawnDeadlyLeaf(spawn: Vector2) {
        GameLogger.debug(TAG, "spawnDeadlyLeaf(): spawn=$spawn")

        val deadlyLeaf = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.DEADLY_LEAF)!!
        deadlyLeaf.spawn(props(ConstKeys.POSITION pairTo spawn))
    }

    private fun spawnGroundPebbles() {
        GameLogger.debug(TAG, "spawnGroundPebbles()")

        val spawn = body.getPositionPoint(Position.BOTTOM_CENTER)
            .add(0f, GROUND_PEBBLES_OFFSET_Y * ConstVals.PPM)

        if (currentState == TimberWomanState.STAND_SWING)
            spawn.add(GROUND_PEBBLES_AXE_SWING_OFFSET_X * ConstVals.PPM * facing.value, 0f)

        for (i in 0 until GROUND_PEBBLE_IMPULSES.size) {
            val impulse = GROUND_PEBBLE_IMPULSES[i].cpy().scl(ConstVals.PPM.toFloat())

            val pebble = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.GROUND_PEBBLE)!!
            pebble.spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.IMPULSE pairTo impulse
                )
            )
        }
    }

    private fun groundPound() {
        GameLogger.debug(TAG, "groundPound()")

        spawnGroundPebbles()

        (0 until LEAVES_PER_POUND).forEach {
            val spawn = GameObjectPools.fetch(Vector2::class)
            val randomLeafSpawn = leafSpawnBounds.getRandomPositionInBounds(spawn)
            spawnDeadlyLeaf(randomLeafSpawn)
        }

        game.eventsMan.submitEvent(
            Event(
                EventType.STUN_PLAYER, props(ConstKeys.TYPE pairTo StunType.STUN_BOUNCE_IF_ON_SURFACE)
            )
        )

        game.eventsMan.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.INTERVAL pairTo ROOM_SHAKE_INTERVAL,
                    ConstKeys.DURATION pairTo ROOM_SHAKE_DUR,
                    ConstKeys.X pairTo ROOM_SHAKE_X * ConstVals.PPM,
                    ConstKeys.Y pairTo ROOM_SHAKE_Y * ConstVals.PPM
                )
            )
        )

        requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
    }

    private fun jump(target: Vector2): Vector2 {
        GameLogger.debug(TAG, "jump(): target=$target")

        val impulse = MegaUtilMethods.calculateJumpImpulse(body.getCenter(), target, JUMP_IMPULSE_Y * ConstVals.PPM)
        impulse.x = impulse.x.coerceIn(-JUMP_MAX_IMPULSE_X * ConstVals.PPM, JUMP_MAX_IMPULSE_X * ConstVals.PPM)
        body.physics.velocity.set(impulse.x, impulse.y)

        return impulse
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.velocityClamp.set(VEL_CLAMP_X * ConstVals.PPM, VEL_CLAMP_Y * ConstVals.PPM)
        body.physics.receiveFrictionX = false
        body.physics.receiveFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.2f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.2f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.YELLOW
        debugShapes.add { headFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -BODY_WIDTH * ConstVals.PPM / 2f
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.ORANGE
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = BODY_WIDTH * ConstVals.PPM / 2f
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.ORANGE
        debugShapes.add { rightFixture }

        val standSwingDamagerBounds = GameRectangle()
        val standSwingDamagerFixture = Fixture(body, FixtureType.DAMAGER, standSwingDamagerBounds)
        standSwingDamagerFixture.attachedToBody = false
        body.addFixture(standSwingDamagerFixture)
        standSwingDamagerFixture.drawingColor = Color.RED
        debugShapes.add { if (standSwingDamagerFixture.isActive()) standSwingDamagerFixture else null }

        val jumpSpinDamagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(JUMP_SPIN_RADIUS * ConstVals.PPM))
        body.addFixture(jumpSpinDamagerFixture)
        debugShapes.add { jumpSpinDamagerFixture }

        val jumpSpinShieldFixture =
            Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(JUMP_SPIN_RADIUS * ConstVals.PPM))
        body.addFixture(jumpSpinShieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.defaultFrictionOnSelf.x = when (currentState) {
                TimberWomanState.STAND -> STAND_FRICTION_X
                TimberWomanState.RUN -> RUN_FRICTION_X
                else -> JUMP_FRICTION_X
            }
            body.physics.defaultFrictionOnSelf.y =
                if (currentState == TimberWomanState.WALLSLIDE) WALLSLIDE_FRICTION_Y else DEFAULT_FRICTION_Y

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            body.physics.gravity.y = when {
                currentState == TimberWomanState.WALLSLIDE -> WALL_SLIDE_GRAVITY * ConstVals.PPM
                body.isSensing(BodySense.FEET_ON_GROUND) -> GROUND_GRAVITY * ConstVals.PPM
                else -> GRAVITY * ConstVals.PPM
            }

            val jumpSpinActive = currentState == TimberWomanState.JUMP_SPIN
            jumpSpinDamagerFixture.drawingColor = if (jumpSpinActive) Color.RED else Color.GRAY
            jumpSpinDamagerFixture.setActive(jumpSpinActive)
            jumpSpinShieldFixture.setActive(jumpSpinActive)

            val standSwingDamagerDef = when (currentState) {
                TimberWomanState.STAND_SWING -> {
                    val animIndex = ((animators[TAG] as Animator).currentAnimation as Animation).getIndex()
                    when {
                        animIndex < AXE_SWING_DAMAGER_ANIM_INDEX_1 -> -1
                        animIndex == AXE_SWING_DAMAGER_ANIM_INDEX_1 -> 1
                        animIndex <= AXE_SWING_DAMAGER_ANIM_INDEX_MAX_2 -> 2
                        else -> -1
                    }
                }

                else -> -1
            }

            standSwingDamagerFixture.setActive(standSwingDamagerDef != -1)

            if (standSwingDamagerDef == 1) {
                standSwingDamagerBounds.setSize(
                    AXE_SWING_DAMAGER_WIDTH_1 * ConstVals.PPM,
                    AXE_SWING_DAMAGER_HEIGHT_1 * ConstVals.PPM
                )

                val position = if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
                standSwingDamagerBounds.positionOnPoint(
                    body.getBounds().getPositionPoint(position),
                    position.opposite()
                )
            } else if (standSwingDamagerDef == 2) {
                standSwingDamagerBounds.setSize(
                    AXE_SWING_DAMAGER_WIDTH_2 * ConstVals.PPM,
                    AXE_SWING_DAMAGER_HEIGHT_2 * ConstVals.PPM
                )

                val position = if (isFacing(Facing.LEFT)) Position.BOTTOM_LEFT else Position.CENTER_RIGHT
                standSwingDamagerBounds.positionOnPoint(
                    body.getBounds().getPositionPoint(position),
                    position.flipHorizontally()
                )
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        // main
        .sprite(
            TAG,
            GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)

            val flipX = when (currentState) {
                TimberWomanState.WALLSLIDE -> body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)
                TimberWomanState.STAND_POUND -> false
                else -> isFacing(Facing.LEFT)
            }
            sprite.setFlip(flipX, false)

            sprite.hidden = damageBlink || game.isProperty(ConstKeys.ROOM_TRANSITION, true)
        }

        // axe wallslide
        .sprite(
            AXE_WALLSLIDE_REGION,
            GameSprite(regions[AXE_WALLSLIDE_REGION], DrawingPriority(DrawingSection.PLAYGROUND, 2))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val show = currentState == TimberWomanState.WALLSLIDE
            sprite.hidden = !show

            val anchor = sprites[TAG].boundingRectangle.getPositionPoint(Position.BOTTOM_CENTER)
            sprite.setPosition(anchor, Position.TOP_CENTER)

            sprite.setFlip(isFacing(Facing.LEFT), false)
        }

        // axe swing 1
        .sprite(
            AXE_SWING_REGION_1,
            GameSprite(regions[AXE_SWING_REGION_1], DrawingPriority(DrawingSection.PLAYGROUND, 2))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val show = when {
                currentState != TimberWomanState.STAND_SWING -> false
                else -> {
                    val animKey = TimberWomanState.STAND_SWING.name.lowercase()
                    val animation = (animators[TAG] as Animator).animations[animKey] as Animation
                    val index = animation.getIndex()
                    index == AXE_SWING_1_INDEX
                }
            }
            sprite.hidden = !show

            val position = if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
            val anchor = sprites[TAG].boundingRectangle.getPositionPoint(position)
            sprite.setPosition(anchor, position.opposite())

            sprite.setFlip(isFacing(Facing.LEFT), false)
        }

        // axe swing 2
        .sprite(
            AXE_SWING_REGION_2,
            GameSprite(regions[AXE_SWING_REGION_2], DrawingPriority(DrawingSection.PLAYGROUND, 2))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val show = when {
                currentState != TimberWomanState.STAND_SWING -> false
                else -> {
                    val animKey = TimberWomanState.STAND_SWING.name.lowercase()
                    val animation = (animators[TAG] as Animator).animations[animKey] as Animation
                    val index = animation.getIndex()
                    AXE_SWING_2_INDICES.contains(index)
                }
            }
            sprite.hidden = !show

            val position = if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
            val anchor = sprites[TAG].boundingRectangle.getPositionPoint(position)
            sprite.setPosition(anchor, position.opposite())

            sprite.setFlip(isFacing(Facing.LEFT), false)
        }

        // axe pound 1
        .sprite(
            AXE_POUND_REGION_1,
            GameSprite(regions[AXE_POUND_REGION_1], DrawingPriority(DrawingSection.PLAYGROUND, 10))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val show = when {
                currentState != TimberWomanState.STAND_POUND -> false
                else -> {
                    val animation = (animators[TAG] as Animator).currentAnimation as Animation
                    val index = animation.getIndex()
                    index == AXE_POUND_1_INDEX
                }
            }

            sprite.hidden = !show

            if (show) {
                val position = Position.BOTTOM_CENTER
                val anchor = sprites[TAG].boundingRectangle.getPositionPoint(position)
                sprite.setPosition(anchor, position)
            }
        }

        // axe pound 2
        .sprite(
            AXE_POUND_REGION_2,
            GameSprite(regions[AXE_POUND_REGION_2], DrawingPriority(DrawingSection.PLAYGROUND, 10))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val show = when {
                currentState != TimberWomanState.STAND_POUND -> false
                else -> {
                    val animation = (animators[TAG] as Animator).currentAnimation as Animation
                    val index = animation.getIndex()
                    index == AXE_POUND_2_INDEX
                }
            }

            sprite.hidden = !show

            if (show) {
                val position = Position.BOTTOM_CENTER
                val anchor = sprites[TAG].boundingRectangle.getPositionPoint(position)
                sprite.setPosition(anchor, position)
            }
        }

        // build
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        defeated -> ConstKeys.DEFEATED
                        else -> when (currentState) {
                            TimberWomanState.INIT -> when {
                                body.isSensing(BodySense.FEET_ON_GROUND) -> TimberWomanState.INIT.name.lowercase()
                                else -> TimberWomanState.JUMP_DOWN.name.lowercase()
                            }

                            else -> currentState.name.lowercase()
                        }
                    }
                }
                .applyToAnimations { animations ->
                    val keys = gdxArrayOf(ConstKeys.DEFEATED)
                    TimberWomanState.entries.forEach { state -> keys.add(state.name.lowercase()) }
                    keys.forEach { key ->
                        val def = animDefs[key]
                        GameLogger.debug(TAG, "defineAnimationsComponent(): putting animation: key=$key, def=$def")
                        animations.put(key, Animation(regions[key], def.rows, def.cols, def.durations, def.loop))
                    }
                }
                .setOnChangeKeyListener { currentKey, nextKey ->
                    GameLogger.debug(
                        TAG,
                        "defineAnimationsComponent(): on change key listener: currentKey=$currentKey, nextKey=$nextKey"
                    )
                }
                .build()
        )
        .build()

    override fun getTag() = TAG
}
