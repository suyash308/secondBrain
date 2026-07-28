import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.example.secondbrain.ui.theme.Theme
import com.example.secondbrain.ui.theme.EmptyListMessage

class LinkItemRepository(private val db: AppDatabase) : Repository {

    // ... existing code ...

    override fun getLinkItems(): List<LinkItem> {
        // ... existing code ...
        if (linkItems.isEmpty()) {
            return listOf(
                LinkItem(
                    id = 0,
                    link = "No links yet. Share a URL from your browser to save it here."
                )
            )
        }
        return linkItems
    }
}