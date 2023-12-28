@file:Suppress("UNCHECKED_CAST")

package com.megaman.maverick.game.world

import com.engine.entities.contracts.IBodyEntity
import com.engine.world.Body
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.utils.VelocityAlteration

const val FIXTURE_EXTENSIONS_TAG = "FIXTURE_EXTENSIONS"

fun Fixture.setEntity(entity: IBodyEntity): Fixture {
  properties.put(ConstKeys.ENTITY, entity)
  return this
}

fun Fixture.getEntity() = properties.get(ConstKeys.ENTITY) as IBodyEntity

fun Fixture.depleteHealth(): Boolean {
  val entity = getEntity()
  if (entity !is IHealthEntity) return false

  entity.getHealthPoints().setToMin()
  return true
}

fun Fixture.getBody() = getEntity().body

fun Fixture.bodyHasType(type: BodyType) = getBody().isBodyType(type)

fun Fixture.bodyHasLabel(label: BodyLabel) = getBody().hasBodyLabel(label)

fun Fixture.bodyIsSensing(sense: BodySense) = getBody().isSensing(sense)

fun Fixture.setVelocityAlteration(alteration: (Body) -> VelocityAlteration): Fixture {
  properties.put(ConstKeys.VELOCITY_ALTERATION, alteration)
  return this
}

fun Fixture.getVelocityAlteration(alterableBody: Body) =
    (properties.get(ConstKeys.VELOCITY_ALTERATION) as (Body) -> VelocityAlteration).invoke(
        alterableBody)

fun Fixture.setRunnable(runnable: () -> Unit): Fixture {
  properties.put(ConstKeys.RUNNABLE, runnable)
  return this
}

fun Fixture.getRunnable() = properties.get(ConstKeys.RUNNABLE) as (() -> Unit)?
