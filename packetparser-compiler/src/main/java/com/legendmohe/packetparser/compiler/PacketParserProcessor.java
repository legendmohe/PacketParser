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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Created by legendmohe on 16/8/25.
 */
@AutoService(Processor.class)
public class PacketParserProcessor extends AbstractProcessor {

    private static final String PARSER_CLASS_SUFFIX = "PacketParser";
    private static final Set<Character> gOptionChars = new HashSet<>(Arrays.asList('~'));
    private Elements mElementUtils;
    private Types mTypeUtils;
    private Filer mFiler;
    private Map<Name, TypeElement> mPendingElement = new HashMap<>();

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
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        mElementUtils = env.getElementUtils();
        mTypeUtils = env.getTypeUtils();
        mFiler = env.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        mPendingElement.clear();
        for (Element element : roundEnv.getElementsAnnotatedWith(ParsePacket.class)) {
            if (!SuperficialValidation.validateElement(element))
                continue;
            if (!(element instanceof TypeElement))
                continue;
            Name name = ((TypeElement) element).getQualifiedName();
            if (!mPendingElement.containsKey(name)) {
                mPendingElement.put(name, (TypeElement) element);
            }
        }
        for (TypeElement element :
                mPendingElement.values()) {
            elementToPacketParser(element);
        }
        return true;
    }

    private void elementToPacketParser(TypeElement srcClass) {
        ParsePacket parsePacket = srcClass.getAnnotation(ParsePacket.class);
        String[] packetPatterns = parsePacket.value();
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
        for (String pattern :
                packetPatterns) {
            if (pattern.length() == 0) {
                continue;
            }
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
            String option = "";
            String exp = "";
            int repeat = 0;

            // parse option
            if (gOptionChars.contains(attr.charAt(0))) {
                option = String.valueOf(attr.charAt(0));
                attr = attr.substring(1);
            }
            // parse list count
            if (Character.isDigit(attr.charAt(0))) {
                repeat = Integer.parseInt(attr.substring(0, 1));
                attr = attr.substring(1);
            }
            // parse list count
            if (attr.charAt(0) == '*') {
                repeat = Integer.MAX_VALUE;
                attr = attr.substring(1);
            }

            if (temp.length > 1) {
                exp = temp[1].trim();
            }
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
            patternList.add(new Pattern(condition, attr, exp, option, repeat));
        }

        MethodSpec.Builder parseMethod = MethodSpec.methodBuilder("parse")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .addException(Exception.class)
                .addParameter(ArrayTypeName.of(TypeName.BYTE), "src")
                .returns(TypeName.get(srcClass.asType()));
        parseMethod.addStatement("$T srcObject = new $T()", TypeName.get(srcClass.asType()), TypeName.get(srcClass.asType()));
        parseMethod.addStatement("parse(src, srcObject)");
        parseMethod.addStatement("return srcObject");
        classBuilder.addMethod(parseMethod.build());

        parseMethod = buildParseMethod(srcClass, patternList, fieldNameSet);
        classBuilder.addMethod(parseMethod.build());

        MethodSpec.Builder toBytesMethod = buildToBytesMethod(srcClass, patternList, fieldNameSet);
        classBuilder.addMethod(toBytesMethod.build());

        MethodSpec.Builder parseLenMethod = buildParseLenMethod(srcClass, patternList, fieldNameSet);
        classBuilder.addMethod(parseLenMethod.build());

        generateJavaFile(packageName, classBuilder.build());
    }

    private String getPackageName(TypeElement type) {
        return mElementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    private MethodSpec.Builder buildParseMethod(TypeElement srcClass, List<Pattern> packetPattern, Map<String, Element> fieldNameSet) {
        MethodSpec.Builder parseMethod = MethodSpec.methodBuilder("parse")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addException(Exception.class)
                .addParameter(ArrayTypeName.of(TypeName.BYTE), "bytes")
                .addParameter(TypeName.get(srcClass.asType()), "src")
                .returns(TypeName.INT);

        parseMethod.addStatement("int wrapStartPos = 0");

        // 1. process enclosing element
        Element parentElement = mTypeUtils.asElement(srcClass.getSuperclass());
        if (parentElement != null && parentElement instanceof TypeElement) {
            TypeElement parentTypeElement = (TypeElement) parentElement;
            if (mPendingElement.containsKey(parentTypeElement.getQualifiedName())) {
                ClassName parentParserName = ClassName.get(getPackageName(parentTypeElement), parentTypeElement.getSimpleName() + PARSER_CLASS_SUFFIX);
                parseMethod.addStatement("wrapStartPos = $T.parse(bytes, src)", parentParserName);
            }
        }

        ClassName readerClassName = ClassName.get("java.nio", "ByteBuffer");
        parseMethod.addStatement("$T byteBuffer = $T.wrap(bytes, wrapStartPos, bytes.length - wrapStartPos)", readerClassName, readerClassName);

        for (Pattern pattern :
                packetPattern) {
            String attr = pattern.attr;
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

            String condition = pattern.getFormattedCondition();
            String exp = pattern.getFormattedExp();

            if (!parseMethodAddPattern(fieldNameSet, parseMethod, pattern, attr, condition, exp)) {
                // error occurred
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("@%s-annotated class with unsupported field type %s. (%s)",
                                ParsePacket.class.getSimpleName(),
                                fieldNameSet.get(attr).asType().getKind().toString(),
                                srcClass.asType().toString()
                        )
                );
                return parseMethod;
            }
        }

        parseMethod.addStatement("return byteBuffer.position()");
        return parseMethod;
    }

    private MethodSpec.Builder buildToBytesMethod(TypeElement srcClass, List<Pattern> packetPattern, Map<String, Element> fieldNameSet) {
        MethodSpec.Builder toBytesMethod = MethodSpec.methodBuilder("toBytes")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addException(Exception.class)
                .addParameter(TypeName.get(srcClass.asType()), "src")
                .returns(ArrayTypeName.of(TypeName.BYTE));

        // return 0 if src is null
        toBytesMethod.beginControlFlow("if (src == null)")
                .addStatement("return null")
                .endControlFlow();

        // calculate buffer len
        ClassName parseName = ClassName.get(getPackageName(srcClass), srcClass.getSimpleName() + PARSER_CLASS_SUFFIX);
        toBytesMethod.addStatement("int bufferLen = $T.parseLen(src)", parseName);

        ClassName readerClassName = ClassName.get("java.nio", "ByteBuffer");
        toBytesMethod.addStatement("$T byteBuffer = $T.allocate(bufferLen)", readerClassName, readerClassName);

        Element parentElement = mTypeUtils.asElement(srcClass.getSuperclass());
        if (parentElement != null && parentElement instanceof TypeElement) {
            TypeElement parentTypeElement = (TypeElement) parentElement;
            if (mPendingElement.containsKey(parentTypeElement.getQualifiedName())) {
                ClassName parentParserName = ClassName.get(getPackageName(parentTypeElement), parentTypeElement.getSimpleName() + PARSER_CLASS_SUFFIX);
                toBytesMethod.addStatement("byte[] parentBytes = $T.toBytes(src)", parentParserName);
                toBytesMethod.beginControlFlow("if (parentBytes != null)")
                        .addStatement("byteBuffer.put(parentBytes)")
                        .endControlFlow();
            }
        }

        // 3. process attributes
        for (Pattern pattern :
                packetPattern) {

            // ignore opt
            if (pattern.containsIgnoreOpt()) {
                continue;
            }

            String attr = pattern.attr;
            String exp = pattern.getFormattedExp();
            String condition = pattern.getFormattedCondition();

            if (exp.length() == 0) {
                Element fieldElement = mTypeUtils.asElement(fieldNameSet.get(pattern.attr).asType());
                if (fieldElement != null && fieldElement instanceof TypeElement) {
                    TypeElement attrTypeElement = (TypeElement) fieldElement;
                    if (mPendingElement.containsKey(attrTypeElement.getQualifiedName())) {
                        ClassName attrParserName = ClassName.get(getPackageName(attrTypeElement), attrTypeElement.getSimpleName() + PARSER_CLASS_SUFFIX);
                        exp = attrParserName.simpleName() + ".parseLen(src." + pattern.attr + ")";
                    }
                }
            }

            if (!toBytesMethodAddPattern(fieldNameSet, toBytesMethod, pattern, attr, exp, condition)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("@%s-annotated class with unsupported field type %s. (%s)",
                                ParsePacket.class.getSimpleName(),
                                fieldNameSet.get(attr).asType().getKind().toString(),
                                srcClass.asType().toString()
                        )
                );
                return toBytesMethod;
            }
        }

        toBytesMethod.addStatement("return byteBuffer.array()");
        return toBytesMethod;
    }

    private MethodSpec.Builder buildParseLenMethod(TypeElement srcClass, List<Pattern> patternList, Map<String, Element> fieldNameSet) {
        MethodSpec.Builder parseLenMethod = MethodSpec.methodBuilder("parseLen")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(TypeName.get(srcClass.asType()), "src")
                .returns(TypeName.INT);

        // return 0 if src is null
        parseLenMethod.beginControlFlow("if (src == null)")
                .addStatement("return 0")
                .endControlFlow();

        // calculate buffer len
        parseLenMethod.addStatement("int bufferLen = 0");

        // 1. collect unconditional pattern
        StringBuilder unconditionalLen = new StringBuilder();
        for (Pattern pattern :
                patternList) {
            // ignore opt
            if (pattern.containsIgnoreOpt()) {
                continue;
            }

            String exp = pattern.getFormattedExp();
            String condition = pattern.getFormattedCondition();

            boolean hasCondition = condition != null && condition.length() > 0;
            if (hasCondition) {
                parseLenMethod.beginControlFlow("if(" + condition + ")");
            }

            if (exp.length() == 0) {
                Element fieldElement = mTypeUtils.asElement(fieldNameSet.get(pattern.attr).asType());
                if (fieldElement != null && fieldElement instanceof TypeElement) {
                    TypeElement attrTypeElement = (TypeElement) fieldElement;
                    if (mPendingElement.containsKey(attrTypeElement.getQualifiedName())) {
                        ClassName attrParserName = ClassName.get(getPackageName(attrTypeElement), attrTypeElement.getSimpleName() + PARSER_CLASS_SUFFIX);
                        exp = attrParserName.simpleName() + ".parseLen(src." + pattern.attr + ")";
                    }
                }
            }

            if (pattern.getRepeatCount() > 0) {
                Element fieldElement = mTypeUtils.asElement(fieldNameSet.get(pattern.attr).asType());
                if (fieldElement != null && fieldElement instanceof TypeElement) {
                    TypeMirror fieldType = fieldNameSet.get(pattern.attr).asType();
                    fieldType = ((DeclaredType) fieldType).getTypeArguments().get(0);
                    Name simpleName = ((DeclaredType) fieldType).asElement().getSimpleName();
                    if (simpleName.contentEquals("Integer")
                            || simpleName.contentEquals("Byte")
                            || simpleName.contentEquals("Short")
                            || simpleName.contentEquals("Long")
                            || simpleName.contentEquals("Character")
                            || simpleName.contentEquals("byte[]")) {
                        exp = exp + "*" + pattern.repeat;
                    } else {
                        parseLenMethod.beginControlFlow("for (int i = 0; i < $L && i < src." + pattern.attr + ".size(); i++)", pattern.repeat);
                        TypeElement attrTypeElement = (TypeElement) mTypeUtils.asElement(fieldType);
                        if (mPendingElement.containsKey(attrTypeElement.getQualifiedName())) {
                            ClassName attrParserName = ClassName.get(getPackageName(attrTypeElement), attrTypeElement.getSimpleName() + PARSER_CLASS_SUFFIX);
                            parseLenMethod.addStatement("bufferLen += " + attrParserName.simpleName() + ".parseLen(src." + pattern.attr + ".get(i))");
                        }
                        parseLenMethod.endControlFlow();

                        // condition control flow
                        if (hasCondition) {
                            parseLenMethod.endControlFlow();
                        }
                        continue;
                    }
                }
            }

            if (condition == null || condition.length() == 0) {
                unconditionalLen.append(exp);
                unconditionalLen.append(" + ");
            } else {
                parseLenMethod.beginControlFlow("if(" + condition + ")")
                        .addStatement("bufferLen += " + exp)
                        .endControlFlow();
            }

            // condition control flow
            if (hasCondition) {
                parseLenMethod.endControlFlow();
            }
        }
        if (unconditionalLen.length() != 0) {
            unconditionalLen.delete(unconditionalLen.length() - 3, unconditionalLen.length());
            parseLenMethod.addStatement("bufferLen += " + unconditionalLen.toString());
        }

        // 2. process enclosing element
        Element parentElement = mTypeUtils.asElement(srcClass.getSuperclass());
        if (parentElement != null && parentElement instanceof TypeElement) {
            TypeElement parentTypeElement = (TypeElement) parentElement;
            if (mPendingElement.containsKey(parentTypeElement.getQualifiedName())) {
                ClassName parentParserName = ClassName.get(getPackageName(parentTypeElement), parentTypeElement.getSimpleName() + PARSER_CLASS_SUFFIX);
                parseLenMethod.addStatement("bufferLen += $T.parseLen(src)", parentParserName);
            }
        }

        parseLenMethod.addStatement("return bufferLen");
        return parseLenMethod;
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

    private boolean parseMethodAddPattern(Map<String, Element> fieldNameSet, MethodSpec.Builder parseMethod, Pattern pattern, String attr, String condition, String exp) {
        boolean hasCondition = condition != null && condition.length() > 0;
        if (hasCondition) {
            parseMethod.beginControlFlow("if(" + condition + ")");
        }

        String byteBufferString = pattern.containsIgnoreOpt() ? "byteBuffer.slice()" : "byteBuffer";
        String assignString = pattern.getRepeatCount() > 0 ? ".add(" : " = ";
        String encloseString = pattern.getRepeatCount() > 0 ? ")" : "";
        Element fieldElement = fieldNameSet.get(attr);
        TypeMirror fieldType = fieldElement.asType();
        TypeKind fieldKind = fieldType.getKind();

        if (pattern.getRepeatCount() > 0) {
            parseMethod.addStatement("src." + attr + " = new $T<>()", ArrayList.class);
            parseMethod.beginControlFlow("for (int i = 0; i < $L && byteBuffer.hasRemaining(); i++)", pattern.repeat);
            fieldType = ((DeclaredType) fieldType).getTypeArguments().get(0);
            Name simpleName = ((DeclaredType) fieldType).asElement().getSimpleName();
            if (simpleName.contentEquals("Integer")) {
                fieldKind = TypeKind.INT;
            } else if (simpleName.contentEquals("Byte")) {
                fieldKind = TypeKind.BYTE;
            } else if (simpleName.contentEquals("Short")) {
                fieldKind = TypeKind.SHORT;
            } else if (simpleName.contentEquals("Long")) {
                fieldKind = TypeKind.LONG;
            } else if (simpleName.contentEquals("Character")) {
                fieldKind = TypeKind.CHAR;
            } else if (simpleName.contentEquals("byte[]")) {
                fieldKind = TypeKind.ARRAY;
            }
        }
        switch (fieldKind) {
            case BYTE:
                parseMethod.addStatement("src." + attr + assignString + byteBufferString + ".get()" + encloseString);
                break;
            case SHORT:
                parseMethod.addStatement("src." + attr + assignString + byteBufferString + ".getShort()" + encloseString);
                break;
            case INT:
                parseMethod.addStatement("src." + attr + assignString + byteBufferString + ".getInt()" + encloseString);
                break;
            case LONG:
                parseMethod.addStatement("src." + attr + assignString + byteBufferString + ".getLong()" + encloseString);
                break;
            case CHAR:
                parseMethod.addStatement("src." + attr + assignString + byteBufferString + ".getChar()" + encloseString);
                break;
            case ARRAY:
                parseMethod.addStatement("byte[] " + attr + "Array = new byte[" + exp + "]");
                parseMethod.beginControlFlow("if(" + attr + "Array.length > 0)");
                parseMethod.addStatement(byteBufferString + ".get(" + attr + "Array)");
                parseMethod.addStatement("src." + attr + assignString + attr + "Array" + encloseString);
                parseMethod.endControlFlow();
                break;
            case DECLARED:
                Element tempElement = mTypeUtils.asElement(fieldType);
                if (tempElement instanceof TypeElement) {
                    TypeElement attrTypeElement = (TypeElement) tempElement;
                    if (mPendingElement.containsKey(attrTypeElement.getQualifiedName())) {
                        ClassName attrParserName = ClassName.get(getPackageName(attrTypeElement), attrTypeElement.getSimpleName() + PARSER_CLASS_SUFFIX);

                        parseMethod.addStatement("byte[] " + attr + "Bytes = new byte[byteBuffer.remaining()]");
                        parseMethod.addStatement("byteBuffer.slice().get(" + attr + "Bytes)");
                        parseMethod.addStatement("$T " + attr + "Object = $T.parse(" + attr + "Bytes)", ClassName.get(attrTypeElement), attrParserName);
                        parseMethod.addStatement("src." + attr + assignString + attr + "Object" + encloseString);
                        parseMethod.addStatement(byteBufferString + ".position(byteBuffer.position() + $T.parseLen(" + attr + "Object))", attrParserName);
                    }
                }
                break;
            default:
                return false;
        }
        if (pattern.getRepeatCount() > 0) {
            parseMethod.endControlFlow();
        }

        // condition control flow
        if (hasCondition) {
            parseMethod.endControlFlow();
        }
        return true;
    }

    private boolean toBytesMethodAddPattern(Map<String, Element> fieldNameSet, MethodSpec.Builder toBytesMethod, Pattern pattern, String attr, String exp, String condition) {
        boolean hasCondition = condition != null && condition.length() > 0;
        if (hasCondition) {
            toBytesMethod.beginControlFlow("if(" + condition + ")");
        }

        String attrString = "src." + attr + (pattern.getRepeatCount() > 0 ? ".get(i)" : "");
        Element fieldElement = fieldNameSet.get(attr);
        TypeMirror fieldType = fieldElement.asType();
        TypeKind fieldKind = fieldType.getKind();
        if (pattern.getRepeatCount() > 0) {
            toBytesMethod.beginControlFlow("for (int i = 0; i < $L && byteBuffer.hasRemaining(); i++)", pattern.repeat);
            fieldType = ((DeclaredType) fieldType).getTypeArguments().get(0);
            Name simpleName = ((DeclaredType) fieldType).asElement().getSimpleName();
            if (simpleName.contentEquals("Integer")) {
                fieldKind = TypeKind.INT;
            } else if (simpleName.contentEquals("Byte")) {
                fieldKind = TypeKind.BYTE;
            } else if (simpleName.contentEquals("Short")) {
                fieldKind = TypeKind.SHORT;
            } else if (simpleName.contentEquals("Long")) {
                fieldKind = TypeKind.LONG;
            } else if (simpleName.contentEquals("Character")) {
                fieldKind = TypeKind.CHAR;
            } else if (simpleName.contentEquals("byte[]")) {
                fieldKind = TypeKind.ARRAY;
            }
        }
        switch (fieldKind) {
            case BYTE:
                toBytesMethod.addStatement("byteBuffer.put(" + attrString + ")");
                break;
            case SHORT:
                toBytesMethod.addStatement("byteBuffer.putShort(" + attrString + ")");
                break;
            case INT:
                toBytesMethod.addStatement("byteBuffer.putInt(" + attrString + ")");
                break;
            case LONG:
                toBytesMethod.addStatement("byteBuffer.putLong(" + attrString + ")");
                break;
            case CHAR:
                toBytesMethod.addStatement("byteBuffer.putChar(" + attrString + ")");
                break;
            case ARRAY:
                toBytesMethod.beginControlFlow("if(" + attrString + " != null && " + attrString + ".length != 0)")
                        .addStatement("byteBuffer.put(" + attrString + ")")
                        .nextControlFlow("else")
                        .addStatement("byteBuffer.put(new byte[" + exp + "])")
                        .endControlFlow();
                break;
            case DECLARED:
                Element tempElement = mTypeUtils.asElement(fieldType);
                if (tempElement instanceof TypeElement) {
                    TypeElement attrTypeElement = (TypeElement) tempElement;
                    if (mPendingElement.containsKey(attrTypeElement.getQualifiedName())) {
                        ClassName attrParserName = ClassName.get(getPackageName(attrTypeElement), attrTypeElement.getSimpleName() + PARSER_CLASS_SUFFIX);
                        toBytesMethod.addStatement("byteBuffer.put($T.toBytes(" + attrString + "))", attrParserName);
                    }
                }
                break;
            default:
                return false;
        }
        if (pattern.getRepeatCount() > 0) {
            toBytesMethod.endControlFlow();
        }
        if (hasCondition) {
            toBytesMethod.endControlFlow();
        }
        return true;
    }

}
