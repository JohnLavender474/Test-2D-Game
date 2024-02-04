package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IChildEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class ElectrocutieChild(game: MegamanMaverickGame) : GameEntity(game), IHazard, IBodyEntity, ISpriteEntity,
    IAnimatedEntity, IChildEntity {

    enum class ElectrocutieState {
        MOVE, CHARGE, SHOCK
    }

    companion object {
        const val TAG = "Electrocutie"
        private const val MOVE_DURATION = 1f
        private const val CHARGE_DURATION = 0.75f
        private const val SHOCK_DURATION = 0.5f
        private var moveRegion: TextureRegion? = null
        private var chargeRegion: TextureRegion? = null
        private var shockRegion: TextureRegion? = null
    }

    override var parent: IGameEntity? = null

    private val loop = Loop(ElectrocutieState.values().toGdxArray())
    private val timers = objectMapOf(
        ElectrocutieState.MOVE to Timer(MOVE_DURATION),
        ElectrocutieState.CHARGE to Timer(CHARGE_DURATION),
        ElectrocutieState.SHOCK to Timer(SHOCK_DURATION)
    )

    private lateinit var direction: Direction

    override fun init() {
        if (moveRegion == null || chargeRegion == null || shockRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            moveRegion = atlas.findRegion("Electrocutie/Move")
            chargeRegion = atlas.findRegion("Electrocutie/Charge")
            shockRegion = atlas.findRegion("Electrocutie/Shock")
        }
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        loop.reset()
        timers.values().forEach { it.reset() }

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        direction = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!
        when (direction) {
            Direction.UP -> body.setBottomCenterToPoint(spawn)
            Direction.DOWN -> body.setTopCenterToPoint(spawn)
            Direction.LEFT -> body.setCenterRightToPoint(spawn)
            Direction.RIGHT -> body.setCenterLeftToPoint(spawn)
        }

        parent = spawnProps.get(ConstKeys.ENTITY) as IGameEntity?
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.85f * ConstVals.PPM, 0.35f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        // body fixture
        val bodyFixture = Fixture(GameRectangle().setSize(body.width, body.height), FixtureType.BODY)
        body.addFixture(bodyFixture)
        bodyFixture.shape.color = Color.GRAY
        debugShapes.add { bodyFixture.shape }

        // damager fixture
        val damagerFixture = Fixture(GameRectangle().setSize(body.width, body.height), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)
        damagerFixture.shape.color = Color.RED
        debugShapes.add { damagerFixture.shape }

        body.preProcess.put(ConstKeys.DEFAULT) {

        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, "electrocutie" to sprite)
        spritesComponent.putUpdateFunction("electrocutie") { _, _sprite ->
            _sprite as GameSprite

            _sprite.setOriginCenter()
            _sprite.rotation = direction.rotation

            val position = when (direction) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setCenter(bodyPosition.x, bodyPosition.y)

            val offset = when (direction) {
                Direction.UP -> Vector2(0f, 0.15f * ConstVals.PPM)
                Direction.DOWN -> Vector2(0f, -0.15f * ConstVals.PPM)
                Direction.LEFT -> Vector2(-0.15f * ConstVals.PPM, 0f)
                Direction.RIGHT -> Vector2(0.15f * ConstVals.PPM, 0f)
            }
            _sprite.translate(offset.x, offset.y)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { loop.getCurrent().name }
        val animations = objectMapOf<String, IAnimation>(
            ElectrocutieState.MOVE.name to Animation(moveRegion!!),
            ElectrocutieState.CHARGE.name to Animation(chargeRegion!!, 1, 2, 0.1f, true),
            ElectrocutieState.SHOCK.name to Animation(shockRegion!!)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}