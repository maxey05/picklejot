package com.maxeydev.picklejot.ui.common.duck

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

private const val CREAM_STOP = 0.35f

private fun parse(data: String): Path = PathParser().parsePathString(data).toPath()

private fun creamBrush(
    top: Float,
    bottom: Float,
): Brush =
    Brush.verticalGradient(
        CREAM_STOP to DuckColors.cream,
        1f to DuckColors.creamShade,
        startY = top,
        endY = bottom,
    )

private const val SHELL_ZIGZAG =
    "M45.6 130 L57 120 L68 130 L80 119 L92 130 L104 119 L116 130 L128 119 L140 130 L151 120 L154.4 130"

internal object DuckPaths {
    val bodyBrush = creamBrush(48f, 206f)
    val headBrush = creamBrush(38f, 170f)
    val hatchBrush = creamBrush(56f, 136f)

    val body =
        parse(
            "M110 48 C146 48 164 74 166 112 C168 140 182 168 172 186 C162 204 132 206 110 206 " +
                "C88 206 58 204 48 186 C38 168 52 140 54 112 C56 74 74 48 110 48 Z",
        )
    val tuft = parse("M100 54 C96 42 99 32 107 27 C106 36 109 42 113 46 C115 38 121 33 129 33 C123 39 121 46 120 54 Z")
    val band = parse("M60 82 L160 82 L164.8 100 L55.2 100 Z")
    val bandEdges = parse("M60 82 L160 82 M55.2 100 L164.8 100")
    val highlight = parse("M68 152 Q72 180 100 188")
    val wingLeft = parse("M58 134 C44 124 30 132 33 144 C36 154 50 154 58 150 Z")
    val joyBody = parse("M87 114 Q94 103 101 114 M119 114 Q126 103 133 114")
    val mouthReady = parse("M99 127 Q110 150 121 127 Z")
    val mouthSmash = parse("M98 127 Q110 156 122 127 Z")
    val mouthBuddy = parse("M100 127 Q110 146 120 127 Z")

    val readyGrip = parse("M172.5 132 L180.5 130 M172.5 139 L180.5 137 M172.5 146 L180.5 144")
    val readyPaddleHighlight = parse("M166 96 Q165 66 182 58")
    val paddleWing = parse("M150 150 C148 138 166 132 176 140 C186 148 180 162 166 163 C156 164 151 158 150 150 Z")
    val paddleMark = parse("M165 143 Q173 145 173 153")

    val smashWrist = parse("M162 140 C176 132 192 138 190 150 C188 160 172 160 162 154 Z")
    val smashTailUpper = parse("M160 86 C174 76 188 72 200 64 C196 78 184 88 164 96 Z")
    val smashTailLower = parse("M160 94 C176 96 190 102 198 114 C184 112 172 108 160 102 Z")
    val smashGrip = parse("M52 104 L60 104 M52 111 L60 111 M52 118 L60 118")
    val smashPaddleHighlight = parse("M45 72 Q44 42 61 34")
    val smashHugWing = parse("M62 138 C50 142 40 134 43 123 C46 112 62 113 67 124 Z")
    val smashSpeedOuter = parse("M92 30 Q146 2 204 30")
    val smashSpeedInner = parse("M112 44 Q150 26 190 42")
    val smashBallLines = parse("M40 132 L52 128 M42 145 L56 146 M38 158 L50 162")

    val buddyWingLeft = parse("M86 150 C72 154 70 172 80 182 C88 188 98 180 98 170 C98 160 94 152 86 150 Z")
    val buddyWingRight = parse("M134 150 C148 154 150 172 140 182 C132 188 122 180 122 170 C122 160 126 152 134 150 Z")
    val buddyMarkLeft = parse("M86 160 Q82 167 85 174")
    val buddyMarkRight = parse("M134 160 Q138 167 135 174")
    val sparkle =
        parse(
            "M0 -12 C1.5 -3.5 3.5 -1.5 12 0 C3.5 1.5 1.5 3.5 0 12 C-1.5 3.5 -3.5 1.5 -12 0 " +
                "C-3.5 -1.5 -1.5 -3.5 0 -12 Z",
        )

    val shell = parse("$SHELL_ZIGZAG A58 58 0 1 1 45.6 130 Z")
    val shellCap = parse("$SHELL_ZIGZAG A58 58 0 0 0 45.6 130 Z")
    val shellCrack = parse(SHELL_ZIGZAG)
    val eggShade = parse("M45.6 170 A58 58 0 0 0 154.4 170 Q100 194 45.6 170 Z")
    val hatchTuft =
        parse(
            "M92 58 C89 48 92 40 98 36 " +
                "C98 43 100 47 103 50 C105 44 110 40 117 40 C112 45 110 51 110 58 Z",
        )
    val hatchBand = parse("M61.9 76 L138.1 76 L143.1 88 L56.9 88 Z")
    val hatchBandEdges = parse("M61.9 76 L138.1 76 M56.9 88 L143.1 88")
    val hatchMouth = parse("M93 110 Q100 124 107 110 Z")
    val hatchWingLeft = parse("M58 124 C52 114 60 106 70 110 C78 114 80 124 74 128 C68 132 60 130 58 124 Z")
    val hatchWingRight = parse("M142 124 C148 114 140 106 130 110 C122 114 120 124 126 128 C132 132 140 130 142 124 Z")

    val headTuft =
        parse(
            "M92 42 C88 28 92 18 100 13 " +
                "C99 22 102 28 106 32 C108 24 115 19 124 19 C117 25 115 33 114 42 Z",
        )
    val headBand = parse("M31.8 72 L168.2 72 L175.7 88 L24.3 88 Z")
    val headBandEdges = parse("M31.8 72 L168.2 72 M24.3 88 L175.7 88")
    val joyLeft = parse("M69 111 Q78 99 87 111")
    val joyRight = parse("M113 111 Q122 99 131 111")
    val mouthSmile = parse("M86 131 Q100 156 114 131 Z")
    val mouthBig = parse("M81 131 Q100 170 119 131 Z")
    val drop =
        parse(
            "M158 58 C158 58 150 70 150 75 " +
                "C150 80 154 83 158 83 C162 83 166 80 166 75 C166 70 158 58 158 58 Z",
        )
}
