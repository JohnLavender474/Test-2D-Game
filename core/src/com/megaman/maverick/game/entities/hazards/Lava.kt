package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimator
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.UpdateFunction
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Matrix
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpritesEntity
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
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.utils.overlapsGameCamera
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Lava(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IAudioEntity, IDirectionRotatable, IFaceable {

    companion object {
        const val TAG = "Lava"
        const val FLOW = "Flow"
        const val FALL = "Fall"
        const val MOVE_BEFORE_KILL = "move_before_kill"
        private const val SPEED = 10f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var directionRotation: Direction
    override lateinit var facing: Facing

    private val moving: Boolean
        get() = !body.getCenter().epsilonEquals(moveTarget, 0.1f * ConstVals.PPM)

    private lateinit var drawingSection: DrawingSection
    private lateinit var type: String
    private lateinit var moveTarget: Vector2
    private lateinit var bodyMatrix: Matrix<GameRectangle>

    private var spritePriorityValue = 0

    private var moveBeforeKill = false
    private var movingBeforeKill = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            regions.put(FLOW, atlas.findRegion("Lava/${FLOW}"))
            regions.put(FALL, atlas.findRegion("Lava/${FALL}"))
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(SpritesComponent(this))
        addComponent(AnimationsComponent(this))
        addComponent(AudioComponent(this))
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn props = $spawnProps")
        super.spawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, FLOW, String::class)

        drawingSection = DrawingSection.valueOf(
            spawnProps.getOrDefault(ConstKeys.SECTION, "foreground", String::class).uppercase()
        )

        directionRotation =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())
        facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, "right", String::class).uppercase())
        spritePriorityValue = spawnProps.getOrDefault(ConstKeys.PRIORITY, if (type == FALL) 1 else 0, Int::class)

        val lavaStartX = spawnProps.getOrDefault("${ConstKeys.MOVE}_${ConstKeys.X}", 0f, Float::class)
        val lavaStartY = spawnProps.getOrDefault("${ConstKeys.MOVE}_${ConstKeys.Y}", 0f, Float::class)
        moveTarget = body.getCenter().add(lavaStartX * ConstVals.PPM, lavaStartY * ConstVals.PPM)

        val dimensions = bounds.getSplitDimensions(ConstVals.PPM.toFloat())
        defineDrawables(dimensions.first, dimensions.second)

        moveBeforeKill = spawnProps.containsKey(MOVE_BEFORE_KILL)
        movingBeforeKill = false

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ATOMIC_FIRE_SOUND, false)
    }

    override fun kill(props: Properties?) =
        if (moveBeforeKill && !movingBeforeKill) {
            GameLogger.debug(TAG, "Moving to target before killing lava.")
            moveBeforeKill()
        } else {
            GameLogger.debug(TAG, "Killing lava now.")
            super<GameEntity>.kill(props)
        }

    private fun moveBeforeKill() {
        GameLogger.debug(TAG, "Moving before killing lava.")
        movingBeforeKill = true
        val moveBeforeKillTargetRaw = getProperty(MOVE_BEFORE_KILL, String::class)!!
            .split(",").map { it.toFloat() }
        val targetOffset = Vector2(moveBeforeKillTargetRaw[0], moveBeforeKillTargetRaw[1]).scl(ConstVals.PPM.toFloat())
        moveTarget.add(targetOffset)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, {
        bodyMatrix = body.splitByCellSize(ConstVals.PPM.toFloat())
        if (movingBeforeKill && !moving) kill()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.color = Color.BLUE

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val deathFixture = Fixture(body, FixtureType.DEATH, GameRectangle())
        deathFixture.putProperty(ConstKeys.INSTANT, true)
        body.addFixture(deathFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (moving) {
                GameLogger.debug(TAG, "Moving to target: $moveTarget. Current pos: ${body.getCenter()}")
                val direction = moveTarget.cpy().sub(body.getCenter()).nor()
                body.physics.velocity.set(direction.scl(SPEED * ConstVals.PPM))
                deathFixture.active = false
            } else {
                body.physics.velocity.set(0f, 0f)
                deathFixture.active = true
            }

            (deathFixture.rawShape as GameRectangle).set(body)
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineDrawables(rows: Int, cols: Int) {
        val sprites = OrderedMap<String, GameSprite>()
        val updateFunctions = ObjectMap<String, UpdateFunction<GameSprite>>()
        val animators = Array<Pair<() -> GameSprite, IAnimator>>()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val sprite = GameSprite()
                sprite.setSize(ConstVals.PPM.toFloat())

                val key = "lava_$${col}_${row}"
                sprites.put(key, sprite)

                updateFunctions.put(key, UpdateFunction { _, _sprite ->
                    val bounds = bodyMatrix[col, row]!!
                    _sprite.setCenter(bounds.getCenter())
                    _sprite.setOriginCenter()
                    _sprite.rotation = directionRotation.rotation
                    _sprite.setFlip(isFacing(Facing.LEFT), false)
                    _sprite.priority.section = drawingSection
                    _sprite.priority.value = spritePriorityValue
                })

                val region = regions.get(type)
                val animation = Animation(region!!, 1, 3, 0.1f, true)
                val animator = Animator(animation)
                animators.add({ sprite } to animator)
            }
        }

        addComponent(SpritesComponent(this, sprites, updateFunctions))
        addComponent(AnimationsComponent(this, animators))
    }
}