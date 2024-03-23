package com.megaman.maverick.game

import com.badlogic.gdx.math.Vector3

object ConstVals {
    const val VIEW_WIDTH = 16f
    const val VIEW_HEIGHT = 12f
    const val PPM = 32
    const val FIXED_TIME_STEP = 1 / 150f
    const val STANDARD_RESISTANCE_X = 1.035f
    const val STANDARD_RESISTANCE_Y = 1.025f
    const val MAX_HEALTH = 30
    const val MIN_HEALTH = 0
    const val BOSS_DROP_DOWN_DURATION = 0.25f
    const val MEGAMAN_MAVERICK_FONT = "Megaman10Font.ttf"
}

object ConstKeys {
    const val HEAD = "head"
    const val ON_DAMAGE_INFLICTED_TO = "on_damage_inflicted_to"
    const val DROP_ITEM_ON_DEATH = "drop_item_on_death"
    const val BLOCK_FILTERS = "block_filters"
    const val CLOSE = "close"
    const val FIXTURE_LABELS = "fixture_labels"
    const val FRONT = "front"
    const val BACK = "back"
    const val FIXTURES = "fixtures"
    const val BODY = "body"
    const val TANK = "tank"
    const val SLOW = "slow"
    const val PROPS = "props"
    const val OBJECT = "object"
    const val MUSIC = "music"
    const val INSTANT = "instant"
    const val BOUNDS_SUPPLIER = "bounds_supplier"
    const val POSITION_SUPPLIER = "position_supplier"
    const val CULL_TIME = "cull_time"
    const val ENEMY_SPAWN = "enemy_spawn"
    const val CULL_EVENTS = "cull_events"
    const val CULL_OUT_OF_BOUNDS = "cull_out_of_bounds"
    const val TARGET = "target"
    const val RADIANCE = "radiance"
    const val CENTER = "center"
    const val RADIUS = "radius"
    const val LIGHT = "light"
    const val KEYS = "keys"
    const val HIDDEN = "hidden"
    const val SOUND = "sound"
    const val THROWN = "thrown"
    const val TRIGGER = "trigger"
    const val FLIP = "flip"
    const val NEXT = "next"
    const val STOP = "stop"
    const val ON_PORTAL_HOPPER_START = "on_portal_start"
    const val ON_PORTAL_HOPPER_CONTINUE = "on_portal_continue"
    const val ON_PORTAL_HOPPER_END = "on_portal_end"
    const val COLOR = "color"
    const val DRAW_LINE = "draw_line"
    const val CHILD_KEY = "child_key"
    const val SPEED = "speed"
    const val DEFINITION = "definition"
    const val BACKGROUND_PARALLAX_FACTOR = "background_parallax_factor"
    const val FOREGROUND_PARALLAX_FACTOR = "foreground_parallax_factor"
    const val FOREGROUND = "foreground"
    const val BACKGROUND = "background"
    const val ENTITY_TYPE = "entity_type"
    const val CULL = "cull"
    const val PAIR = "pair"
    const val ON = "on"
    const val OFF = "off"
    const val TEXT = "text"
    const val MIN = "min"
    const val MAX = "max"
    const val TOP = "top"
    const val VERTICAL = "vertical"
    const val LENGTH = "length"
    const val DEFAULT = "default"
    const val CART = "cart"
    const val LINE = "line"
    const val PASS_THROUGH = "pass_through"
    const val DELTA = "delta"
    const val SIZE = "size"
    const val DEATH = "death"
    const val IMPULSE = "impulse"
    const val RUN_ON_SPAWN = "run_on_spawn"
    const val ANIMATION = "animation"
    const val DURATION = "duration"
    const val NAME = "name"
    const val KEY = "key"
    const val FACING = "facing"
    const val FORM = "form"
    const val ROWS = "rows"
    const val COLUMNS = "columns"
    const val RESET = "reset"
    const val SUCCESS = "success"
    const val END = "end"
    const val SPLASH = "splash"
    const val BLOCK_ON = "block_on"
    const val ORIENTATION = "orientation"
    const val OFFSET_X = "offset_x"
    const val OFFSET_Y = "offset_y"
    const val WIDTH = "width"
    const val HEIGHT = "height"
    const val CHILD = "child"
    const val PARENT = "parent"
    const val UPSIDE_DOWN = "upside_down"
    const val GRAVITY_ROTATABLE = "gravity_rotatable"
    const val HAZARDS = "hazards"
    const val PPM = "ppm"
    const val GRAVITY = "gravity"
    const val SENSORS = "sensors"
    const val RESPAWNABLE = "respawnable"
    const val X = "x"
    const val Y = "y"
    const val LARGE = "large"
    const val TIMED = "timed"
    const val PENDULUM = "pendulum"
    const val ROTATION = "rotation"
    const val PERSIST = "persist"
    const val VALUE = "value"
    const val DIRECTION = "direction"
    const val MASK = "mask"
    const val DISPOSABLES = "disposables"
    const val EVENTS = "events"
    const val EVENT = "event"
    const val SPAWN_TYPE = "spawn_type"
    const val SPAWNERS = "spawners"
    const val READY = "ready"
    const val TYPE = "type"
    const val ARRAY = "array"
    const val COLLECTION = "collection"
    const val RUNNABLE = "runnable"
    const val ENTITY = "entity"
    const val CONSUMER = "consumer"
    const val OWNER = "owner"
    const val TRAJECTORY = "trajectory"
    const val BOOLEAN = "boolean"
    const val BODY_LABELS = "body_labels"
    const val SPAWNS = "spawns"
    const val ROOM = "room"
    const val BOSS = "boss"
    const val ATLAS = "atlas"
    const val REGION = "region"
    const val GAME = "game"
    const val UI = "ui"
    const val SYSTEMS = "systems"
    const val UP = "up"
    const val DOWN = "down"
    const val LEFT = "left"
    const val RIGHT = "right"
    const val SPECIALS = "specials"
    const val BACKGROUNDS = "backgrounds"
    const val POSITION = "position"
    const val CURRENT = "current"
    const val PRIOR = "prior"
    const val WORLD_GRAPH_MAP = "world_graph_map"
    const val DRAWABLES = "drawables"
    const val SHAPES = "shapes"
    const val PLAYER = "player"
    const val ENEMIES = "enemies"
    const val ITEMS = "items"
    const val BLOCKS = "blocks"
    const val TRIGGERS = "triggers"
    const val FOREGROUNDS = "foregrounds"
    const val GAME_ROOMS = "game_rooms"
    const val BOUNDS = "bounds"
    const val RESIST_ON = "resist_on"
    const val GRAVITY_ON = "gravity_on"
    const val FRICTION_X = "friction_x"
    const val FRICTION_Y = "friction_y"
    const val BOUNCE = "force"
    const val SIDE = "side"
    const val RUNNING = "running"
    const val VELOCITY_ALTERATION = "velocity_alteration"
    const val LADDER = "ladder"
    const val HEALTH = "health"
}

object ConstFuncs {

    fun getCamInitPos(): Vector3 {
        val v = Vector3()
        v.x = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f
        v.y = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
        return v
    }
}
