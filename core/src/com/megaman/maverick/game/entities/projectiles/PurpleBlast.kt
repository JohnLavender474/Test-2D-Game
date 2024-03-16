package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
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
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class PurpleBlast(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity, IFaceable {

    companion object {
        const val TAG = "PurpleBlast"
        private const val CHARGE_DELAY = 0.175f
        private var chargeRegion: TextureRegion? = null
        private var blastRegion: TextureRegion? = null
    }

    override var owner: IGameEntity? = null
    override lateinit var facing: Facing

    private val chargeDelayTimer = Timer(CHARGE_DELAY)

    override fun init() {
        if (chargeRegion == null || blastRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            chargeRegion = atlas.findRegion("PurpleBlast/Charge")
            blastRegion = atlas.findRegion("PurpleBlast/Blast")
        }
        addComponents(defineProjectileComponents())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        body.physics.velocity.setZero()
        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity = trajectory
        chargeDelayTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        chargeDelayTimer.update(delta)
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val damagerFixture = Fixture(GameRectangle().setSize(0.5f * ConstVals.PPM), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)
        damagerFixture.shape.color = Color.RED
        debugShapes.add { damagerFixture.shape }

        val projectileFixture = Fixture(GameRectangle().setSize(0.5f * ConstVals.PPM), FixtureType.PROJECTILE)
        body.addFixture(projectileFixture)
        projectileFixture.shape.color = Color.BLUE
        debugShapes.add { projectileFixture.shape }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            val angle = body.physics.velocity.angleDeg()
            _sprite.setOriginCenter()
            _sprite.setRotation(angle)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (!chargeDelayTimer.isFinished()) "charge" else "blast" }
        val animations = objectMapOf<String, IAnimation>(
            "charge" to Animation(chargeRegion!!, 1, 7, 0.025f, false),
            "blast" to Animation(blastRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}