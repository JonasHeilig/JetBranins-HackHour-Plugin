package de.jonasheilig.jetbrainshackhourplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import java.awt.Desktop
import java.net.URI

class MyAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val pluginInfo = """
            Plugin Name: Hack Hour Plugin
            Developer: Jonas Heilig
            Version: 1.0.0
            Description: This Plugin is a simple UI for the Hack Hour API. It allows you to view your Hack Hour sessions. See your total Stats and start a new session.
            Website: https://jonasheilig.de
        """.trimIndent()

        val response = Messages.showYesNoDialog(
            pluginInfo,
            "Plugin-Informationen",
            "Go to Website",
            "Exit",
            Messages.getInformationIcon()
        )

        if (response == Messages.YES) {
            openWebsite("https://jonasheilig.de")
        }
    }

    private fun openWebsite(url: String) {
        try {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
            }
        } catch (ex: Exception) {
            Messages.showErrorDialog("Die Website konnte nicht geöffnet werden.", "Fehler")
        }
    }
}
