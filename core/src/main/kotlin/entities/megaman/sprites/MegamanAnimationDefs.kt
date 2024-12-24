package com.megaman.maverick.game.entities.megaman.sprites

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.pairTo
import com.megaman.maverick.game.animations.AnimationDef

object MegamanAnimationDefs {

    const val JETPACK_FRAME_DUR = 0.1f
    const val DEFAULT_FRAME_DUR = 0.125f
    const val CHARGING_FRAME_DUR = 0.0625f
    const val CROUCH_FRAME_DUR = 0.05f

    private val defs = orderedMapOf(
        "jetpack" pairTo AnimationDef(1, 2, JETPACK_FRAME_DUR),
        "jetpack_shoot" pairTo AnimationDef(1, 2, JETPACK_FRAME_DUR),

        "cartin" pairTo AnimationDef(1, 2, gdxArrayOf(1.5f, 0.15f)),
        "cartin_damaged" pairTo AnimationDef(1, 3, 0.05f),
        "cartin_charge_full" pairTo AnimationDef(1, 2, CHARGING_FRAME_DUR),
        "cartin_charge_half" pairTo AnimationDef(1, 2, CHARGING_FRAME_DUR),
        "cartin_shoot" pairTo AnimationDef(),
        "cartin_jump" pairTo AnimationDef(),
        "cartin_jump_charge_half" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "cartin_jump_charge_full" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "cartin_jump_shoot" pairTo AnimationDef(),

        "climb" pairTo AnimationDef(2, 1, DEFAULT_FRAME_DUR),
        "climb_left" pairTo AnimationDef(2, 1, DEFAULT_FRAME_DUR),
        "climb_shoot" pairTo AnimationDef(),
        "climb_shoot_left" pairTo AnimationDef(),
        "climb_charge_half" pairTo AnimationDef(3, 2, DEFAULT_FRAME_DUR),
        "climb_charge_half_left" pairTo AnimationDef(3, 2, DEFAULT_FRAME_DUR),
        "climb_charge_full" pairTo AnimationDef(2, 2, CHARGING_FRAME_DUR),
        "climb_charge_full_left" pairTo AnimationDef(2, 2, CHARGING_FRAME_DUR),

        "climb_finish" pairTo AnimationDef(),
        "climb_finish_left" pairTo AnimationDef(),
        "climb_finish_charge_full" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "climb_finish_charge_full_left" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "climb_finish_charge_half" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "climb_finish_charge_half_left" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),

        "climb_still" pairTo AnimationDef(),
        "climb_still_left" pairTo AnimationDef(),
        "climb_still_shoot" pairTo AnimationDef(),
        "climb_still_charge_full" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "climb_still_charge_full_left" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "climb_still_charge_half" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "climb_still_charge_half_left" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),

        "stand" pairTo AnimationDef(2, 1, gdxArrayOf(1.5f, 0.15f)),
        "stand_left" pairTo AnimationDef(2, 1, gdxArrayOf(1.5f, 0.15f)),
        "stand_charge_full" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "stand_charge_full_left" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "stand_charge_half" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "stand_charge_half_left" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),

        "stand_shoot" pairTo AnimationDef(),
        "stand_shoot_left" pairTo AnimationDef(),

        "damaged" pairTo AnimationDef(3, 1, 0.05f),
        "damaged_left" pairTo AnimationDef(3, 1, 0.05f),

        "run" pairTo AnimationDef(2, 2, DEFAULT_FRAME_DUR),
        "run_left" pairTo AnimationDef(2, 2, DEFAULT_FRAME_DUR),
        "run_charge_full" pairTo AnimationDef(2, 4, CHARGING_FRAME_DUR),
        "run_charge_full_left" pairTo AnimationDef(2, 4, CHARGING_FRAME_DUR),
        "run_charge_half" pairTo AnimationDef(2, 2, DEFAULT_FRAME_DUR),
        "run_charge_half_left" pairTo AnimationDef(2, 2, DEFAULT_FRAME_DUR),
        "run_shoot" pairTo AnimationDef(2, 2, DEFAULT_FRAME_DUR),
        "run_shoot_left" pairTo AnimationDef(2, 2, DEFAULT_FRAME_DUR),

        "jump" pairTo AnimationDef(),
        "jump_left" pairTo AnimationDef(),
        "jump_charge_full" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "jump_charge_full_left" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "jump_charge_half" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "jump_charge_half_left" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "jump_shoot" pairTo AnimationDef(),
        "jump_shoot_left" pairTo AnimationDef(),

        "swim" pairTo AnimationDef(),
        "swim_left" pairTo AnimationDef(),
        "swim_attack" pairTo AnimationDef(),
        "swim_attack_left" pairTo AnimationDef(),
        "swim_charge_full" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "swim_charge_full_left" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "swim_charge_half" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "swim_charge_half_left" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "swim_shoot" pairTo AnimationDef(),
        "swim_shoot_left" pairTo AnimationDef(),

        "wallslide" pairTo AnimationDef(),
        "wallslide_left" pairTo AnimationDef(),
        "wallslide_charge_full" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "wallslide_charge_full_left" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "wallslide_charge_half" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "wallslide_charge_half_left" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "wallslide_shoot" pairTo AnimationDef(),
        "wallslide_shoot_left" pairTo AnimationDef(),

        "groundslide" pairTo AnimationDef(),
        "groundslide_left" pairTo AnimationDef(),
        "groundslide_shoot" pairTo AnimationDef(),
        "groundslide_shoot_left" pairTo AnimationDef(),
        "groundslide_charge_full" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "groundslide_charge_full_left" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "groundslide_charge_half" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "groundslide_charge_half_left" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),

        "airdash" pairTo AnimationDef(),
        "airdash_left" pairTo AnimationDef(),
        "airdash_shoot" pairTo AnimationDef(),
        "airdash_charge_full" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "airdash_charge_full_left" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "airdash_charge_half" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "airdash_charge_half_left" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),

        "crouch" pairTo AnimationDef(),
        "crouch_shoot" pairTo AnimationDef(),
        "crouch_charge_half" pairTo AnimationDef(3, 1, CROUCH_FRAME_DUR, false),
        "crouch_charge_full" pairTo AnimationDef(2, 1, CROUCH_FRAME_DUR, false),

        "slip" pairTo AnimationDef(),
        "slip_left" pairTo AnimationDef(),
        "slip_charge_full" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "slip_charge_full_left" pairTo AnimationDef(2, 1, CHARGING_FRAME_DUR),
        "slip_charge_half" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "slip_charge_half_left" pairTo AnimationDef(3, 1, CHARGING_FRAME_DUR),
        "slip_shoot" pairTo AnimationDef(),
        "slip_shoot_left" pairTo AnimationDef(),
    )

    fun get() = defs

    fun get(key: String): AnimationDef = get().get(key)

    fun getKeys(): ObjectMap.Keys<String> = get().keys()
}
