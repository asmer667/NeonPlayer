package com.asmer.neonplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView

private val Night = Color(0xFF050914)
private val Panel = Color(0xE6101A34)
private val Panel2 = Color(0xCC0A1228)
private val Cyan = Color(0xFF22C8FF)
private val Violet = Color(0xFF9C4DFF)
private val Magenta = Color(0xFFFF3EAE)
private val TextDim = Color(0xFF9DADD0)

private data class LocalVideo(val uri: Uri, val name: String)
private val mainTabs = listOf("الرئيسية", "المكتبة", "المفضلة", "الموسيقى", "التاريخ", "الإعدادات")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5, 9, 20)
        window.navigationBarColor = android.graphics.Color.rgb(5, 9, 20)
        setContent { NeonPlayerApp() }
    }
}

@Composable
private fun NeonPlayerApp() {
    var page by remember { mutableStateOf("الرئيسية") }
    var activeVideo by remember { mutableStateOf<LocalVideo?>(null) }
    val library = remember { mutableStateListOf<LocalVideo>() }
    val favorites = remember { mutableStateListOf<String>() }
    var filter by remember { mutableStateOf("الكل") }
    var darkMode by remember { mutableStateOf(true) }
    var glass by remember { mutableStateOf(true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val raw = uri.lastPathSegment?.substringAfterLast('/') ?: "video_${library.size + 1}"
            val item = LocalVideo(uri, raw.substringAfterLast(':').substringAfterLast('/'))
            if (library.none { it.uri == uri }) library.add(0, item)
            activeVideo = item
            page = "المشغل"
        }
    }
    val chooseVideo = { picker.launch(arrayOf("video/*")) }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Night, surface = Panel, primary = Violet, secondary = Cyan,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Box(Modifier.fillMaxSize().background(Night)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(onAdd = chooseVideo, onSettings = { page = "الإعدادات" })
                Box(Modifier.weight(1f)) {
                when (page) {
                    "الرئيسية" -> Dashboard(
                        videos = library, favorites = favorites, filter = filter,
                        onFilter = { filter = it }, onAdd = chooseVideo,
                        onOpen = { activeVideo = it; page = "المشغل" },
                        onNavigate = { page = it },
                        onFavorite = { name ->
                            if (favorites.contains(name)) favorites.remove(name) else favorites.add(name)
                        }
                    )
                    "المكتبة" -> LibraryPage(library, onAdd = chooseVideo,
                        onOpen = { activeVideo = it; page = "المشغل" },
                        favorites = favorites, onFavorite = { name ->
                            if (favorites.contains(name)) favorites.remove(name) else favorites.add(name)
                        })
                    "المفضلة" -> FavoritesPage(library.filter { favorites.contains(it.name) },
                        onOpen = { activeVideo = it; page = "المشغل" })
                    "الموسيقى" -> SimplePage("الموسيقى", "اختر ملفات صوتية من مكتبتك في إصدار الموسيقى القادم.")
                    "التاريخ" -> SimplePage("المشاهدة الأخيرة", "سيظهر سجل المشاهدة هنا. أضف فيديو وابدأ تشغيله.")
                    "الإعدادات" -> SettingsPage(darkMode, { darkMode = it }, glass, { glass = it })
                    "المشغل" -> PlayerPage(activeVideo)
                    else -> CategoryPage(page, library, onOpen = { activeVideo = it; page = "المشغل" })
                }
                }
                BottomNavigation(page, onNavigate = { page = it })
            }
        }
    }
}

@Composable
private fun TopBar(onAdd: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xF0182548), Color(0xF21A103C))))
            .border(1.dp, Brush.horizontalGradient(listOf(Cyan, Violet, Magenta)), RoundedCornerShape(22.dp))
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Violet, Cyan))), Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White)
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text("Neon Player", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text("LOCAL VIDEO PLAYER", color = TextDim, fontSize = 8.sp, letterSpacing = 1.2.sp)
        }
        IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "إضافة فيديو", tint = Cyan) }
        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "الإعدادات", tint = Color.White) }
    }
}

@Composable
private fun Dashboard(
    videos: List<LocalVideo>, favorites: List<String>, filter: String,
    onFilter: (String) -> Unit, onAdd: () -> Unit, onOpen: (LocalVideo) -> Unit,
    onNavigate: (String) -> Unit, onFavorite: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 13.dp, end = 13.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            HeroPanel(
                headline = videos.firstOrNull()?.name ?: "اكتشف مكتبتك",
                subtitle = if (videos.isEmpty()) "أضف أفلامك ومسلسلاتك المحلية" else "متابعة المشاهدة • ${videos.size} ملف",
                button = if (videos.isEmpty()) "إضافة فيديو" else "تشغيل الآن",
                onClick = { if (videos.isEmpty()) onAdd() else onOpen(videos.first()) }
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeading("الأقسام", "استكشف المحتوى")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onNavigate("المكتبة") }) { Text("عرض الكل", color = Cyan, fontSize = 11.sp) }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("الكل", "أفلام", "مسلسلات", "أنمي", "برامج", "موسيقى", "مجلدات").forEach {
                    FilterChipNeon(it, filter == it) { onFilter(it) }
                }
            }
        }
        item { SectionHeading("مكتبة الفيديو", "${videos.size} عنصر") }
        if (videos.isEmpty()) {
            item { EmptyLibrary(onAdd) }
        } else {
            items(videos.take(30), key = { it.uri.toString() }) { video ->
                VideoCard(video, favorites.contains(video.name),
                    onClick = { onOpen(video) }, onFavorite = { onFavorite(video.name) })
            }
        }
        item { SectionHeading("الوصول السريع", "اختصارات") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Shortcut("المفضلة", Icons.Default.Favorite) { onNavigate("المفضلة") }
                Shortcut("التاريخ", Icons.Default.History) { onNavigate("التاريخ") }
                Shortcut("الإعدادات", Icons.Default.Settings) { onNavigate("الإعدادات") }
            }
        }
    }
}

@Composable
private fun HeroPanel(headline: String, subtitle: String, button: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(188.dp).clip(RoundedCornerShape(25.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF173A68), Color(0xFF25134F), Color(0xFF071629))))
            .border(1.dp, Brush.linearGradient(listOf(Cyan, Violet, Magenta)), RoundedCornerShape(25.dp))
            .padding(17.dp)
    ) {
        Box(Modifier.align(Alignment.CenterEnd).size(145.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0x779B4DFF), Color.Transparent))))
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.76f)) {
            Text("YOUR LOCAL CINEMA", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Text(headline, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color(0xFFD0D9F2), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(13.dp))
            Button(onClick = onClick, shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 7.dp)) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(button, fontSize = 11.sp)
            }
        }
        Icon(Icons.Default.Movie, null, tint = Color(0x99C8B5FF),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 23.dp).size(52.dp))
    }
}

@Composable
private fun SectionHeading(title: String, hint: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(hint, color = TextDim, fontSize = 10.sp)
    }
}

@Composable
private fun FilterChipNeon(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(13.dp))
            .background(if (selected) Brush.horizontalGradient(listOf(Violet, Color(0xFF344BC6))) else Brush.linearGradient(listOf(Panel, Panel2)))
            .border(1.dp, if (selected) Cyan.copy(alpha = .8f) else Color(0x554B6A9C), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 9.dp)
    ) { Text(label, color = Color.White, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
}

@Composable
private fun EmptyLibrary(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel)
            .border(1.dp, Color(0x664D78BA), RoundedCornerShape(22.dp)).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.VideoLibrary, null, tint = Cyan, modifier = Modifier.size(39.dp))
        Spacer(Modifier.height(8.dp))
        Text("مكتبتك فارغة", color = Color.White, fontWeight = FontWeight.Bold)
        Text("اختر فيديو من الهاتف لإضافته", color = TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Violet)) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text("اختيار فيديو")
        }
    }
}

@Composable
private fun VideoCard(video: LocalVideo, favorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel)
            .border(1.dp, Brush.horizontalGradient(listOf(Color(0x8871CFFF), Color(0x887C4EFF))), RoundedCornerShape(17.dp))
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(57.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1D4771), Color(0xFF47205F)))), Alignment.Center) {
            Icon(Icons.Default.Movie, null, tint = Color.White, modifier = Modifier.size(29.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(video.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("فيديو محلي • اضغط للتشغيل", color = TextDim, fontSize = 9.sp)
        }
        IconButton(onClick = onFavorite) {
            Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "إضافة إلى المفضلة", tint = if (favorite) Magenta else TextDim)
        }
        Icon(Icons.Default.PlayCircleFilled, null, tint = Cyan, modifier = Modifier.size(27.dp))
    }
}

@Composable
private fun LibraryPage(videos: List<LocalVideo>, onAdd: () -> Unit, onOpen: (LocalVideo) -> Unit,
                        favorites: List<String>, onFavorite: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 13.dp)) {
        SectionHeading("مكتبة الفيديو", "${videos.size} ملف")
        Spacer(Modifier.height(12.dp))
        if (videos.isEmpty()) EmptyLibrary(onAdd)
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(videos, key = { it.uri.toString() }) {
                VideoCard(it, favorites.contains(it.name), { onOpen(it) }, { onFavorite(it.name) })
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Text("إضافة فيديو")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FavoritesPage(videos: List<LocalVideo>, onOpen: (LocalVideo) -> Unit) {
    Column(Modifier.fillMaxSize().padding(13.dp)) {
        SectionHeading("المفضلة", "${videos.size} عنصر")
        Spacer(Modifier.height(10.dp))
        if (videos.isEmpty()) Text("لم تضف أي فيديو إلى المفضلة بعد.", color = TextDim)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(videos) { VideoCard(it, true, { onOpen(it) }, {}) }
        }
    }
}

@Composable
private fun CategoryPage(title: String, videos: List<LocalVideo>, onOpen: (LocalVideo) -> Unit) {
    Column(Modifier.fillMaxSize().padding(13.dp)) {
        SectionHeading(title, "${videos.size} عنصر في المكتبة")
        Spacer(Modifier.height(10.dp))
        Text("يمكنك تصنيف ملفاتك وإدارتها من مكتبة الفيديو.", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(videos) { VideoCard(it, false, { onOpen(it) }, {}) }
        }
    }
}

@Composable
private fun SimplePage(title: String, message: String) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(title, "NEON PLAYER")
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel)
            .border(1.dp, Color(0x554A70A8), RoundedCornerShape(20.dp)).padding(20.dp)) {
            Text(message, color = TextDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsPage(dark: Boolean, onDark: (Boolean) -> Unit, glass: Boolean, onGlass: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading("الإعدادات", "تخصيص التطبيق")
        SettingRow("الوضع الداكن", "واجهة سينمائية داكنة", dark, onDark)
        SettingRow("تأثير الزجاج", "شفافية اللوحات والبطاقات", glass, onGlass)
        InfoRow("لغة الواجهة", "العربية")
        InfoRow("محرك الفيديو", "AndroidX Media3")
        InfoRow("الإصدار", "1.0.0")
    }
}

@Composable
private fun SettingRow(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel)
        .border(1.dp, Color(0x443D6DAA), RoundedCornerShape(16.dp)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(description, color = TextDim, fontSize = 9.sp)
        }
        Switch(checked, onChecked)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(value, color = Cyan, fontSize = 11.sp)
    }
}

@Composable
private fun Shortcut(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(Modifier.width(100.dp).clip(RoundedCornerShape(15.dp)).background(Panel)
        .border(1.dp, Color(0x554C75B5), RoundedCornerShape(15.dp))
        .clickable(onClick = onClick).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Violet)
        Spacer(Modifier.height(5.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun PlayerPage(video: LocalVideo?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(video?.uri) {
        if (video == null) null else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        SectionHeading("المشغل", video?.name ?: "لا يوجد فيديو")
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp))
            .background(Color.Black).border(1.dp, Violet, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center) {
            if (player != null) {
                AndroidView(factory = { ctx -> PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    controllerAutoShow = true
                } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
            } else Text("اختر فيديو من المكتبة لبدء التشغيل", color = TextDim)
        }
        Spacer(Modifier.height(10.dp))
        Text(video?.name ?: "Neon Player", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("تشغيل محلي • عناصر التحكم الأصلية للمشغل", color = TextDim, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BottomNavigation(current: String, onNavigate: (String) -> Unit) {
    val nav = listOf(
        Triple("التاريخ", Icons.Default.History, "التاريخ"),
        Triple("الموسيقى", Icons.Default.MusicNote, "الموسيقى"),
        Triple("الرئيسية", Icons.Default.Home, "الرئيسية"),
        Triple("المكتبة", Icons.Default.VideoLibrary, "المكتبة"),
        Triple("الإعدادات", Icons.Default.Settings, "الإعدادات")
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xF0172547), Color(0xF21A103A))))
            .border(1.dp, Brush.horizontalGradient(listOf(Cyan, Violet, Magenta)), RoundedCornerShape(23.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
    ) {
        nav.forEach { (label, icon, destination) ->
            val active = current == destination
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(if (active) Brush.linearGradient(listOf(Violet, Color(0xFF3449B8))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onNavigate(destination) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, null, tint = if (active) Color.White else TextDim, modifier = Modifier.size(20.dp))
                Text(label, color = if (active) Color.White else TextDim, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}
