import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.example.secondbrain.data.repository.MockRepository
import com.example.secondbrain.ui.theme.EmptyListMessage
import org.junit.Rule
import org.junit.Test

class ThemeTest {
    @get:Rule
    val composeRule = ComposeContentTestRule()

    @Test
    fun testEmptyListMessage() {
        val repository = MockRepository()
        val message = repository.getTextItems().first().text
        composeRule.setContent {
            EmptyListMessage(message = message)
        }
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }
}