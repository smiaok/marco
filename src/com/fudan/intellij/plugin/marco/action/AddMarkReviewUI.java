package com.fudan.intellij.plugin.marco.action;

import com.fudan.intellij.plugin.marco.common.CommonUtil;
import com.fudan.intellij.plugin.marco.common.InnerProjectCache;
import com.fudan.intellij.plugin.marco.common.ProjectInstanceManager;
import com.fudan.intellij.plugin.marco.entity.MarkReview;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public class AddMarkReviewUI {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;

    private JTextArea commentsTextArea;
    private JButton saveButton;
    private JButton cancelButton;
    private JPanel addReviewCommentPanel;
    private JComboBox typeComboBox;
    private JTextField tmpIdTextField;
    private JLabel lineLabel;
    private JLabel fileLabel;
    private JLabel versionLabel;
    private JLabel branchLabel;
    private JLabel projectLabel;
    private JLabel hintLabel;
    private JComboBox saveComboBox;
    private JLabel reviewLabel;

    public static void showDialog(MarkReview markReview, Project project) {
        InnerProjectCache projectCache = ProjectInstanceManager.getInstance().getProjectCache(project.getLocationHash());

        List<MarkReview> draftMarkReviewList = projectCache.getDraftMarkReviewList();

        JDialog dialog = new JDialog();
        dialog.setTitle("Add MarkReview");
        AddMarkReviewUI reviewCommentUI = new AddMarkReviewUI();
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        reviewCommentUI.lineLabel.setText(markReview.getLine());
        reviewCommentUI.fileLabel.setText(markReview.getFile());
        reviewCommentUI.versionLabel.setText(markReview.getVersion());
        reviewCommentUI.branchLabel.setText(markReview.getBranch());
        reviewCommentUI.projectLabel.setText(markReview.getProject());
        reviewCommentUI.reviewLabel.setText(markReview.getReviewer());



        reviewCommentUI.tmpIdTextField.addKeyListener(new KeyAdapter(){
            @Override
            public void keyTyped(KeyEvent event)
            {
                char ch=event.getKeyChar();
                if(ch<'0'||ch>'9'){
                    event.consume();
                }

            }
        });
        reviewCommentUI.saveComboBox.addItem("As New MarkReview");

        for(MarkReview draftMarkReview:draftMarkReviewList){
            if(draftMarkReview.getProject().equals(markReview.getProject())
                    && draftMarkReview.getBranch().equals(markReview.getBranch())
                    && draftMarkReview.getVersion().equals(markReview.getVersion())
                    && draftMarkReview.getFile().equals(markReview.getFile())
            ){
                reviewCommentUI.saveComboBox.addItem("to draft " + draftMarkReview.getMarkReviewId());
            }
        }

        reviewCommentUI.saveButton.addActionListener(e -> {

            String saveType = (String)reviewCommentUI.saveComboBox.getSelectedItem();

            if(saveType.equals("As New MarkReview")){
                long tmpId = Long.parseLong(reviewCommentUI.tmpIdTextField.getText());
                for(MarkReview draftMarkReview:draftMarkReviewList){
                    if(draftMarkReview.getMarkReviewId() == tmpId){
                        reviewCommentUI.hintLabel.setText("tmpId 与 已有的重复");
                        return;
                    }
                }
                markReview.setMarkReviewId(tmpId);
                markReview.setType((String)reviewCommentUI.typeComboBox.getSelectedItem());
                markReview.setComment(reviewCommentUI.commentsTextArea.getText());
                projectCache.addNewComment(markReview);
            }else{
                long id = Long.parseLong(saveType.split(" ")[2]);
                for(MarkReview draftMarkReview:draftMarkReviewList){
                    if(draftMarkReview.getMarkReviewId() == id){
                        String line1 = markReview.getLine();
                        String line2 = draftMarkReview.getLine();
                        String tmpLine = line1 + "*" + line2;
                        String line = arrayToLine(mergeLine(lineToArray(tmpLine)));
                        draftMarkReview.setLine(line);
                        draftMarkReview.setComment(draftMarkReview.getComment() + "\n" + reviewCommentUI.commentsTextArea.getText());

                        String totalName = markReview.getReviewer()+","+draftMarkReview.getReviewer();
                        String[] names = totalName.split(",");
                        Set<String> reviewers = new HashSet<>();
                        for(String name:names){
                            if(name.length()>0){
                                reviewers.add(name);
                            }
                        }
                        String reviewName = "";
                        for(String name:reviewers){
                            reviewName += name +",";
                        }
                        if(reviewName.length() > 1 && reviewName.charAt(reviewName.length()-1) == ','){
                            reviewName = reviewName.substring(0,reviewName.length()-1);
                        }
                        draftMarkReview.setReviewer(reviewName);
                    }
                }
                projectCache.updateDraftMarkReview();
            }
            projectCache.getMarkReviewWindowUI().refreshTableDataShow();


            CommonUtil.reloadCommentListShow(project);


            dialog.dispose();
        });

        reviewCommentUI.cancelButton.addActionListener(e -> {
            dialog.dispose();
        });

        // 屏幕中心显示
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int w = (screenSize.width - WIDTH) / 2;
        int h = (screenSize.height * 95 / 100 - HEIGHT) / 2;
        dialog.setLocation(w, h);

        dialog.setContentPane(reviewCommentUI.addReviewCommentPanel);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }

    public static int[][] mergeLine(int[][] arr){
        Arrays.parallelSort(arr, Comparator.comparingInt(x -> x[0]));

        LinkedList<int[]> list = new LinkedList<>();
        for(int i=0;i<arr.length;i++){
            if(list.size()==0||list.getLast()[1]<arr[i][0] - 1){
                list.add(arr[i]);
            }else{
                list.getLast()[1] = Math.max(list.getLast()[1],arr[i][1]);
            }
        }
        int[][] res = new int[list.size()][2];
        int index = 0;
        while(!list.isEmpty()){
            res[index++] = list.removeFirst();
        }
        return res;
    }

    public static int[][] lineToArray(String line){
        String[] areas = line.split("\\*");
        int[][] res = new int[areas.length][2];
        for(int i = 0;i < areas.length;i++){
            res[i][0] = Integer.parseInt(areas[i].split("~")[0]);
            res[i][1] = Integer.parseInt(areas[i].split("~")[1]);
        }
        return res;
    }

    public static String arrayToLine(int[][] arr){
        String line = "";
        for(int i = 0;i < arr.length;i++){
            line += arr[i][0] + "~" +arr[i][1];
            if(i != arr.length - 1){
                line += "*";
            }
        }
        return line;
    }
}
