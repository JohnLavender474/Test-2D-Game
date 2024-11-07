package com.megaman.maverick.game

import com.badlogic.gdx.math.Vector3

object PreferenceFiles {
    const val MEGAMAN_MAVERICK_SAVE_FILE = "Megaman Maverick - Save File"
    const val MEGAMAN_MAVERICK_CONTROLLER_PREFERENCES = "Megaman Maverick - Controller Preferences"
    const val MEGAMAN_MAVERICK_KEYBOARD_PREFERENCES = "Megaman Maverick - Keyboard Preferences"
}

object ConstVals {
    const val VIEW_WIDTH = 16f
    const val VIEW_HEIGHT = 12f
    const val PPM = 32
    const val FPS = 60
    const val FIXED_TIME_STEP = 1 / 150f
    const val STANDARD_RESISTANCE_X = 6f
    const val STANDARD_RESISTANCE_Y = 4f
    const val MAX_HEALTH = 30
    const val MIN_HEALTH = 0
    const val BOSS_DROP_DOWN_DURATION = 0.25f
    const val MEGAMAN_MAVERICK_FONT = "Megaman10Font.ttf"
    const val UI_ARROW_BLINK_DUR = 0.3f
    const val ROOM_TRANS_DELAY_DURATION = 0.35f
    const val ROOM_TRANS_DURATION = 1f
    const val HEALTH_BAR_X = 0.25f
    const val WEAPON_BAR_X = 0.75f
    const val STATS_BAR_Y = 7.5f
    const val STANDARD_MAX_STAT_BITS = 30
    const val STAT_BIT_WIDTH = 0.5f
    const val STAT_BIT_HEIGHT = 0.125f
    const val DEFAULT_PATHFINDING_MAX_ITERATIONS = 100
    const val DEFAULT_PATHFINDING_MAX_DISTANCE = 100
    const val DEFAULT_RETURN_BEST_PATH = true
    const val GAME_CAM_ROTATE_TIME = 1f
    const val DEFAULT_PARALLAX_X = 0.25f
    const val DEFAULT_PARALLAX_Y = 0f
}

object ConstKeys {
    const val NO = "no"
    const val ID = "id"
    const val WALL = "wall"
    const val A = "a"
    const val B = "b"
    const val SELECT = "select"
    const val COLLIDE = "collide"
    const val SCANNER = "scanner"
    const val BUTTON = "button"
    const val COMMAND = "command"
    const val RUN = "run"
    const val LISTENER = "listener"
    const val BEAM = "beam"
    const val SCREEN = "screen"
    const val SPRITE = "sprite"
    const val SUPPLIER = "supplier"
    const val DAMAGER = "damager"
    const val SHIELD = "shield"
    const val ARROW = "arrow"
    const val PRESSED = "pressed"
    const val DAMAGED = "damaged"
    const val AFTER = "after"
    const val OVERLAP = "overlap"
    const val NOW = "now"
    const val DIVISOR = "divisor"
    const val TAKE_FRICTION = "take_friction"
    const val SAND = "sand"
    const val ICE = "ice"
    const val ORB = "orb"
    const val VSYNC = "vsyinc"
    const val BULLET = "bullet"
    const val ALPHA = "alpha"
    const val FREQUENCY = "frequency"
    const val BLACK = "black"
    const val CAM = "cam"
    const val INVALID = "invalid"
    const val FPS = "fps"
    const val FAST = "fast"
    const val MEDIUM = "medium"
    const val LERP = "lerp"
    const val SENSE = "sense"
    const val NOT = "not"
    const val TAG = "tag"
    const val FORCE = "force"
    const val CLAMP = "clamp"
    const val HIT_WATER = "hit_water"
    const val WATER = "water"
    const val POOL = "pool"
    const val BALL = "ball"
    const val WAIT = "wait"
    const val DISTANCE = "distance"
    const val ITERATIONS = "iterations"
    const val HEURISTIC = "heuristic"
    const val DRAW = "draw"
    const val BODY_FILTER = "body_filter"
    const val FIXTURE_FILTER = "fixture_filter"
    const val ALLOW_OUT_OF_BOUNDS = "allow_out_of_bounds"
    const val TILED_MAP_LOAD_RESULT = "tiled_map_load_result"
    const val DECISION = "decision"
    const val HIT_BY_BODY = "hit_by_body"
    const val FOOT = "foot"
    const val HIT_BY_BLOCK = "hit_by_block"
    const val DAMAGEABLE = "damageable"
    const val SPIN = "spin"
    const val STICK_TO_BLOCK = "stick_to_block"
    const val FEET = "feet"
    const val CONDITIONAL = "conditional"
    const val DEBUG = "debug"
    const val EXPLOSION = "explosion"
    const val FACE = "face"
    const val CONTROLLER = "controller"
    const val SYSTEM = "system"
    const val VELOCITY = "velocity"
    const val ROOM_TRANSITION = "room_transition"
    const val BOSS_ROOM = "boss_room"
    const val RETURN = "return"
    const val SPOT = "spot"
    const val FRAME = "frame"
    const val ROLL = "roll"
    const val SHOOT = "shoot"
    const val RISE = "rise"
    const val EDIT = "edit"
    const val SHOW = "show"
    const val HIDE = "hide"
    const val FIRST = "first"
    const val AREA = "area"
    const val PROJECTILES = "projectiles"
    const val APPLY_SCALAR_TO_CHILDREN = "apply_scalar_to_children"
    const val HIT = "hit"
    const val BLOCK = "block"
    const val MOVEMENT = "movement"
    const val SCALAR = "scalar"
    const val RETREAT = "retreat"
    const val ATTACK = "attack"
    const val MOVE = "move"
    const val START = "start"
    const val HIT_BY_PROJECTILE = "hit_by_projectile"
    const val HIT_BY_PLAYER = "hit_by_player"
    const val DEATH_IMMUNE = "death_immune"
    const val ACTIVE = "active"
    const val BIG = "big"
    const val SMALL = "small"
    const val LOG = "log"
    const val LIFECYCLE = "lifecycle"
    const val DELAY = "delay"
    const val FALL = "fall"
    const val ELAPSE = "elapse"
    const val PRIORITY = "priority"
    const val SECTION = "section"
    const val PASSWORD = "password"
    const val LINES = "lines"
    const val CIRCLE = "circle"
    const val POLYGON = "polygon"
    const val MINI = "mini"
    const val INDEX = "index"
    const val JUMP = "jump"
    const val DECORATIONS = "decorations"
    const val FILTER = "filter"
    const val ENTTIY_KILLED_BY_DEATH_FIXTURE = "entity_killed_by_death_fixture"
    const val DEATH_LISTENER = "death_listener"
    const val GREEN = "green"
    const val ORANGE = "orange"
    const val GRAVITY_CHANGEABLE = "gravity_changeable"
    const val INTERVAL = "interval"
    const val MIDDLE = "middle"
    const val SCALE = "scale"
    const val ANGLE = "angle"
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
    const val ON_TELEPORT_START = "on_teleport_start"
    const val ON_TELEPORT_CONTINUE = "on_teleport_continue"
    const val ON_TELEPORT_END = "on_teleport_end"
    const val COLOR = "color"
    const val DRAW_LINE = "draw_line"
    const val CHILD_KEY = "child_key"
    const val SPEED = "speed"
    const val DEFINITION = "definition"
    const val PARALLAX = "parallax"
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
    const val MAX_X = "max_x"
    const val MAX_Y = "max_y"
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
    const val SPAWN = "spawn"
    const val SPAWNER = "spawner"
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
    const val WORLD_CONTAINER = "world_graph_map"
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
