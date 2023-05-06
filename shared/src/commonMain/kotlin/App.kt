import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.floor

const val CELL_SIZE = 20
const val ROWS = 75
const val COLS = 75
private val initialOffset = 16.dp
private const val MAX_SPEED = 9f
private const val MIN_SPEED = 1f
private const val STARTING_SPEED = 6f

@Composable
fun App() {
    GameOfLife()
}

@Composable
fun GameOfLife() {
    var grid by remember { mutableStateOf(createInitialGrid()) }
    var isPlaying by remember { mutableStateOf(false) }
    var invalidate by remember {
        mutableStateOf(0)
    }
    var generation by remember {
        mutableStateOf(0)
    }
    val sliderValue = remember { mutableStateOf(STARTING_SPEED) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val inverseSpeed = 1001 - (sliderValue.value.toInt() * 100L)
            delay(inverseSpeed)
            grid = getNextGeneration(grid)
            generation++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Conway's Game of Life") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                isPlaying = !isPlaying
            }) {
                if (isPlaying) {
                    Icon(Icons.Default.Clear, contentDescription = "Pause")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                }
            }
        },
        content = {
            Box(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
                    .fillMaxHeight()
                    .width((COLS * CELL_SIZE + (initialOffset.value * 2)).dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                val cellCoordinate = Pair(
                                    floor((offset.x - initialOffset.toPx()) / CELL_SIZE.dp.toPx()).toInt(),
                                    floor((offset.y - initialOffset.toPx()) / CELL_SIZE.dp.toPx()).toInt(),
                                )

                                if (cellCoordinate.second < grid.size && cellCoordinate.second >= 0) {
                                    if (cellCoordinate.first < grid[cellCoordinate.second].size && cellCoordinate.first >= 0) {
                                        grid[cellCoordinate.second][cellCoordinate.first] = true
                                        invalidate++
                                    }
                                }

                            }
                        )
                    }
            ) {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                ) {

                    Canvas(
                        modifier = Modifier
                            .height((ROWS * CELL_SIZE).dp)
                            .width((COLS * CELL_SIZE).dp),
                        onDraw = {
                            invalidate.let {
                                // Draw the grid
                                drawGrid()

                                // Draw the cells
                                for (row in 0 until ROWS) {
                                    for (col in 0 until COLS) {
                                        if (grid[row][col]) {
                                            val x = col * CELL_SIZE.dp.toPx()
                                            val y = row * CELL_SIZE.dp.toPx()
                                            drawRect(
                                                Color.Black,
                                                topLeft = Offset(x + initialOffset.toPx(), y + initialOffset.toPx()),
                                                size = Size(CELL_SIZE.dp.toPx(), CELL_SIZE.dp.toPx())
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )

                    HorizontalSlider(
                        modifier = Modifier.padding(top = 16.dp, start = 8.dp),
                        value = sliderValue,
                        min = MIN_SPEED.toInt(),
                        max = MAX_SPEED.toInt(),
                        onFinished = {
                            sliderValue.value = it.toFloat()
                        }
                    )

                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "Speed : ${sliderValue.value}"
                    )

                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "Generation = $generation"
                    )
                }
            }
        }
    )
}

@Composable
fun HorizontalSlider(
    modifier: Modifier = Modifier,
    value: MutableState<Float>,
    min: Int,
    max: Int,
    onFinished: (Int) -> Unit,
) {
    Slider(
        modifier = modifier.width(300.dp),
        value = value.value,
        valueRange = min.toFloat()..max.toFloat(),
        onValueChange = {
            value.value = it.toInt().toFloat()
        },
        onValueChangeFinished = {
            onFinished(value.value.toInt())
        },
    )
}


fun createInitialGrid(): Array<BooleanArray> {
    val grid = Array(ROWS) { BooleanArray(COLS) { false } }
    grid[2][1] = true
    grid[3][2] = true
    grid[3][3] = true
    grid[2][3] = true
    grid[1][3] = true
    return grid
}

fun createEmptyGrid(): Array<BooleanArray> {
    return Array(ROWS) { BooleanArray(COLS) { false } }
}

fun getNextGeneration(grid: Array<BooleanArray>): Array<BooleanArray> {
    val nextGrid = createEmptyGrid()

    for (row in 0 until ROWS) {
        for (col in 0 until COLS) {
            val cell = grid[row][col]
            val numNeighbors = countNeighbors(grid, row, col)

            // Apply the rules of the game
            when {
                cell && (numNeighbors == 2 || numNeighbors == 3) -> nextGrid[row][col] = true // Survival
                cell && numNeighbors > 3 -> nextGrid[row][col] = false // Overpopulation
                cell && numNeighbors < 2 -> nextGrid[row][col] = false // Underpopulation
                !cell && numNeighbors == 3 -> nextGrid[row][col] = true // Reproduction
            }
        }
    }

    return nextGrid
}

fun countNeighbors(grid: Array<BooleanArray>, row: Int, col: Int): Int {
    var count = 0

    for (i in -1..1) {
        for (j in -1..1) {
            val r = row + i
            val c = col + j

            // Make sure we're not counting the cell itself
            if (i == 0 && j == 0) continue

            // Make sure the cell is within the grid
            if (r < 0 || r >= ROWS || c < 0 || c >= COLS) continue

            // Increment the count if the neighbor is alive
            if (grid[r][c]) count++
        }
    }

    return count
}

fun DrawScope.drawGrid() {
    val densityCellSize = CELL_SIZE.dp.toPx()
    for (i in 0..ROWS) {
        drawLine(
            color = Color.Gray,
            start = Offset(initialOffset.toPx(), i * densityCellSize + initialOffset.toPx()),
            end = Offset(COLS * densityCellSize + initialOffset.toPx(), (i * densityCellSize) + initialOffset.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }

    for (j in 0..COLS) {
        drawLine(
            color = Color.Gray,
            start = Offset(j * densityCellSize + initialOffset.toPx(), initialOffset.toPx()),
            end = Offset(
                j * densityCellSize + initialOffset.toPx(),
                ROWS * densityCellSize + initialOffset.toPx()
            ),
            strokeWidth = 1.dp.toPx()
        )
    }
}