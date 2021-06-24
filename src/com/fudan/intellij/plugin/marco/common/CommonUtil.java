package com.fudan.intellij.plugin.marco.common;

import com.fudan.intellij.plugin.marco.action.MarkReviewWindowUI;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import java.io.Closeable;
import java.io.IOException;
public class CommonUtil {

    public static void closeQuitely(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void reloadCommentListShow(Project project) {
        try {
            InnerProjectCache projectCache = ProjectInstanceManager.getInstance().getProjectCache(project.getLocationHash());

            MarkReviewWindowUI markReviewWindowUI = projectCache.getMarkReviewWindowUI();
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CodeReview");

            if (markReviewWindowUI != null && toolWindow != null) {
                markReviewWindowUI.reloadDraftTableData();
            } else {
                System.out.println("manageReviewCommentUI = " + markReviewWindowUI);
                System.out.println("toolWindow = " + toolWindow);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
