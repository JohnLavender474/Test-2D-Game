package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IMotionEntity
import com.engine.motion.MotionComponent
import com.engine.motion.MotionComponent.MotionDefinition
import com.engine.motion.SineWave
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class Adamski(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IMotionEntity {

    companion object {
        const val TAG = "Adamski"
        private const val SPEED = 3f
        private const val FREQUENCY = 3f
        private const val AMPLITUDE = 0.025f
        private var purpleRegion: TextureRegion? = null
        private var blueRegion: TextureRegion? = null
        private var orangeRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, Int>(

    )

    private var type = 0

    override fun init() {
        if (purpleRegion == null || blueRegion == null || orangeRegion == null) {
            purpleRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Adamski/Purple")
            blueRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Adamski/Blue")
            orangeRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Adamski/Orange")
        }
        super<AbstractEnemy>.init()
        addComponent(MotionComponent(this))
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawning Adamski with props = $spawnProps")
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, 0, Int::class)

        val left = spawnProps.getOrDefault(ConstKeys.LEFT, megaman.body.x <= body.x, Boolean::class)
        val motion = SineWave(
            body.getCenter(), (if (left) -SPEED else SPEED) * ConstVals.PPM, AMPLITUDE * ConstVals.PPM, FREQUENCY
        )
        putMotionDefinition("sineWave", MotionDefinition(motion, { position, _ ->
            body.setCenter(position)
        }))
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        GameLogger.debug(TAG, "Adamski destroyed at position = ${body.getCenter()}")
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.65f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(GameRectangle().set(body), FixtureType.BODY)
        body.addFixture(bodyFixture)
        bodyFixture.shape.color = Color.GRAY
        debugShapes.add { bodyFixture.shape }

        val damagerFixture = Fixture(GameRectangle().set(body), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)
        damagerFixture.shape.color = Color.RED
        debugShapes.add { damagerFixture.shape }

        val damageableFixture = Fixture(GameRectangle().set(body), FixtureType.DAMAGEABLE)
        body.addFixture(damageableFixture)
        damageableFixture.shape.color = Color.PURPLE
        debugShapes.add { damageableFixture.shape }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, "adamski" to sprite)
        spritesComponent.putUpdateFunction("adamski") { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { "$type" }
        val animations = objectMapOf<String, IAnimation>(
            "0" to Animation(purpleRegion!!, 1, 2, 0.1f, true),
            "1" to Animation(blueRegion!!, 1, 2, 0.1f, true),
            "2" to Animation(orangeRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}