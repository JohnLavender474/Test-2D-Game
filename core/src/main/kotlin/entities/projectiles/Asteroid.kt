package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.getRandom
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.points.PointsComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class Asteroid(game: MegamanMaverickGame) : AbstractProjectile(game), IOwnable {

    companion object {
        const val TAG = "Asteroid"
        const val REGULAR = "Regular"
        const val BLUE = "Blue"
        const val MIN_ROTATION_SPEED = 0.5f
        const val MAX_ROTATION_SPEED = 1.5f
        private val HIT_PROJS = objectSetOf<KClass<out IProjectileEntity>>(
            Asteroid::class,
            ExplodingBall::class,
            RocketBomb::class
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var owner: GameEntity? = null

    private lateinit var type: String
    private var rotation = 0f
    private var rotationSpeed = 0f

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            regions.put(REGULAR, atlas.findRegion("$TAG/$REGULAR"))
            regions.put(BLUE, atlas.findRegion("$TAG/$BLUE"))
        }
        super.init()
        addComponent(definePointsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)

        val spawn =
            if (spawnProps.containsKey(ConstKeys.BOUNDS))
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
            else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.getOrDefault(ConstKeys.IMPULSE, Vector2(), Vector2::class)
        body.physics.velocity.set(impulse)

        rotationSpeed = spawnProps.getOrDefault(
            "${ConstKeys.ROTATION}_${ConstKeys.SPEED}",
            getRandom(MIN_ROTATION_SPEED, MAX_ROTATION_SPEED), Float::class
        )
        type = spawnProps.getOrDefault(ConstKeys.TYPE, REGULAR, String::class)
        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
    }

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = bodyFixture.getEntity()
        if (entity == owner || (entity is IOwnable && entity.owner == owner)) return
        explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (blockFixture.getBody().hasBlockFilter(TAG)) return
        explodeAndDie()
    }

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val other = projectileFixture.getEntity() as IProjectileEntity
        if (HIT_PROJS.contains(other::class)) {
            explodeAndDie()
            other.explodeAndDie()
        }
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.ASTEROID_EXPLOSION)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(0.3f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        projectileFixture.rawShape.color = Color.BLUE
        debugShapes.add { projectileFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(shieldFixture)
        shieldFixture.rawShape.color = Color.CYAN
        debugShapes.add { shieldFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { delta, _ ->
            val region = regions.get(type)
            sprite.setRegion(region)
            sprite.setCenter(body.getCenter())
            rotation += rotationSpeed * ConstVals.PPM * delta
            sprite.setOriginCenter()
            sprite.rotation = rotation
        }
        return spritesComponent
    }

    private fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent()
        pointsComponent.putPoints(
            ConstKeys.HEALTH,
            max = MegamanValues.START_HEALTH,
            current = MegamanValues.START_HEALTH,
            min = ConstVals.MIN_HEALTH
        )
        pointsComponent.putListener(ConstKeys.HEALTH) {
            if (it.current <= ConstVals.MIN_HEALTH) {
                GameLogger.debug(TAG, "Asteroid has died due pairTo health reaching zero.")
                destroy()
            }
        }
        return pointsComponent
    }
}
