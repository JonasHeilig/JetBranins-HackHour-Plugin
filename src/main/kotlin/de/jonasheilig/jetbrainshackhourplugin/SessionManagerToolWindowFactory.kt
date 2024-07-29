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

        val sessionManagerPanel = JPanel(GridBagLayout())
        val sessionConstraints = GridBagConstraints().apply {
            gridx = 0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.CENTER
            insets = Insets(10, 10, 10, 10)
        }

        val startButton = JButton("Start Session")
        val pauseButton = JButton("Pause Session")
        val stopButton = JButton("Stop Session")

        sessionManagerPanel.add(startButton, sessionConstraints.apply { gridy = 0 })
        sessionManagerPanel.add(pauseButton, sessionConstraints.apply { gridy = 1 })
        sessionManagerPanel.add(stopButton, sessionConstraints.apply { gridy = 2 })

        mainPanel.add(slackIdLabel, constraints.apply { gridy = 0 })
        mainPanel.add(slackIdField, constraints.apply { gridy = 1 })
        mainPanel.add(apiKeyLabel, constraints.apply { gridy = 2 })
        mainPanel.add(apiKeyField, constraints.apply { gridy = 3 })
        mainPanel.add(submitButton, constraints.apply { gridy = 4 })

        constraints.gridy = 5
        constraints.weighty = 0.0
        mainPanel.add(Box.createVerticalStrut(20), constraints)

        panel.add(mainPanel, BorderLayout.CENTER)
        panel.add(sessionManagerPanel, BorderLayout.EAST)

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

    }
}
