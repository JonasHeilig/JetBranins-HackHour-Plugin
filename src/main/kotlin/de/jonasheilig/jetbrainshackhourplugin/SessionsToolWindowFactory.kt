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

class SessionsToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: com.intellij.openapi.project.Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())

        val mainPanel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            gridx = 0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.CENTER
            insets = Insets(10, 10, 10, 10)
        }

        val viewSessionsButton = JButton("View All Sessions")

        mainPanel.add(viewSessionsButton, constraints.apply { gridy = 0 })

        viewSessionsButton.addActionListener {
            val properties = PropertiesComponent.getInstance()
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
