package com.fudan.intellij.plugin.marco.action;

import com.fudan.intellij.plugin.marco.common.InnerProjectCache;
import com.fudan.intellij.plugin.marco.common.ProjectInstanceManager;
import com.fudan.intellij.plugin.marco.entity.MarkReview;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.history.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsSelection;
import com.intellij.vcsUtil.VcsSelectionUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddMarkReviewAction extends AnAction {


    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        String locationHash = project.getLocationHash();
        InnerProjectCache projectCache = ProjectInstanceManager.getInstance().getProjectCache(locationHash);

        if (projectCache == null) {
            projectCache = new InnerProjectCache(project);
            ProjectInstanceManager.getInstance().addProjectCache(locationHash, projectCache);
        }

        if(projectCache.getGit().getRepository() == null){
            Messages.showErrorDialog("Cause:" + System.lineSeparator() + "未配置git仓库", "Init Failed");
            return;
        }

        try{
            if(!projectCache.getGit().status().call().isClean()){
                Messages.showErrorDialog("Cause:" + System.lineSeparator() + "git工作区的改动没有commit或者stash", "AddReview Failed");
                return;
            }
        }catch (Exception exception){

        }


        Editor data = e.getData(CommonDataKeys.EDITOR);
        SelectionModel selectionModel = data.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        if (selectedText == null || "".equals(selectedText)) {
            return;
        }

        final VcsSelection selection = VcsSelectionUtil.getSelection(e.getDataContext());
        assert selection != null;

        final VirtualFile file = FileDocumentManager.getInstance().getFile(selection.getDocument());
        assert file != null;

        final AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
        if(activeVcs == null){
            Messages.showErrorDialog("open failed! Cause:" + System.lineSeparator() + "此项目没有配置git仓库", "Open Failed");
            return;
        }

        final VcsHistoryProvider provider = activeVcs.getVcsBlockHistoryProvider();
        if(provider == null){
            Messages.showErrorDialog("open failed! Cause:" + System.lineSeparator() + "此项目没有配置git仓库", "Open Failed");
            return;
        }

        int startLine = selection.getSelectionStartLineNumber();
        int endLine = selection.getSelectionEndLineNumber() + 1;

        String projectBasePath = project.getBasePath();
        String filePath = file.getPath();
        String fileName = filePath.substring(projectBasePath.length());

        String revision,branch,reviewer = "";
        Set<String> reviewSet = new HashSet<>();
        try{
            VcsHistorySession session = VcsCachingHistory.collectSession(activeVcs, VcsUtil.getFilePath(file), null);
            List<VcsFileRevision> revisions = session.getRevisionList();
            VcsRevisionNumber vcsRevisionNumber = revisions.get(0).getRevisionNumber();

            revision = vcsRevisionNumber.asString();
            branch = projectCache.getRepository().getBranch();


            Repository repository = projectCache.getRepository();
            ObjectId commitID = repository.resolve("HEAD");
            BlameCommand blamer = new BlameCommand(repository);
            blamer.setStartCommit(commitID);
            blamer.setFilePath(filePath.substring(projectBasePath.length()+1));
            BlameResult blame = blamer.call();
            int count = blame.getResultContents().size();
            for(int i = startLine;i <= endLine && i <= count;i++){
                int preLine = blame.getSourceLine(i - 1) + 1;
                RevCommit commit = blame.getSourceCommit(i - 1);
                String name = commit.getAuthorIdent().getName();
                reviewSet.add(name);
            }
            if(endLine > count)endLine = count;
        }catch (Exception exception){
            return;
        }
        String myName = PropertiesComponent.getInstance().getValue("marco_username");
        reviewSet.remove(myName);

        for(String reviewName : reviewSet){
            reviewer += reviewName+",";
        }
        if(reviewer.length() > 1 && reviewer.charAt(reviewer.length()-1) == ','){
            reviewer = reviewer.substring(0,reviewer.length()-1);
        }

        MarkReview markReview = new MarkReview();

        markReview.setFile(fileName);
        startLine++;
        markReview.setLine(startLine + "~" + endLine);
        markReview.setBranch(branch);
        markReview.setVersion(revision);
        markReview.setProject(project.getName());
        markReview.setComment("");
        markReview.setMarker("You");
        markReview.setStatus("open");
        markReview.setReviewer(reviewer);

        AddMarkReviewUI.showDialog(markReview, project);
    }
}
