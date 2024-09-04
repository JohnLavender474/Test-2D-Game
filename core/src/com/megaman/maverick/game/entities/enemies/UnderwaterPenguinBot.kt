package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.isFacing
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.getCenter
import com.mega.game.engine.common.shapes.toGameRectangle
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.Body
import com.mega.game.engine.world.BodyComponent
import com.mega.game.engine.world.BodyType
import com.mega.game.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.*
import kotlin.reflect.KClass

class UnderwaterPenguinBot(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "UnderwaterPenguinBot"
        private const val SWIM_SPEED = 8f
        private const val GRAVITY = -0.1f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class UnderwaterPenguinBotState { WAIT, SWIM, BENT }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(15),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class to dmgNeg(10)
    )
    override lateinit var facing: Facing

    private lateinit var state: UnderwaterPenguinBotState
    private lateinit var triggerBox: GameRectangle
    private lateinit var startPosition: Vector2

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("swim", atlas.findRegion("$TAG/swim"))
            regions.put("bent", atlas.findRegion("$TAG/bent"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        triggerBox = spawnProps.get(ConstKeys.TRIGGER, RectangleMapObject::class)!!.rectangle.toGameRectangle()
        startPosition = spawnProps.get(ConstKeys.START, RectangleMapObject::class)!!.rectangle.getCenter()

        state = UnderwaterPenguinBotState.WAIT
        body.physics.gravityOn = false
        body.fixtures.forEach { (it.second as Fixture).active = false }

        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
    }

    override fun canDamage(damageable: IDamageable) = state != UnderwaterPenguinBotState.WAIT

    private fun startSwim() {
        state = UnderwaterPenguinBotState.SWIM
        body.setCenter(startPosition)
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        val speed = SWIM_SPEED * ConstVals.PPM * facing.value
        body.physics.velocity.x = speed
        body.fixtures.forEach { (it.second as Fixture).active = true }
    }

    private fun hitNose() {
        state = UnderwaterPenguinBotState.BENT
        body.physics.velocity.setZero()
        body.physics.gravityOn = true
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.MARIO_FIREBALL_SOUND, false)
    }

    private fun explodeAndDie() {
        explode()
        destroy()
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (state == UnderwaterPenguinBotState.WAIT && getMegaman().body.overlaps(triggerBox as Rectangle)) startSwim()
            else if (state == UnderwaterPenguinBotState.BENT && body.isSensing(BodySense.FEET_ON_GROUND)) explodeAndDie()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.15f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val noseFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        noseFixture.setConsumer { processState, fixture ->
            if (state == UnderwaterPenguinBotState.SWIM &&
                processState == ProcessState.BEGIN &&
                fixture.getFixtureType() == FixtureType.BLOCK
            ) hitNose()
        }
        body.addFixture(noseFixture)
        noseFixture.rawShape.color = Color.BLUE
        debugShapes.add { noseFixture.getShape() }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -0.375f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            noseFixture.offsetFromBodyCenter.x = 0.575f * ConstVals.PPM * facing.value
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            val position = if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
            _sprite.setPosition(body.getPositionPoint(position), position)
            _sprite.hidden = damageBlink || state == UnderwaterPenguinBotState.WAIT
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? =
            { if (state == UnderwaterPenguinBotState.WAIT) null else state.name.lowercase() }
        val animations = objectMapOf<String, IAnimation>(
            "swim" to Animation(regions["swim"], 3, 1, 0.1f, true),
            "bent" to Animation(regions["bent"])
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}