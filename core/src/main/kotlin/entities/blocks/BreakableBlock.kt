package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.enemies.Wanaan
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory

import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getEntity

class BreakableBlock(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "BreakableBlock"
        const val BRICK_TYPE = "Brick"

        // set this to true to make brick pieces "thump" on blocks, else false
        private const val BRICK_TYPE_THUMP = false

        private val BRICK_PIECE_IMPULSES =
            gdxArrayOf(Vector2(-5f, 3f), Vector2(-3f, 5f), Vector2(3f, 5f), Vector2(5f, 3f))

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private lateinit var type: String

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            regions.put(BRICK_TYPE, atlas.findRegion(BRICK_TYPE))
        }
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        type = spawnProps.get(ConstKeys.TYPE, String::class)!!
    }

    override fun hitByHead(headFixture: IFixture) {
        val entity = headFixture.getEntity()
        when (type) {
            BRICK_TYPE -> if (entity is Wanaan) {
                destroy()

                for (i in 0 until BRICK_PIECE_IMPULSES.size) {
                    val spawn = body.getCenter()
                    val impulse = BRICK_PIECE_IMPULSES[i].cpy().scl(ConstVals.PPM.toFloat())
                    val brickPiece = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.BRICK_PIECE)!!
                    brickPiece.spawn(
                        props(
                            ConstKeys.POSITION pairTo spawn,
                            ConstKeys.IMPULSE pairTo impulse,
                            ConstKeys.THUMP pairTo BRICK_TYPE_THUMP
                        )
                    )
                }

                game.audioMan.playSound(SoundAsset.THUMP_SOUND, false)
            }
        }
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            val bounds = body.getBounds()
            sprite.setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { type }
        val animations = objectMapOf<String, IAnimation>(
            BRICK_TYPE pairTo Animation(regions[BRICK_TYPE])
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
