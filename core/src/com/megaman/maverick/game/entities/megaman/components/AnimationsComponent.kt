package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.gdxFilledArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.Megaman.Companion.TAG
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing
import kotlin.math.abs

internal fun Megaman.defineAnimationsComponent(): AnimationsComponent {
  // define key supplier
  val keySupplier = {
    var key =
        if (isUnderDamage()) "Damaged"
        else if (isBehaviorActive(BehaviorType.CLIMBING)) {
          if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) {
            if (shooting) "ClimbShoot"
            else if (fullyCharged) "FinishClimbCharging"
            else if (halfCharged) "FinishClimbHalfCharging" else "FinishClimb"
          } else if (body.physics.velocity.y != 0f) {
            if (shooting) "ClimbShoot"
            else if (fullyCharged) "ClimbCharging"
            else if (halfCharged) "ClimbHalfCharging" else "Climb"
          } else {
            if (shooting) "ClimbShoot"
            else if (fullyCharged) "StillClimbCharging"
            else if (halfCharged) "StillClimbHalfCharging" else "StillClimb"
          }
        } else if (isBehaviorActive(BehaviorType.AIR_DASHING)) {
          if (fullyCharged) "AirDashCharging"
          else if (halfCharged) "AirDashHalfCharging" else "AirDash"
        } else if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) {
          if (shooting) "GroundSlideShoot"
          else if (fullyCharged) "GroundSlideCharging"
          else if (halfCharged) "GroundSlideHalfCharging" else "GroundSlide"
        } else if (isBehaviorActive(BehaviorType.WALL_SLIDING)) {
          if (shooting) "WallSlideShoot"
          else if (fullyCharged) "WallSlideCharging"
          else if (halfCharged) "WallSlideHalfCharging" else "WallSlide"
        } else if (isBehaviorActive(BehaviorType.SWIMMING)) {
          if (shooting) "SwimShoot"
          else if (fullyCharged) "SwimCharging" else if (halfCharged) "SwimHalfCharging" else "Swim"
        } else if (isBehaviorActive(BehaviorType.JUMPING) ||
            !body.isSensing(BodySense.FEET_ON_GROUND)) {
          if (shooting) "JumpShoot"
          else if (fullyCharged) "JumpCharging" else if (halfCharged) "JumpHalfCharging" else "Jump"
        } else if (body.isSensing(BodySense.FEET_ON_GROUND) && running) {
          if (shooting) "RunShoot"
          else if (fullyCharged) "RunCharging" else if (halfCharged) "RunHalfCharging" else "Run"
        } else if (body.isSensing(BodySense.FEET_ON_GROUND) &&
            abs(body.physics.velocity.x) > ConstVals.PPM / 16f) {
          if (shooting) "SlipSlideShoot"
          else if (fullyCharged) "SlipSlideCharging"
          else if (halfCharged) "SlipSlideHalfCharging" else "SlipSlide"
        } else {
          if (shooting) "StandShoot"
          else if (fullyCharged) "StandCharging"
          else if (halfCharged) "StandHalfCharging" else "Stand"
        }
    if (maverick && facing == Facing.LEFT) key += "_Left"
    key += if (maverick) "_MegamanMaverick" else "_Megaman"
    key += "_${currentWeapon.name}"
    key
  }

  // animations map
  val animations = ObjectMap<String, IAnimation>()
  gdxArrayOf("Megaman", "MegamanMaverick").forEach { megamanType ->
    for (weapon in MegamanWeapon.values()) {
      val assetSource =
          if (megamanType == "Megaman")
              when (weapon) {
                MegamanWeapon.BUSTER -> TextureAsset.MEGAMAN_BUSTER.source
              // TODO: MegamanWeapon.FLAME_TOSS -> TextureAsset.MEGAMAN_FLAME_TOSS.source
              }
          else
              when (weapon) {
                MegamanWeapon.BUSTER -> TextureAsset.MEGAMAN_MAVERICK_BUSTER.source
              // TODO: MegamanWeapon.FLAME_TOSS -> ""
              }
      if (assetSource == "") continue
      val atlas = game.assMan.getTextureAtlas(assetSource)

      for (animationKey in animationKeys) {
        if (megamanType == "Megaman" && animationKey.contains("Left")) continue

        // get the animation definitio for the key
        val def = animationDefMap[animationKey]

        // create the modified key
        var _animationKey = animationKey
        _animationKey += "_${megamanType}"
        _animationKey += "_${weapon.name}"

        GameLogger.debug(
            TAG,
            "defineAnimationsComponent(): Putting animation \'${animationKey}\' with key \'${_animationKey}\'")

        // put the key animation pair into the map
        animations.put(
            _animationKey,
            Animation(atlas.findRegion(animationKey), def.rows, def.cols, def.durations))
      }
    }
  }

  val animator = Animator(keySupplier, animations)
  return AnimationsComponent(this, { sprites.get("player") }, animator)
}

private val animationKeys =
    gdxArrayOf(
        "Climb",
        "Climb_Left",
        "ClimbHalfCharging",
        "ClimbHalfCharging_Left",
        "ClimbCharging",
        "ClimbCharging_Left",
        "ClimbShoot",
        "ClimbShoot_Left",
        "StillClimb",
        "StillClimb_Left",
        "StillClimbCharging",
        "StillClimbCharging_Left",
        "StillClimbHalfCharging",
        "StillClimbHalfCharging_Left",
        "FinishClimb",
        "FinishClimb_Left",
        "FinishClimbCharging",
        "FinishClimbCharging_Left",
        "FinishClimbHalfCharging",
        "FinishClimbHalfCharging_Left",
        "Stand",
        "Stand_Left",
        "StandCharging",
        "StandCharging_Left",
        "StandHalfCharging",
        "StandHalfCharging_Left",
        "StandShoot",
        "StandShoot_Left",
        "Damaged",
        "Damaged_Left",
        "Run",
        "Run_Left",
        "RunCharging",
        "RunCharging_Left",
        "RunHalfCharging",
        "RunHalfCharging_Left",
        "RunShoot",
        "RunShoot_Left",
        "Jump",
        "Jump_Left",
        "JumpCharging",
        "JumpCharging_Left",
        "JumpHalfCharging",
        "JumpHalfCharging_Left",
        "JumpShoot",
        "JumpShoot_Left",
        "Swim",
        "Swim_Left",
        "SwimAttack",
        "SwimAttack_Left",
        "SwimCharging",
        "SwimCharging_Left",
        "SwimHalfCharging",
        "SwimHalfCharging_Left",
        "SwimShoot",
        "SwimShoot_Left",
        "WallSlide",
        "WallSlide_Left",
        "WallSlideCharging",
        "WallSlideCharging_Left",
        "WallSlideHalfCharging",
        "WallSlideHalfCharging_Left",
        "WallSlideShoot",
        "WallSlideShoot_Left",
        "GroundSlide",
        "GroundSlide_Left",
        "GroundSlideShoot",
        "GroundSlideShoot_Left",
        "GroundSlideCharging",
        "GroundSlideCharging_Left",
        "GroundSlideHalfCharging",
        "GroundSlideHalfCharging_Left",
        "AirDash",
        "AirDash_Left",
        "AirDashCharging",
        "AirDashCharging_Left",
        "AirDashHalfCharging",
        "AirDashHalfCharging_Left",
        "SlipSlide",
        "SlipSlide_Left",
        "SlipSlideCharging",
        "SlipSlideCharging_Left",
        "SlipSlideHalfCharging",
        "SlipSlideHalfCharging_Left",
        "SlipSlideShoot",
        "SlipSlideShoot_Left",
    )

internal data class AnimationDef(
    internal val rows: Int,
    internal val cols: Int,
    internal val durations: Array<Float>,
) {

  internal constructor(
      rows: Int = 1,
      cols: Int = 1,
      duration: Float = 1f,
  ) : this(rows, cols, gdxFilledArrayOf(rows * cols, duration))
}

private val animationDefMap =
    objectMapOf(
        "Climb" to AnimationDef(1, 2, .125f),
        "Climb_Left" to AnimationDef(1, 2, .125f),
        "ClimbShoot" to AnimationDef(1, 1, .125f),
        "ClimbShoot_Left" to AnimationDef(),
        "ClimbHalfCharging" to AnimationDef(1, 2, .125f),
        "ClimbHalfCharging_Left" to AnimationDef(1, 2, .125f),
        "ClimbCharging" to AnimationDef(1, 2, .125f),
        "ClimbCharging_Left" to AnimationDef(1, 2, .125f),
        "FinishClimb" to AnimationDef(),
        "FinishClimb_Left" to AnimationDef(),
        "FinishClimbCharging" to AnimationDef(1, 2, .15f),
        "FinishClimbCharging_Left" to AnimationDef(1, 2, .15f),
        "FinishClimbHalfCharging" to AnimationDef(1, 2, .15f),
        "FinishClimbHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "StillClimb" to AnimationDef(),
        "StillClimb_Left" to AnimationDef(),
        "StillClimbCharging" to AnimationDef(1, 2, .15f),
        "StillClimbCharging_Left" to AnimationDef(1, 2, .15f),
        "StillClimbHalfCharging" to AnimationDef(1, 2, .15f),
        "StillClimbHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "Stand" to AnimationDef(1, 2, gdxArrayOf(1.5f, .15f)),
        "Stand_Left" to AnimationDef(1, 2, gdxArrayOf(1.5f, .15f)),
        "StandCharging" to AnimationDef(1, 2, .15f),
        "StandCharging_Left" to AnimationDef(1, 2, .15f),
        "StandHalfCharging" to AnimationDef(1, 2, .15f),
        "StandHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "StandShoot" to AnimationDef(),
        "StandShoot_Left" to AnimationDef(),
        "Damaged" to AnimationDef(1, 5, .05f),
        "Damaged_Left" to AnimationDef(1, 5, .05f),
        "Run" to AnimationDef(1, 4, .125f),
        "Run_Left" to AnimationDef(1, 4, .125f),
        "RunCharging" to AnimationDef(1, 4, .125f),
        "RunCharging_Left" to AnimationDef(1, 4, .125f),
        "RunHalfCharging" to AnimationDef(1, 4, .125f),
        "RunHalfCharging_Left" to AnimationDef(1, 4, .125f),
        "RunShoot" to AnimationDef(1, 4, .125f),
        "RunShoot_Left" to AnimationDef(1, 4, .125f),
        "Jump" to AnimationDef(),
        "Jump_Left" to AnimationDef(),
        "JumpCharging" to AnimationDef(1, 2, .15f),
        "JumpCharging_Left" to AnimationDef(1, 2, .15f),
        "JumpHalfCharging" to AnimationDef(1, 2, .15f),
        "JumpHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "JumpShoot" to AnimationDef(),
        "JumpShoot_Left" to AnimationDef(),
        "Swim" to AnimationDef(),
        "Swim_Left" to AnimationDef(),
        "SwimAttack" to AnimationDef(),
        "SwimAttack_Left" to AnimationDef(),
        "SwimCharging" to AnimationDef(1, 2, .15f),
        "SwimCharging_Left" to AnimationDef(1, 2, .15f),
        "SwimHalfCharging" to AnimationDef(1, 2, .15f),
        "SwimHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "SwimShoot" to AnimationDef(),
        "SwimShoot_Left" to AnimationDef(),
        "WallSlide" to AnimationDef(),
        "WallSlide_Left" to AnimationDef(),
        "WallSlideCharging" to AnimationDef(1, 2, .15f),
        "WallSlideCharging_Left" to AnimationDef(1, 2, .15f),
        "WallSlideHalfCharging" to AnimationDef(1, 2, .15f),
        "WallSlideHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "WallSlideShoot" to AnimationDef(),
        "WallSlideShoot_Left" to AnimationDef(),
        "GroundSlide" to AnimationDef(),
        "GroundSlide_Left" to AnimationDef(),
        "GroundSlideShoot" to AnimationDef(),
        "GroundSlideShoot_Left" to AnimationDef(),
        "GroundSlideCharging" to AnimationDef(1, 2, .15f),
        "GroundSlideCharging_Left" to AnimationDef(1, 2, .15f),
        "GroundSlideHalfCharging" to AnimationDef(1, 2, .15f),
        "GroundSlideHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "AirDash" to AnimationDef(),
        "AirDash_Left" to AnimationDef(),
        "AirDashCharging" to AnimationDef(1, 2, .15f),
        "AirDashCharging_Left" to AnimationDef(1, 2, .15f),
        "AirDashHalfCharging" to AnimationDef(1, 2, .15f),
        "AirDashHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "SlipSlide" to AnimationDef(),
        "SlipSlide_Left" to AnimationDef(),
        "SlipSlideCharging" to AnimationDef(1, 2, .15f),
        "SlipSlideCharging_Left" to AnimationDef(1, 2, .15f),
        "SlipSlideHalfCharging" to AnimationDef(1, 2, .15f),
        "SlipSlideHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "SlipSlideShoot" to AnimationDef(),
        "SlipSlideShoot_Left" to AnimationDef(),
    )
