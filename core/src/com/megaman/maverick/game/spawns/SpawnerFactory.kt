package com.megaman.maverick.game.spawns

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.GameLogger
import com.engine.common.shapes.IGameShape2D
import com.engine.spawns.Spawn
import com.engine.spawns.SpawnerForBoundsEntered
import com.engine.spawns.SpawnerForEvent
import com.megaman.maverick.game.utils.toGameRectangle

object SpawnerFactory {

    const val TAG = "SpawnerFactory"

    fun spawnerForWhenInCamera(
        camera: Camera,
        spawnShape: IGameShape2D,
        spawnSupplier: () -> Spawn,
        respawnable: Boolean = true
    ): SpawnerForBoundsEntered {
        GameLogger.debug(TAG, "spawnerForWhenEnteringCamera(): Creating spawner for camera: $camera")
        return SpawnerForBoundsEntered(
            spawnSupplier, { spawnShape }, { camera.toGameRectangle() }, respawnable = respawnable
        )
    }

    fun spawnerForWhenEventCalled(
        events: ObjectSet<Any>,
        spawnSupplier: () -> Spawn,
        respawnable: Boolean = true
    ): SpawnerForEvent {
        GameLogger.debug(TAG, "spawnerForWhenEventCalled(): Creating spawner for events: $events")
        return SpawnerForEvent(
            { events.contains(it.key) },
            spawnSupplier,
            eventKeyMask = events,
            respawnable = respawnable
        )
    }
}
