package de.jonasheilig.jetbrainshackhourplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.ide.util.PropertiesComponent
import java.net.HttpURLConnection
import java.net.URL
import java.io.InputStreamReader
import com.google.gson.Gson
import com.google.gson.JsonObject

class ShowHoursAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val properties = PropertiesComponent.getInstance()
        var slackId = properties.getValue("hackhour.slackId")
        var apiKey = properties.getValue("hackhour.apiKey")

        if (slackId.isNullOrEmpty() || apiKey.isNullOrEmpty()) {
            slackId = Messages.showInputDialog(
                "Please enter your Slack ID:",
                "Slack ID",
                Messages.getQuestionIcon()
            )
            apiKey = Messages.showInputDialog(
                "Please enter your API Key:",
                "API Key",
                Messages.getQuestionIcon()
            )

            if (slackId.isNullOrEmpty() || apiKey.isNullOrEmpty()) {
                Messages.showMessageDialog(
                    "Slack ID and API Key cannot be empty.",
                    "Error",
                    Messages.getErrorIcon()
                )
                return
            }

            properties.setValue("hackhour.slackId", slackId)
            properties.setValue("hackhour.apiKey", apiKey)
        }

        val urlString = "https://hackhour.hackclub.com/api/stats/$slackId"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $apiKey")

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = InputStreamReader(connection.inputStream).use { it.readText() }
            val json = Gson().fromJson(response, JsonObject::class.java)
            val sessions = json["data"].asJsonObject["sessions"].asInt
            val totalHours = json["data"].asJsonObject["total"].asInt

            Messages.showMessageDialog(
                "You have $sessions sessions and a total of $totalHours hours.",
                "Hack Hours",
                Messages.getInformationIcon()
            )
        } else {
            Messages.showMessageDialog(
                "Failed to fetch hours. Response code: $responseCode",
                "Error",
                Messages.getErrorIcon()
            )
        }
    }
}
