package com.megaman.maverick.game.entities.megaman.components

import com.engine.common.time.Timer
import com.engine.damage.Damageable
import com.engine.damage.DamageableComponent
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues

internal fun Megaman.defineDamageableComponent(): DamageableComponent {
  val damageable = Damageable { damager ->
    // TODO: take damage
    false
  }

  return DamageableComponent(
      this,
      damageable,
      Timer(MegamanValues.DAMAGE_DURATION),
      Timer(MegamanValues.DAMAGE_RECOVERY_TIME))
}
