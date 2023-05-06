import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

private val windowPadding = 64.dp
private val fabSize = 64.dp
private val toolbarHeight = 64.dp
private val toolsPadding = 64.dp

fun main() = application {
    Window(
        onCloseRequest = {
            exitApplication()
        },
    ) {
        window.minimumSize = Dimension(
            700,
            1000,
        )
        App()
    }
}
