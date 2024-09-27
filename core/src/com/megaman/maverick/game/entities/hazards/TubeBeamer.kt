package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.BodyComponentCreator

class TubeBeamer(game: MegamanMaverickGame) : MegaGameEntity(game), IAudioEntity, IBodyEntity, ICullableEntity,
    IDirectionRotatable {

    companion object {
        const val TAG = "TubeBeamer"
        private const val VELOCITY = 10f
        private const val SPAWN_DELAY = 1.25f
        private const val DEFAULT_INITIAL_DELAY = 0f
    }

    override var directionRotation: Direction? = null

    private val spawnTimer = Timer(SPAWN_DELAY)
    private lateinit var initialDelayTimer: Timer

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        addComponent(AudioComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        directionRotation = Direction.valueOf(spawnProps.get(ConstKeys.DIRECTION, String::class)!!.uppercase())
        spawnTimer.reset()
        val initialDelay = spawnProps.getOrDefault(ConstKeys.DELAY, DEFAULT_INITIAL_DELAY, Float::class)
        initialDelayTimer = Timer(initialDelay)
    }

    private fun beamTube() {
        val tubeBeam = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.TUBE_BEAM)!!
        val trajectory = (when (directionRotation!!) {
            Direction.RIGHT -> Vector2(VELOCITY, 0f)
            Direction.LEFT -> Vector2(-VELOCITY, 0f)
            Direction.UP -> Vector2(0f, VELOCITY)
            Direction.DOWN -> Vector2(0f, -VELOCITY)
        }).scl(ConstVals.PPM.toFloat())
        tubeBeam.spawn(
            props(
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.DIRECTION pairTo directionRotation,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.BURST_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        initialDelayTimer.update(delta)
        if (!initialDelayTimer.isFinished()) return@UpdatablesComponent

        spawnTimer.update(delta)
        if (spawnTimer.isFinished()) {
            beamTube()
            spawnTimer.reset()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullableOutOfBounds = getGameCameraCullingLogic(this)
        val cullEvents =
            objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING, EventType.PLAYER_SPAWN)
        val cullableOnEvents = CullableOnEvent({ cullEvents.contains(it) }, cullEvents)
        return CullablesComponent(
            objectMapOf(
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullableOutOfBounds, ConstKeys.CULL_EVENTS pairTo cullableOnEvents
            )
        )
    }
}