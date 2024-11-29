package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interpolate
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.getCenter
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import kotlin.reflect.KClass

class BombChute(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity {

    companion object {
        const val TAG = "BombChute"
        private const val RISE_SPEED = 8f
        private const val FALL_SPEED = -3f
        private const val X_ACCELERATION = 5f
        private const val X_MAX_SPEED = 2f
        private const val TURN_DUR = 0.85f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )

    private val turnTimer = Timer(TURN_DUR)
    private var targetY = 0f
    private var targetReached = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("up", atlas.findRegion("$TAG/up"))
            regions.put("turn", atlas.findRegion("$TAG/turn"))
            regions.put("down", atlas.findRegion("$TAG/down"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        body.physics.velocity.set(0f, RISE_SPEED * ConstVals.PPM)
        turnTimer.reset()
        targetY = spawnProps.get(ConstKeys.TARGET, RectangleMapObject::class)!!.rectangle.getCenter().y
        targetReached = false
    }

    private fun explodeAndDie() {
        explode()
        destroy()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (body.isSensing(BodySense.BODY_TOUCHING_BLOCK)) explodeAndDie()

            if (!targetReached) {
                if (body.getCenter().y >= targetY) targetReached = true
                else return@add
            }

            if (!turnTimer.isFinished()) {
                turnTimer.update(delta)
                body.physics.velocity.y = interpolate(RISE_SPEED, FALL_SPEED, turnTimer.getRatio()) * ConstVals.PPM
                return@add
            }

            body.physics.velocity.y = FALL_SPEED * ConstVals.PPM

            var xSpeed = body.physics.velocity.x

            if (body.getX() < megaman().body.getX()) xSpeed += X_ACCELERATION * delta * ConstVals.PPM
            else xSpeed -= X_ACCELERATION * delta * ConstVals.PPM

            if (xSpeed > X_MAX_SPEED * ConstVals.PPM) xSpeed = X_MAX_SPEED * ConstVals.PPM
            else if (xSpeed < -X_MAX_SPEED * ConstVals.PPM) xSpeed = -X_MAX_SPEED * ConstVals.PPM

            body.physics.velocity.x = xSpeed
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.35f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? =
            { if (!targetReached) "up" else if (!turnTimer.isFinished()) "turn" else "down" }
        val animations = objectMapOf<String, IAnimation>(
            "up" pairTo Animation(regions.get("up"), 2, 1, 0.1f, true),
            "turn" pairTo Animation(regions.get("turn"), 2, 2, 0.2f, false),
            "down" pairTo Animation(regions.get("down"), 2, 2, 0.25f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
