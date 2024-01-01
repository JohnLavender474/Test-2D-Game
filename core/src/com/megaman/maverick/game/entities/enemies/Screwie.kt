package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class Screwie(game: MegamanMaverickGame) : AbstractEnemy(game) {

  companion object {
    private var atlas: TextureAtlas? = null
    private const val SHOOT_DUR = 2f
    private const val DOWN_DUR = 1f
    private const val RISE_DROP_DUR = .3f
    private const val BULLET_VEL = 10f
  }

  override val damageNegotiations =
      objectMapOf<KClass<out IDamager>, Int>(
          Bullet::class to 10,
          Fireball::class to ConstVals.MAX_HEALTH,
          ChargedShot::class to 10,
          ChargedShotExplosion::class to 5)

  private val downTimer = Timer(DOWN_DUR)
  private val riseTimer = Timer(RISE_DROP_DUR)
  private val shootTimer = Timer(SHOOT_DUR)
  private val dropTimer = Timer(RISE_DROP_DUR)

  var upsideDown = false
    private set

  private var type = ""

  private val down: Boolean
    get() = !downTimer.isFinished()

  private val shooting: Boolean
    get() = !shootTimer.isFinished()

  private val rising: Boolean
    get() = !riseTimer.isFinished()

  override fun init() {
    super.init()
    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
    shootTimer.setRunnables(
        gdxArrayOf(
            TimeMarkedRunnable(0.5f) { shoot() },
            TimeMarkedRunnable(1.0f) { shoot() },
            TimeMarkedRunnable(1.5f) { shoot() }))
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    type = spawnProps.getOrDefault(ConstKeys.TYPE, "red") as String
    upsideDown = spawnProps.getOrDefault(ConstKeys.DOWN, false) as Boolean

    downTimer.reset()
    riseTimer.setToEnd()
    shootTimer.reset()
    dropTimer.setToEnd()

    val position = if (upsideDown) Position.TOP_CENTER else Position.BOTTOM_CENTER
    val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
    body.positionOnPoint(spawn, position)
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(0.65f * ConstVals.PPM, 0.5f * ConstVals.PPM)

    val shapes = Array<() -> IDrawableShape>()

    // damager fixture
    val damagerFixture =
        Fixture(GameRectangle().setSize(0.15f * ConstVals.PPM), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    damagerFixture.shape.color = Color.RED
    shapes.add { damagerFixture.shape }

    // damageable fixture
    val damageableFixture =
        Fixture(
            GameRectangle().setSize(0.65f * ConstVals.PPM, 0.5f * ConstVals.PPM),
            FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    damageableFixture.shape.color = Color.PURPLE
    shapes.add { damageableFixture.shape }

    // pre-process
    body.preProcess = Updatable {
      val damageableBounds = damageableFixture.shape as GameRectangle
      if (down) {
        damageableBounds.height = 0.2f * ConstVals.PPM
        damageableFixture.offsetFromBodyCenter.y =
            (if (upsideDown) 0.15f else -0.15f) * ConstVals.PPM
      } else {
        damageableBounds.height = 0.65f * ConstVals.PPM
        damageableFixture.offsetFromBodyCenter.y = 0f
      }
    }

    addComponent(DrawableShapesComponent(this, debugShapeSuppliers = shapes, debug = true))

    return BodyComponentCreator.create(this, body)
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)
    updatablesComponent.add {
      if (!downTimer.isFinished()) {
        downTimer.update(it)
        if (downTimer.isFinished()) riseTimer.reset()
      } else if (!riseTimer.isFinished()) {
        riseTimer.update(it)
        if (riseTimer.isFinished()) shootTimer.reset()
      } else if (!shootTimer.isFinished()) {
        shootTimer.update(it)
        if (shootTimer.isFinished()) dropTimer.reset()
      } else if (!dropTimer.isFinished()) {
        dropTimer.update(it)
        if (dropTimer.isFinished()) downTimer.reset()
      }
    }
  }

  override fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(1.35f * ConstVals.PPM)
    val SpritesComponent = SpritesComponent(this, "screwie" to sprite)
    SpritesComponent.putUpdateFunction("screwie") { _, _sprite ->
      _sprite as GameSprite
      val position = if (upsideDown) Position.TOP_CENTER else Position.BOTTOM_CENTER
      val bodyPosition = body.getPositionPoint(position)
      _sprite.setPosition(bodyPosition, position)
      _sprite.setFlip(false, upsideDown)
    }
    return SpritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier: () -> String = {
      val key = if (down) "down" else if (shooting) "shoot" else if (rising) "rise" else "drop"
      "$type-$key"
    }
    val animations =
        objectMapOf<String, IAnimation>(
            "red-down" to Animation(atlas!!.findRegion("RedScrewie/Down")),
            "red-rise" to Animation(atlas!!.findRegion("RedScrewie/Rise"), 1, 3, 0.1f, false),
            "red-drop" to Animation(atlas!!.findRegion("RedScrewie/Drop"), 1, 3, 0.1f, false),
            "red-shoot" to Animation(atlas!!.findRegion("RedScrewie/Shoot"), 1, 3, 0.1f, true),
            "blue-down" to Animation(atlas!!.findRegion("BlueScrewie/Down")),
            "blue-rise" to Animation(atlas!!.findRegion("BlueScrewie/Rise"), 1, 3, 0.1f, false),
            "blue-drop" to Animation(atlas!!.findRegion("BlueScrewie/Drop"), 1, 3, 0.1f, false),
            "blue-shoot" to Animation(atlas!!.findRegion("BlueScrewie/Shoot"), 1, 3, 0.1f, true),
        )
    val animator = Animator(keySupplier, animations)
    return AnimationsComponent(this, animator)
  }

  private fun shoot() {
    for (i in 0 until 3) {
      val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!

      val spawn = Vector2(body.getCenter())
      spawn.x += (if (i == 0) -0.2f else 0.2f) * ConstVals.PPM
      spawn.y += (if (upsideDown) -0.215f else 0.215f) * ConstVals.PPM

      val trajectory = Vector2()
      trajectory.x = if (i == 0) -BULLET_VEL else BULLET_VEL

      game.gameEngine.spawn(
          bullet,
          props(
              ConstKeys.TRAJECTORY to trajectory,
              ConstKeys.POSITION to spawn,
              ConstKeys.OWNER to this))
    }
  }
}
