package com.fudan.intellij.plugin.marco.common;

import com.fudan.intellij.plugin.marco.action.MarkReviewWindowUI;
import com.fudan.intellij.plugin.marco.entity.MarkReview;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import lombok.Data;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.util.*;

@Data
public class InnerProjectCache {

    private List<MarkReview> draftMarkReviewList;

    private MarkReviewWindowUI markReviewWindowUI;

    private Project project;

    private Git git;

    private Repository repository;

    private Map<String , Map<Integer,String>> lineCommentMap = new HashMap<>();

    private Map<String , Map<Integer,String>> lastLineCommentMap = new HashMap<>();


    public InnerProjectCache(Project project) {
        this.project = project;
        try{
            git = Git.open( new File(Objects.requireNonNull(project.getBasePath())) );
            repository = git.getRepository();
        }catch (Exception e){
            Messages.showErrorDialog("git仓库创建失败，请init仓库并重启intellij" , "Plugin marco initialization failed");
        }

        reloadCacheData();
    }

    public Git getGit(){
        try{
            git = Git.open( new File(Objects.requireNonNull(project.getBasePath())) );
            repository = git.getRepository();
        }catch (Exception e){
            Messages.showErrorDialog("git仓库创建失败，请init仓库并重启intellij" , "Plugin marco initialization failed");
        }
        return this.git;
    }

    private void reloadCacheData() {
        List<MarkReview> deserializeCache = DataPersistentUtil.deserialize(this.project);
        if (deserializeCache == null) {
            draftMarkReviewList = new ArrayList<MarkReview>();
        } else {
            draftMarkReviewList = deserializeCache;
        }
    }

    public boolean addNewComment(MarkReview markReview) {
        if (markReview == null) {
            return false;
        }
        draftMarkReviewList.add(markReview);

        DataPersistentUtil.serialize(draftMarkReviewList, this.project);

        return true;
    }

    public boolean updateDraftMarkReview() {

        DataPersistentUtil.serialize(draftMarkReviewList, this.project);

        return true;
    }

    public boolean deleteMarkReview(long id) {
        draftMarkReviewList.removeIf(markReview1 -> markReview1.getMarkReviewId() == id);

        DataPersistentUtil.serialize(draftMarkReviewList, this.project);

        return true;
    }
}
