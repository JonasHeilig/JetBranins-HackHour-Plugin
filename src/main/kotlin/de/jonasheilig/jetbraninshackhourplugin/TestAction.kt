package de.jonasheilig.jetbraninshackhourplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class MyAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        Messages.showMessageDialog("Hello, World!", "Plugin", Messages.getInformationIcon())
    }
}
