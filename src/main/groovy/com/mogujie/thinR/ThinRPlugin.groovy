package com.mogujie.thinR

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
/**
 * Created by dolphinWang on 15/11/02.
 */
public class ThinRPlugin implements Plugin<Project> {


    ThinRExtension extension


    @Override
    void apply(Project project) {

        project.extensions.create("thinR", ThinRExtension);

        project.afterEvaluate {
            extension = project.extensions.findByName("thinR") as ThinRExtension
            PrintUtil.logLevel = extension.logLevel
            PrintUtil.info(extension.toString())

            project.android.applicationVariants.all { variant ->
                ContextProvider contextProvider = new ContextProvider(project, variant.name.capitalize() as String,
                 extension.buildVariants as String)
                boolean skipThinR = contextProvider.isSkipThinR(extension)
                PrintUtil.info("skipThinR: " + skipThinR)
                if (!skipThinR) {
                    doWhenDexFirst(project, contextProvider)
                }

            }
        }

    }

    void doWhenDexFirst(Project project, ContextProvider contextProvider) {
        String intermediatesPath = Utils.joinPath(project.buildDir.absolutePath, "intermediates")
        contextProvider.dexTask.doFirst {
            long time1 = System.currentTimeMillis()
            ThinRProcessor thinRProcessor = new ThinRProcessor(contextProvider.getRClassDir())
            Collection<File> inputFile = contextProvider.getDexInputFile(new ContextProvider.Filter() {
                @Override
                boolean accept(String path) {
                    return path.startsWith(intermediatesPath)
                }
            })
            inputFile.each { file ->
                PrintUtil.info("start process input : " + file)
                if (file.isDirectory()) {
                    processDir(file, thinRProcessor)
                } else if (file.name.endsWith(".jar")) {
                    processJar(file, thinRProcessor)
                } else {
//                    throw new GradleException("unknown file input file ${file} ")
                }
            }
            long time2 = System.currentTimeMillis()
            PrintUtil.info("process time: " + (time2 - time1))
        }
    }

    void processDir(File dir, ThinRProcessor thinRProcessor) {
        dir.eachFileRecurse { file ->
            PrintUtil.verbose("  " + "file--> " + file.absolutePath)
            if (file.name.endsWith(".class")) {
                File tempFile = new File(file.parentFile, file.name + "_bak")
                InputStream originIns = file.newInputStream()
                byte[] bytes = Utils.toByteArray(originIns)
                originIns.close()
                bytes = thinRProcessor.getEntryBytes(file.absolutePath, bytes)
                if (thinRProcessor.needKeepEntry(file.absolutePath)) {
                    OutputStream outputStream = tempFile.newOutputStream()
                    outputStream.write(bytes, 0, bytes.length)
                    outputStream.flush()
                    outputStream.close()
                    Utils.renameFile(tempFile, file)
                } else {
                    Utils.delFile(file)
                }
            } else if (file.name.endsWith(".jar")) {
                processJar(file, thinRProcessor)
            }
        }
    }


    void processJar(File jarFile, ThinRProcessor thinRProcessor) {
        JarFile jf = new JarFile(jarFile);
        Enumeration<JarEntry> je = jf.entries()
        File tempJar = new File(jarFile.parentFile, "temp.jar")
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar))

        while (je.hasMoreElements()) {
            JarEntry jarEntry = je.nextElement();
            ZipEntry zipEntry = new ZipEntry(jarEntry.getName());
            InputStream originIns = jf.getInputStream(jarEntry);
            byte[] bytes = Utils.toByteArray(originIns)
            originIns.close()
            bytes = thinRProcessor.getEntryBytes(jarEntry.getName(), bytes)
            PrintUtil.verbose("  " + "jarEntry--> " + jarEntry.name)
            if (thinRProcessor.needKeepEntry(jarEntry.getName())) {
                jos.putNextEntry(zipEntry);
                jos.write(bytes);
                jos.closeEntry();
            }

        }
        jos.close()
        jf.close()
        jarFile.delete()
        Utils.renameFile(tempJar, jarFile)
    }


}
