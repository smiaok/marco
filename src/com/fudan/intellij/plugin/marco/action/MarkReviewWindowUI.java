package com.fudan.intellij.plugin.marco.action;

import com.fudan.intellij.plugin.marco.common.InnerProjectCache;
import com.fudan.intellij.plugin.marco.common.ProjectInstanceManager;
import com.fudan.intellij.plugin.marco.constant.KeyConstant;
import com.fudan.intellij.plugin.marco.entity.MarkReview;
import com.fudan.intellij.plugin.marco.entity.User;
import com.fudan.intellij.plugin.marco.model.CommentTableModel;
import com.fudan.intellij.plugin.marco.service.remote.MarkReviewRemoteService;
import com.fudan.intellij.plugin.marco.service.remote.UserRemoteService;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.history.VcsCachingHistory;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.IntPair;
import com.intellij.vcsUtil.VcsUtil;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

//            Messages.showErrorDialog("open failed! Cause:" + System.lineSeparator() + "当前工程中未找到此文件", "Open Failed");

@Data
public class MarkReviewWindowUI {
    private static final Object[] COLUMN_NAMES = {"ID", "Marker", "Reviewer", "Project", "branch", "Version",
            "File", "LIne", "Type", "Status", "Comment", "CreatTime", "UpdateTime"};

    private static final Object[] COLUMN_NAMES_DRAFT = {"TmpID", "Marker", "Reviewer", "Project", "branch", "Version",
            "File", "LIne", "Type", "Comment"};

    private JTable markReviewTable;
    public JPanel fullPanel;
    private JComboBox viewComboBox;
    private JComboBox statusComboBox;
    private JComboBox typeComboBox;
    private JButton refreshButton;
    private JLabel viewLabel;
    private JLabel statusLabel;
    private JLabel typeLabel;
    private JLabel hintLabel;
    private JTable draftTable;
    private JButton loginButton;
    private JButton logoutButton;
    private JButton registerButton;
    private JButton setUriButton;

    private JPopupMenu markReviewMenu;
    private JPopupMenu draftReviewMenu;

    private JMenuItem uploadMenItem = new JMenuItem();

    private JMenuItem addReviewerMenItem = new JMenuItem();
    private JMenuItem closedMenItem = new JMenuItem();
    private JMenuItem abortedMenItem = new JMenuItem();
    private JMenuItem openMenItem = new JMenuItem();
    private JMenuItem modifyFileNameMenItem = new JMenuItem();
    private JMenuItem modifyCommentMenItem = new JMenuItem();


    private JMenuItem historyMenItem = new JMenuItem();

    private final Project project;

    private List<MarkReview> markReviewList;

    private Object[][] rowData;

    private CommentTableModel dataModel = new CommentTableModel(rowData, COLUMN_NAMES);

    private UserRemoteService userRemoteService = new UserRemoteService();

    private MarkReviewRemoteService markReviewRemoteService = new MarkReviewRemoteService();

    public MarkReviewWindowUI(Project project) {
        this.project = project;
    }


    public void initUI() {
        initComponent();
        bindTables();

        bindButtons();
        bindComboBoxs();
        bindPopupMenu();
        bindDraftPopupMenu();
        reloadTableData();

        bindTableListeners();

    }


    public void refreshTableDataShow() {
        reloadTableData();
    }


    public void reloadDraftTableData() {
        InnerProjectCache projectCache = ProjectInstanceManager.getInstance().getProjectCache(MarkReviewWindowUI.this.project.getLocationHash());

        List<MarkReview> draftMarkReviewList = projectCache.getDraftMarkReviewList();
        putMarkReviewIntoTable(draftMarkReviewList, draftTable);
    }

    private void reloadTableData() {
        reloadDraftTableData();
        if (!userRemoteService.ping()) {
            loginButton.setEnabled(false);
            registerButton.setEnabled(false);
            logoutButton.setEnabled(false);
            markReviewList = null;
            putMarkReviewIntoTable(markReviewList, markReviewTable);
            return;
        }
        if (!userRemoteService.isLogin()) {
            hintLabel.setText("login plz");
            dataModel.setRowCount(0);
            loginButton.setEnabled(true);
            registerButton.setEnabled(true);
            logoutButton.setEnabled(false);
            markReviewList = null;
            putMarkReviewIntoTable(markReviewList, markReviewTable);
            return;
        } else {
            loginButton.setEnabled(false);
            registerButton.setEnabled(true);
            logoutButton.setEnabled(true);
            uploadMenItem.setEnabled(true);
            hintLabel.setText("");
        }


//        List<MarkReview> asMarker = markReviewRemoteService.getAllMarkReviewAsMarker();
//        List<MarkReview> asReviewer = markReviewRemoteService.getAllMarkReviewAsMarker();
//
//        markReviewList = Stream.of(asMarker, asReviewer)
//                .flatMap(Collection::stream)
//                .distinct()
//                .collect(Collectors.toList());

        markReviewList = markReviewRemoteService.getAllMarkReview();

        for (MarkReview markReview : markReviewList) {
            List<User> reviewers = markReviewRemoteService.getAllReviewerForMarkReview(markReview.getMarkReviewId());
            String reviewerNames = "";
            for (User user : reviewers) {
                reviewerNames += user.getUsername();
                reviewerNames += ",";
            }
            if (reviewerNames.length() > 1 && reviewerNames.charAt(reviewerNames.length() - 1) == ',') {
                reviewerNames = reviewerNames.substring(0, reviewerNames.length() - 1);
            }
            markReview.setReviewer(reviewerNames);
        }

        putMarkReviewIntoTable(markReviewList, markReviewTable);

    }


    private void initComponent() {

        if (!userRemoteService.ping()) {
            loginButton.setEnabled(false);
            registerButton.setEnabled(false);
            logoutButton.setEnabled(false);
            return;
        }

        if (userRemoteService.isLogin()) {
            loginButton.setEnabled(false);
            logoutButton.setEnabled(true);
        } else {
            loginButton.setEnabled(true);
            logoutButton.setEnabled(false);
        }


    }

    private void bindTableListeners() {
        // 双击跳转到源码位置
        markReviewTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = ((JTable) e.getSource()).rowAtPoint(e.getPoint());
                    doubleClickDumpToOriginal(project,row);
                }
                super.mouseClicked(e);
            }
        });
    }

    private void doubleClickDumpToOriginal(Project project, int row) {
        String file = (String) markReviewTable.getValueAt(row, 6);
        String[] basePath = file.split("/");

        String filePath = basePath[basePath.length - 1];
        int startLine = 0;
        if (filePath == null || filePath.length() == 0) {
            Messages.showErrorDialog("invalid file for this version" , "Jump Failed");
            return;
        }

        PsiFile[] filesByName = PsiShortNamesCache.getInstance(project).getFilesByName(filePath);
        Arrays.stream(filesByName).filter(psiFile -> psiFile.getVirtualFile().getPath().contains(file));
        if (filesByName.length > 0) {
            PsiFile psiFile = filesByName[0];
            VirtualFile virtualFile = psiFile.getVirtualFile();
            // 打开对应的文件
            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, virtualFile);
            Editor editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
            if (editor == null) {
                Messages.showErrorDialog("invalid file for this version" , "Jump Failed");
                return;
            }

            // 跳转到指定的位置
            CaretModel caretModel = editor.getCaretModel();
            LogicalPosition logicalPosition = caretModel.getLogicalPosition();
            logicalPosition.leanForward(true);
            LogicalPosition logical = new LogicalPosition(startLine, logicalPosition.column);
            caretModel.moveToLogicalPosition(logical);
            SelectionModel selectionModel = editor.getSelectionModel();
            selectionModel.selectLineAtCaret();
        } else {
            Messages.showErrorDialog("invalid file for this version" , "Jump Failed");
        }

    }


    public void bindPopupMenu() {
        markReviewMenu = new JPopupMenu();

        openMenItem.setText("-> open");
        openMenItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int rowNum = markReviewTable.getSelectedRow();
                if (KeyConstant.STATUS_OPEN.equals(markReviewTable.getValueAt(rowNum, 9))) {
                    openMenItem.setEnabled(false);
                } else {
                    if (markReviewRemoteService.updateMarkReviewStatus((Long) markReviewTable.getValueAt(rowNum, 0), KeyConstant.STATUS_OPEN)) {
                        JOptionPane.showMessageDialog(null, "提示消息", "success", JOptionPane.INFORMATION_MESSAGE);
                        reloadTableData();
                    } else {
                        JOptionPane.showMessageDialog(null, "提示消息", "failed", JOptionPane.INFORMATION_MESSAGE);
                    }

                }

            }
        });

        closedMenItem.setText("-> closed");
        closedMenItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int rowNum = markReviewTable.getSelectedRow();
                if (KeyConstant.STATUS_CLOSED.equals(markReviewTable.getValueAt(rowNum, 9))) {
                    closedMenItem.setEnabled(false);
                } else {
                    if (markReviewRemoteService.updateMarkReviewStatus((Long) markReviewTable.getValueAt(rowNum, 0), KeyConstant.STATUS_CLOSED)) {
                        JOptionPane.showMessageDialog(null, "提示消息", "success", JOptionPane.INFORMATION_MESSAGE);
                        reloadTableData();
                    } else {
                        JOptionPane.showMessageDialog(null, "提示消息", "failed", JOptionPane.INFORMATION_MESSAGE);
                    }

                }

            }
        });

        abortedMenItem.setText("-> aborted");
        abortedMenItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int rowNum = markReviewTable.getSelectedRow();
                if (KeyConstant.STATUS_ABORTED.equals(markReviewTable.getValueAt(rowNum, 9))) {
                    abortedMenItem.setEnabled(false);
                } else {
                    if (markReviewRemoteService.updateMarkReviewStatus((Long) markReviewTable.getValueAt(rowNum, 0), KeyConstant.STATUS_ABORTED)) {
                        JOptionPane.showMessageDialog(null, "提示消息", "success", JOptionPane.INFORMATION_MESSAGE);
                        reloadTableData();
                    } else {
                        JOptionPane.showMessageDialog(null, "提示消息", "failed", JOptionPane.INFORMATION_MESSAGE);
                    }

                }

            }
        });


        addReviewerMenItem.setText("add reviewer");
        addReviewerMenItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int rowNum = markReviewTable.getSelectedRow();
                long markReviewId = (long) markReviewTable.getValueAt(rowNum, 0);
                String reviewerName = (String) markReviewTable.getValueAt(rowNum, 2);
                List<User> userList = userRemoteService.getAllUser();
                List<String> reviewers = Stream.of(reviewerName.split(",")).collect(Collectors.toList());
                userList.removeIf(user -> reviewers.contains(user.getUsername()));
                if (userList.size() == 0) {
                    JOptionPane.showMessageDialog(null, "无法增加", "failed", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                String myName = PropertiesComponent.getInstance().getValue("marco_username");
                Object[] objects = userList.stream().filter(user -> !user.getUsername().equals(myName)).map(User::getUsername).toArray();
                String newReviewer = (String) JOptionPane.showInputDialog(null, "请选择reviewer\n", "add reviewer",
                        JOptionPane.PLAIN_MESSAGE, null, objects, null);
                if (newReviewer == null) {
                    return;
                }
                if (markReviewRemoteService.addReviewerForMarkReview(markReviewId, newReviewer)) {
                    JOptionPane.showMessageDialog(null, "提示消息", "success", JOptionPane.INFORMATION_MESSAGE);
                    reloadTableData();
                } else {
                    JOptionPane.showMessageDialog(null, "提示消息", "failed", JOptionPane.INFORMATION_MESSAGE);
                }


            }
        });

        historyMenItem.setText("show history");
        historyMenItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int rowNum = markReviewTable.getSelectedRow();
                long markReviewId = (long) markReviewTable.getValueAt(rowNum, 0);

                String projectName = (String) markReviewTable.getValueAt(rowNum, 3);
                String fileName = (String) markReviewTable.getValueAt(rowNum, 6);
                String revision = (String) markReviewTable.getValueAt(rowNum, 5);
                String line = (String) markReviewTable.getValueAt(rowNum, 7);
                String branch = (String) markReviewTable.getValueAt(rowNum, 4);

                if(!project.getName().equals(projectName)){
                    Messages.showErrorDialog("open failed! Cause:" + System.lineSeparator() + "此评审任务并非对应此项目", "Open Failed");
                    return;
                }
                InnerProjectCache projectCache = ProjectInstanceManager.getInstance().getProjectCache(MarkReviewWindowUI.this.project.getLocationHash());
                String branchName = "";
                try {
                    branchName = projectCache.getGit().getRepository().getBranch();
                } catch (Exception exception) {
                    Messages.showErrorDialog("open failed! Cause:" + System.lineSeparator() + "当前工程没有配置git", "Open Failed");
                    return;
                }
                if(!branchName.equals(branch)){
                    Messages.showErrorDialog("open failed! Cause:" + System.lineSeparator() + "当前工作区分支与此评审任务不符", "Open Failed");
                    return;
                }


                String filePath = project.getBasePath() + fileName;
                VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);

                if(file == null){
                    Messages.showErrorDialog("open failed! Cause:" + System.lineSeparator() + "文件不存在于此项目中", "Open Failed");
                    return;
                }

                AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
                if(activeVcs == null){
                    Messages.showErrorDialog("open failed! Cause:" + System.lineSeparator() + "项目不支持版本控制", "Open Failed");
                    return;
                }

                VcsHistoryProvider provider = activeVcs.getVcsBlockHistoryProvider();
                if(provider == null){
                    Messages.showErrorDialog("open failed! Cause:" + System.lineSeparator() + "项目不支持版本控制", "Open Failed");
                    return;
                }

                //特定版本的文件信息
                String text = "";

                try {
                    VcsHistorySession session = VcsCachingHistory.collectSession(activeVcs, VcsUtil.getFilePath(file), null);

                    List<VcsFileRevision> revisionList = session.getRevisionList();
                    List<VcsFileRevision> revisions = revisionList.stream().filter(re -> re.getRevisionNumber().asString().equals(revision)).collect(Collectors.toList());
                    if(revisions.size() == 0){
                        Messages.showErrorDialog("open failed! Cause:" + System.lineSeparator() + "当前项目不存在此版本，请确认项目是否更新", "Open Failed");
                        return;
                    }

                    byte[] bytes = revisions.get(0).loadContent();
                    text = new String(bytes, file.getCharset());

                    //生成list
                    List<IntPair> rangeList = new ArrayList<>();

                    String[] rangeString = line.split("\\*");
                    for (String s : rangeString) {
                        String[] tmp = s.split("~");
                        int start = Integer.parseInt(tmp[0]) - 1;
                        int end = Integer.parseInt(tmp[1]) - 1;
                        rangeList.add(new IntPair(start, end));
                    }

                    MarkReviewDiffDialog dialog = new MarkReviewDiffDialog(project,
                            file,
                            text,
                            provider,
                            activeVcs,
                            rangeList,
                            "History for MarkReview",
                            revision);
                    dialog.show();
                } catch (Exception exception) {

                }

            }
        });

        modifyFileNameMenItem.setText("update file path");
        modifyFileNameMenItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int rowNum = markReviewTable.getSelectedRow();
                long id = (long)markReviewTable.getValueAt(rowNum, 0);
                String oldFilePath = (String)markReviewTable.getValueAt(rowNum,6);
                String newFilePath = JOptionPane.showInputDialog(null,"New File Path",oldFilePath);
                if(newFilePath == null||newFilePath.length() == 0){
                    return;
                }
                markReviewRemoteService.updateFilePath(id,newFilePath);
                reloadTableData();
            }
        });

        modifyCommentMenItem.setText("modify comment");
        modifyCommentMenItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int rowNum = markReviewTable.getSelectedRow();
                long id = (long)markReviewTable.getValueAt(rowNum, 0);
                String oldComment = (String)markReviewTable.getValueAt(rowNum,10);
                JFrame jf = new JFrame();
                jf.setSize(380, 400);
                jf.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                jf.setLocationRelativeTo(null);
                jf.setTitle("Modify Comment");
                FlowLayout flow = new FlowLayout();
                jf.setLayout(flow);
                jf.setVisible(true);
                JLabel jl1 = new JLabel("Comment：");
                JTextArea jt1 = new JTextArea(oldComment);
                jt1.setPreferredSize(new Dimension(300, 300));
                jf.add(jl1);
                jf.add(jt1);
                JButton jb = new JButton("确认");
                jf.add(jb);
                jb.addActionListener(event -> {
                    String newComment = jt1.getText();
                    markReviewRemoteService.updateComment(id,newComment);
                    jf.dispose();
                    reloadTableData();
                });
            }
        });

        markReviewMenu.add(openMenItem);
        markReviewMenu.add(closedMenItem);
        markReviewMenu.add(abortedMenItem);
        markReviewMenu.add(addReviewerMenItem);
        markReviewMenu.add(historyMenItem);
        markReviewMenu.add(modifyFileNameMenItem);
        markReviewMenu.add(modifyCommentMenItem);
    }

    public void bindDraftPopupMenu() {
        draftReviewMenu = new JPopupMenu();
        uploadMenItem.setText("upload");
        uploadMenItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int rowNum = draftTable.getSelectedRow();
                long id = (long) draftTable.getValueAt(rowNum, 0);
                InnerProjectCache projectCache = ProjectInstanceManager.getInstance().getProjectCache(MarkReviewWindowUI.this.project.getLocationHash());
                List<MarkReview> draftMarkReviewList = projectCache.getDraftMarkReviewList();
                MarkReview markReviewUp = new MarkReview();
                for (MarkReview markReview : draftMarkReviewList) {
                    if (markReview.getMarkReviewId() == id) {
                        markReviewUp = markReview;
                        break;
                    }
                }
                long newID = markReviewRemoteService.uploadMarkReview(markReviewUp);
                if(newID >= 0){
                    JOptionPane.showMessageDialog(null, "提示消息", "success", JOptionPane.INFORMATION_MESSAGE);
                    String[] names = markReviewUp.getReviewer().split(",");
                    for (String name : names) {
                        if(name!=null && name.length() > 0){
                            markReviewRemoteService.addReviewerForMarkReview(newID, name);
                        }
                    }
                    projectCache.deleteMarkReview(markReviewUp.getMarkReviewId());
                    reloadTableData();
                } else {
                    JOptionPane.showMessageDialog(null, "提示消息", "failed", JOptionPane.INFORMATION_MESSAGE);
                }


            }
        });

        JMenuItem deleteMenItem = new JMenuItem();
        deleteMenItem.setText("delete");
        deleteMenItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int rowNum = draftTable.getSelectedRow();
                long id = (long) draftTable.getValueAt(rowNum, 0);
                InnerProjectCache projectCache = ProjectInstanceManager.getInstance().getProjectCache(MarkReviewWindowUI.this.project.getLocationHash());
                if (projectCache.deleteMarkReview(id)) {
                    JOptionPane.showMessageDialog(null, "提示消息", "success", JOptionPane.INFORMATION_MESSAGE);
                    reloadTableData();
                } else {
                    JOptionPane.showMessageDialog(null, "提示消息", "failed", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });


        draftReviewMenu.add(uploadMenItem);
        draftReviewMenu.add(deleteMenItem);
    }

    private void bindComboBoxs() {
        statusComboBox.addItemListener(e -> {
            reloadTableData();
        });

        typeComboBox.addItemListener(e -> {
            reloadTableData();
        });

        viewComboBox.addItemListener(e -> {
            reloadTableData();
        });
    }

    private void bindTables() {
        markReviewTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getButton() == java.awt.event.MouseEvent.BUTTON3) {

                    int focusedRowIndex = markReviewTable.rowAtPoint(evt.getPoint());
                    if (focusedRowIndex == -1) {
                        return;
                    }
                    String type = (String) markReviewTable.getValueAt(focusedRowIndex, 9);
                    if (type.equals(KeyConstant.STATUS_OPEN)) {
                        openMenItem.setEnabled(false);
                        closedMenItem.setEnabled(true);
                        abortedMenItem.setEnabled(true);
                    } else if (type.equals(KeyConstant.STATUS_CLOSED)) {
                        openMenItem.setEnabled(true);
                        closedMenItem.setEnabled(false);
                        abortedMenItem.setEnabled(true);
                    } else if (type.equals(KeyConstant.STATUS_ABORTED)) {
                        openMenItem.setEnabled(true);
                        closedMenItem.setEnabled(true);
                        abortedMenItem.setEnabled(false);
                    }

                    markReviewTable.setRowSelectionInterval(focusedRowIndex, focusedRowIndex);

                    markReviewMenu.show(markReviewTable, evt.getX(), evt.getY());
                }
            }
        });

        draftTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    if (!userRemoteService.ping() || !userRemoteService.isLogin()) {
                        uploadMenItem.setEnabled(false);
                    } else {
                        uploadMenItem.setEnabled(true);
                    }
                    int focusedRowIndex = draftTable.rowAtPoint(evt.getPoint());
                    if (focusedRowIndex == -1) {
                        return;
                    }

                    draftTable.setRowSelectionInterval(focusedRowIndex, focusedRowIndex);

                    draftReviewMenu.show(draftTable, evt.getX(), evt.getY());
                }
            }
        });


    }

    private void bindButtons() {

        PropertiesComponent.getInstance().getValue(KeyConstant.URI_POLO);

        setUriButton.addActionListener(e -> {
            String uri = PropertiesComponent.getInstance().getValue(KeyConstant.URI_POLO);
            String newUri = (String) JOptionPane
                    .showInputDialog(null, "请输入：\n", "url", JOptionPane.PLAIN_MESSAGE, null, null, uri);
            PropertiesComponent.getInstance().setValue(KeyConstant.URI_POLO, newUri);
            userRemoteService.refreshHttp();
            markReviewRemoteService.refreshHttp();
            if (!userRemoteService.ping()) {
                PropertiesComponent.getInstance().setValue(KeyConstant.URI_POLO, uri);
                userRemoteService.refreshHttp();
                markReviewRemoteService.refreshHttp();
                JOptionPane.showMessageDialog(null, "提示消息", "illegal uri", JOptionPane.INFORMATION_MESSAGE);
            }
            reloadTableData();
        });

        refreshButton.addActionListener(e -> {
            reloadTableData();
        });

        loginButton.addActionListener(e -> {
            JFrame jf = new JFrame();
            jf.setSize(380, 400);
            jf.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            jf.setLocationRelativeTo(null);
            jf.setTitle("Login");
            FlowLayout flow = new FlowLayout();
            jf.setLayout(flow);
            jf.setVisible(true);
            JLabel jl1 = new JLabel("username：");
            JTextField jt1 = new JTextField();
            jt1.setPreferredSize(new Dimension(300, 30));
            JLabel jl2 = new JLabel("password：");
            JPasswordField jt2 = new JPasswordField();
            jt2.setPreferredSize(new Dimension(300, 30));
            jf.add(jl1);
            jf.add(jt1);
            jf.add(jl2);
            jf.add(jt2);
            JButton jb = new JButton("login");
            jf.add(jb);
            jb.addActionListener(event -> {
                String username = jt1.getText();
                String password = new String(jt2.getPassword());
                if (userRemoteService.login(username, password)) {
                    JOptionPane.showMessageDialog(null, "提示消息", "success", JOptionPane.INFORMATION_MESSAGE);
                    jf.dispose();
                    PropertiesComponent.getInstance().setValue("marco_username", username);
                    loginButton.setEnabled(false);
                    logoutButton.setEnabled(true);
                    reloadTableData();
                } else {
                    JOptionPane.showMessageDialog(null, "提示消息", "fail", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        });

        registerButton.addActionListener(e -> {
            JFrame jf = new JFrame();
            jf.setSize(380, 600);
            jf.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            jf.setLocationRelativeTo(null);
            jf.setTitle("Register");
            FlowLayout flow = new FlowLayout();
            jf.setLayout(flow);
            jf.setVisible(true);
            JLabel jl1 = new JLabel("username：\n");
            JTextField jt1 = new JTextField();
            jt1.setPreferredSize(new Dimension(300, 30));
            JLabel jl2 = new JLabel("\n password：\n");
            JPasswordField jt2 = new JPasswordField();
            jt2.setPreferredSize(new Dimension(300, 30));
            JLabel jl3 = new JLabel("\n    email:    \n");
            JTextField jt3 = new JTextField();
            jt3.setPreferredSize(new Dimension(300, 30));
            JLabel jl4 = new JLabel("\n phone:    \n");
            JTextField jt4 = new JTextField();
            jt4.setPreferredSize(new Dimension(300, 30));
            jf.add(jl1);
            jf.add(jt1);
            jf.add(jl2);
            jf.add(jt2);
            jf.add(jl3);
            jf.add(jt3);
            jf.add(jl4);
            jf.add(jt4);
            JButton jb = new JButton("register");
            jf.add(jb);
            jb.addActionListener(event -> {
                String username = jt1.getText();
                String password = new String(jt2.getPassword());
                String email = jt3.getText();
                String phone = jt4.getText();
                if (userRemoteService.register(username, password, email, phone)) {
                    JOptionPane.showMessageDialog(null, "提示消息", "success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "提示消息", "fail", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        });

        logoutButton.addActionListener(e -> {
            int resp = JOptionPane.showConfirmDialog(null, "确定logout？", "logout确认", JOptionPane.YES_NO_OPTION);
            if (resp == 0) {
                userRemoteService.logout();
                PropertiesComponent.getInstance().unsetValue("marco_username");
                loginButton.setEnabled(true);
                logoutButton.setEnabled(false);
                reloadTableData();
            }
        });
    }


    private void putMarkReviewIntoTable(List<MarkReview> markReviews, JTable table) {
        if (markReviews == null || markReviews.size() == 0) {
            CommentTableModel dataModel;
            if (table == markReviewTable) {
                dataModel = new CommentTableModel(null, COLUMN_NAMES);

            } else {
                dataModel = new CommentTableModel(null, COLUMN_NAMES_DRAFT);
            }

            table.setModel(dataModel);
            table.setEnabled(true);
            return;
        }
        List<Object[]> rowDataList = new ArrayList<>();

        List<MarkReview> tmpMarkReviewList = new ArrayList<>();
        CollectionUtils.addAll(tmpMarkReviewList, new Object[markReviews.size()]);
        Collections.copy(tmpMarkReviewList, markReviews);

        if (table == this.markReviewTable) {
            String type = (String) typeComboBox.getSelectedItem();
            String view = (String) viewComboBox.getSelectedItem();
            String status = (String) statusComboBox.getSelectedItem();

            tmpMarkReviewList = filterView(tmpMarkReviewList, view);
            tmpMarkReviewList = filterStatus(tmpMarkReviewList, status);
            tmpMarkReviewList = filterType(tmpMarkReviewList, type);
        }

        Object[][] rowData;
        CommentTableModel dataModel;

        if (table == this.markReviewTable) {
            for (MarkReview markReview : tmpMarkReviewList) {
                Object[] row = {markReview.getMarkReviewId(), markReview.getMarker(), markReview.getReviewer(), markReview.getProject(), markReview.getBranch(),
                        markReview.getVersion(), markReview.getFile(), markReview.getLine(), markReview.getType(), markReview.getStatus(),
                        markReview.getComment(), markReview.getCreateTime(), markReview.getUpdateTime()
                };
                rowDataList.add(row);
            }
            rowData = rowDataList.stream().toArray(Object[][]::new);
            dataModel = new CommentTableModel(rowData, COLUMN_NAMES);
        } else {
            for (MarkReview markReview : tmpMarkReviewList) {
                Object[] row = {markReview.getMarkReviewId(), markReview.getMarker(), markReview.getReviewer(), markReview.getProject(), markReview.getBranch(),
                        markReview.getVersion(), markReview.getFile(), markReview.getLine(), markReview.getType(),
                        markReview.getComment()
                };
                rowDataList.add(row);
            }
            rowData = rowDataList.stream().toArray(Object[][]::new);
            dataModel = new CommentTableModel(rowData, COLUMN_NAMES_DRAFT);
        }

        table.setModel(dataModel);
        table.setEnabled(true);
    }

    private List<MarkReview> filterType(List<MarkReview> markReviews, String type) {
        List<MarkReview> tmpMarkReviewList = new ArrayList<>();
        if (type.equals(KeyConstant.ALL)) {
            tmpMarkReviewList = markReviews;
        } else {
            for (MarkReview markReview : markReviews) {
                if (markReview.getType().equals(type)) {
                    tmpMarkReviewList.add(markReview);
                }
            }
        }
        return tmpMarkReviewList;
    }

    private List<MarkReview> filterStatus(List<MarkReview> markReviews, String status) {
        List<MarkReview> tmpMarkReviewList = new ArrayList<>();
        if (status.equals(KeyConstant.ALL)) {
            tmpMarkReviewList = markReviews;
        } else {
            for (MarkReview markReview : markReviews) {
                if (markReview.getStatus().equals(status)) {
                    tmpMarkReviewList.add(markReview);
                }
            }
        }
        return tmpMarkReviewList;
    }

    private List<MarkReview> filterView(List<MarkReview> markReviews, String view) {
        List<MarkReview> tmpMarkReviewList = new ArrayList<>();
        String username = PropertiesComponent.getInstance().getValue("marco_username");
        if (view.equals(KeyConstant.ALL)) {
            tmpMarkReviewList = markReviews;
        } else if (view.equals(KeyConstant.AS_MARKER)) {
            for (MarkReview markReview : markReviews) {
                if (markReview.getMarker().equals(username)) {
                    tmpMarkReviewList.add(markReview);
                }
            }
        } else {
            for (MarkReview markReview : markReviews) {

                List<String> reviewers = Stream.of(markReview.getReviewer().split(",")).collect(Collectors.toList());
                if (reviewers.contains(username)) {
                    tmpMarkReviewList.add(markReview);
                }
            }
        }
        return tmpMarkReviewList;
    }

}
