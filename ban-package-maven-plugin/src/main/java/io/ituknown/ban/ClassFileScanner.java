package io.ituknown.ban;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassFileScanner {

    public ScanResult scan(byte[] bytes) {
        Collector collector = new Collector();
        new ClassReader(bytes).accept(collector, 0);
        return new ScanResult(collector.className, collector.sourceFile, collector.types);
    }

    private static final class Collector extends ClassVisitor {

        private static final Pattern SIGNATURE_CLASS = Pattern.compile("L([^;<>\\s]+)(?=[<;])");

        final Set<String> types = new HashSet<>();
        String className;
        String sourceFile;

        Collector() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            className = name;
            addSignature(signature);
            addInternal(superName);
            if (interfaces != null) {
                for (String i : interfaces) {
                    addInternal(i);
                }
            }
        }

        @Override
        public void visitSource(String source, String debug) {
            sourceFile = source;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return annotationVisitor(descriptor);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath,
                                                     String descriptor, boolean visible) {
            return annotationVisitor(descriptor);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            addDescriptor(descriptor);
            addSignature(signature);
            return new FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return annotationVisitor(descriptor);
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath,
                                                             String descriptor, boolean visible) {
                    return annotationVisitor(descriptor);
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            addDescriptor(descriptor);
            addSignature(signature);
            if (exceptions != null) {
                for (String e : exceptions) {
                    addInternal(e);
                }
            }
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return annotationVisitor(descriptor);
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath,
                                                             String descriptor, boolean visible) {
                    return annotationVisitor(descriptor);
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                    return annotationVisitor(descriptor);
                }

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    addInternal(type);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    addInternal(owner);
                    addDescriptor(descriptor);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name,
                                            String descriptor, boolean isInterface) {
                    addInternal(owner);
                    addDescriptor(descriptor);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String descriptor,
                                                   Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                    addDescriptor(descriptor);
                    addHandle(bootstrapMethodHandle);
                    for (Object arg : bootstrapMethodArguments) {
                        if (arg instanceof Type t) {
                            addElementType(t);
                        } else if (arg instanceof Handle h) {
                            addHandle(h);
                        }
                    }
                }

                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof Type t) {
                        addElementType(t);
                    }
                }
            };
        }

        private void addInternal(String name) {
            if (name == null || name.isEmpty()) {
                return;
            }
            if (name.charAt(0) == '[') {
                addElementType(Type.getType(name));
            } else {
                types.add(name);
            }
        }

        private void addHandle(Handle handle) {
            if (handle == null) {
                return;
            }
            addInternal(handle.getOwner());
            addDescriptor(handle.getDesc());
        }

        private void addSignature(String signature) {
            if (signature == null || signature.isEmpty()) {
                return;
            }
            Matcher matcher = SIGNATURE_CLASS.matcher(signature);
            while (matcher.find()) {
                types.add(matcher.group(1));
            }
        }

        private void addDescriptor(String descriptor) {
            if (descriptor == null || descriptor.isEmpty()) {
                return;
            }
            Type t = descriptor.charAt(0) == '(' ? Type.getMethodType(descriptor) : Type.getType(descriptor);
            if (t.getSort() == Type.METHOD) {
                for (Type arg : t.getArgumentTypes()) {
                    addElementType(arg);
                }
                addElementType(t.getReturnType());
            } else {
                addElementType(t);
            }
        }

        private void addElementType(Type t) {
            if (t == null) {
                return;
            }
            Type element = t.getSort() == Type.ARRAY ? t.getElementType() : t;
            if (element.getSort() == Type.OBJECT) {
                types.add(element.getInternalName());
            }
        }

        private AnnotationVisitor annotationVisitor(String descriptor) {
            addDescriptor(descriptor);
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override
                public void visit(String name, Object value) {
                    if (value instanceof Type t) {
                        addElementType(t);
                    }
                }

                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    addDescriptor(descriptor);
                }

                @Override
                public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                    addDescriptor(descriptor);
                    return this;
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    return this;
                }
            };
        }
    }
}
