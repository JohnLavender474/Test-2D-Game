package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.enums.Direction
import com.engine.common.extensions.gdxArrayOf
import com.engine.screens.BaseScreen
import com.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.controllers.ControllerButton
import com.megaman.maverick.game.MegamanMaverickGame

abstract class AbstractMenuScreen(game: MegamanMaverickGame, protected var firstButtonKey: String) : BaseScreen(game) {

    override val eventKeyMask = ObjectSet<Any>()

    protected val castGame: MegamanMaverickGame = game
    protected abstract val menuButtons: ObjectMap<String, IMenuButton>
    protected var selectionMade = false
        private set

    var currentButtonKey = firstButtonKey

    protected open fun onAnyMovement() {
        // do nothing
    }

    protected open fun onAnySelection() {
        // do nothing
    }

    protected open fun undoSelection() {
        selectionMade = false
    }

    override fun show() {
        super.show()
        selectionMade = false
        currentButtonKey = firstButtonKey
    }

    override fun render(delta: Float) {
        super.render(delta)
        if (selectionMade || game.paused) return

        val menuButton = menuButtons.get(currentButtonKey)
        menuButton?.let {
            val direction =
                if (game.controllerPoller.isJustPressed(ControllerButton.UP)) Direction.UP
                else if (game.controllerPoller.isJustPressed(ControllerButton.DOWN))
                    Direction.DOWN
                else if (game.controllerPoller.isJustPressed(ControllerButton.LEFT))
                    Direction.LEFT
                else if (game.controllerPoller.isJustPressed(ControllerButton.RIGHT))
                    Direction.RIGHT
                else null

            direction?.let { d ->
                onAnyMovement()
                menuButton.onNavigate(d, delta)?.let { currentButtonKey = it }
            }

            if (game.controllerPoller.isAnyJustPressed(
                    gdxArrayOf(ControllerButton.START, ControllerButton.A)
                )
            ) {
                selectionMade = menuButton.onSelect(delta)
                if (selectionMade) onAnySelection()
            }
        }
    }

    override fun pause() {
        super.pause()
        castGame.audioMan.pauseAllSound()
        castGame.audioMan.pauseMusic()
    }

    override fun resume() {
        super.resume()
        castGame.audioMan.resumeAllSound()
        castGame.audioMan.playMusic()
    }

    override fun dispose() {
        super.dispose()
        castGame.audioMan.stopMusic()
    }
}
