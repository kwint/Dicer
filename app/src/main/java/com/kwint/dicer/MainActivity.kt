package com.kwint.dicer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kwint.dicer.ui.theme.DicerTheme // Theme name updated
import kotlinx.coroutines.launch

// Score list as a constant
val SCORES = listOf(1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78)
const val BONUS_SCORE = 10
const val PENALTY_SCORE = -5

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Use the corrected theme name
            DicerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DiceRollerApp()
                }
            }
        }
    }
}

@Composable
fun DiceRollerApp(modifier: Modifier = Modifier) {
    // State for the current dice results
    var results by remember { mutableStateOf(List(6) { 1 }) }
    // State for the grid cells (4 rows, 12 columns), false = not crossed out
    var gridState by remember { mutableStateOf(List(4) { List(12) { false } }) }
    // State for which rows are "closed"
    var closedRowsState by remember { mutableStateOf(List(4) { false }) }
    // State for the penalty cells
    var penaltyState by remember { mutableStateOf(List(4) { false }) }

    // Coroutine scope to launch the animation
    val coroutineScope = rememberCoroutineScope()

    // Animatable for the rotation effect
    val rotation = remember { Animatable(0f) }

    // Define the colors for each die using your new RGBA values
    val diceColors = listOf(
        Color.White,
        Color.White,
        Color.Red.copy(alpha = 0.7f),
        Color(255, 220, 0, alpha = 180),
        Color.Green.copy(alpha = 0.7f),
        Color.Blue.copy(alpha = 0.7f)
    )

    // Box layout allows us to align children to different parts of the screen
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // The main content area with the grid
        Column(
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(top = 32.dp)
            ,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            CrossOutGrid(
                gridState = gridState,
                closedRowsState = closedRowsState,
                onCellClick = { row, col ->
                    // --- Game Logic for Clicking a Cell ---
                    val newGridState = gridState.map { it.toMutableList() }.toMutableList()
                    newGridState[row][col] = !newGridState[row][col]
                    gridState = newGridState

                    // --- Game Logic for Closing a Row ---
                    val isLastCellInRow = (col == 11)
                    val isLastCellChecked = newGridState[row][11]
                    val checkedCountInRow = newGridState[row].count { it }
                    val shouldBeClosed = isLastCellChecked && checkedCountInRow >= 6

                    if (closedRowsState[row] != shouldBeClosed) {
                        val newClosedRowsState = closedRowsState.toMutableList()
                        newClosedRowsState[row] = shouldBeClosed
                        closedRowsState = newClosedRowsState
                    }
                }
            )

            // *** THE CHANGE IS HERE: Combined Penalty and Total Score into one row ***
            ScoreSummaryRow(
                gridState = gridState,
                closedRowsState = closedRowsState,
                penaltyState = penaltyState,
                onPenaltyClick = { index ->
                    val newPenaltyState = penaltyState.toMutableList()
                    newPenaltyState[index] = !newPenaltyState[index]
                    penaltyState = newPenaltyState
                }
            )
        }


        // Place all dice in a single row at the bottom of the screen
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .clickable {
                    results = List(6) { (1..6).random() }
                    coroutineScope.launch {
                        rotation.animateTo(
                            targetValue = rotation.value + 360f,
                            animationSpec = tween(durationMillis = 500)
                        )
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            results.forEachIndexed { index, result ->
                DiceImageWithRotation(
                    result = result,
                    rotationAngle = rotation.value,
                    color = diceColors[index]
                )
            }
        }
    }
}

/**
 * Composable that builds the 4x12 grid with a score column and bonus indicator.
 */
@Composable
fun CrossOutGrid(
    gridState: List<List<Boolean>>,
    closedRowsState: List<Boolean>,
    onCellClick: (row: Int, col: Int) -> Unit
) {
    val rowColors = listOf(
        Color.Red.copy(alpha = 0.7f),
        Color(255, 220, 0, alpha = 255),
        Color.Green.copy(alpha = 0.7f),
        Color.Blue.copy(alpha = 0.7f)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        for (rowIndex in 0..3) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(rowColors[rowIndex])
            ) {
                val lastCrossedOutIndex = gridState[rowIndex].lastIndexOf(true)

                for (colIndex in 0..10) {
                    val number = if (rowIndex < 2) colIndex + 2 else 12 - colIndex
                    val isEnabled = (lastCrossedOutIndex == -1) || (colIndex >= lastCrossedOutIndex)

                    GridCell(
                        number = number,
                        isCrossedOut = gridState[rowIndex][colIndex],
                        isEnabled = isEnabled,
                        onClick = { onCellClick(rowIndex, colIndex) }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                BonusIndicator(isClosed = closedRowsState[rowIndex])
                Spacer(modifier = Modifier.width(16.dp))

                val checkedCount = gridState[rowIndex].count { it }
                var score = if (checkedCount > 0) SCORES[checkedCount - 1] else 0
                if (closedRowsState[rowIndex]) {
                    score += BONUS_SCORE
                }

                Text(
                    text = score.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * A new composable that combines the penalty boxes and the final total score.
 */
@Composable
fun ScoreSummaryRow(
    gridState: List<List<Boolean>>,
    closedRowsState: List<Boolean>,
    penaltyState: List<Boolean>,
    onPenaltyClick: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        // Display the 4 penalty cells
        for (i in 0..3) {
            PenaltyCell(
                isCrossedOut = penaltyState[i],
                onClick = { onPenaltyClick(i) }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- Calculate Total Score ---
        var gridAndBonusScore = 0
        for (rowIndex in 0..3) {
            val checkedCount = gridState[rowIndex].count { it }
            var rowScore = if (checkedCount > 0) SCORES[checkedCount - 1] else 0
            if (closedRowsState[rowIndex]) {
                rowScore += BONUS_SCORE
            }
            gridAndBonusScore += rowScore
        }

        val penaltyScore = penaltyState.count { it } * PENALTY_SCORE
        val totalScore = gridAndBonusScore + penaltyScore
        // --- End Calculation ---

        // Display the total score
        Text(
            text = totalScore.toString(),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


/**
 * Composable for a single clickable penalty cell.
 */
@Composable
fun PenaltyCell(isCrossedOut: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .background(Color.Gray.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .drawWithContent {
                drawContent()
                if (isCrossedOut) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
    )
}

/**
 * Composable for a single cell in the grid.
 */
@Composable
fun GridCell(number: Int, isCrossedOut: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val textColor = if (isEnabled) Color.White else Color.White.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .size(50.dp)
            .padding(4.dp)
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontSize = 25.sp,
            fontWeight = FontWeight(800),
            textAlign = TextAlign.Center,
            color = textColor,
            modifier = Modifier.drawWithContent {
                drawContent()
                if (isCrossedOut) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        )
    }
}

/**
 * A composable for the lock icon that indicates if a row is closed.
 */
@Composable
fun BonusIndicator(isClosed: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🔒",
            fontSize = 25.sp,
            color = Color.White,
            modifier = Modifier.drawWithContent {
                drawContent()
                if (isClosed) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        )
    }
}


/**
 * A helper composable to display a single die image with rotation and color tinting.
 */
@Composable
fun DiceImageWithRotation(result: Int, rotationAngle: Float, color: Color) {
    val imageResource = when (result) {
        1 -> R.drawable.dice_1
        2 -> R.drawable.dice_2
        3 -> R.drawable.dice_3
        4 -> R.drawable.dice_4
        5 -> R.drawable.dice_5
        else -> R.drawable.dice_6
    }

    Image(
        painter = painterResource(id = imageResource),
        contentDescription = result.toString(),
        modifier = Modifier
            .size(60.dp)
            .rotate(rotationAngle),
        colorFilter = if (color == Color.White) null else ColorFilter.tint(
            color,
            blendMode = BlendMode.SrcAtop
        )
    )
}


@Preview(showBackground = true, device = "spec:shape=Normal,width=1280,height=800,unit=dp,dpi=480")
@Composable
fun DefaultPreview() {
    DicerTheme {
        DiceRollerApp()
    }
}
