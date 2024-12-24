package com.megaman.maverick.game.entities.megaman.sprites

import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

fun Megaman.getAnimationKey(priorAnimKey: String?) = when {
    !roomTransPauseTimer.isFinished() -> null

    priorAnimKey != null && game.isProperty(ConstKeys.ROOM_TRANSITION, true) -> {
        var key = priorAnimKey
        if (key.contains("stand")) key = key.replace("stand", "run")
        key
    }

    !ready -> "spawn"

    game.isCameraRotating() -> amendKey("jump")

    else -> when {
        isBehaviorActive(BehaviorType.JETPACKING) -> amendKey("jetpack")
        isBehaviorActive(BehaviorType.RIDING_CART) -> {
            when {
                damaged -> "cartin_damaged"
                isBehaviorActive(BehaviorType.JUMPING) || !body.isSensing(BodySense.FEET_ON_GROUND) ->
                    amendKey("cartin_jump")
                else -> amendKey("cartin")
            }
        }

        damaged || stunned -> "damaged"

        isBehaviorActive(BehaviorType.CLIMBING) -> when {
            !body.isSensing(BodySense.HEAD_TOUCHING_LADDER) && !shooting -> amendKey("climb_finish")
            else -> {
                val movement = if (direction.isHorizontal()) body.physics.velocity.x else body.physics.velocity.y
                val key = if (movement != 0f) "climb" else "climb_still"
                amendKey(key)
            }
        }

        isBehaviorActive(BehaviorType.AIR_DASHING) -> amendKey("airdash")
        isBehaviorActive(BehaviorType.CROUCHING) -> amendKey("crouch")
        isBehaviorActive(BehaviorType.GROUND_SLIDING) -> amendKey("groundslide")
        isBehaviorActive(BehaviorType.WALL_SLIDING) -> amendKey("wallslide")
        isBehaviorActive(BehaviorType.SWIMMING) -> amendKey("swim")
        body.isSensing(BodySense.FEET_ON_GROUND) && running -> amendKey("run")
        isBehaviorActive(BehaviorType.JUMPING) || !body.isSensing(BodySense.FEET_ON_GROUND) -> amendKey("jump")
        slipSliding -> amendKey("slip")
        else -> amendKey("stand")
    }
}

private fun Megaman.amendKey(baseKey: String) = when {
    shooting -> "${baseKey}_shoot"
    fullyCharged -> "${baseKey}_charge_full"
    halfCharged -> "${baseKey}_charge_half"
    else -> baseKey

}

