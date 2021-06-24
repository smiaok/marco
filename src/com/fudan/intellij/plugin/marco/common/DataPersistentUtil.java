package com.fudan.intellij.plugin.marco.common;

import com.fudan.intellij.plugin.marco.entity.MarkReview;
import com.intellij.openapi.project.Project;

import java.io.*;
import java.util.List;

public class DataPersistentUtil {

    private static File prepareAndGetCacheDataPath(String projectHash) {
        String usrHome = System.getProperty("user.home");
        File userDir = new File(usrHome);
        File cacheDir = new File(userDir, ".idea_code_review_data");
        if (!cacheDir.exists() || !cacheDir.isDirectory()) {
            boolean mkdirs = cacheDir.mkdirs();
            if (!mkdirs) {
                System.out.println("create cache path failed...");
            }
        }
        File cacheDataFile = new File(cacheDir, projectHash + ".dat");
        try{
            FileWriter fileWriter = new FileWriter(cacheDataFile,true);
        }catch (Exception e){
//            e.printStackTrace();
        }

        return cacheDataFile;
    }

    public synchronized static void serialize(List<MarkReview> cache, Project project) {
        File file = prepareAndGetCacheDataPath(project.getLocationHash());
        ObjectOutputStream oout = null;

        try {
            oout = new ObjectOutputStream(new FileOutputStream(file));
            if(cache == null || cache.size() == 0){
                oout.writeObject(null);
            }else{
                oout.writeObject(cache);
            }
            oout.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CommonUtil.closeQuitely(oout);
        }
    }

    public synchronized static List<MarkReview> deserialize(Project project) {
        File file = prepareAndGetCacheDataPath(project.getLocationHash());
        ObjectInputStream oin = null;
        List<MarkReview> cache = null;
        try {
            oin = new ObjectInputStream(new FileInputStream(file));
            cache = (List<MarkReview>) oin.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CommonUtil.closeQuitely(oin);
        }
        return cache;
    }



}
