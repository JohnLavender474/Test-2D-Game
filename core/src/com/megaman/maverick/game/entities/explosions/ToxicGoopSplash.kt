package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class ToxicGoopSplash(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IAudioEntity, IDirectionRotatable, IOwnable, IDamager {

    companion object {
        const val TAG = "ToxicGoopSplash"
        private const val SPLASH_DUR = 0.3f
        private var region: TextureRegion? = null
    }

    override var owner: GameEntity? = null
    override var directionRotation: Direction? = null

    private val splashTimer = Timer(SPLASH_DUR)

    override fun getEntityType() = EntityType.EXPLOSION

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, ExplosionsFactory.TOXIC_GOOP_SPLASH)
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        directionRotation = spawnProps.getOrDefaultNotNull(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        when (directionRotation!!) {
            Direction.UP -> body.setBottomCenterToPoint(spawn)
            Direction.DOWN -> body.setTopCenterToPoint(spawn)
            Direction.LEFT -> body.setCenterRightToPoint(spawn)
            Direction.RIGHT -> body.setCenterLeftToPoint(spawn)
        }
        splashTimer.reset()
        requestToPlaySound(SoundAsset.SPLASH_SOUND, false)
    }

    override fun canDamage(damageable: IDamageable) = damageable != owner

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        splashTimer.update(delta)
        if (splashTimer.isFinished()) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation?.rotation ?: 0f
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 3, 0.1f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}