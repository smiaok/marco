package com.fudan.intellij.plugin.marco.action;

import com.fudan.intellij.plugin.marco.common.InnerProjectCache;
import com.fudan.intellij.plugin.marco.common.ProjectInstanceManager;
import com.fudan.intellij.plugin.marco.constant.KeyConstant;
import com.fudan.intellij.plugin.marco.entity.MarkReview;
import com.fudan.intellij.plugin.marco.service.remote.UserRemoteService;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.diff.Block;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.history.VcsCachingHistory;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.Icons;
import com.intellij.util.IntPair;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LeftMarkIconProvider extends RelatedItemLineMarkerProvider {

    UserRemoteService userRemoteService = new UserRemoteService();

    boolean ping = false;

    @Override
    public void collectNavigationMarkers(@NotNull List<? extends PsiElement> elements,
                                         @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                         boolean forNavigation) {


        if(elements.isEmpty()){
            super.collectNavigationMarkers(elements, result,forNavigation);
            return;
        }

        if(!userRemoteService.ping() || !userRemoteService.isLogin()){
            ping = false;
            super.collectNavigationMarkers(elements, result,forNavigation);
            return;
        }
        ping = true;

        PsiFile containingFile = elements.get(0).getContainingFile();
        Project project = elements.get(0).getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(containingFile);
        String filePath = elements.get(0).getContainingFile().getVirtualFile().getPath();
        if (document == null) return;

        initMap(filePath, document, project);
        for (int i = 0, size = elements.size(); i < size; i++) {
            PsiElement element = elements.get(i);
            collectNavigationMarkers(element, result);
            if (forNavigation && element instanceof PsiNameIdentifierOwner) {
                PsiElement nameIdentifier = ((PsiNameIdentifierOwner) element).getNameIdentifier();
                if (nameIdentifier != null && !elements.contains(nameIdentifier)) {
                    collectNavigationMarkers(nameIdentifier, result);
                }
            }
        }
        InnerProjectCache projectCache = ProjectInstanceManager.getInstance().getProjectCache(project.getLocationHash());
        projectCache.getLineCommentMap().get(filePath).clear();
        projectCache.getLastLineCommentMap().get(filePath).clear();
    }

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof PsiWhiteSpace)) {
            super.collectNavigationMarkers(element, result);
            return;
        }
        Project project = element.getProject();

        InnerProjectCache projectCache = ProjectInstanceManager.getInstance().getProjectCache(project.getLocationHash());

        if (projectCache == null || !ping) {
            super.collectNavigationMarkers(element, result);
            return;
        }

        int textOffset = element.getTextOffset();
        int textLength = element.getTextLength();
        int textEndOffset = textOffset + textLength;

        if (textOffset < 0) {
            super.collectNavigationMarkers(element, result);
            return;
        }

        PsiFile containingFile = element.getContainingFile();
        Document document = PsiDocumentManager.getInstance(project).getDocument(containingFile);
        if (document != null) {
            int startLineNumber = document.getLineNumber(textOffset);
            int endLineNumber = document.getLineNumber(textEndOffset);
            if (startLineNumber == endLineNumber) {
                super.collectNavigationMarkers(element, result);
                return;
            }
            String filePath = element.getContainingFile().getVirtualFile().getPath();

            int currentLine = startLineNumber + 1;

            String comment = projectCache.getLineCommentMap().get(filePath).get(currentLine);
            if (comment != null) {
                NavigationGutterIconBuilder<PsiElement> builder = (projectCache.getLastLineCommentMap().get(filePath).get(currentLine) == null)?
                        NavigationGutterIconBuilder.create(Icons.UI_FORM_ICON):NavigationGutterIconBuilder.create(Icons.NEW_PARAMETER);
                builder.setTarget(element);
                builder.setTooltipText(comment);
                result.add(builder.createLineMarkerInfo(element));
                return;
            }

        }

        super.collectNavigationMarkers(element, result);
    }

    public  void initMap(String filePath, Document document, Project project) {
        String currentText = document.getText();

        String fileRelativePath = filePath.substring(project.getBasePath().length());
        String[] fileSplit = fileRelativePath.split("/");
        String fileName = fileSplit[fileSplit.length - 1];
        InnerProjectCache projectCache = ProjectInstanceManager.getInstance().getProjectCache(project.getLocationHash());
        if (projectCache == null) {
            projectCache = new InnerProjectCache(project);
            ProjectInstanceManager.getInstance().addProjectCache(project.getLocationHash(), projectCache);
        }

        projectCache.getLineCommentMap().computeIfAbsent(filePath, k -> new HashMap<>());
        projectCache.getLastLineCommentMap().computeIfAbsent(filePath, k -> new HashMap<>());

        projectCache.getLineCommentMap().computeIfAbsent(filePath, k -> new HashMap<>());
        projectCache.getLastLineCommentMap().computeIfAbsent(filePath, k -> new HashMap<>());

        if(projectCache.getMarkReviewWindowUI() == null){
            return;
        }

        List<MarkReview> markReviewList = projectCache.getMarkReviewWindowUI().getMarkReviewList();


        if(markReviewList == null)return;
        String myName = PropertiesComponent.getInstance().getValue("marco_username");
        List<MarkReview> tmpMarkReviewList = markReviewList.stream().filter(markReview ->
                markReview.getProject().equals(project.getName())
                        && markReview.getFile() != null
                        && markReview.getFile().length() > 0
                        && markReview.getFile().equals(fileRelativePath)
                        && markReview.getReviewer() != null
                        && markReview.getReviewer().length() > 0
                        && Stream.of(markReview.getReviewer().split(",")).collect(Collectors.toList()).contains(myName)
                        && markReview.getStatus().equals(KeyConstant.STATUS_OPEN)
        ).collect(Collectors.toList());

        try {
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
            AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
            List<VcsFileRevision> revisions = VcsCachingHistory.collect(activeVcs, VcsUtil.getFilePath(file), null);
            String version = revisions.get(0).getRevisionNumber().asString();

            for (MarkReview markReview : tmpMarkReviewList) {
                String lineString = markReview.getLine();
                String[] linePair = lineString.split("\\*");
                List<IntPair> rangePair = new ArrayList<>();
                for (String pair : linePair) {
                    String[] tmp = pair.split("~");
                    IntPair intPair = new IntPair(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]));
                    rangePair.add(intPair);
                }
               List<VcsFileRevision> theRevision =  revisions.stream()
                        .filter(vcsFileRevision -> vcsFileRevision.getRevisionNumber().asString().equals(markReview.getVersion()))
                        .collect(Collectors.toList());
                if(theRevision.size() == 0){
                    return;
                }
                byte[] bytes = theRevision
                        .get(0).loadContent();
                if (bytes == null) continue;
                String text = new String(bytes, file.getCharset());
                for (IntPair intPair : rangePair) {
                    Block block = new Block(text, intPair.first, intPair.second);
                    Block newBlock = block.createPreviousBlock(currentText);
                    int startLine = newBlock.getStart();
                    int endLine = newBlock.getEnd();
                    for (int i = startLine; i < endLine + 1; i++) {
                        if (projectCache.getLineCommentMap().get(filePath).get(i) == null) {
                            projectCache.getLineCommentMap().get(filePath).put(i, "ID:" + markReview.getMarkReviewId() + "\nComment:\n" + markReview.getComment()+"\n-------");
                        } else {
                            projectCache.getLineCommentMap().get(filePath).put(i, projectCache.getLineCommentMap().get(filePath).get(i) + "\nID: " + markReview.getMarkReviewId() + "\nComment:\n" + markReview.getComment()+"\n-------");
                        }
                        if (markReview.getVersion().equals(version) && projectCache.getLastLineCommentMap().get(filePath).get(i) == null) {
                            projectCache.getLastLineCommentMap().get(filePath).put(i, "new");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
