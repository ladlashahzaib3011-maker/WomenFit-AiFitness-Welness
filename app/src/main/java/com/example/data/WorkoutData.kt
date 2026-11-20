package com.example.data

import com.example.R
import com.example.model.Exercise
import com.example.model.WorkoutCategory
import com.example.model.WorkoutPlan

object WorkoutData {
    val sampleWorkouts: List<WorkoutPlan> = listOf(
        WorkoutPlan(
            id = "w_beginner_01",
            title = "Gentle Morning Core & Posture",
            category = WorkoutCategory.BEGINNER,
            durationMinutes = 15,
            estimatedCalories = 120,
            difficulty = "Beginner",
            description = "Awaken deep core stabilizers, improve spinal alignment, and relieve neck & lower back tension.",
            bannerDrawableRes = R.drawable.img_hero_workout,
            exercises = listOf(
                Exercise(
                    id = "e_beg_01",
                    name = "Cat-Cow Spinal Waves",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "10 slow cycles",
                    targetMuscles = "Spine, Core & Shoulders",
                    instructions = "Inhale while arching back gently, lifting chest. Exhale while rounding spine toward ceiling and tucking chin.",
                    formTip = "Move with your breath; do not force cervical neck hyperextension."
                ),
                Exercise(
                    id = "e_beg_02",
                    name = "Bird-Dog Balance",
                    durationSeconds = 50,
                    restSeconds = 15,
                    repsOrHold = "12 alternating reps",
                    targetMuscles = "Lower Back, Glutes & Abs",
                    instructions = "Extend right arm forward and left leg straight back. Hold for 2 seconds, return and switch sides.",
                    formTip = "Keep hips level with the mat, resist rotating your pelvis."
                ),
                Exercise(
                    id = "e_beg_03",
                    name = "Deadbug Core Activation",
                    durationSeconds = 50,
                    restSeconds = 15,
                    repsOrHold = "14 reps total",
                    targetMuscles = "Transverse Abdominis",
                    instructions = "Lie on back with knees at 90 degrees. Lower opposite arm and heel toward ground without arching lower back.",
                    formTip = "Press your lower back firmly into the floor throughout."
                ),
                Exercise(
                    id = "e_beg_04",
                    name = "Glute Bridge Hold",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "Hold with pulses",
                    targetMuscles = "Gluteus Maximus & Hamstrings",
                    instructions = "Drive through heels to lift hips until knees, hips, and shoulders form a straight diagonal. Squeeze glutes at peak.",
                    formTip = "Avoid hyper-extending the lower lumbar spine; squeeze glutes directly."
                ),
                Exercise(
                    id = "e_beg_05",
                    name = "Child's Pose Breath Release",
                    durationSeconds = 45,
                    restSeconds = 10,
                    repsOrHold = "Relaxation hold",
                    targetMuscles = "Lats, Hips & Nervous System",
                    instructions = "Widen knees, sink hips back to heels, extend fingertips forward and relax forehead on floor.",
                    formTip = "Take deep diaphragmatic belly breaths to lower cortisol."
                )
            )
        ),
        WorkoutPlan(
            id = "w_weightloss_01",
            title = "Metabolic Low-Impact Fat Torch",
            category = WorkoutCategory.WEIGHT_LOSS,
            durationMinutes = 22,
            estimatedCalories = 240,
            difficulty = "Intermediate",
            description = "High-energy interval circuit that maximizes caloric burn without joint-pounding jumps.",
            bannerDrawableRes = R.drawable.img_hero_workout,
            exercises = listOf(
                Exercise(
                    id = "e_wl_01",
                    name = "Low-Impact Skater Taps",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "Continuous rhythm",
                    targetMuscles = "Glutes, Quads & Heart Rate",
                    instructions = "Step laterally to the right into a shallow squat, tapping left foot behind, then switch dynamically.",
                    formTip = "Keep chest tall and maintain a swift, steady cadence."
                ),
                Exercise(
                    id = "e_wl_02",
                    name = "Sumo Squat with Pulse",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "15 reps + pulses",
                    targetMuscles = "Inner Thighs & Glutes",
                    instructions = "Take a wide stance with toes turned 45 degrees out. Lower into squat, pulse twice at bottom, drive up.",
                    formTip = "Track knees directly in line with your second toe."
                ),
                Exercise(
                    id = "e_wl_03",
                    name = "Standing Oblique Knee-to-Elbow",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "20 reps alternating",
                    targetMuscles = "Obliques & Core",
                    instructions = "Hands behind ears. Bring right knee up sideways toward right elbow, crunching waistline. Alternate sides.",
                    formTip = "Focus on waist contraction rather than dragging your head down."
                ),
                Exercise(
                    id = "e_wl_04",
                    name = "Incline Mountain Climbers",
                    durationSeconds = 40,
                    restSeconds = 20,
                    repsOrHold = "Paced pace",
                    targetMuscles = "Shoulders, Full Core & Cardio",
                    instructions = "In a strong high plank, drive knees forward toward chest rhythmically.",
                    formTip = "Maintain shoulders directly over wrists and keep hips level."
                ),
                Exercise(
                    id = "e_wl_05",
                    name = "Plié Calf Raise Burnout",
                    durationSeconds = 45,
                    restSeconds = 10,
                    repsOrHold = "Continuous pulse",
                    targetMuscles = "Calves & Adductors",
                    instructions = "Hold low plié squat, lift heels off ground, lower slowly, and repeat without rising from squat.",
                    formTip = "Engage lower core to stay upright and balanced."
                )
            )
        ),
        WorkoutPlan(
            id = "w_yoga_01",
            title = "Hormone Harmony & Hip Release",
            category = WorkoutCategory.YOGA,
            durationMinutes = 20,
            estimatedCalories = 110,
            difficulty = "All Levels",
            description = "Soothe tension, regulate cortisol levels, and open the hips to release emotional and hormonal stress.",
            bannerDrawableRes = R.drawable.img_hero_workout,
            exercises = listOf(
                Exercise(
                    id = "e_yg_01",
                    name = "Goddess Pose Hip Opener",
                    durationSeconds = 60,
                    restSeconds = 15,
                    repsOrHold = "Hold with slow breath",
                    targetMuscles = "Pelvic Floor & Hips",
                    instructions = "Wide stance, turn toes out, bend knees deep, bring hands to heart center and focus on pelvic relaxation.",
                    formTip = "Lengthen tailbone downward; breathe smoothly."
                ),
                Exercise(
                    id = "e_yg_02",
                    name = "Lizard Pose Deep Hip Stretch",
                    durationSeconds = 60,
                    restSeconds = 15,
                    repsOrHold = "30s each side",
                    targetMuscles = "Hip Flexors & Groin",
                    instructions = "From low lunge, place hands inside front foot. Slowly ease down onto forearms or keep arms straight.",
                    formTip = "Do not force range; allow gravity and breath to open the hip."
                ),
                Exercise(
                    id = "e_yg_03",
                    name = "Supine Figure-4 Piriformis Release",
                    durationSeconds = 60,
                    restSeconds = 15,
                    repsOrHold = "30s each side",
                    targetMuscles = "Piriformis & Outer Glutes",
                    instructions = "Lie on back, cross right ankle over left knee, reach through and draw left hamstring toward chest.",
                    formTip = "Flex both feet to protect the knee joint."
                ),
                Exercise(
                    id = "e_yg_04",
                    name = "Reclined Butterfly (Supta Baddha Konasana)",
                    durationSeconds = 75,
                    restSeconds = 15,
                    repsOrHold = "Relaxation hold",
                    targetMuscles = "Pelvic Diaphragm & Nervous System",
                    instructions = "Lie flat, bring soles of feet together, letting knees fall open to sides. One hand on heart, one on belly.",
                    formTip = "Notice the chest and belly rise together on each deep inhale."
                )
            )
        ),
        WorkoutPlan(
            id = "w_cardio_01",
            title = "Dance Cardio Glow Sweat",
            category = WorkoutCategory.CARDIO,
            durationMinutes = 18,
            estimatedCalories = 210,
            difficulty = "All Levels",
            description = "Fun, upbeat rhythmic movement sequences that elevate mood, release endorphins, and burn calories.",
            bannerDrawableRes = R.drawable.img_hero_workout,
            exercises = listOf(
                Exercise(
                    id = "e_dc_01",
                    name = "Mambo Step & Hip Swivel",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "Dance cadence",
                    targetMuscles = "Full Body & Coordination",
                    instructions = "Step forward with right foot, shift weight, step back, and add an expressive arm sweep.",
                    formTip = "Stay light on the balls of your feet."
                ),
                Exercise(
                    id = "e_dc_02",
                    name = "Grapevine with Shoulder Shimmy",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "Rhythmic lateral steps",
                    targetMuscles = "Adductors & Shoulders",
                    instructions = "Step side, cross behind, step side, tap together with a light hop or shimmy. Reverse direction.",
                    formTip = "Keep core gently braced while having fun with the flow."
                ),
                Exercise(
                    id = "e_dc_03",
                    name = "Pony Bounce & Overhead Reach",
                    durationSeconds = 50,
                    restSeconds = 15,
                    repsOrHold = "Continuous pulse",
                    targetMuscles = "Calves & Upper Body",
                    instructions = "Bounce softly side-to-side, reaching arms overhead in diagonal sync with your hips.",
                    formTip = "Land soft on your knees to absorb impact."
                ),
                Exercise(
                    id = "e_dc_04",
                    name = "Salsa Knee Cross & Tap",
                    durationSeconds = 50,
                    restSeconds = 15,
                    repsOrHold = "Latin tempo",
                    targetMuscles = "Waistline & Glutes",
                    instructions = "Drive knee across opposite hip with a quick pivot and dynamic finger snap.",
                    formTip = "Engage obliques on the twist."
                )
            )
        ),
        WorkoutPlan(
            id = "w_strength_01",
            title = "Hourglass Glute & Waist Sculpt",
            category = WorkoutCategory.STRENGTH,
            durationMinutes = 25,
            estimatedCalories = 260,
            difficulty = "Intermediate",
            description = "Targeted hypertrophy for upper and lower glutes paired with core compression for hourglass definition.",
            bannerDrawableRes = R.drawable.img_hero_workout,
            exercises = listOf(
                Exercise(
                    id = "e_str_01",
                    name = "Curtsy Lunge to Lateral Kick",
                    durationSeconds = 50,
                    restSeconds = 15,
                    repsOrHold = "12 reps per leg",
                    targetMuscles = "Gluteus Medius & Quads",
                    instructions = "Step back diagonally behind lead leg, bend both knees into lunge, drive up and extend leg to side.",
                    formTip = "Keep hips square to the front wall during the descent."
                ),
                Exercise(
                    id = "e_str_02",
                    name = "Elevated Glute Bridge March",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "16 reps alternating",
                    targetMuscles = "Glute Max & Posterior Chain",
                    instructions = "Hold high glute bridge, march one knee to 90 degrees while keeping hips completely level, switch sides.",
                    formTip = "Squeeze the supporting glute intensely to prevent hip dipping."
                ),
                Exercise(
                    id = "e_str_03",
                    name = "Side Plank Hip Dips",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "10 reps each side",
                    targetMuscles = "Obliques & Quadratus Lumborum",
                    instructions = "In forearm side plank, dip bottom hip 2 inches down toward floor, then contract waist to lift higher than parallel.",
                    formTip = "Press through supporting shoulder elbow into floor."
                ),
                Exercise(
                    id = "e_str_04",
                    name = "Fire Hydrant with Circle",
                    durationSeconds = 50,
                    restSeconds = 15,
                    repsOrHold = "12 circles per side",
                    targetMuscles = "Glute Medius & Minimus",
                    instructions = "On all fours, lift knee out to side at 90 degrees, trace small controlled circular circles backward.",
                    formTip = "Resist shifting all your body weight into opposite arm."
                ),
                Exercise(
                    id = "e_str_05",
                    name = "Reverse Hyper Frog Pumps",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "20 pumps",
                    targetMuscles = "Upper Glute Shelf",
                    instructions = "Lie on stomach, heels together, knees wide. Contract glutes to lift knees 2 inches off mat.",
                    formTip = "Focus purely on glute contraction, not lower back extension."
                )
            )
        ),
        WorkoutPlan(
            id = "w_home_01",
            title = "15-Min Living Room Express",
            category = WorkoutCategory.HOME,
            durationMinutes = 15,
            estimatedCalories = 160,
            difficulty = "All Levels",
            description = "Quick, efficient full-body routine you can do in your living room with zero equipment.",
            bannerDrawableRes = R.drawable.img_hero_workout,
            exercises = listOf(
                Exercise(
                    id = "e_hm_01",
                    name = "Bodyweight Tempo Squats",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "15 controlled reps",
                    targetMuscles = "Quads, Hamstrings & Glutes",
                    instructions = "Lower slowly for 3 seconds, pause 1 second at parallel, power up cleanly through heels.",
                    formTip = "Keep chest proud and spine neutral."
                ),
                Exercise(
                    id = "e_hm_02",
                    name = "Inchworm to Knee Tap",
                    durationSeconds = 50,
                    restSeconds = 15,
                    repsOrHold = "8 repetitions",
                    targetMuscles = "Shoulders, Hamstrings & Core",
                    instructions = "Hinge forward from hips, walk hands out to high plank, tap right knee then left knee, walk hands back.",
                    formTip = "Keep legs as straight as comfortable to stretch hamstrings."
                ),
                Exercise(
                    id = "e_hm_03",
                    name = "Reverse Lunge to High Knee",
                    durationSeconds = 45,
                    restSeconds = 15,
                    repsOrHold = "10 reps each leg",
                    targetMuscles = "Glutes, Core & Balance",
                    instructions = "Step back into reverse lunge, drive front heel to stand while lifting back knee explosively to chest.",
                    formTip = "Engage deep core at top for balance."
                ),
                Exercise(
                    id = "e_hm_04",
                    name = "Superman Back Retractions",
                    durationSeconds = 45,
                    restSeconds = 10,
                    repsOrHold = "12 reps",
                    targetMuscles = "Upper Back, Lats & Glutes",
                    instructions = "Lie face down, lift chest and arms, draw elbows back like a row squeezing shoulder blades, release slowly.",
                    formTip = "Great posture reversal for desk fatigue."
                )
            )
        )
    )
}
