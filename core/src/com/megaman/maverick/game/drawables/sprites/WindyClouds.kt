package com.megaman.maverick.game.drawables.sprites

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.getTextureRegion
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

class WindyClouds(game: MegamanMaverickGame, start: Vector2, width: Float, height: Float) : ScrollingBackground(
    game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_2.source, "BKG04"),
    start, start.cpy().sub(width, 0f), DUR, width, height, ROWS, COLS
) {

    companion object {
        private const val ROWS = 2
        private const val COLS = 20
        private const val DUR = 10f
    }
}