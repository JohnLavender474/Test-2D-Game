package com.megaman.maverick.game.entities.megaman.components

import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.controller.ControllerComponent
import com.engine.controller.buttons.ButtonActuator
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.ControllerButton
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.extensions.shoot
import com.megaman.maverick.game.entities.megaman.extensions.stopCharging
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing

internal fun Megaman.defineControllerComponent(): ControllerComponent {
  // left
  val left =
      ButtonActuator(
          onJustPressed = { _ ->
            GameLogger.debug(Megaman.TAG, "left controller button just pressed")
          },
          onPressContinued = { poller, delta ->
            if (damaged || poller.isButtonPressed(ControllerButton.RIGHT.name))
                return@ButtonActuator

            facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.RIGHT else Facing.LEFT
            if (isBehaviorActive(BehaviorType.CLIMBING)) return@ButtonActuator
            running = !isBehaviorActive(BehaviorType.WALL_SLIDING)

            val threshold =
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_RUN_SPEED
                else MegamanValues.RUN_SPEED) * ConstVals.PPM

            if (body.physics.velocity.x > -threshold) {
              val impulseX =
                  if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
                  else MegamanValues.RUN_IMPULSE
              body.physics.velocity.x -= impulseX * delta * ConstVals.PPM
            }
          },
          onJustReleased = { poller ->
            GameLogger.debug(Megaman.TAG, "left controller button just released")
            if (!poller.isButtonPressed(ControllerButton.RIGHT.name)) running = false
          },
          onReleaseContinued = { poller, _ ->
            if (!poller.isButtonPressed(ControllerButton.RIGHT.name)) running = false
          })

  // right
  val right =
      ButtonActuator(
          onJustPressed = { _ ->
            GameLogger.debug(Megaman.TAG, "right controller button just pressed")
          },
          onPressContinued = { poller, delta ->
            if (damaged || poller.isButtonPressed(ConstKeys.LEFT)) return@ButtonActuator

            facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.LEFT else Facing.RIGHT
            if (isBehaviorActive(BehaviorType.CLIMBING)) return@ButtonActuator
            running = !isBehaviorActive(BehaviorType.WALL_SLIDING)

            val threshold =
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_RUN_SPEED
                else MegamanValues.RUN_SPEED) * ConstVals.PPM

            if (body.physics.velocity.x < threshold) {
              val impulseX =
                  if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
                  else MegamanValues.RUN_IMPULSE
              body.physics.velocity.x += impulseX * delta * ConstVals.PPM
            }
          },
          onJustReleased = { poller ->
            GameLogger.debug(Megaman.TAG, "right controller button just released")
            if (!poller.isButtonPressed(ControllerButton.LEFT.name)) running = false
          },
          onReleaseContinued = { poller, _ ->
            if (!poller.isButtonPressed(ControllerButton.LEFT.name)) running = false
          })

  // attack
  val attack =
      ButtonActuator(
          onPressContinued = { _, delta ->
            if (damaged) {
              stopCharging()
              return@ButtonActuator
            }

            if (!charging &&
                !weaponHandler.canFireWeapon(currentWeapon, MegaChargeStatus.HALF_CHARGED)) {
              stopCharging()
              return@ButtonActuator
            }

            if (charging &&
                !weaponHandler.canFireWeapon(currentWeapon, MegaChargeStatus.FULLY_CHARGED))
                return@ButtonActuator

            chargingTimer.update(delta)
          },
          onJustReleased = {
            if (damaged || !weaponHandler.canFireWeapon(currentWeapon, chargeStatus) || !shoot())
                requestToPlaySound(SoundAsset.ERROR_SOUND, false)

            stopCharging()
          },
          // TODO: onReleaseContinued = { _, _ -> stopCharging() }
      )

  // swap weapon
  val select =
      ButtonActuator(
          onJustPressed = {
            // TODO: implement this
            /*
            var x: Int = currWeapon.ordinal() + 1
            if (x >= MegamanWeapon.values().length) {
                x = 0
            }
            currWeapon = MegamanWeapon.values().get(x)
             */
          })

  return ControllerComponent(
      this,
      ControllerButton.LEFT.name to left,
      ControllerButton.RIGHT.name to right,
      ControllerButton.A.name to attack,
      ControllerButton.SELECT.name to select)
}
