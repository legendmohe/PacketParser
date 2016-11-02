package com.legendmohe.packetparser.compiler;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.legendmohe.packetparser.annotation.ParsePacket;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Created by legendmohe on 16/8/25.
 */
@AutoService(Processor.class)
public class PacketParserProcessor extends AbstractProcessor {

    private static final String PARSER_CLASS_SUFFIX = "PacketParser";
    private Elements mElementUtils;
    private Types mTypeUtils;
    private Filer mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        mElementUtils = env.getElementUtils();
        mTypeUtils = env.getTypeUtils();
        mFiler = env.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(ParsePacket.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(ParsePacket.class)) {
            if (!SuperficialValidation.validateElement(element))
                continue;
            if (!(element instanceof TypeElement))
                continue;
            elementToPacketParser((TypeElement) element);
        }
        return true;
    }

    private void elementToPacketParser(TypeElement srcClass) {
        ParsePacket parsePacket = srcClass.getAnnotation(ParsePacket.class);
        String packetPattern = parsePacket.value();
        if (packetPattern == null || packetPattern.length() == 0) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("@%s-annotated class with empty pattern. (%s)",
                    parsePacket.getClass().getSimpleName(), srcClass.getSimpleName()));
            return;
        }

        String packageName = getPackageName(srcClass);

        //build class
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(srcClass, packageName) + PARSER_CLASS_SUFFIX)
                .addModifiers(Modifier.PUBLIC);

        // find all fields
        List<? extends Element> fields = srcClass.getEnclosedElements();
        Map<String, Element> fieldNameSet = new HashMap<>();
        for (Element element :
                fields) {
            if (element.getKind() == ElementKind.FIELD) {
                if (element.getKind() == ElementKind.FIELD && !element.getModifiers().contains(Modifier.PRIVATE)) {
                    fieldNameSet.put(element.getSimpleName().toString(), element);
                }
            }
        }

        List<Pattern> patternList = new ArrayList<>();
        String[] patterns = packetPattern.split("\\|");
        for (String pattern :
                patterns) {

            // check condition
            String condition = null;
            int lastConditionIdx = pattern.lastIndexOf("]");
            if (lastConditionIdx != -1) {
                if (pattern.charAt(0) != '[') {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            String.format("@%s-annotated class with invalid condition %s. (%s)",
                                    ParsePacket.class.getSimpleName(),
                                    pattern,
                                    srcClass.asType().toString()
                            )
                    );
                    return;
                } else {
                    condition = pattern.substring(1, lastConditionIdx);
                    if (condition.length() == 0) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                String.format("@%s-annotated class with empty condition %s. (%s)",
                                        ParsePacket.class.getSimpleName(),
                                        pattern,
                                        srcClass.asType().toString()
                                )
                        );
                        return;
                    } else {
                        pattern = pattern.substring(lastConditionIdx + 1);
                    }
                }
            }

            // check attr & length expression
            String[] temp = pattern.split(":", 2);
            String attr = temp[0].trim();
            String exp = temp[1].trim();
            if (!fieldNameSet.containsKey(attr)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("@%s-annotated class with miss pattern %s. (%s)",
                                ParsePacket.class.getSimpleName(),
                                attr,
                                srcClass.asType().toString()
                        )
                );
                return;
            }
            patternList.add(new Pattern(condition, attr, exp));
        }

        MethodSpec.Builder parseMethod = MethodSpec.methodBuilder("parse")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .addParameter(ArrayTypeName.of(TypeName.BYTE), "src")
                .returns(TypeName.get(srcClass.asType()));
        parseMethod.addStatement("return parse(src, new $T())", TypeName.get(srcClass.asType()));
        classBuilder.addMethod(parseMethod.build());

        parseMethod = buildParseMethod(srcClass, patternList, fieldNameSet);
        classBuilder.addMethod(parseMethod.build());

        MethodSpec.Builder toBytesMethod = buildToBytesMethod(srcClass, patternList, fieldNameSet);
        classBuilder.addMethod(toBytesMethod.build());

        generateJavaFile(packageName, classBuilder.build());
    }

    private MethodSpec.Builder buildToBytesMethod(TypeElement srcClass, List<Pattern> packetPattern, Map<String, Element> fieldNameSet) {
        MethodSpec.Builder toBytesMethod = MethodSpec.methodBuilder("toBytes")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .addParameter(TypeName.get(srcClass.asType()), "src")
                .returns(ArrayTypeName.of(TypeName.BYTE));

        StringBuilder dataLen = new StringBuilder();
        for (Pattern pattern :
                packetPattern) {
            String exp = pattern.exp;
            if (exp.contains("this.")) {
                exp = exp.replace("this.", "src.");
            }
            dataLen.append(exp);
            dataLen.append(" + ");

            String condition = pattern.condition;
            if (condition != null && condition.length() > 0 && condition.contains("this.")) {
                pattern.condition = condition.replace("this.", "src.");
            }
        }
        if (dataLen.length() != 0) {
            dataLen.delete(dataLen.length() - 3, dataLen.length());
        }

        ClassName readerClassName = ClassName.get("java.nio", "ByteBuffer");
        toBytesMethod.addStatement("$T byteBuffer = $T.allocate(" + dataLen.toString() + ")", readerClassName, readerClassName);

        for (Pattern pattern :
                packetPattern) {
            boolean hasCondition = pattern.condition != null && pattern.condition.length() > 0;
            if (hasCondition) {
                toBytesMethod.beginControlFlow("if(" + pattern.condition + ")");
            }
            String attr = pattern.attr;
            Element fieldElement = fieldNameSet.get(attr);
            switch (fieldElement.asType().getKind()) {
                case BYTE:
                    toBytesMethod.addStatement("byteBuffer.put(src." + attr + ")");
                    break;
                case SHORT:
                    toBytesMethod.addStatement("byteBuffer.putShort(src." + attr + ")");
                    break;
                case INT:
                    toBytesMethod.addStatement("byteBuffer.putInt(src." + attr + ")");
                    break;
                case LONG:
                    toBytesMethod.addStatement("byteBuffer.putLong(src." + attr + ")");
                    break;
                case CHAR:
                    toBytesMethod.addStatement("byteBuffer.putChar(src." + attr + ")");
                    break;
                case ARRAY:
                    toBytesMethod.beginControlFlow("if(src." + attr + " != null && src." + attr + ".length != 0)");
                    toBytesMethod.addStatement("byteBuffer.put(src." + attr + ")");
                    toBytesMethod.endControlFlow();
                    break;
                default:
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            String.format("@%s-annotated class with unsupported field type %s. (%s)",
                                    ParsePacket.class.getSimpleName(),
                                    fieldElement.asType().getKind().toString(),
                                    srcClass.asType().toString()
                            )
                    );
                    return toBytesMethod;
            }
            if (hasCondition) {
                toBytesMethod.endControlFlow();
            }
        }

        toBytesMethod.addStatement("return byteBuffer.array()");
        return toBytesMethod;
    }

    private MethodSpec.Builder buildParseMethod(TypeElement srcClass, List<Pattern> packetPattern, Map<String, Element> fieldNameSet) {
        MethodSpec.Builder parseMethod = MethodSpec.methodBuilder("parse")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .addParameter(ArrayTypeName.of(TypeName.BYTE), "bytes")
                .addParameter(TypeName.get(srcClass.asType()), "src")
                .returns(TypeName.get(srcClass.asType()));

        ClassName readerClassName = ClassName.get("java.nio", "ByteBuffer");
        parseMethod.addStatement("$T byteBuffer = $T.wrap(bytes)", readerClassName, readerClassName);

        ClassName BufferOverflowExceptionClassName = ClassName.get("java.nio", "BufferUnderflowException");
        parseMethod.beginControlFlow("try");

        for (Pattern pattern :
                packetPattern) {
            String condition = pattern.condition;
            String attr = pattern.attr;
            String exp = pattern.exp;
            if (!fieldNameSet.containsKey(attr)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("@%s-annotated class with miss pattern %s. (%s)",
                                ParsePacket.class.getSimpleName(),
                                attr,
                                srcClass.asType().toString()
                        )
                );
                return parseMethod;
            }
            if (exp.contains("this.")) {
                exp = exp.replace("this.", "src.");
            }
            if (condition != null && condition.contains("this.")) {
                condition = condition.replace("this.", "src.");
            }
            boolean hasCondition = pattern.condition != null && pattern.condition.length() > 0;
            if (hasCondition) {
                parseMethod.beginControlFlow("if(" + condition + ")");
            }

            Element fieldElement = fieldNameSet.get(attr);
            switch (fieldElement.asType().getKind()) {
                case BYTE:
                    parseMethod.addStatement("src." + attr + " = byteBuffer.get()");
                    break;
                case SHORT:
                    parseMethod.addStatement("src." + attr + " = byteBuffer.getShort()");
                    break;
                case INT:
                    parseMethod.addStatement("src." + attr + " = byteBuffer.getInt()");
                    break;
                case LONG:
                    parseMethod.addStatement("src." + attr + " = byteBuffer.getLong()");
                    break;
                case CHAR:
                    parseMethod.addStatement("src." + attr + " = byteBuffer.getChar()");
                    break;
                case ARRAY:
                    parseMethod.addStatement("src." + attr + " = new byte[" + exp + "]");
                    parseMethod.addStatement("byteBuffer.get(src." + attr + ")");
                    break;
                default:
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            String.format("@%s-annotated class with unsupported field type %s. (%s)",
                                    ParsePacket.class.getSimpleName(),
                                    fieldElement.asType().getKind().toString(),
                                    srcClass.asType().toString()
                            )
                    );
                    return parseMethod;
            }
            if (hasCondition) {
                parseMethod.endControlFlow();
            }
        }

        parseMethod.endControlFlow("catch ($T ignore) {}", BufferOverflowExceptionClassName);
        parseMethod.addStatement("return src");
        return parseMethod;
    }

    private void generateJavaFile(String packageName, TypeSpec typeSpec) {
        try {
            JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                    .addFileComment(" This codes are generated automatically. Do not modify!")
                    .indent("    ")
                    .skipJavaLangImports(true)
                    .build();
            javaFile.writeTo(mFiler);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    private String getPackageName(TypeElement type) {
        return mElementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    private static class Pattern {
        public String condition;
        public String attr;
        public String exp;

        public Pattern(String condition, String attr, String exp) {
            this.condition = condition;
            this.attr = attr;
            this.exp = exp;
        }
    }
}
