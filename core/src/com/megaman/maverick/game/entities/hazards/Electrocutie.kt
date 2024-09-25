package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import kotlin.math.roundToInt

class Electrocutie(game: MegamanMaverickGame) : MegaGameEntity(game), IHazard, IBodyEntity, IParentEntity {

    enum class ElectrocutieState { MOVE, CHARGE, SHOCK }

    companion object {
        const val TAG = "ElectrocutieParent"
        const val SPEED = 2f
        const val MOVE_DURATION = 1f
        const val CHARGE_DURATION = 0.75f
        const val SHOCK_DURATION = 0.5f
    }

    override var children = Array<GameEntity>()

    val currentState: ElectrocutieState
        get() = loop.getCurrent()

    private val loop = Loop(ElectrocutieState.values().toGdxArray())
    private val timers = objectMapOf(
        ElectrocutieState.MOVE to Timer(MOVE_DURATION),
        ElectrocutieState.CHARGE to Timer(CHARGE_DURATION),
        ElectrocutieState.SHOCK to Timer(SHOCK_DURATION)
    )
    private var vertical = true
    private var left = true
    private var minPosition = 0f
    private var maxPosition = 0f

    override fun getEntityType() = EntityType.HAZARD

    override fun getTag() = TAG

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        if (!children.isEmpty) throw IllegalStateException("Children array should be empty when spawning ElectrocutieParent")
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
        super.onSpawn(spawnProps)

        loop.reset()
        timers.values().forEach { it.reset() }

        left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        val min = spawnProps.get(ConstKeys.MIN, Float::class)!!
        val max = spawnProps.get(ConstKeys.MAX, Float::class)!!

        vertical = spawnProps.getOrDefault(ConstKeys.VERTICAL, true, Boolean::class)
        if (vertical) {
            minPosition = bounds.getCenter().x - (min * ConstVals.PPM)
            maxPosition = bounds.getCenter().x + (max * ConstVals.PPM)

            val bottomElectrocutieChildProps = props(
                ConstKeys.POSITION to bounds.getBottomCenterPoint(),
                ConstKeys.DIRECTION to Direction.UP,
                ConstKeys.PARENT to this
            )
            val bottomElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            bottomElectrocutieChild.spawn(bottomElectrocutieChildProps)
            children.add(bottomElectrocutieChild)

            val topElectrocutieProps = props(
                ConstKeys.POSITION to bounds.getTopCenterPoint(),
                ConstKeys.DIRECTION to Direction.DOWN,
                ConstKeys.PARENT to this
            )
            val topElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            topElectrocutieChild.spawn(topElectrocutieProps)
            children.add(topElectrocutieChild)

            val length = bounds.height.roundToInt() / ConstVals.PPM
            for (i in 0 until length) {
                val position = body.getBottomCenterPoint().add(0f, i * ConstVals.PPM.toFloat())
                val bolt = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.BOLT)!! as Bolt
                bolt.spawn(
                    props(
                        ConstKeys.POSITION to position,
                        ConstKeys.DIRECTION to Direction.UP,
                        ConstKeys.PARENT to this,
                        ConstKeys.CULL_OUT_OF_BOUNDS to false
                    )
                )
                children.add(bolt)
            }
        } else {
            minPosition = bounds.getCenter().y - (min * ConstVals.PPM)
            maxPosition = bounds.getCenter().y + (max * ConstVals.PPM)

            val bottomElectrocutieProps = props(
                ConstKeys.POSITION to bounds.getCenterLeftPoint(),
                ConstKeys.DIRECTION to Direction.RIGHT,
                ConstKeys.PARENT to this
            )
            val bottomElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            bottomElectrocutieChild.spawn(bottomElectrocutieProps)
            children.add(bottomElectrocutieChild)

            val topElectrocutieProps = props(
                ConstKeys.POSITION to bounds.getCenterRightPoint(),
                ConstKeys.DIRECTION to Direction.LEFT,
                ConstKeys.PARENT to this
            )
            val topElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            topElectrocutieChild.spawn(topElectrocutieProps)
            children.add(topElectrocutieChild)

            val length = bounds.width.roundToInt() / ConstVals.PPM
            for (i in 0 until length) {
                val position = body.getCenterLeftPoint().add(i * ConstVals.PPM.toFloat(), 0f)
                val bolt = Bolt(game)
                bolt.spawn(
                    props(
                        ConstKeys.POSITION to position,
                        ConstKeys.DIRECTION to Direction.RIGHT,
                        ConstKeys.PARENT to this,
                        ConstKeys.CULL_OUT_OF_BOUNDS to false
                    )
                )
                children.add(bolt)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        children.forEach { it.destroy() }
        children.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val currentPosition = if (vertical) body.getCenter().x else body.getCenter().y

        if (left && currentPosition <= minPosition) left = false
        else if (!left && currentPosition >= maxPosition) left = true

        val speed = (if (left) -SPEED else SPEED) * ConstVals.PPM
        body.physics.velocity = if (vertical) Vector2(speed, 0f)
        else Vector2(0f, speed)

        val currentState = loop.getCurrent()

        val timer = timers.get(currentState)
        timer.update(delta)
        if (timer.isFinished()) {
            timer.reset()
            loop.next()
        }

        val shock = currentState == ElectrocutieState.SHOCK
        children.forEach { child ->
            if (child is Bolt) {
                child.body.fixtures.forEach { childFixture -> (childFixture.second as Fixture).active = shock }
                child.sprites.values().forEach { childSprite -> childSprite.hidden = !shock }
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            children.forEach {
                if (it is IBodyEntity) {
                    if (vertical) it.body.setCenterX(body.getCenter().x) else it.body.setCenterY(body.getCenter().y)
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

}