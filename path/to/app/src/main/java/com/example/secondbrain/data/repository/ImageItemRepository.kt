import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.example.secondbrain.ui.theme.Theme
import com.example.secondbrain.ui.theme.EmptyListMessage

class ImageItemRepository(private val db: AppDatabase) : Repository {

    // ... existing code ...

    override fun getImageItems(): List<ImageItem> {
        // ... existing code ...
        if (imageItems.isEmpty()) {
            return listOf(
                ImageItem(
                    id = 0,
                    image = "No images yet. Use the upload button or share an image."
                )
            )
        }
        return imageItems
    }
}