kotlin
package com.magadan.gertnertides

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

data class TideEvent(val time: String, val type: String, val label: String, val height: Double)
data class HourlyPoint(val hour: Int, val height: Double)
data class DayTides(val date: String, val events: List<TideEvent>, val hourly: List<HourlyPoint>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TideAppScreen()
            }
        }
    }
}

@Composable
fun TideAppScreen() {
    val context = LocalContext.current
    val daysList = remember { loadTidesFromAssets(context) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    if (daysList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ошибка загрузки данных")
        }
        return
    }

    val currentDay = daysList[selectedIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F6FA))
            .padding(16.dp)
    ) {
        Text("Бухта Гертнера", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D2B45))
        Text("Декабрь 2026 • Время местное (UTC+11)", fontSize = 14.sp, color = Color(0xFF5C768D))

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(daysList.indices.toList()) { index ->
                val dayNum = daysList[index].date.takeLast(2)
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isSelected) Color(0xFF0066CC) else Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedIndex = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(dayNum, color = if (isSelected) Color.White else Color(0xFF0D2B45), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("График уровня воды (м)", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                TideChart(hourly = currentDay.hourly, modifier = Modifier.fillMaxSize())
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Полная и малая вода (${currentDay.date})", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D2B45))

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            currentDay.events.forEach { event ->
                val isHigh = event.type == "HIGH"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isHigh) Color(0xFFE3F2FD) else Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(event.time, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D2B45))
                            Text(event.label, fontSize = 14.sp, color = if (isHigh) Color(0xFF0066CC) else Color(0xFF607D8B))
                        }
                        Text("${event.height} м", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (isHigh) Color(0xFF0066CC) else Color(0xFF37474F))
                    }
                }
            }
        }
    }
}

@Composable
fun TideChart(hourly: List<HourlyPoint>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (hourly.size < 2) return@Canvas
        val minH = 0.0f
        val maxH = 5.2f
        val stepX = size.width / (hourly.size - 1)
        val path = Path()
        hourly.forEachIndexed { i, point ->
            val x = i * stepX
            val normalizedY = ((point.height.toFloat() - minH) / (maxH - minH)).coerceIn(0f, 1f)
            val y = size.height * (1f - normalizedY)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            if (point.hour % 6 == 0) {
                drawCircle(color = Color(0xFF0066CC), radius = 5f, center = Offset(x, y))
            }
        }
        drawPath(path = path, color = Color(0xFF0066CC), style = Stroke(width = 6f))
    }
}

fun loadTidesFromAssets(context: android.content.Context): List<DayTides> {
    val result = mutableListOf<DayTides>()
    try {
        val jsonStr = context.assets.open("tides_gertner_dec2026.json").bufferedReader().use { it.readText() }
        val root = JSONObject(jsonStr)
        val daysArr = root.getJSONArray("days")
        for (i in 0 until daysArr.length()) {
            val dObj = daysArr.getJSONObject(i)
            val date = dObj.getString("date")
            val events = mutableListOf<TideEvent>()
            val evArr = dObj.getJSONArray("events")
            for (j in 0 until evArr.length()) {
                val e = evArr.getJSONObject(j)
                events.add(TideEvent(e.getString("time"), e.getString("type"), e.getString("label"), e.getDouble("height")))
            }
            val hourly = mutableListOf<HourlyPoint>()
            val hArr = dObj.getJSONArray("hourly")
            for (j in 0 until hArr.length()) {
                val h = hArr.getJSONObject(j)
                hourly.add(HourlyPoint(h.getInt("hour"), h.getDouble("height")))
            }
            result.add(DayTides(date, events, hourly))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return result
}
