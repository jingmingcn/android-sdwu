package edu.sdwu.teaching.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

// The core icon set has no outlined bookmark (and Icons.Outlined.Star is drawn filled), and the
// extended set adds ~5 MB. Defining the two icons we need is a few lines with materialIcon {}.
// Path data: Material Design "bookmark" and "bookmark_border" (Apache 2.0).

val Icons.Filled.Bookmark: ImageVector by lazy {
    materialIcon(name = "Filled.Bookmark") {
        materialPath {
            moveTo(17f, 3f)
            horizontalLineTo(7f)
            curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
            lineTo(5f, 21f)
            lineToRelative(7f, -3f)
            lineToRelative(7f, 3f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
        }
    }
}

val Icons.Outlined.BookmarkBorder: ImageVector by lazy {
    materialIcon(name = "Outlined.BookmarkBorder") {
        materialPath {
            moveTo(17f, 3f)
            horizontalLineTo(7f)
            curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
            lineTo(5f, 21f)
            lineToRelative(7f, -3f)
            lineToRelative(7f, 3f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(17f, 18f)
            lineToRelative(-5f, -2.18f)
            lineTo(7f, 18f)
            verticalLineTo(5f)
            horizontalLineToRelative(10f)
            verticalLineToRelative(13f)
            close()
        }
    }
}
