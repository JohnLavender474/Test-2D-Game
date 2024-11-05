package com.megaman.maverick.game.controllers

import com.badlogic.gdx.Input
import com.mega.game.engine.common.objects.ImmutableCollection

enum class MegaControllerButton(val defaultKeyboardKey: Int) {
    START(Input.Keys.ENTER),
    SELECT(Input.Keys.L),
    UP(Input.Keys.W),
    DOWN(Input.Keys.S),
    LEFT(Input.Keys.A),
    RIGHT(Input.Keys.D),
    A(Input.Keys.K),
    B(Input.Keys.J);

    fun isDpadButton() = DPAD_BUTTONS.contains(this)

    fun isActionButton() = ACTION_BUTTONS.contains(this)

    fun isCommandButton() = COMMAND_BUTTONS.contains(this)

    companion object {
        val DPAD_BUTTONS = ImmutableCollection(hashSetOf(UP, DOWN, LEFT, RIGHT))
        val ACTION_BUTTONS = ImmutableCollection(hashSetOf(A, B))
        val COMMAND_BUTTONS = ImmutableCollection(hashSetOf(START, SELECT))
    }
}
