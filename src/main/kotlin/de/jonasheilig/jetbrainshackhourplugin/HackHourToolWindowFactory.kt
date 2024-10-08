package de.jonasheilig.jetbrainshackhourplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import java.awt.*
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import com.google.gson.Gson
import com.google.gson.JsonObject

class HackHourToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: com.intellij.openapi.project.Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())

        val mainPanel = JPanel(GridBagLayout())
        mainPanel.border = EmptyBorder(10, 10, 10, 10)
        val constraints = GridBagConstraints().apply {
            gridx = 0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.CENTER
            insets = Insets(10, 10, 10, 10)
        }

        val credentialsPanel = JPanel(GridBagLayout()).apply {
            border = TitledBorder("Credentials")
        }
        val slackIdLabel = JLabel("Slack ID:")
        val slackIdField = JTextField(20)
        val apiKeyLabel = JLabel("API Key:")
        val apiKeyField = JTextField(20)
        val submitButton = JButton("Submit")

        val properties = PropertiesComponent.getInstance()
        val savedSlackId = properties.getValue("hackhour.slackId", "")
        val savedApiKey = properties.getValue("hackhour.apiKey", "")
        slackIdField.text = savedSlackId
        apiKeyField.text = savedApiKey

        credentialsPanel.add(slackIdLabel, constraints.apply { gridy = 0 })
        credentialsPanel.add(slackIdField, constraints.apply { gridy = 1 })
        credentialsPanel.add(apiKeyLabel, constraints.apply { gridy = 2 })
        credentialsPanel.add(apiKeyField, constraints.apply { gridy = 3 })
        credentialsPanel.add(submitButton, constraints.apply { gridy = 4 })

        val statsPanel = JPanel(GridBagLayout()).apply {
            border = TitledBorder("Your Stats")
        }
        val yourMinutesLabel = JLabel("Your Sessions:").apply { font = font.deriveFont(Font.BOLD) }
        val yourMinutesValue = JLabel("0").apply { font = font.deriveFont(Font.PLAIN, 16f) }
        val yourHoursLabel = JLabel("Your Minutes:").apply { font = font.deriveFont(Font.BOLD) }
        val yourHoursValue = JLabel("0").apply { font = font.deriveFont(Font.PLAIN, 16f) }
        val updateButton = JButton("Update")

        statsPanel.add(yourMinutesLabel, constraints.apply { gridy = 0 })
        statsPanel.add(yourMinutesValue, constraints.apply { gridy = 1 })
        statsPanel.add(yourHoursLabel, constraints.apply { gridy = 2 })
        statsPanel.add(yourHoursValue, constraints.apply { gridy = 3 })
        statsPanel.add(updateButton, constraints.apply { gridy = 4 })

        val actionsPanel = JPanel(GridBagLayout()).apply {
            border = TitledBorder("Actions")
        }
        val viewSessionsButton = JButton("View All Sessions")
        val debugButton = JButton("Show Raw Response")

        actionsPanel.add(viewSessionsButton, constraints.apply { gridy = 0 })
        actionsPanel.add(debugButton, constraints.apply { gridy = 1 })

        mainPanel.add(credentialsPanel, constraints.apply { gridy = 0; gridx = 0 })
        mainPanel.add(statsPanel, constraints.apply { gridy = 1; gridx = 0 })
        mainPanel.add(actionsPanel, constraints.apply { gridy = 2; gridx = 0 })

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

        viewSessionsButton.addActionListener {
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
                    val urlString = "https://hackhour.hackclub.com/api/history/$slackId"
                    val uri = URI(urlString)
                    val url = uri.toURL()
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $apiKey")

                    val responseCode = connection.responseCode
                    val response = InputStreamReader(connection.inputStream).use { it.readText() }

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val json = Gson().fromJson(response, JsonObject::class.java)
                        val sessionsArray = json["data"].asJsonArray

                        SwingUtilities.invokeLater {
                            val sessionsFrame = JFrame("All Sessions")
                            sessionsFrame.setSize(800, 600)
                            sessionsFrame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE

                            val sessionsTable = JTable(
                                Array(sessionsArray.size()) { i ->
                                    val session = sessionsArray[i].asJsonObject
                                    arrayOf(
                                        session["createdAt"].asString,
                                        session["time"].asInt.toString(),
                                        session["elapsed"].asInt.toString(),
                                        session["goal"].asString,
                                        session["ended"].asBoolean.toString(),
                                        session["work"].asString
                                    )
                                },
                                arrayOf("Created At", "Time", "Elapsed", "Goal", "Ended", "Work")
                            )

                            sessionsFrame.add(JScrollPane(sessionsTable))
                            sessionsFrame.isVisible = true
                        }
                    } else {
                        SwingUtilities.invokeLater {
                            Messages.showMessageDialog(
                                "Failed to fetch session history. Response code: $responseCode",
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

        panel.add(mainPanel, BorderLayout.CENTER)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
