package com.mogujie.thinR

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.builder.core.DefaultManifestParser
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Created by wangzhi on 16/9/12.
 */
class ContextProvider {
    Project project

    DefaultTask compileTask
    DefaultTask dexTask
    DefaultTask processManifestTask
    String varNameCap

    public ContextProvider(Project project, String varNameCap, String buildVariants) {
        this.project = project
        this.varNameCap = varNameCap
        boolean isHighVersion = isHighVersion()

        if (isHighVersion) {
            compileTask = project.tasks.findByName("compile${varNameCap}JavaWithJavac")
            if(null != buildVariants){
              dexTask = project.tasks.findByName("transformClassesWithDexFor${buildVariants}")
            }else {
              dexTask = project.tasks.findByName("transformClassesWithDexFor${varNameCap}")
            }
        } else {
            compileTask = project.tasks.findByName("compile${varNameCap}Java")
            dexTask = project.tasks.findByName("dex${varNameCap}")
        }

        processManifestTask = project.tasks.findByName("process${varNameCap}Manifest")

        if (dexTask == null) {
            throw new GradleException("Can not found dex task!")
        }

    }

    boolean isHighVersion() {
        boolean isHighVersion = true;
        try {
            Class clazz = com.android.build.gradle.tasks.Dex;
            isHighVersion = false;
        } catch (Throwable ignored) {
            // ignored
        }
        return isHighVersion

    }

    interface Filter {
        boolean accept(String path)
    }

    public Collection<File> getDexInputFile(Filter filter) {
        Collection<File> inputs = dexTask.inputs.files.files
        ArrayList<File> jarFileList = new ArrayList()
        inputs.each { file ->
            PrintUtil.verbose("dex input ==> " + file)
            if (filter.accept(file.absolutePath)) {
                jarFileList.add(file)
            }
        }
//        if (jarFileList.size() != 1) {
//            throw new GradleException(" Number of the  dex inputs size is not 1 ,found size : ${jarFileList.size()} ")
//        }
        return jarFileList
    }


    @Nullable
    String getPackageName() {
        return getManifestPackage(getManifestFile())
    }

    File getManifestFile() {
        return processManifestTask.getMainManifest()
    }

    private String getManifestPackage(@NonNull File manifestFile) {
        //在android build tools 2.2.2 及以上DefaultManifestParser的构造函数变了
        try {
            return new DefaultManifestParser().getPackage(manifestFile);
        } catch (Throwable t) {
            return new DefaultManifestParser(manifestFile).getPackage();
        }
    }

    File getRClassDir() {
        String packageNameByPath = getPackageName().replace(".", "/")
        File rDir = new File(compileTask.getDestinationDir(), packageNameByPath)
        checkFile(rDir)
        return rDir
    }


    void checkFile(File file) {
        if (file == null || !file.exists()) {
            throw new GradleException("Can not found allClassesJar: " + file == null ? "null" : file.absolutePath)
        }

    }

    public boolean isSkipThinR(ThinRExtension extension) {
        return (extension.skipThinR || (extension.skipThinRDebug && varNameCap.toLowerCase().contains("debug")))
    }


}
