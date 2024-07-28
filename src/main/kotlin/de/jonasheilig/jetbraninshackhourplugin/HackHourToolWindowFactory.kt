package de.jonasheilig.jetbraninshackhourplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.*
import java.awt.BorderLayout
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson
import com.google.gson.JsonObject

class HackHourToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: com.intellij.openapi.project.Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        val inputPanel = JPanel()
        val displayPanel = JPanel()

        val slackIdLabel = JLabel("Slack ID:")
        val slackIdField = JTextField(20)

        val apiKeyLabel = JLabel("API Key:")
        val apiKeyField = JTextField(20)

        val submitButton = JButton("Submit")

        val yourMinutesLabel = JLabel("Your Minutes:")
        val yourMinutesValue = JLabel("0")

        val yourHoursLabel = JLabel("Your Hours:")
        val yourHoursValue = JLabel("0")

        val updateButton = JButton("Update")

        val properties = PropertiesComponent.getInstance()
        val savedSlackId = properties.getValue("hackhour.slackId", "")
        val savedApiKey = properties.getValue("hackhour.apiKey", "")
        slackIdField.text = savedSlackId
        apiKeyField.text = savedApiKey

        submitButton.addActionListener {
            val slackId = slackIdField.text
            val apiKey = apiKeyField.text

            if (slackId.isEmpty() || apiKey.isEmpty()) {
                Messages.showMessageDialog(
                    "Please enter both Slack ID and API Key.",
                    "Error",
                    Messages.getErrorIcon()
                )
            } else {
                properties.setValue("hackhour.slackId", slackId)
                properties.setValue("hackhour.apiKey", apiKey)
                Messages.showMessageDialog(
                    "Slack ID and API Key saved successfully!",
                    "Info",
                    Messages.getInformationIcon()
                )
            }
        }

        updateButton.addActionListener {
            val slackId = properties.getValue("hackhour.slackId", "")
            val apiKey = properties.getValue("hackhour.apiKey", "")

            if (slackId.isEmpty() || apiKey.isEmpty()) {
                Messages.showMessageDialog(
                    "Slack ID or API Key not set. Please configure them first.",
                    "Error",
                    Messages.getErrorIcon()
                )
                return@addActionListener
            }

            ApplicationManager.getApplication().executeOnPooledThread {
                try {
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

                        SwingUtilities.invokeLater {
                            yourMinutesValue.text = sessions.toString()
                            yourHoursValue.text = totalHours.toString()
                        }
                    } else {
                        SwingUtilities.invokeLater {
                            Messages.showMessageDialog(
                                "Failed to fetch hours. Response code: $responseCode",
                                "Error",
                                Messages.getErrorIcon()
                            )
                        }
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        Messages.showMessageDialog(
                            "An error occurred: ${e.message}",
                            "Error",
                            Messages.getErrorIcon()
                        )
                    }
                }
            }
        }

        inputPanel.layout = BoxLayout(inputPanel, BoxLayout.Y_AXIS)
        inputPanel.add(slackIdLabel)
        inputPanel.add(slackIdField)
        inputPanel.add(apiKeyLabel)
        inputPanel.add(apiKeyField)
        inputPanel.add(submitButton)

        displayPanel.layout = BoxLayout(displayPanel, BoxLayout.Y_AXIS)
        displayPanel.add(yourMinutesLabel)
        displayPanel.add(yourHoursValue)
        displayPanel.add(yourHoursLabel)
        displayPanel.add(yourMinutesValue)
        displayPanel.add(updateButton)

        panel.add(inputPanel, BorderLayout.NORTH)
        panel.add(displayPanel, BorderLayout.CENTER)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
