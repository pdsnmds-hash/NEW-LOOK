package bg.newlook.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Small, consistent local vector set; no font or raster icon dependencies. */
object AppIcons {
    private fun icon(name: String, geometry: String, filled: Boolean = false) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(
                pathData = PathParser().parsePathString(geometry).toNodes(),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()

    val Dashboard by lazy {
        icon("Dashboard", "M3,3h7v7H3z M14,3h7v7h-7z M3,14h7v7H3z M14,14h7v7h-7z")
    }
    val Checklist by lazy {
        icon(
            "Checklist",
            "M3,6l2,2 3,-4 M11,6h10 M3,13l2,2 3,-4 M11,13h10 M3,20l2,2 3,-4 M11,20h10",
        )
    }
    val CalendarMonth by lazy {
        icon(
            "Calendar",
            "M5,4h14q2,0 2,2v13q0,2 -2,2H5q-2,0 -2,-2V6q0,-2 2,-2z M3,9h18 M7,2v4 M17,2v4 M7,13h2 M15,13h2 M7,17h2 M15,17h2",
        )
    }
    val Business by lazy {
        icon(
            "Business",
            "M5,21V3h10v18 M15,9h5v12 M3,21h19 M8,7h3 M8,11h3 M8,15h3 M17,13h1 M17,17h1",
        )
    }
    val MoreHoriz by lazy { icon("More", "M5,12h0.1 M12,12h0.1 M19,12h0.1") }
    val MoreVert by lazy { icon("Options", "M12,5v0.1 M12,12v0.1 M12,19v0.1") }
    val FolderOpen by lazy { icon("Folder", "M3,9V6q0,-2 2,-2h5l2,3h7q2,0 2,2 M3,9h18l-3,11H3z") }
    val Description by lazy { icon("Document", "M5,3h9l5,5v13H5z M14,3v6h5 M8,13h8 M8,17h6") }
    val AutoAwesome by lazy {
        icon("Templates", "M12,3l3,6 6,3 -6,3 -3,6 -3,-6 -6,-3 6,-3z M20,2v4 M18,4h4")
    }
    val MenuBook by lazy {
        icon("Guide", "M12,5q-5,-3 -10,-1v15q5,-2 10,1 5,-3 10,-1V4q-5,-2 -10,1z M12,5v15")
    }
    val BookmarkBorder by lazy { icon("Bookmark", "M6,3h12v18l-6,-4 -6,4z") }
    val Bookmark by lazy { icon("Bookmarked", "M6,3h12v18l-6,-4 -6,4z", true) }
    val SaveAlt by lazy { icon("Export", "M12,3v12 M7,10l5,5 5,-5 M4,16v5h16v-5") }
    val Settings by lazy {
        icon(
            "Settings",
            "M9,3h6l1,3 3,1 2,5 -2,5 -3,1 -1,3H9l-1,-3 -3,-1 -2,-5 2,-5 3,-1z M16,12a4,4 0,1 1,-8,0a4,4 0,1 1,8,0",
        )
    }
    val Lock by lazy { icon("Private", "M5,10h14v11H5z M8,10V6a4,4 0,1 1,8,0v4 M12,14v3") }
    val Search by lazy { icon("Search", "M17,10a7,7 0,1 1,-14,0a7,7 0,1 1,14,0 M15,15l6,6") }
    val ArrowBack by lazy { icon("Back", "M20,12H4 M11,5l-7,7 7,7") }
    val ArrowForward by lazy { icon("Forward", "M4,12h16 M13,5l7,7 -7,7") }
    val ChevronRight by lazy { icon("Next", "M9,5l7,7 -7,7") }
    val ChevronLeft by lazy { icon("Previous", "M15,5l-7,7 7,7") }
    val ExpandMore by lazy { icon("Expand", "M5,9l7,7 7,-7") }
    val Add by lazy { icon("Add", "M12,4v16 M4,12h16") }
    val Close by lazy { icon("Close", "M5,5l14,14 M19,5L5,19") }
    val CheckCircle by lazy {
        icon("Complete", "M22,12a10,10 0,1 1,-20,0a10,10 0,1 1,20,0 M7,12l3,3 7,-7")
    }
    val Tune by lazy {
        icon(
            "Filters",
            "M3,6h8 M15,6h6 M3,12h3 M10,12h11 M3,18h11 M18,18h3 M11,3v6 M6,9v6 M14,15v6",
        )
    }
    val ChatBubbleOutline by lazy { icon("Consultation", "M4,3h16v14H9l-5,4z M8,8h8 M8,12h5") }
    val AlternateEmail by lazy {
        icon(
            "Email",
            "M16,12a4,4 0,1 1,-8,0a4,4 0,1 1,8,0 M16,8v7q0,3 4,1 3,-2 2,-6a10,10 0,1 0,-4,11",
        )
    }
}
