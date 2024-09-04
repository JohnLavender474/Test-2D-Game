package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.MotionComponent.MotionDefinition
import com.mega.game.engine.motion.Pendulum
import com.mega.game.engine.motion.RotatingLine
import com.mega.game.engine.motion.Trajectory
import com.mega.game.engine.world.Body
import com.mega.game.engine.world.BodyComponent
import com.mega.game.engine.world.BodyType
import com.mega.game.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Saw(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IMotionEntity {

    companion object {
        const val PENDULUM_TYPE = "P"
        const val ROTATION_TYPE = "R"
        const val TRAJECTORY_TYPE = "T"

        private var region: TextureRegion? = null

        private const val LENGTH = 3f
        private const val ROTATION_SPEED = 2f
        private const val PENDULUM_GRAVITY = 10f
    }

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, "Saw")

        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(MotionComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        clearMotionDefinitions()

        val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
        val type = spawnProps.get(ConstKeys.TYPE) as String

        when (type.uppercase()) {
            PENDULUM_TYPE -> setToPendulum(bounds)
            ROTATION_TYPE -> setToRotation(bounds, spawnProps)
            TRAJECTORY_TYPE -> {
                val trajectory = spawnProps.get(ConstKeys.TRAJECTORY) as String
                setToTrajectory(bounds, trajectory)
            }
        }
    }

    private fun setToPendulum(bounds: GameRectangle) {
        val pendulum =
            Pendulum(
                LENGTH * ConstVals.PPM, PENDULUM_GRAVITY * ConstVals.PPM, bounds.getCenter(), 1 / 60f
            )
        putMotionDefinition(
            ConstKeys.PENDULUM,
            MotionDefinition(motion = pendulum, function = { value, _ ->
                body.setCenter(value)
            })
        )

        val shapes = Array<() -> IDrawableShape?>()

        shapes.add {
            val line = GameLine(pendulum.anchor, body.getCenter())
            line.color = Color.DARK_GRAY
            line.shapeType = ShapeRenderer.ShapeType.Filled
            line.thickness = ConstVals.PPM / 8f
            line
        }

        val circle1 = GameCircle()
        circle1.setRadius(ConstVals.PPM / 8f)
        circle1.shapeType = ShapeRenderer.ShapeType.Filled
        circle1.color = Color.DARK_GRAY
        shapes.add { circle1.setCenter(pendulum.anchor) }

        val circle2 = GameCircle()
        circle2.setRadius(ConstVals.PPM / 4f)
        circle2.shapeType = ShapeRenderer.ShapeType.Filled
        circle2.color = Color.DARK_GRAY
        shapes.add { circle2.setCenter(body.getCenter()) }

        addComponent(DrawableShapesComponent(shapes))
    }

    private fun setToRotation(bounds: GameRectangle, spawnProps: Properties) {
        val startRotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)
        val rotation =
            RotatingLine(
                bounds.getCenter(),
                LENGTH * ConstVals.PPM,
                ROTATION_SPEED * ConstVals.PPM,
                startRotation
            )
        putMotionDefinition(
            ConstKeys.ROTATION,
            MotionDefinition(motion = rotation, function = { value, _ ->
                body.setCenter(value)
            })
        )

        val shapes = Array<() -> IDrawableShape?>()

        shapes.add {
            val line = GameLine(rotation.getOrigin(), body.getCenter())
            line.color = Color.DARK_GRAY
            line.shapeType = ShapeRenderer.ShapeType.Filled
            line.thickness = ConstVals.PPM / 8f
            line
        }

        val circle1 = GameCircle()
        circle1.setRadius(ConstVals.PPM / 8f)
        circle1.color = Color.DARK_GRAY
        circle1.shapeType = ShapeRenderer.ShapeType.Filled
        shapes.add { circle1.setCenter(rotation.getOrigin()) }

        val circle2 = GameCircle()
        circle2.setRadius(ConstVals.PPM / 4f)
        circle2.color = Color.DARK_GRAY
        circle2.shapeType = ShapeRenderer.ShapeType.Filled
        shapes.add { circle2.setCenter(body.getCenter()) }

        addComponent(DrawableShapesComponent(shapes))
    }

    private fun setToTrajectory(bounds: GameRectangle, trajectoryDefinition: String) {
        val spawn = bounds.getCenter()
        body.setCenter(spawn)

        val trajectory = Trajectory(trajectoryDefinition, ConstVals.PPM)
        putMotionDefinition(
            ConstKeys.TRAJECTORY,
            MotionDefinition(
                motion = trajectory,
                function = { value, _ -> body.setCenter(value) },
                onReset = { body.setCenter(spawn) })
        )
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)

        val deathCircle = GameCircle()
        deathCircle.setRadius(ConstVals.PPM.toFloat())
        val deathFixture = Fixture(body, FixtureType.DEATH, deathCircle)
        body.addFixture(deathFixture)

        val shieldCircle = GameCircle()
        shieldCircle.setRadius(ConstVals.PPM.toFloat())
        val shieldFixture = Fixture(body, FixtureType.SHIELD, shieldCircle)
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getCenter(), Position.CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 2, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
