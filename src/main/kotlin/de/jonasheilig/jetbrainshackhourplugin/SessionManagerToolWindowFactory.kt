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
        val mainPanel = JPanel(BorderLayout())
        val formPanel = JPanel(GridBagLayout())
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

        constraints.gridy = 0
        formPanel.add(slackIdLabel, constraints)
        constraints.gridy = 1
        formPanel.add(slackIdField, constraints)
        constraints.gridy = 2
        formPanel.add(apiKeyLabel, constraints)
        constraints.gridy = 3
        formPanel.add(apiKeyField, constraints)
        constraints.gridy = 4
        formPanel.add(submitButton, constraints)

        val buttonPanel = JPanel(GridBagLayout())
        constraints.weightx = 1.0
        constraints.fill = GridBagConstraints.HORIZONTAL

        val startButton = JButton("Start Session")
        val pauseButton = JButton("Pause Session")
        val stopButton = JButton("Stop Session")
        val sessionInfoButton = JButton("Get Session Info")
        val statsButton = JButton("Get Stats")
        val goalsButton = JButton("Get Goals")
        val historyButton = JButton("Get History")

        constraints.gridy = 0
        buttonPanel.add(startButton, constraints)
        constraints.gridy = 1
        buttonPanel.add(pauseButton, constraints)
        constraints.gridy = 2
        buttonPanel.add(stopButton, constraints)
        constraints.gridy = 3
        buttonPanel.add(sessionInfoButton, constraints)
        constraints.gridy = 4
        buttonPanel.add(statsButton, constraints)
        constraints.gridy = 5
        buttonPanel.add(goalsButton, constraints)
        constraints.gridy = 6
        buttonPanel.add(historyButton, constraints)

        val displayPanel = JPanel(GridBagLayout())
        val displayConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH
            insets = Insets(10, 10, 10, 10)
        }

        val sessionInfoArea = JTextArea(10, 30)
        sessionInfoArea.isEditable = false
        displayPanel.add(JScrollPane(sessionInfoArea), displayConstraints.apply { gridy = 0 })

        val statsArea = JTextArea(10, 30)
        statsArea.isEditable = false
        displayPanel.add(JScrollPane(statsArea), displayConstraints.apply { gridy = 1 })

        val goalsArea = JTextArea(10, 30)
        goalsArea.isEditable = false
        displayPanel.add(JScrollPane(goalsArea), displayConstraints.apply { gridy = 2 })

        val historyArea = JTextArea(10, 30)
        historyArea.isEditable = false
        displayPanel.add(JScrollPane(historyArea), displayConstraints.apply { gridy = 3 })

        mainPanel.add(formPanel, BorderLayout.NORTH)
        mainPanel.add(buttonPanel, BorderLayout.WEST)
        mainPanel.add(displayPanel, BorderLayout.CENTER)

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
        val content = contentFactory.createContent(mainPanel, "", false)
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

    private fun fetchGoals(slackId: String, apiKey: String, goalsArea: JTextArea) {
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
                val urlString = "https://hackhour.hackclub.com/api/goals/$slackId"
                val uri = URI(urlString)
                val url = uri.toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $apiKey")

                val responseCode = connection.responseCode
                val response = InputStreamReader(connection.inputStream).use { it.readText() }

                SwingUtilities.invokeLater {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        goalsArea.text = response
                    } else {
                        goalsArea.text = "Failed to fetch goals. Response code: $responseCode"
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    goalsArea.text = "An error occurred: ${e.message}"
                }
            }
        }
    }

    private fun fetchHistory(slackId: String, apiKey: String, historyArea: JTextArea) {
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
                val urlString = "https://hackhour.hackclub.com/api/history/$slackId"
                val uri = URI(urlString)
                val url = uri.toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $apiKey")

                val responseCode = connection.responseCode
                val response = InputStreamReader(connection.inputStream).use { it.readText() }

                SwingUtilities.invokeLater {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        historyArea.text = response
                    } else {
                        historyArea.text = "Failed to fetch history. Response code: $responseCode"
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    historyArea.text = "An error occurred: ${e.message}"
                }
            }
        }
    }
}
