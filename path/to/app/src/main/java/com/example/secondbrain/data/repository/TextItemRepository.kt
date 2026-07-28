import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.example.secondbrain.ui.theme.Theme
import com.example.secondbrain.ui.theme.EmptyListMessage

class TextItemRepository(private val db: AppDatabase) : Repository {

    // ... existing code ...

    override fun getTextItems(): List<TextItem> {
        // ... existing code ...
        if (textItems.isEmpty()) {
            return listOf(
                TextItem(
                    id = 0,
                    text = "No notes yet. Share text from any app to save it here."
                )
            )
        }
        return textItems
    }
}