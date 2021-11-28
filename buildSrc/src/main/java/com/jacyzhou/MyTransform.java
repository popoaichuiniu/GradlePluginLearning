package com.jacyzhou;


import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.Opcodes.ASM7;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class MyTransform extends Transform {

    @Override
    public String getName() {
        return "MyTransform";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.PROJECT_ONLY;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        TransformOutputProvider provider = transformInvocation.getOutputProvider();
        if (provider == null) {
            return;
        }
        provider.deleteAll();
        for (TransformInput input : transformInvocation.getInputs()) {
            if (input == null) {
                continue;
            }
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File file = directoryInput.getFile();
                if (!file.exists()) {
                    continue;
                }
                handleForDirectoryInput(directoryInput, provider);
            }

            for (JarInput jarInput : input.getJarInputs()) {
                File file = jarInput.getFile();
                if (!file.exists()) {
                    continue;
                }
                handleForJarInput(jarInput, provider);
            }
        }
    }

    private void handleForJarInput(JarInput jarInput, TransformOutputProvider outputProvider) throws IOException {
        File dest = outputProvider.getContentLocation(
                jarInput.getName(), jarInput.getContentTypes(),
                jarInput.getScopes(), Format.JAR);
        System.out.println(dest);
        File src = jarInput.getFile();
        FileUtils.copyDirectory(src.getAbsolutePath(), dest.getAbsolutePath());
    }

    private void handleForDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) throws IOException {
        File dest = outputProvider.getContentLocation(
                directoryInput.getName(), directoryInput.getContentTypes(),
                directoryInput.getScopes(), Format.DIRECTORY);
        System.out.println(dest);
        File src = directoryInput.getFile();
        Set<File> classFileList = new HashSet<>();
        findClassFile(src, classFileList);
        for (File classFile : classFileList) {
            if (!classFile.getName().equals("MainActivity.class")) {
                continue;
            }
            ClassReader classReader = new ClassReader(FileUtils.readFile(classFile));

            ClassWriter classWriter = new ClassWriter(COMPUTE_FRAMES);
            ClassVisitor adapterVisitor = new AdapterClassVisitor(classWriter);

            //classReader.accept(adapterVisitor, 0);

            ClassNode classNode = new ClassNode();
            classReader.accept(classNode,0);
            classNode.accept(adapterVisitor);

            FileUtils.writeFile(classWriter.toByteArray(), classFile.getAbsolutePath());
            System.out.println("zms");
        }
        FileUtils.copyDirectory(src.getAbsolutePath(), dest.getAbsolutePath());
    }

    private void findClassFile(File file, Set<File> classFileList) {
        if (!file.isDirectory()) {
            if (file.getName().endsWith(".class")) {
                classFileList.add(file);
            }
            return;
        } else {
            for (File childFile : file.listFiles()) {
                findClassFile(childFile, classFileList);
            }
        }
    }

    public static class AdapterClassVisitor extends ClassVisitor {

        private ClassWriter classWriter = null;

        public AdapterClassVisitor(ClassWriter classWriter) {
            super(ASM7);
            this.classWriter = classWriter;
        }

        public AdapterClassVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            classWriter.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public void visitSource(String source, String debug) {
            classWriter.visitSource(source, debug);
        }

        @Override
        public ModuleVisitor visitModule(String name, int access, String version) {
            return classWriter.visitModule(name, access, version);
        }

        @Override
        public void visitNestHost(String nestHost) {
            classWriter.visitNestHost(nestHost);
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            classWriter.visitOuterClass(owner, name, descriptor);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return classWriter.visitAnnotation(descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            return classWriter.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            classWriter.visitAttribute(attribute);
        }

        @Override
        public void visitNestMember(String nestMember) {
            classWriter.visitNestMember(nestMember);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            classWriter.visitInnerClass(name, outerName, innerName, access);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            return classWriter.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor methodVisitor = classWriter.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("onCreate")) {
                methodVisitor = new MyMethodVisitor(methodVisitor);
            }
            return methodVisitor;
        }

        @Override
        public void visitEnd() {
            classWriter.visitEnd();
        }
    }

    public static class MyMethodVisitor extends MethodVisitor {
        private MethodVisitor methodVisitor;

        public MyMethodVisitor(MethodVisitor methodVisitor) {
            super(ASM7);
            this.methodVisitor = methodVisitor;
        }

        public MyMethodVisitor(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
            this.methodVisitor = methodVisitor;
        }

        @Override
        public void visitParameter(String name, int access) {
            methodVisitor.visitParameter(name, access);
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return methodVisitor.visitAnnotationDefault();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return methodVisitor.visitAnnotation(descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            return methodVisitor.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }

        @Override
        public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
            methodVisitor.visitAnnotableParameterCount(parameterCount, visible);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            return methodVisitor.visitParameterAnnotation(parameter, descriptor, visible);
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            methodVisitor.visitAttribute(attribute);
        }

        @Override
        public void visitCode() {
            methodVisitor.visitCode();
        }

        @Override
        public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
            methodVisitor.visitFrame(type, numLocal, local, numStack, stack);
        }

        @Override
        public void visitInsn(int opcode) {
            methodVisitor.visitInsn(opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            methodVisitor.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            methodVisitor.visitVarInsn(opcode, var);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            methodVisitor.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            methodVisitor.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            methodVisitor.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            if (opcode == 182) {
                System.out.println(name);
                // aload_0
                methodVisitor.visitVarInsn(25, 0);
                //  invokespecial #6 <com/jacyzhou/testgradle/MainActivity.setText : ()V>
                methodVisitor.visitMethodInsn(183, owner, "setText", "()V", false);
            }
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            methodVisitor.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            methodVisitor.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLabel(Label label) {
            methodVisitor.visitLabel(label);
        }

        @Override
        public void visitLdcInsn(Object value) {
            methodVisitor.visitLdcInsn(value);
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            methodVisitor.visitIincInsn(var, increment);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            methodVisitor.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            methodVisitor.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            methodVisitor.visitMultiANewArrayInsn(descriptor, numDimensions);
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            return methodVisitor.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            methodVisitor.visitTryCatchBlock(start, end, handler, type);
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            return methodVisitor.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            methodVisitor.visitLocalVariable(name, descriptor, signature, start, end, index);
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
            return methodVisitor.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            methodVisitor.visitLineNumber(line, start);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            methodVisitor.visitMaxs(maxStack, maxLocals);
        }

        @Override
        public void visitEnd() {
            methodVisitor.visitEnd();
        }
    }
//
//    public static class RemoveMethodTransformer extends ClassTransformer {
//
//    }
}
