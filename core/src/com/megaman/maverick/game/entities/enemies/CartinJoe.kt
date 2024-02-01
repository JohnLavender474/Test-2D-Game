package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.interfaces.isFacing
import com.engine.common.interfaces.swapFacing
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.world.*
import kotlin.reflect.KClass

class CartinJoe(game: MegamanMaverickGame) : AbstractEnemy(game), ISpriteEntity, IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "CartinJoe"
        private var moveRegion: TextureRegion? = null
        private var shootRegion: TextureRegion? = null
        private const val VEL_X = 5f
        private const val GROUND_GRAVITY = -0.0015f
        private const val GRAVITY = -0.5f
        private const val WAIT_DURATION = 1f
        private const val SHOOT_DURATION = 0.25f
    }

    override var facing = Facing.RIGHT
    override val damageNegotiations = objectMapOf<KClass<out IDamager>, Int>()

    val shooting: Boolean
        get() = !shootTimer.isFinished()

    private val waitTimer = Timer(WAIT_DURATION)
    private val shootTimer = Timer(SHOOT_DURATION)

    override fun init() {
        super<AbstractEnemy>.init()
        if (moveRegion == null || shootRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            moveRegion = atlas.findRegion("CartinJoe/Move")
            shootRegion = atlas.findRegion("CartinJoe/Shoot")
        }
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        val left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)
        facing = if (left) Facing.LEFT else Facing.RIGHT
        waitTimer.reset()
        shootTimer.setToEnd()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            shootTimer.update(it)
            if (shootTimer.isJustFinished()) waitTimer.reset()

            waitTimer.update(it)
            if (waitTimer.isJustFinished()) {
                shoot()
                shootTimer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM * 1.25f)

        val debugShapes = Array<() -> IDrawableShape?>()

        // body fixture
        val bodyFixture =
            Fixture(GameRectangle().setSize(1.25f * ConstVals.PPM, 1.85f * ConstVals.PPM), FixtureType.BODY)
        body.addFixture(bodyFixture)
        bodyFixture.shape.color = Color.GRAY
        debugShapes.add { bodyFixture.shape }

        // shield fixture
        val shieldFixture =
            Fixture(GameRectangle().setSize(1.25f * ConstVals.PPM, 0.75f * ConstVals.PPM), FixtureType.SHIELD)
        shieldFixture.offsetFromBodyCenter.y = -0.25f * ConstVals.PPM
        body.addFixture(shieldFixture)
        shieldFixture.shape.color = Color.BLUE
        debugShapes.add { shieldFixture.shape }

        // damager fixture
        val damagerFixture =
            Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)
        damagerFixture.shape.color = Color.RED
        debugShapes.add { damagerFixture.shape }

        // damageable fixture
        val damageableFixture =
            Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.75f * ConstVals.PPM), FixtureType.DAMAGER)
        damageableFixture.offsetFromBodyCenter.y = 0.45f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damageableFixture.shape.color = Color.PURPLE
        debugShapes.add { damageableFixture.shape }

        // feet fixture
        val feetFixture =
            Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM), FixtureType.FEET)
        feetFixture.offsetFromBodyCenter.y = -0.6f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.shape.color = Color.GREEN
        debugShapes.add { feetFixture.shape }

        val onBounce: () -> Unit = {
            swapFacing()
            GameLogger.debug(TAG, "onBounce: swap facing to $facing")
        }

        // left fixture
        val leftFixture =
            Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM), FixtureType.SIDE)
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.setRunnable(onBounce)
        leftFixture.offsetFromBodyCenter.x = -0.75f * ConstVals.PPM
        body.addFixture(leftFixture)
        leftFixture.shape.color = Color.YELLOW
        debugShapes.add { leftFixture.shape }

        // right fixture
        val rightFixture =
            Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM), FixtureType.SIDE)
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.setRunnable(onBounce)
        rightFixture.offsetFromBodyCenter.x = 0.75f * ConstVals.PPM
        body.addFixture(rightFixture)
        rightFixture.shape.color = Color.YELLOW
        debugShapes.add { rightFixture.shape }

        body.preProcess = Updatable {
            body.physics.gravity.y =
                ConstVals.PPM * if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.velocity.x = VEL_X * ConstVals.PPM * facing.value
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, "cartin_joe" to sprite)
        spritesComponent.putUpdateFunction("cartin_joe") { _, _sprite ->
            _sprite as GameSprite
            val position = body.getBottomCenterPoint()
            _sprite.setPosition(position, Position.BOTTOM_CENTER)
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (shooting) "shoot" else "move"
        }
        val animations = objectMapOf<String, IAnimation>(
            "shoot" to Animation(shootRegion!!), "move" to Animation(moveRegion!!)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun shoot() {

    }
}