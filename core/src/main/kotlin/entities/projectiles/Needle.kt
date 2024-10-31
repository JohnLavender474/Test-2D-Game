package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class Needle(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "Needle"
        private var region: TextureRegion? = null
    }

    lateinit var damagerFixture: Fixture

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val bodyPosition =
            spawnProps.getOrDefault("${ConstKeys.BODY}_${ConstKeys.POSITION}", Position.CENTER, Position::class)
        body.positionOnPoint(spawn, bodyPosition)

        val gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, 0f, Float::class)
        body.physics.gravity = Vector2(0f, gravity)

        val impulse = spawnProps.getOrDefault(ConstKeys.IMPULSE, Vector2(), Vector2::class)
        body.physics.velocity.set(impulse)

        val damagerActive = spawnProps.getOrDefault("${ConstKeys.DAMAGER}_${ConstKeys.ACTIVE}", true, Boolean::class)
        damagerFixture.active = damagerActive
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
        val flash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.MUZZLE_FLASH)!!
        flash.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.25f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        projectileFixture.rawShape.color = Color.RED
        debugShapes.add { projectileFixture.getShape() }

        damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.25f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(0.25f * ConstVals.PPM))
        body.addFixture(shieldFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!)
        sprite.setSize(0.75f * ConstVals.PPM)
        sprite.setOriginCenter()
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.rotation = body.physics.velocity.angleDeg() + 270f
        }
        return spritesComponent
    }
}
