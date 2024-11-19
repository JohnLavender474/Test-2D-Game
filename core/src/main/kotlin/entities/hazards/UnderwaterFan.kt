package com.megaman.maverick.game.entities.hazards


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class UnderwaterFan(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IDamager, IHazard, IDirectionRotatable {

    companion object {
        const val TAG = "UnderwaterFan"
        private var region: TextureRegion? = null
    }

    override var directionRotation: Direction
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, TAG)
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        directionRotation =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())
        val position = DirectionPositionMapper.getPosition(directionRotation)
        val spawnBounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.positionOnPoint(spawnBounds.getPositionPoint(position), position)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.875f * ConstVals.PPM, 0.875f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val shieldFixture1 =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(1.875f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        shieldFixture1.offsetFromBodyCenter.y = 0.25f * ConstVals.PPM
        body.addFixture(shieldFixture1)
        shieldFixture1.rawShape.color = Color.BLUE
        debugShapes.add { shieldFixture1.getShape() }

        val shieldFixture2 = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.5f * ConstVals.PPM))
        shieldFixture2.offsetFromBodyCenter.y = -0.25f * ConstVals.PPM
        body.addFixture(shieldFixture2)
        shieldFixture2.rawShape.color = Color.GREEN
        debugShapes.add { shieldFixture2.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.5f * ConstVals.PPM, 0.875f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation.rotation
            val position = DirectionPositionMapper.getInvertedPosition(directionRotation)
            _sprite.setPosition(body.getPositionPoint(position), position)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 2, 0.075f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
