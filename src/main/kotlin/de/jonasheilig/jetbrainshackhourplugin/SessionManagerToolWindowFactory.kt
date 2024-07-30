package de.jonasheilig.jetbrainshackhourplugin

import javax.swing.table.AbstractTableModel
import javax.swing.JTable
import javax.swing.JScrollPane
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import javax.swing.*
import javax.swing.border.TitledBorder
import java.awt.*
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

class SessionManagerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: com.intellij.openapi.project.Project, toolWindow: ToolWindow) {
        val mainPanel = JPanel(BorderLayout())

        val formPanel = JPanel(GridBagLayout()).apply {
            border = TitledBorder("Credentials")
        }
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

        val manageSessionPanel = JPanel(GridBagLayout()).apply {
            border = TitledBorder("Manage Session")
        }
        constraints.gridy = 0
        val startButton = JButton("Start Session")
        val pauseButton = JButton("Pause Session")
        val stopButton = JButton("Stop Session")
        manageSessionPanel.add(startButton, constraints)
        constraints.gridy = 1
        manageSessionPanel.add(pauseButton, constraints)
        constraints.gridy = 2
        manageSessionPanel.add(stopButton, constraints)

        val infoPanel = JPanel(GridBagLayout()).apply {
            border = TitledBorder("Info")
        }
        constraints.gridy = 0
        val sessionInfoButton = JButton("Get Session Info")
        val statsButton = JButton("Get Stats")
        val goalsButton = JButton("Get Goals")
        val historyButton = JButton("Get History")
        infoPanel.add(sessionInfoButton, constraints)
        constraints.gridy = 1
        infoPanel.add(statsButton, constraints)
        constraints.gridy = 2
        infoPanel.add(goalsButton, constraints)
        constraints.gridy = 3
        infoPanel.add(historyButton, constraints)

        val displayPanel = JPanel(GridBagLayout()).apply {
            border = TitledBorder("Display")
        }
        val displayConstraints = GridBagConstraints().apply {
            gridx = 0
            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH
            insets = Insets(10, 10, 10, 10)
        }

        val sessionInfoArea = JTextArea(10, 30).apply { isEditable = false }
        val statsArea = JTextArea(10, 30).apply { isEditable = false }
        val goalsArea = JTextArea(10, 30).apply { isEditable = false }

        val sessionInfoScrollPane = JScrollPane(sessionInfoArea)
        val statsScrollPane = JScrollPane(statsArea)
        val goalsScrollPane = JScrollPane(goalsArea)

        displayPanel.add(sessionInfoScrollPane, displayConstraints.apply { gridy = 0 })
        displayPanel.add(statsScrollPane, displayConstraints.apply { gridy = 1 })
        displayPanel.add(goalsScrollPane, displayConstraints.apply { gridy = 2 })

        mainPanel.add(formPanel, BorderLayout.NORTH)
        mainPanel.add(manageSessionPanel, BorderLayout.WEST)
        mainPanel.add(infoPanel, BorderLayout.EAST)
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

                startButton.addActionListener { manageSession(slackId, apiKey, "start", "Session started successfully.", sessionInfoArea) }
                pauseButton.addActionListener { manageSession(slackId, apiKey, "pause", "Session paused successfully.", sessionInfoArea) }
                stopButton.addActionListener { manageSession(slackId, apiKey, "cancel", "Session canceled successfully.", sessionInfoArea) }
                sessionInfoButton.addActionListener { fetchSessionInfo(slackId, apiKey, sessionInfoArea) }
                statsButton.addActionListener { fetchStats(slackId, apiKey, statsArea) }
                goalsButton.addActionListener { fetchGoals(slackId, apiKey, goalsArea) }
                historyButton.addActionListener { fetchHistory(slackId, apiKey) }

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

    private fun manageSession(slackId: String, apiKey: String, action: String, successMessage: String, displayArea: JTextArea) {
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
                        displayArea.text = formatResponse(response)
                        Messages.showMessageDialog(
                            successMessage,
                            "Info",
                            Messages.getInformationIcon()
                        )
                    }
                } else {
                    SwingUtilities.invokeLater {
                        displayArea.text = "Failed to perform action. Response code: $responseCode"
                        Messages.showMessageDialog(
                            "Failed to perform action. Response code: $responseCode",
                            "Error",
                            Messages.getErrorIcon()
                        )
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    displayArea.text = "An error occurred: ${e.message}"
                    Messages.showMessageDialog(
                        "An error occurred: ${e.message}",
                        "Error",
                        Messages.getErrorIcon()
                    )
                }
            }
        }
    }

    private fun fetchSessionInfo(slackId: String, apiKey: String, displayArea: JTextArea) {
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
                        val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                        val data = jsonObject.getAsJsonObject("data")
                        displayArea.text = buildString {
                            append("Session ID: ${data["id"].asString}\n")
                            append("Created At: ${data["createdAt"].asString}\n")
                            append("Elapsed Time: ${data["elapsed"].asInt} minutes\n")
                            append("Remaining Time: ${data["remaining"].asInt} minutes\n")
                            append("Status: ${data["status"].asString}\n")
                        }
                    } else {
                        displayArea.text = "Failed to fetch session info. Response code: $responseCode"
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    displayArea.text = "An error occurred: ${e.message}"
                }
            }
        }
    }

    private fun fetchStats(slackId: String, apiKey: String, displayArea: JTextArea) {
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
                        val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                        val data = jsonObject.getAsJsonObject("data")
                        displayArea.text = buildString {
                            append("Total Sessions: ${data["totalSessions"].asInt}\n")
                            append("Total Hours: ${data["totalHours"].asFloat}\n")
                        }
                    } else {
                        displayArea.text = "Failed to fetch stats. Response code: $responseCode"
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    displayArea.text = "An error occurred: ${e.message}"
                }
            }
        }
    }

    private fun fetchGoals(slackId: String, apiKey: String, displayArea: JTextArea) {
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
                        val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                        val data = jsonObject.getAsJsonObject("data")
                        displayArea.text = buildString {
                            append("Current Goal: ${data["currentGoal"].asString}\n")
                            append("Completed Goals: ${data["completedGoals"].asInt}\n")
                        }
                    } else {
                        displayArea.text = "Failed to fetch goals. Response code: $responseCode"
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    displayArea.text = "An error occurred: ${e.message}"
                }
            }
        }
    }

    private fun fetchHistory(slackId: String, apiKey: String) {
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
                        val jsonArray = Gson().fromJson(response, JsonArray::class.java)
                        val tableModel = HistoryTableModel(jsonArray)
                        val historyTable = JTable(tableModel)

                        val historyScrollPane = JScrollPane(historyTable)
                        historyTable.autoResizeMode = JTable.AUTO_RESIZE_OFF
                        historyTable.setFillsViewportHeight(true)

                        val historyFrame = JFrame("Session History")
                        historyFrame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                        historyFrame.size = Dimension(800, 600)
                        historyFrame.layout = BorderLayout()
                        historyFrame.add(historyScrollPane, BorderLayout.CENTER)
                        historyFrame.isVisible = true
                    } else {
                        Messages.showMessageDialog(
                            "Failed to fetch history. Response code: $responseCode",
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

    private fun formatResponse(response: String): String {
        return response
    }
}

class HistoryTableModel(private val jsonArray: JsonArray) : AbstractTableModel() {

    private val columnNames = listOf("ID", "Created At", "Elapsed", "Remaining", "Status")

    override fun getRowCount(): Int = jsonArray.size()

    override fun getColumnCount(): Int = columnNames.size

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val jsonObject = jsonArray.get(rowIndex).asJsonObject
        return when (columnIndex) {
            0 -> jsonObject.get("id").asString
            1 -> jsonObject.get("createdAt").asString
            2 -> jsonObject.get("elapsed").asInt
            3 -> jsonObject.get("remaining").asInt
            4 -> jsonObject.get("status").asString
            else -> ""
        }
    }

    override fun getColumnName(column: Int): String {
        return columnNames[column]
    }
}
