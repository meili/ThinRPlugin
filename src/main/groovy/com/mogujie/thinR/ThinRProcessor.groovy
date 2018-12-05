package com.mogujie.thinR

import org.gradle.api.GradleException
import org.objectweb.asm.*

import java.util.regex.Matcher

/**
 * Created by wangzhi on 16/7/28.
 */
class ThinRProcessor {
    HashMap<String, Integer> map
    static def MATCHER_NO_STYLEABLE = '''.*/R2?\\$(?!styleable).*?\\.class|.*/R2?\\.class'''
    static def FIELD_R_REGEX = '''/R2?\\$(?!styleable).*'''
    static def MATCHER_R = '''.*/R\\$.*\\.class|.*/R\\.class'''
    static def MATCHER_R2 = '''.*/R2?\\$.*\\.class|.*/R2?\\.class'''


    ThinRProcessor(File rClassDir) {
        this.map = getRsMap(rClassDir)
        PrintUtil.verbose("r map ==> " + map.size())
    }

    boolean needKeepEntry(String path) {
        if (path ==~ MATCHER_NO_STYLEABLE) {
            PrintUtil.verbose("del r " + path)
            return false
        } else {
            return true
        }
    }


    byte[] getEntryBytes(String path, byte[] bytes) {
        if (!path.endsWith(".class") || path ==~ MATCHER_R2) {
            PrintUtil.verbose("is origin r or not class  ")
            return bytes
        } else {
            return replaceRToConstant(bytes, map)
        }
    }


    public static byte[] replaceRToConstant(byte[] bytes, Map<String, Integer> map) {
        ClassReader cr = new ClassReader(bytes)
        ClassWriter cw = new ClassWriter(cr, 0)
        String className = ""
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                PrintUtil.verbose("class name => " + name)
                className = name;
                super.visit(version, access, name, signature, superName, interfaces)
            }
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {

                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv = new MethodVisitor(Opcodes.ASM5, mv) {

                    @Override
                    void visitFieldInsn(int i, String owner, String fieldName, String fdesc) {
                        //xxx/R$mipmap.xxx

                        def m = owner =~ ThinRProcessor.FIELD_R_REGEX
                        assert m instanceof Matcher
                        if (m) {
                            String p = m[0] - "/"
                            int value = -1
                            try {
                                value = map.get(p + "," + fieldName)
                            } catch (Exception e) {
                                throw new GradleException(owner + ":" + fieldName
                                        + " not found! in " + className + "'s " + name)
                            }

                            PrintUtil.verbose("R  Ref==> " + owner + "," + fieldName + "," + " replace to " + value)
                            Integer integer = new Integer(value)
                            super.visitLdcInsn(integer)
                        } else {
                            super.visitFieldInsn(i, owner, fieldName, fdesc)
                        }

                    }

                }

                return mv

            }
        }
        cr.accept(cv, 0);

        return cw.toByteArray()
    }


    public static Map getRsMap(File dir) {
        HashMap<String, Integer> map = new HashMap<>()
        if (dir.exists()) {
            dir.eachFile { file ->
                if (file.absolutePath ==~ MATCHER_R) {
                    PrintUtil.verbose("find r file ==> " + file.absolutePath)
                    map.putAll(getOneRMap(file))
                }
            }
        }
        return map

    }

    public static Map getOneRMap(File rFile) {
        InputStream inputStream = new FileInputStream(rFile)

        String type = rFile.name - ".class"
        ClassReader cr = new ClassReader(inputStream)
        PrintUtil.info("--------------------- start process file: " + rFile + "  ------------------")
        Map<String, Integer> map = new HashMap<>()
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5) {

            @Override
            FieldVisitor visitField(int i, String s, String s1, String s2, Object o) {
                FieldVisitor fv = super.visitField(i, s, s1, s2, o)
                if (o instanceof Integer) {
                    map.put(type + "," + s, o)
                }
                return fv
            }

        }
        cr.accept(cv, 0);
        inputStream.close()
        PrintUtil.info("--------------------- end process file: " + rFile + "  ------------------")
        return map

    }

}
