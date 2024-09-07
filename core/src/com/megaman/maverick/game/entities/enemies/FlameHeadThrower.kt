package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.coerceX
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class FlameHeadThrower(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "FlameHeadThrower"
        private const val STAND_DUR = 1f
        private const val THROW_DUR = 0.3f
        private const val IMPULSE_Y = 4f
        private const val MAX_THROW_X = 8f
        private const val FIREBALL_GRAVITY = -0.15f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override lateinit var facing: Facing

    private val standTimer = Timer(STAND_DUR)
    private val throwTimer = Timer(THROW_DUR, TimeMarkedRunnable(0.2f) { throwFlame() })

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("stand", atlas.findRegion("$TAG/Stand"))
            regions.put("throw", atlas.findRegion("$TAG/Throw"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        standTimer.reset()
        throwTimer.setToEnd()
    }

    private fun throwFlame() {
        val trajectory = MegaUtilMethods.calculateJumpImpulse(
            body.getTopCenterPoint(),
            getMegaman().body.getCenter(),
            IMPULSE_Y * ConstVals.PPM
        ).coerceX(-MAX_THROW_X * ConstVals.PPM, MAX_THROW_X * ConstVals.PPM)

        val fireBall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIREBALL)!!
        fireBall.spawn(
            props(
                ConstKeys.POSITION to body.getCenter().add(0.1f * ConstVals.PPM * facing.value, 0.1f * ConstVals.PPM),
                ConstKeys.OWNER to this,
                ConstKeys.TRAJECTORY to trajectory,
                ConstKeys.GRAVITY to Vector2(0f, FIREBALL_GRAVITY * ConstVals.PPM),
                Fireball.BURST_ON_DAMAGE_INFLICTED to true
            )
        )
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!standTimer.isFinished()) {
                standTimer.update(delta)
                if (standTimer.isJustFinished()) throwTimer.reset()
                return@add
            }

            throwTimer.update(delta)
            if (throwTimer.isJustFinished()) standTimer.reset()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.75f * ConstVals.PPM, 0.85f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (!standTimer.isFinished()) "stand" else "throw" }
        val animations = objectMapOf<String, IAnimation>(
            "stand" to Animation(regions.get("stand")),
            "throw" to Animation(regions.get("throw"), 1, 3, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}