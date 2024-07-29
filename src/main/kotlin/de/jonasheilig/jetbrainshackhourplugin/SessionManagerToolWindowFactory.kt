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

class SessionManagerToolWindowFactory : ToolWindowFactory {

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

        val startButton = JButton("Start Session")
        val pauseButton = JButton("Pause Session")
        val stopButton = JButton("Stop Session")
        val sessionInfoButton = JButton("Get Session Info")
        val statsButton = JButton("Get Stats")
        val goalsButton = JButton("Get Goals")
        val historyButton = JButton("Get History")

        val sessionInfoPanel = JPanel(BorderLayout())
        val sessionInfoArea = JTextArea(10, 30)
        sessionInfoArea.isEditable = false
        sessionInfoPanel.add(JScrollPane(sessionInfoArea), BorderLayout.CENTER)

        val statsPanel = JPanel(BorderLayout())
        val statsArea = JTextArea(10, 30)
        statsArea.isEditable = false
        statsPanel.add(JScrollPane(statsArea), BorderLayout.CENTER)

        val goalsPanel = JPanel(BorderLayout())
        val goalsArea = JTextArea(10, 30)
        goalsArea.isEditable = false
        goalsPanel.add(JScrollPane(goalsArea), BorderLayout.CENTER)

        val historyPanel = JPanel(BorderLayout())
        val historyArea = JTextArea(10, 30)
        historyArea.isEditable = false
        historyPanel.add(JScrollPane(historyArea), BorderLayout.CENTER)

        mainPanel.add(slackIdLabel, constraints.apply { gridy = 0 })
        mainPanel.add(slackIdField, constraints.apply { gridy = 1 })
        mainPanel.add(apiKeyLabel, constraints.apply { gridy = 2 })
        mainPanel.add(apiKeyField, constraints.apply { gridy = 3 })
        mainPanel.add(submitButton, constraints.apply { gridy = 4 })

        constraints.gridy = 5
        constraints.weighty = 0.0
        mainPanel.add(Box.createVerticalStrut(20), constraints)

        mainPanel.add(startButton, constraints.apply { gridy = 6 })
        mainPanel.add(pauseButton, constraints.apply { gridy = 7 })
        mainPanel.add(stopButton, constraints.apply { gridy = 8 })
        mainPanel.add(sessionInfoButton, constraints.apply { gridy = 9 })
        mainPanel.add(statsButton, constraints.apply { gridy = 10 })
        mainPanel.add(goalsButton, constraints.apply { gridy = 11 })
        mainPanel.add(historyButton, constraints.apply { gridy = 12 })

        panel.add(mainPanel, BorderLayout.CENTER)
        panel.add(sessionInfoPanel, BorderLayout.EAST)
        panel.add(statsPanel, BorderLayout.SOUTH)
        panel.add(goalsPanel, BorderLayout.WEST)
        panel.add(historyPanel, BorderLayout.NORTH)

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
                val properties = PropertiesComponent.getInstance()
                properties.setValue("hackhour.slackId", slackId)
                properties.setValue("hackhour.apiKey", apiKey)

                startButton.addActionListener { manageSession(slackId, apiKey, "start-session", "Session started successfully.") }
                pauseButton.addActionListener { manageSession(slackId, apiKey, "pause-session", "Session paused successfully.") }
                stopButton.addActionListener { manageSession(slackId, apiKey, "stop-session", "Session stopped successfully.") }
                sessionInfoButton.addActionListener { fetchSessionInfo(slackId, apiKey, sessionInfoArea) }
                statsButton.addActionListener { fetchStats(slackId, apiKey, statsArea) }
                goalsButton.addActionListener { fetchGoals(slackId, apiKey, goalsArea) }
                historyButton.addActionListener { fetchHistory(slackId, apiKey, historyArea) }

                Messages.showMessageDialog(
                    "Slack ID and API Key saved successfully!",
                    "Info",
                    Messages.getInformationIcon()
                )
            }
        }

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun manageSession(slackId: String, apiKey: String, action: String, successMessage: String) {
        if (slackId.isEmpty() || apiKey.isEmpty()) {
            Messages.showMessageDialog(
                "Slack ID or API Key not set. Please configure them first.",
                "Error",
                Messages.getErrorIcon()
            )
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val urlString = "https://hackhour.hackclub.com/api/$action/$slackId"
                val uri = URI(urlString)
                val url = uri.toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $apiKey")

                val responseCode = connection.responseCode
                val response = InputStreamReader(connection.inputStream).use { it.readText() }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    SwingUtilities.invokeLater {
                        Messages.showMessageDialog(
                            successMessage,
                            "Info",
                            Messages.getInformationIcon()
                        )
                    }
                } else {
                    SwingUtilities.invokeLater {
                        Messages.showMessageDialog(
                            "Failed to perform action. Response code: $responseCode",
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

    private fun fetchSessionInfo(slackId: String, apiKey: String, sessionInfoArea: JTextArea) {
        if (slackId.isEmpty() || apiKey.isEmpty()) {
            Messages.showMessageDialog(
                "Slack ID or API Key not set. Please configure them first.",
                "Error",
                Messages.getErrorIcon()
            )
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val urlString = "https://hackhour.hackclub.com/api/session/$slackId"
                val uri = URI(urlString)
                val url = uri.toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $apiKey")

                val responseCode = connection.responseCode
                val response = InputStreamReader(connection.inputStream).use { it.readText() }

                SwingUtilities.invokeLater {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        sessionInfoArea.text = response
                    } else {
                        sessionInfoArea.text = "Failed to fetch session info. Response code: $responseCode"
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    sessionInfoArea.text = "An error occurred: ${e.message}"
                }
            }
        }
    }

    private fun fetchStats(slackId: String, apiKey: String, statsArea: JTextArea) {
        if (slackId.isEmpty() || apiKey.isEmpty()) {
            Messages.showMessageDialog(
                "Slack ID or API Key not set. Please configure them first.",
                "Error",
                Messages.getErrorIcon()
            )
            return
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

                SwingUtilities.invokeLater {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        statsArea.text = response
                    } else {
                        statsArea.text = "Failed to fetch stats. Response code: $responseCode"
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    statsArea.text = "An error occurred: ${e.message}"
                }
            }
        }
    }


}
