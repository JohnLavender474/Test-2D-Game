package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class ElectricBall(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity {

  companion object {
    const val TAG = "ElectricBall"
    private var smallRegion: TextureRegion? = null
    private var largeRegion: TextureRegion? = null
  }

  override var owner: IGameEntity? = null

  val trajectory = Vector2()

  var large = false

  override fun init() {
    if (smallRegion == null)
        game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Electric/SmallElectric")
    if (largeRegion == null)
        game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Electric/BigElectric")
    addComponent(defineBodyComponent())
    addComponent(defineSpritesComponent())
    addComponent(defineAnimationsComponent())
    defineProjectileComponents().forEach { addComponent(it) }
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
    body.setCenter(spawn)
    trajectory.x = spawnProps.get(ConstKeys.X, Float::class)!!
    trajectory.y = spawnProps.get(ConstKeys.Y, Float::class)!!
    large = spawnProps.getOrDefault(ConstKeys.LARGE, false, Boolean::class)
  }

  override fun hitBlock(blockFixture: Fixture) {
    // shock explosion
    requestToPlaySound(SoundAsset.MM3_ELECTRIC_PULSE_SOUND, false)
  }

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)

    val bounds = GameRectangle()

    // projectile fixture
    val projectileFixture = Fixture(bounds, FixtureType.PROJECTILE)
    body.addFixture(projectileFixture)

    // damager fixture
    val damagerFixture = Fixture(bounds, FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    body.preProcess = Updatable {
      val size = if (large) ConstVals.PPM.toFloat() else ConstVals.PPM / 4f
      body.setSize(size)
      bounds.setSize(size)
    }

    return BodyComponentCreator.create(this, body)
  }

  private fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(ConstVals.PPM.toFloat())

    val spritesComponent = SpritesComponent(this, "electric" to sprite)
    spritesComponent.putUpdateFunction("electric") { _, _sprite ->
      _sprite as GameSprite
      val center = body.getCenter()
      _sprite.setCenter(center.x, center.y)
    }

    return spritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier: () -> String? = { if (large) "large" else "small" }
    val animations =
        objectMapOf<String, IAnimation>(
            "large" to Animation(largeRegion!!, 1, 2, 0.15f, true),
            "small" to Animation(smallRegion!!, 1, 2, 0.15f, true))
    val animator = Animator(keySupplier, animations)
    return AnimationsComponent(this, animator)
  }
}
