package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.math.Vector2
import com.engine.audio.AudioComponent
import com.engine.behaviors.Behavior
import com.engine.behaviors.BehaviorsComponent
import com.engine.common.enums.Facing
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing
import com.megaman.maverick.game.world.isSensingAny

internal fun Megaman.defineBehaviorsComponent(): BehaviorsComponent {
  val behaviorsComponent = BehaviorsComponent(this)

  // jump
  val jump =
      Behavior(
          // evaluate
          evaluate = { _ ->
            if (isAnyBehaviorActive(BehaviorType.SWIMMING, BehaviorType.CLIMBING)) {
              return@Behavior false
            }
            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) {
              return@Behavior false
            }

            val controllerPoller = game.controllerPoller
            if (controllerPoller.isButtonPressed(
                if (upsideDown) ConstKeys.UP else ConstKeys.DOWN)) {
              return@Behavior false
            }

            if (isBehaviorActive(BehaviorType.JUMPING)) {
              val velocity = body.physics.velocity
              return@Behavior if (upsideDown) velocity.y <= 0f else velocity.y >= 0f
            }

            return@Behavior aButtonTask == AButtonTask.JUMP &&
                controllerPoller.isButtonPressed(ConstKeys.A) &&
                body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.FEET_ON_GROUND)
          },
          // init
          init = {
            val v = Vector2()

            v.x =
                if (isBehaviorActive(BehaviorType.WALL_SLIDING)) {
                  var x = wallJumpVel * ConstVals.PPM
                  if (facing == Facing.LEFT) x *= -1
                  x
                } else {
                  body.physics.velocity.x
                }

            v.y =
                if (body.isSensing(BodySense.IN_WATER)) {
                  if (isBehaviorActive(BehaviorType.WALL_SLIDING)) wallJumpVel * ConstVals.PPM
                  else waterWallJumpVel * ConstVals.PPM
                } else {
                  if (isBehaviorActive(BehaviorType.WALL_SLIDING)) wallJumpVel * ConstVals.PPM
                  else jumpVel * ConstVals.PPM
                }

            body.physics.velocity.set(v)

            if (isBehaviorActive(BehaviorType.WALL_SLIDING)) {
              getComponent(AudioComponent::class)?.requestToPlaySound("wall_jump", false)
            }
          },
          // end
          end = { body.physics.velocity.y = 0f })

  behaviorsComponent.addBehavior(BehaviorType.JUMPING, jump)
  return behaviorsComponent
}
