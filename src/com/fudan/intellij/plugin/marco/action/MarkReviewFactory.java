package com.fudan.intellij.plugin.marco.action;

import com.fudan.intellij.plugin.marco.common.InnerProjectCache;
import com.fudan.intellij.plugin.marco.common.ProjectInstanceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.Icons;
import org.jetbrains.annotations.NotNull;

public class MarkReviewFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        String locationHash = project.getLocationHash();

        InnerProjectCache projectCache = ProjectInstanceManager.getInstance().getProjectCache(locationHash);
        if (projectCache == null) {
            projectCache = new InnerProjectCache(project);
            ProjectInstanceManager.getInstance().addProjectCache(project.getLocationHash(), projectCache);
        }
        MarkReviewWindowUI markReviewWindowUI = projectCache.getMarkReviewWindowUI();
        if (markReviewWindowUI == null) {
            markReviewWindowUI = new MarkReviewWindowUI(project);
            projectCache.setMarkReviewWindowUI(markReviewWindowUI);
        }
        markReviewWindowUI.initUI();

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(markReviewWindowUI.fullPanel,"", false);
        toolWindow.getContentManager().addContent(content);
        toolWindow.setIcon(Icons.UI_FORM_ICON);

    }
}
