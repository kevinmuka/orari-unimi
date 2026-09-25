package app.orariunimi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FavoritesScreen(
    favorites: List<FavoriteCourse>,
    onOpenCourse: (FavoriteCourse) -> Unit,
    onRemoveCourse: (FavoriteCourse) -> Unit,
    onExploreCourses: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        AppScreenHeader(
            title = "Preferiti",
            subtitle = if (favorites.isEmpty()) "I corsi di laurea che vuoi ritrovare facilmente."
                else "${favorites.size} ${if (favorites.size == 1) "corso di laurea salvato" else "corsi di laurea salvati"}",
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
        )
        FavoriteCoursesContent(
            favorites = favorites,
            onOpen = onOpenCourse,
            onRemove = onRemoveCourse,
            onExplore = onExploreCourses,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
