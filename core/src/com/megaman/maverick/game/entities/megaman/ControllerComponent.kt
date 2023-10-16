package com.megaman.maverick.game.entities.megaman

import com.engine.controller.ControllerComponent
import com.engine.controller.buttons.ButtonActuator
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues

/**
 * Returns the [ControllerComponent] of this [Megaman], or creates a new one if it doesn't have one.
 */
fun Megaman.controllerComponent(): ControllerComponent {
  if (hasComponent(ControllerComponent::class)) return getComponent(ControllerComponent::class)!!

  val left =
      ButtonActuator(
          onPressContinued = { poller, delta ->
            if (poller.isButtonPressed(ConstKeys.RIGHT)) return@ButtonActuator

            running = true
            val threshold = MegamanValues.RUN_SPEED * ConstVals.PPM
            if (body.physics.velocity.x > -threshold)
                body.physics.velocity.x -= MegamanValues.RUN_IMPULSE * delta * ConstVals.PPM
          },
          onJustReleased = { poller ->
            if (!poller.isButtonPressed(ConstKeys.RIGHT)) running = true
          })

  val right =
      ButtonActuator(
          onPressContinued = { poller, delta ->
            if (poller.isButtonPressed(ConstKeys.LEFT)) return@ButtonActuator

            running = true
            val threshold = MegamanValues.RUN_SPEED * ConstVals.PPM
            if (body.physics.velocity.x < threshold) {
              body.physics.velocity.x += MegamanValues.RUN_IMPULSE * delta * ConstVals.PPM
            }
          },
          onJustReleased = { poller ->
            if (!poller.isButtonPressed(ConstKeys.LEFT)) running = true
          })

  return ControllerComponent(this, ConstKeys.LEFT to left, ConstKeys.RIGHT to right)
}
