package de.jonasheilig.jetbrainshackhourplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.*
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import com.google.gson.Gson
import com.google.gson.JsonObject

class HackHourToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: com.intellij.openapi.project.Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())

        val mainPanel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            gridx = 0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.CENTER
            insets = Insets(10, 10, 10, 10)
        }

        val slackIdLabel = JLabel("Slack ID:")
        val slackIdField = JTextField(20)
        val apiKeyLabel = JLabel("API Key:")
        val apiKeyField = JTextField(20)
        val submitButton = JButton("Submit")

        val yourMinutesLabel = JLabel("Your Sessions:")
        val yourMinutesValue = JLabel("0")
        val yourHoursLabel = JLabel("Your Minutes:")
        val yourHoursValue = JLabel("0")

        val updateButton = JButton("Update")
        val debugButton = JButton("Show Raw Response")

        val properties = PropertiesComponent.getInstance()
        val savedSlackId = properties.getValue("hackhour.slackId", "")
        val savedApiKey = properties.getValue("hackhour.apiKey", "")
        slackIdField.text = savedSlackId
        apiKeyField.text = savedApiKey

        mainPanel.add(slackIdLabel, constraints.apply { gridy = 0 })
        mainPanel.add(slackIdField, constraints.apply { gridy = 1 })
        mainPanel.add(apiKeyLabel, constraints.apply { gridy = 2 })
        mainPanel.add(apiKeyField, constraints.apply { gridy = 3 })
        mainPanel.add(submitButton, constraints.apply { gridy = 4 })

        val smallerSpacer = Box.createVerticalStrut(20)
        constraints.gridy = 5
        constraints.weighty = 0.0
        mainPanel.add(smallerSpacer, constraints)

        constraints.gridy = 6
        constraints.weighty = 0.0
        mainPanel.add(yourMinutesLabel, constraints)
        constraints.gridy = 7
        mainPanel.add(yourMinutesValue, constraints)
        constraints.gridy = 8
        mainPanel.add(yourHoursLabel, constraints)
        constraints.gridy = 9
        mainPanel.add(yourHoursValue, constraints)
        constraints.gridy = 10
        mainPanel.add(updateButton, constraints)
        constraints.gridy = 11
        mainPanel.add(debugButton, constraints)

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
                    val uri = URI(urlString)
                    val url = uri.toURL()
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $apiKey")

                    val responseCode = connection.responseCode
                    val response = InputStreamReader(connection.inputStream).use { it.readText() }

                    if (responseCode == HttpURLConnection.HTTP_OK) {
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

                    debugButton.addActionListener {
                        SwingUtilities.invokeLater {
                            Messages.showMessageDialog(
                                response,
                                "Raw Response",
                                Messages.getInformationIcon()
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

        panel.add(mainPanel, BorderLayout.CENTER)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
