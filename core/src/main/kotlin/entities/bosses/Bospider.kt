package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.PolylineMapObject
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toProps
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.reflect.KClass

class Bospider(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IParentEntity {

    companion object {
        const val TAG = "Bospider"
        private const val INIT_DUR = 5f
        private const val SPAWN_DELAY = 2f
        private const val MAX_CHILDREN = 4
        private const val MIN_SPEED = 7f
        private const val MAX_SPEED = 14f
        private const val START_POINT_OFFSET = 2f
        private const val OPEN_EYE_MAX_DURATION = 2f
        private const val OPEN_EYE_MIN_DURATION = 0.75f
        private const val CLOSE_EYE_DURATION = 0.35f
        private const val DEBUG_TIMER = 1f
        private const val WEB_SPEED = 10f
        private const val ANGLE_X = 25f
        private var climbRegion: TextureRegion? = null
        private var stillRegion: TextureRegion? = null
        private var openEyeRegion: TextureRegion? = null
    }

    private enum class BospiderState { SPAWN, CLIMB, OPEN_EYE, CLOSE_EYE, RETREAT }

    // set damage negotiations dynamically when spawning
    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    override var children = Array<IGameEntity>()

    private val paths = Array<Array<Vector2>>()
    private val currentPath = Queue<Vector2>()

    private val stateLoop = Loop(BospiderState.entries.toTypedArray().toGdxArray())

    private val childrenSpawnPoints = Array<RectangleMapObject>()

    private val spawnDelayTimer = Timer(SPAWN_DELAY)
    private val closeEyeTimer = Timer(CLOSE_EYE_DURATION)
    private val debugTimer = Timer(DEBUG_TIMER)
    private val initTimer = Timer(INIT_DUR)
    private val openEyeTimer = Timer()

    private val spawn = Vector2()
    private var firstSpawn = true

    override fun init() {
        if (climbRegion == null || stillRegion == null || openEyeRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            climbRegion = atlas.findRegion("$TAG/Climb")
            stillRegion = atlas.findRegion("$TAG/Still")
            openEyeRegion = atlas.findRegion("$TAG/OpenEye")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        spawn.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter())
        body.setCenter(spawn.x, spawn.y + START_POINT_OFFSET * ConstVals.PPM)

        paths.clear()
        childrenSpawnPoints.clear()
        spawnProps.forEach { _, value ->
            if (value is PolylineMapObject) {
                val rawPath = value.polyline.transformedVertices
                val path = Array<Vector2>()
                for (i in rawPath.indices step 2) path.add(Vector2(rawPath[i], rawPath[i + 1]))
                paths.add(path)
            } else if (value is RectangleMapObject) childrenSpawnPoints.add(value)
        }

        stateLoop.reset()
        spawnDelayTimer.reset()

        firstSpawn = true

        when {
            mini -> {
                initTimer.setToEnd()

                damageNegotiations.putAll(
                    objectMapOf(
                        Bullet::class pairTo dmgNeg(2),
                        Fireball::class pairTo dmgNeg(5),
                        ChargedShot::class pairTo dmgNeg {
                            it as ChargedShot
                            if (it.fullyCharged) 3 else 2
                        },
                        ChargedShotExplosion::class pairTo dmgNeg {
                            it as ChargedShotExplosion
                            if (it.fullyCharged) 2 else 1
                        })
                )
            }

            else -> {
                initTimer.reset()

                damageNegotiations.putAll(
                    objectMapOf(
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
                )
            }
        }
    }

    override fun isReady(delta: Float) = mini || initTimer.isFinished()

    override fun onReady() {
        super.onReady()
        if (!mini) game.audioMan.playMusic(MusicAsset.MM7_FINAL_BOSS_LOOP_MUSIC, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        damageNegotiations.clear()
        children.forEach { (it as GameEntity).destroy() }
        children.clear()
        paths.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            initTimer.update(delta)
            if (!initTimer.isFinished()) return@add

            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            debugTimer.update(delta)
            if (debugTimer.isFinished()) {
                GameLogger.debug(TAG, "defineUpdatablesComponent(): state=${stateLoop.getCurrent()}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): position=${body.getCenter()}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): velocity=${body.physics.velocity}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): health=${getCurrentHealth()}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): healthRatio=${getHealthRatio()}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): speed=${getCurrentSpeed()}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): currentPath=$currentPath")
                debugTimer.reset()
            }

            val childIter = children.iterator()
            while (childIter.hasNext()) {
                val child = childIter.next() as MegaGameEntity
                if (child.dead) childIter.remove()
            }

            when (stateLoop.getCurrent()) {
                BospiderState.SPAWN -> {
                    if (!body.getCenter().epsilonEquals(spawn, 0.1f * ConstVals.PPM)) {
                        moveToSpawn()
                        return@add
                    }

                    body.physics.velocity.setZero()

                    if (!firstSpawn && spawnDelayTimer.isAtBeginning()) {
                        val shootWeb = MathUtils.random.nextBoolean()
                        if (shootWeb) shootWebs()
                    }

                    spawnDelayTimer.update(delta)
                    if (spawnDelayTimer.isFinished()) {
                        val numChildrenToSpawn = MAX_CHILDREN - children.size

                        for (i in 0 until numChildrenToSpawn) {
                            val spawnRectObject = childrenSpawnPoints.get(i)
                            val spawnProps = spawnRectObject.properties.toProps()
                            spawnProps.put(ConstKeys.BOUNDS, spawnRectObject.rectangle.toGameRectangle())

                            val babySpider = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.BABY_SPIDER)!!
                            babySpider.spawn(spawnProps)

                            children.add(babySpider)
                        }

                        val path = paths.random()
                        currentPath.clear()
                        path.forEach { currentPath.addLast(it) }

                        firstSpawn = false

                        stateLoop.next()
                    }
                }

                BospiderState.CLIMB -> {
                    if (currentPath.isEmpty) {
                        body.physics.velocity.setZero()

                        val openEyeDuration =
                            OPEN_EYE_MIN_DURATION + (OPEN_EYE_MAX_DURATION - OPEN_EYE_MIN_DURATION) * getHealthRatio()
                        openEyeTimer.resetDuration(openEyeDuration)

                        stateLoop.next()

                        return@add
                    }

                    moveToNextTarget()

                    if (body.getCenter().epsilonEquals(currentPath.first(), 0.1f * ConstVals.PPM))
                        currentPath.removeFirst()
                }

                BospiderState.OPEN_EYE -> {
                    openEyeTimer.update(delta)
                    if (openEyeTimer.isFinished()) {
                        stateLoop.next()
                        closeEyeTimer.reset()
                    }
                }

                BospiderState.CLOSE_EYE -> {
                    closeEyeTimer.update(delta)
                    if (closeEyeTimer.isFinished()) stateLoop.next()
                }

                BospiderState.RETREAT -> {
                    body.physics.velocity.set(0f, getCurrentSpeed())
                    if (body.getY() >= spawn.y + START_POINT_OFFSET * ConstVals.PPM) {
                        body.physics.velocity.setZero()
                        body.setCenter(spawn.x, spawn.y + START_POINT_OFFSET * ConstVals.PPM)

                        spawnDelayTimer.reset()

                        stateLoop.next()
                    }
                }
            }
        }
    }

    override fun triggerDefeat() {
        super.triggerDefeat()
        children.forEach { (it as GameEntity).destroy() }
        children.clear()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(2f * ConstVals.PPM))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(2f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(2.25f * ConstVals.PPM))
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val shielded =
                stateLoop.getCurrent().equalsAny(BospiderState.SPAWN, BospiderState.CLIMB, BospiderState.RETREAT)
            shieldFixture.setActive(shielded)
            damageableFixture.setActive(!shielded)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }


    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.hidden = damageBlink || !initTimer.isFinished()
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (stateLoop.getCurrent()) {
                BospiderState.SPAWN -> "still"
                BospiderState.CLIMB -> "climb"
                BospiderState.OPEN_EYE -> "open_eye"
                BospiderState.CLOSE_EYE -> "close_eye"
                BospiderState.RETREAT -> "climb"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "still" pairTo Animation(stillRegion!!),
            "climb" pairTo Animation(climbRegion!!, 1, 5, 0.1f, true),
            "open_eye" pairTo Animation(openEyeRegion!!, 1, 4, 0.1f, false),
            "close_eye" pairTo Animation(openEyeRegion!!, 1, 4, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun moveToSpawn() {
        val velocity = GameObjectPools.fetch(Vector2::class)
            .set(spawn)
            .sub(body.getCenter())
            .nor()
            .scl(MIN_SPEED * ConstVals.PPM)
        body.physics.velocity.set(velocity)
    }

    private fun moveToNextTarget() {
        val velocity = GameObjectPools.fetch(Vector2::class)
            .set(currentPath.first())
            .sub(body.getCenter())
            .nor()
            .scl(getCurrentSpeed())
        body.physics.velocity.set(velocity)
    }

    private fun getCurrentSpeed() = ConstVals.PPM * (MIN_SPEED + (MAX_SPEED - MIN_SPEED) * (1 - getHealthRatio()))

    private fun shootWebs() {
        val centerTrajectory = megaman.body.getCenter().sub(body.getCenter()).nor()
        shootWeb(centerTrajectory)

        val leftTrajectory = centerTrajectory.cpy().rotateDeg(-ANGLE_X)
        shootWeb(leftTrajectory)

        val rightTrajectory = centerTrajectory.cpy().rotateDeg(ANGLE_X)
        shootWeb(rightTrajectory)

        requestToPlaySound(SoundAsset.SPLASH_SOUND, false)
    }

    private fun shootWeb(trajectory: Vector2) {
        // TODO: Web shooting works, but has been removed from this boss's attack set.
        //   Uncomment this code to add web shooting back to her attack set (yes, Bospider is a she).
        /*
        val scaledTrajectory = trajectory.cpy().scl(WEB_SPEED * ConstVals.PPM)

        val web = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SPIDER_WEB)!!
        val props = props(
            ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER),
            ConstKeys.TRAJECTORY pairTo scaledTrajectory,
            ConstKeys.OWNER pairTo this
        )
        web.spawn(props)
         */
    }
}
