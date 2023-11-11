package com.megaman.maverick.game.entities.sensors

import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IDamageableEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.FixtureType

/**
 * A death sensor that kills every [IDamageableEntity] whose damageable fixture comes into contact
 * with it.
 */
class Death(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity {

  override fun init() = addComponent(defineBodyComponent())

  override fun spawn(spawnProps: Properties) {
    val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
    body.set(bounds)
  }

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)

    // death fixture
    val deathFixture = Fixture(body, FixtureType.DEATH)
    body.addFixture(deathFixture)

    return BodyComponent(this, body)
  }
}
