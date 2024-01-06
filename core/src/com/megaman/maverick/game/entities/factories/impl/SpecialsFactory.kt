package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.special.Ladder
import com.megaman.maverick.game.entities.special.SpringBouncer
import com.megaman.maverick.game.entities.special.GravityChange
import com.megaman.maverick.game.entities.special.Water

class SpecialsFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

  companion object {
    const val WATER = "Water"
    const val LADDER = "Ladder"
    const val GRAVITY_CHANGE = "GravityChange"
    const val SPRING_BOUNCER = "SpringBouncer"
  }

  private val pools = ObjectMap<Any, Pool<IGameEntity>>()

  init {
    pools.put(WATER, EntityPoolCreator.create(3) { Water(game) })
    pools.put(SPRING_BOUNCER, EntityPoolCreator.create(2) { SpringBouncer(game) })
    pools.put(LADDER, EntityPoolCreator.create(10) { Ladder(game) })
    pools.put(GRAVITY_CHANGE, EntityPoolCreator.create(10) { GravityChange(game) })
  }

  override fun fetch(key: Any) = pools.get(key)?.fetch()
}
