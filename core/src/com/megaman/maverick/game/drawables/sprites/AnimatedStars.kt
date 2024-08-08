package com.megaman.maverick.game.drawables.sprites

import com.badlogic.gdx.math.Vector2
import com.engine.common.extensions.getTextureRegion
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

class AnimatedStars(
    game: MegamanMaverickGame,
    start: Vector2,
    width: Float = MODEL_WIDTH * ConstVals.PPM,
    height: Float = MODEL_HEIGHT * ConstVals.PPM
) : AnimatedBackground(
    start.x, start.y, game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_1.source, "AnimatedStarsBG"),
    width, height, ROWS, COLS, ANIM_ROWS, ANIM_COLS, ANIM_DUR
) {

    companion object {
        private const val ROWS = 5
        private const val COLS = 75
        private const val ANIM_ROWS = 1
        private const val ANIM_COLS = 3
        private const val ANIM_DUR = 0.5f
        private const val MODEL_WIDTH = ConstVals.VIEW_WIDTH / 3f
        private const val MODEL_HEIGHT = ConstVals.VIEW_HEIGHT / 4f
    }
}