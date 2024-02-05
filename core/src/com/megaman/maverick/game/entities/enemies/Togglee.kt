package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.ProcessState
import com.engine.common.extensions.equalsAny
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity
import com.megaman.maverick.game.world.setConsumer
import kotlin.reflect.KClass

class Togglee(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDirectionRotatable {

    enum class ToggleeType {
        TEST
    }

    enum class ToggleeState {
        TOGGLED_ON, TOGGLED_OFF, TOGGLING_TO_ON, TOGGLING_TO_OFF
    }

    companion object {
        const val TAG = "Toggle"
        private var leftRegion: TextureRegion? = null
        private var rightRegion: TextureRegion? = null
        private var switchLeftRegion: TextureRegion? = null
        private var switchRightRegion: TextureRegion? = null
        private const val SWITCH_DURATION = 0.45f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, Int>()

    val moving: Boolean
        get() = toggleeState.equalsAny(ToggleeState.TOGGLING_TO_ON, ToggleeState.TOGGLING_TO_OFF)
    val on: Boolean
        get() = toggleeState == ToggleeState.TOGGLED_ON

    lateinit var toggleeType: ToggleeType
        private set
    lateinit var toggleeState: ToggleeState
        private set

    override lateinit var directionRotation: Direction

    private val switchTimer = Timer(SWITCH_DURATION)

    override fun init() {
        super<AbstractEnemy>.init()
        if (leftRegion == null || rightRegion == null || switchLeftRegion == null || switchRightRegion == null) {
            leftRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Togglee/Left")
            rightRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Togglee/Right")
            switchLeftRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Togglee/SwitchToLeft")
            switchRightRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Togglee/SwitchToRight")
        }
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val toggleTypeString = spawnProps.get(ConstKeys.TYPE, String::class)!!
        toggleeType = ToggleeType.valueOf(toggleTypeString.uppercase())

        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class)
        directionRotation = Direction.valueOf(directionString.uppercase())

        toggleeState = ToggleeState.TOGGLED_OFF
        switchTimer.setToEnd()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (!switchTimer.isFinished()) {
                switchTimer.update(it)
                if (switchTimer.isJustFinished()) {
                    toggleeState = if (toggleeState == ToggleeState.TOGGLING_TO_OFF) ToggleeState.TOGGLED_OFF
                    else ToggleeState.TOGGLED_ON

                    runToggleeAction()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()

        // body fixture
        val bodyFixture = Fixture(GameRectangle().setSize(body.getSize()), FixtureType.BODY)
        body.addFixture(bodyFixture)
        bodyFixture.shape.color = Color.GRAY
        debugShapes.add { bodyFixture.shape }

        // damager fixture
        val damagerFixture = Fixture(
            GameRectangle().setSize(ConstVals.PPM.toFloat()), FixtureType.DAMAGER
        )
        body.addFixture(damagerFixture)
        damagerFixture.shape.color = Color.RED
        debugShapes.add { damagerFixture.shape }

        // consumer fixture
        val consumerFixture = Fixture(
            GameRectangle().setSize(ConstVals.PPM.toFloat()), FixtureType.CONSUMER
        )
        consumerFixture.setConsumer { state, it ->
            if (state == ProcessState.BEGIN) consume(it)
        }
        body.addFixture(consumerFixture)
        consumerFixture.shape.color = Color.GREEN
        debugShapes.add { consumerFixture.shape }

        body.preProcess.put(ConstKeys.DEFAULT) {
            damagerFixture.active = !moving
            consumerFixture.active = !moving

            val fixtureOffsetX = if (on) 0.5f * ConstVals.PPM else -0.5f * ConstVals.PPM
            damagerFixture.offsetFromBodyCenter.x = fixtureOffsetX
            consumerFixture.offsetFromBodyCenter.x = fixtureOffsetX
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun consume(fixture: Fixture) {
        if (moving) return

        val fixtureEntity = fixture.getEntity()

        if (fixture.fixtureLabel == FixtureType.PLAYER ||
            (fixture.fixtureLabel == FixtureType.PROJECTILE &&
                    fixtureEntity is IProjectileEntity &&
                    fixtureEntity.owner is Megaman)
        ) switchToggleeState()
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, "togglee" to sprite)
        spritesComponent.putUpdateFunction("togglee") { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { toggleeState.name }
        val animations = objectMapOf<String, IAnimation>(
            ToggleeState.TOGGLED_OFF.name to Animation(leftRegion!!, 1, 2, gdxArrayOf(1f, 0.15f), true),
            ToggleeState.TOGGLED_ON.name to Animation(rightRegion!!, 1, 2, gdxArrayOf(1f, 0.15f), true),
            ToggleeState.TOGGLING_TO_ON.name to Animation(switchRightRegion!!, 1, 3, 0.1f, false),
            ToggleeState.TOGGLING_TO_OFF.name to Animation(switchLeftRegion!!, 1, 3, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun switchToggleeState() {
        GameLogger.debug(TAG, "switchToggleState(): Switching togglee state from $toggleeState")
        toggleeState = if (on) ToggleeState.TOGGLING_TO_OFF else ToggleeState.TOGGLING_TO_ON
        GameLogger.debug(TAG, "switchToggleState(): Switching togglee state to $toggleeState")
        switchTimer.reset()
    }

    private fun runToggleeAction() { // TODO: Implement toggle action
        GameLogger.debug(TAG, "runToggleAction(): Running togglee action with toggle type = $toggleeType")
        when (toggleeType) {
            else -> {}
        }
    }
}