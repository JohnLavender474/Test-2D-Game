package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory

class FloatingCanHole(game: MegamanMaverickGame) : MegaGameEntity(game), IHazard, IParentEntity {

    companion object {
        const val TAG = "FloatingCanHole"
        private const val SPAWN_DELAY = 2.5f
        private const val DEFAULT_MAX_SPAWNED = 3
    }

    override var children = Array<IGameEntity>()

    private val spawnDelayTimer = Timer(SPAWN_DELAY)
    private lateinit var spawn: Vector2
    private var maxToSpawn = DEFAULT_MAX_SPAWNED

    override fun init() {
        addComponent(defineUpdatablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        maxToSpawn = spawnProps.getOrDefault(ConstKeys.MAX, DEFAULT_MAX_SPAWNED, Int::class)
        spawnDelayTimer.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        children.forEach { it.kill() }
        children.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        val iter = children.iterator()
        while (iter.hasNext()) {
            val child = iter.next()
            if (child.dead) iter.remove()
        }
        if (children.size < maxToSpawn) {
            spawnDelayTimer.update(delta)
            if (spawnDelayTimer.isFinished()) {
                val floatingCan = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.FLOATING_CAN)!!
                game.engine.spawn(floatingCan, props(ConstKeys.POSITION to spawn))
                children.add(floatingCan)
                spawnDelayTimer.reset()
            }
        }
    })

}