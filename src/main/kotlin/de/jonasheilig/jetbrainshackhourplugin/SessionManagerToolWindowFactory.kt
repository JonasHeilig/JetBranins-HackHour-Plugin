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
        val pauseResumeButton = JButton("Pause Session")
        val stopButton = JButton("Stop Session")
        manageSessionPanel.add(startButton, constraints)
        constraints.gridy = 1
        manageSessionPanel.add(pauseResumeButton, constraints)
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

                startButton.addActionListener { manageSession(slackId, apiKey, "start", "Session started successfully.", sessionInfoArea) }
                pauseResumeButton.addActionListener { manageSession(slackId, apiKey, "pause", "Session paused successfully.", sessionInfoArea, pauseResumeButton) }
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

    private fun manageSession(slackId: String, apiKey: String, action: String, successMessage: String, displayArea: JTextArea, pauseResumeButton: JButton? = null) {
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

                SwingUtilities.invokeLater {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        displayArea.text = formatResponse(response)
                        Messages.showMessageDialog(
                            successMessage,
                            "Info",
                            Messages.getInformationIcon()
                        )
                        if (action == "pause" && pauseResumeButton != null) {
                            pauseResumeButton.text = "Resume Session"
                            pauseResumeButton.actionCommand = "resume"
                        } else if (action == "resume" && pauseResumeButton != null) {
                            pauseResumeButton.text = "Pause Session"
                            pauseResumeButton.actionCommand = "pause"
                        }
                    } else {
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
                        try {
                            val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                            val data = jsonObject.getAsJsonObject("data")
                            displayArea.text = buildString {
                                append("Created At: ${data["createdAt"]?.asString ?: "N/A"}\n")
                                append("Time: ${data["time"]?.asInt ?: 0} minutes\n")
                                append("Elapsed: ${data["elapsed"]?.asInt ?: 0} minutes\n")
                                append("Remaining: ${data["remaining"]?.asInt ?: 0} minutes\n")
                                append("End Time: ${data["endTime"]?.asString ?: "N/A"}\n")
                                append("Goal: ${data["goal"]?.asString ?: "N/A"}\n")
                                append("Paused: ${data["paused"]?.asBoolean ?: false}\n")
                                append("Completed: ${data["completed"]?.asBoolean ?: false}\n")
                                append("Message Timestamp: ${data["messageTs"]?.asString ?: "N/A"}\n")
                            }
                        } catch (e: Exception) {
                            displayArea.text = "Error parsing JSON response: ${e.message}"
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
                        try {
                            val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                            val data = jsonObject.getAsJsonObject("data")
                            displayArea.text = buildString {
                                append("Sessions: ${data["sessions"]?.asInt ?: 0}\n")
                                append("Total Time: ${data["total"]?.asInt ?: 0} minutes\n")
                            }
                        } catch (e: Exception) {
                            displayArea.text = "Error parsing JSON response: ${e.message}"
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
                        try {
                            val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                            val jsonArray = jsonObject.getAsJsonArray("data")
                            val goalsList = StringBuilder()
                            for (goal in jsonArray) {
                                val goalObj = goal.asJsonObject
                                val goalName = goalObj.get("name")?.asString ?: "N/A"
                                val goalMinutes = goalObj.get("minutes")?.asInt ?: 0
                                goalsList.append("Goal: $goalName, Minutes: $goalMinutes\n")
                            }
                            displayArea.text = goalsList.toString()
                        } catch (e: Exception) {
                            displayArea.text = "Error parsing JSON response: ${e.message}"
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
                        try {
                            val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                            val jsonArray = jsonObject.getAsJsonArray("data")
                            createHistoryDialog(jsonArray)
                        } catch (e: Exception) {
                            Messages.showMessageDialog(
                                "Error parsing JSON response: ${e.message}",
                                "Error",
                                Messages.getErrorIcon()
                            )
                        }
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

    private fun createHistoryDialog(historyArray: JsonArray) {
        val historyFrame = JFrame("History")
        val historyTable = JTable(HistoryTableModel(historyArray))
        val scrollPane = JScrollPane(historyTable)
        historyFrame.add(scrollPane)
        historyFrame.setSize(600, 400)
        historyFrame.setLocationRelativeTo(null)
        historyFrame.isVisible = true
    }

    private fun formatResponse(response: String): String {
        return response
    }

    private inner class HistoryTableModel(private val historyArray: JsonArray) : AbstractTableModel() {
        private val columnNames = arrayOf("Date", "Time", "Goal", "Work")

        override fun getRowCount(): Int = historyArray.size()

        override fun getColumnCount(): Int = columnNames.size

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val entry = historyArray[rowIndex].asJsonObject
            return when (columnIndex) {
                0 -> entry.get("createdAt")?.asString ?: "N/A"
                1 -> entry.get("time")?.asInt ?: 0
                2 -> entry.get("goal")?.asString ?: "N/A"
                3 -> entry.get("work")?.asString ?: "N/A"
                else -> "N/A"
            }
        }

        override fun getColumnName(column: Int): String {
            return columnNames[column]
        }
    }
}
