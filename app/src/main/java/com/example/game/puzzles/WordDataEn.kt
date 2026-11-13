package com.example.game.puzzles

/** English word pool (Code Breaker, Word Hunt, Word Scramble, Reverse Thinking). */
object WordDataEn {
    val CATEGORIES: Map<String, List<String>> = linkedMapOf(
        "Animals" to listOf(
            "CAT", "DOG", "LION", "TIGER", "GIRAFFE", "ELEPHANT", "RABBIT", "EAGLE", "OWL", "DOLPHIN",
            "WHALE", "PENGUIN", "KANGAROO", "FOX", "WOLF", "BEAR", "SQUIRREL", "TURTLE", "BUTTERFLY", "BEE",
            "ANT", "DUCK", "ROOSTER", "COW", "SHEEP", "GOAT", "DONKEY", "CAMEL", "MONKEY", "PARROT",
            "SNAKE", "CROCODILE", "OCTOPUS", "ZEBRA", "RHINO", "LEOPARD", "CHEETAH", "BEAVER", "SPARROW", "STORK",
            "SEAGULL", "DEER", "HEDGEHOG", "BAT", "HORSE"
        ),
        "Fruits & Vegetables" to listOf(
            "APPLE", "PEAR", "CHERRY", "STRAWBERRY", "BANANA", "MELON", "GRAPE", "ORANGE", "LEMON", "PEACH",
            "APRICOT", "PLUM", "FIG", "MANGO", "LIME", "KIWI", "PINEAPPLE", "TOMATO", "PEPPER", "POTATO",
            "ONION", "CARROT", "SPINACH", "LETTUCE", "PUMPKIN", "BEAN", "GARLIC", "PEA", "MUSHROOM", "RADISH",
            "CORN", "CABBAGE", "OLIVE", "BERRY", "CELERY"
        ),
        "Objects" to listOf(
            "TABLE", "CHAIR", "PENCIL", "NOTEBOOK", "BOOK", "CLOCK", "LAMP", "MIRROR", "CARPET", "CURTAIN",
            "PILLOW", "BLANKET", "GLASS", "PLATE", "SPOON", "FORK", "KNIFE", "POT", "UMBRELLA", "BAG",
            "KEY", "LOCK", "SCISSORS", "RULER", "ERASER", "SOFA", "PHONE", "COMPUTER", "CAMERA", "BROOM",
            "TORCH", "HAMMER", "BOTTLE", "WALLET", "CANDLE"
        ),
        "Nature" to listOf(
            "SEA", "MOUNTAIN", "RIVER", "LAKE", "FOREST", "DESERT", "ISLAND", "VALLEY", "WATERFALL", "CAVE",
            "RAIN", "SNOW", "WIND", "CLOUD", "SUN", "STAR", "RAINBOW", "SOIL", "ROCK", "SAND",
            "LEAF", "FLOWER", "TREE", "OCEAN", "VOLCANO", "GLACIER", "THUNDER", "STORM", "FOG", "WAVE",
            "HILL", "BAY", "MEADOW", "SPRING", "STREAM"
        ),
        "Jobs" to listOf(
            "DOCTOR", "TEACHER", "ENGINEER", "LAWYER", "NURSE", "PILOT", "DRIVER", "CHEF", "BAKER", "BARBER",
            "TAILOR", "ARCHITECT", "POLICE", "SOLDIER", "FARMER", "FISHER", "PAINTER", "WRITER", "POET", "DENTIST",
            "VET", "REPORTER", "CAPTAIN", "CARPENTER", "GARDENER", "JUDGE", "ACTOR", "DIVER", "PLUMBER", "SINGER",
            "DANCER", "BANKER", "COACH", "GUARD", "MINER"
        ),
        "Sports & Games" to listOf(
            "FOOTBALL", "BASKETBALL", "VOLLEYBALL", "TENNIS", "SWIMMING", "RUNNING", "CHESS", "WRESTLING", "BOXING", "ARCHERY",
            "SKIING", "SURFING", "CYCLING", "HANDBALL", "GOLF", "FENCING", "DARTS", "JUDO", "KARATE", "MARATHON",
            "ROWING", "BILLIARDS", "HOCKEY", "RUGBY", "BASEBALL"
        ),
        "Cities & Countries" to listOf(
            "LONDON", "PARIS", "ROME", "BERLIN", "MADRID", "TOKYO", "CAIRO", "SYDNEY", "TORONTO", "DUBLIN",
            "VIENNA", "PRAGUE", "ATHENS", "LISBON", "OSLO", "GERMANY", "FRANCE", "ITALY", "JAPAN", "EGYPT",
            "SPAIN", "CANADA", "BRAZIL", "MEXICO", "TURKEY", "INDIA", "CHINA", "KENYA", "PERU", "CHILE"
        ),
        "Ideas" to listOf(
            "LOVE", "HAPPINESS", "HOPE", "COURAGE", "PATIENCE", "SUCCESS", "TIME", "DREAM", "CURIOSITY", "FRIENDSHIP",
            "PEACE", "FREEDOM", "JUSTICE", "MEMORY", "LOGIC", "BRAIN", "THOUGHT", "KNOWLEDGE", "ENERGY", "FOCUS",
            "PRIDE", "WISDOM", "TRUST", "IDEA", "MIND"
        ),
        "Food" to listOf(
            "BREAD", "CHEESE", "HONEY", "JAM", "SOUP", "RICE", "PASTA", "PIZZA", "BURGER", "SALAD",
            "COOKIE", "CAKE", "TEA", "COFFEE", "BUTTER", "PANCAKE", "WAFFLE", "YOGURT", "SANDWICH", "NOODLE",
            "TACO", "CEREAL", "MUFFIN", "CHOCOLATE", "POPCORN"
        ),
        "Science & Space" to listOf(
            "PLANET", "MOON", "ROCKET", "ATOM", "MOLECULE", "GALAXY", "TELESCOPE", "MAGNET", "ELECTRIC", "LIGHT",
            "SOUND", "ROBOT", "LASER", "SPACE", "ASTRONAUT", "METEOR", "ORBIT", "BACTERIA", "CELL", "COMET",
            "MICROSCOPE", "FORMULA", "PROTON", "GRAVITY", "COSMOS"
        )
    )

    /** Filler letters roughly following English letter frequency. */
    const val FILLER = "EEEEEEAAAARRRIIIIOOOOTTTTNNNNSSSSLLLCCCUUDDPPMMHHGGBBFFYYWKVXZJQ"
}
