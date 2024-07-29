package de.jonasheilig.jetbrainshackhourplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import java.net.HttpURLConnection
import java.net.URL
import java.io.InputStreamReader
import java.io.BufferedReader
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class ShowStatusAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val pingResponse = getApiResponse("https://hackhour.hackclub.com/ping")
        val statusResponse = getApiResponse("https://hackhour.hackclub.com/status")

        val statusMessage = if (pingResponse == "pong") {
            val statusJson = JsonParser.parseString(statusResponse).asJsonObject
            val activeSessions = statusJson.get("activeSessions")?.asInt ?: -1
            val airtableConnected = statusJson.get("airtableConnected")?.asBoolean ?: false
            val slackConnected = statusJson.get("slackConnected")?.asBoolean ?: false

            """
            API Status:
            - Ping: $pingResponse
            - Active Sessions: $activeSessions
            - Airtable Connected: $airtableConnected
            - Slack Connected: $slackConnected
            """.trimIndent()
        } else {
            "API is not reachable."
        }

        Messages.showMessageDialog(
            e.project,
            statusMessage,
            "Hack Hour API Status",
            Messages.getInformationIcon()
        )
    }

    private fun getApiResponse(url: String): String? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                response
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
