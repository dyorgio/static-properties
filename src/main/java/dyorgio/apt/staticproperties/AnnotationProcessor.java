package dyorgio.apt.staticproperties;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

/**
 *
 * @author dyorgio
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class AnnotationProcessor extends AbstractProcessor {

    public static final String STATICPROPERTIES_ANNOTATIONS = "staticproperties.topAnnotations";
    public static final String STATICPROPERTIES_PREFIX = "staticproperties.prefix";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        final Set<TypeElement> topAnnotations = new HashSet<TypeElement>();
        if (processingEnv.getOptions().get(STATICPROPERTIES_ANNOTATIONS) != null) {
            for (String topAnnotationStr : processingEnv.getOptions().get(STATICPROPERTIES_ANNOTATIONS).split(",")) {
                TypeElement annotationElement = processingEnv.getElementUtils().getTypeElement(topAnnotationStr);
                if (annotationElement != null) {
                    topAnnotations.add(annotationElement);
                }
            }
        }

        if (topAnnotations.isEmpty()) {
            return true;
        }

        boolean helperClassesCreated = false;
        Set<TypeElement> classes = new LinkedHashSet<TypeElement>();
        for (Element element : roundEnv.getRootElements()) {
            if (element.getKind() == ElementKind.CLASS) {
                for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
                    if (topAnnotations.contains(annotation.getAnnotationType().asElement())) {
                        classes.add((TypeElement) element);
                    }
                }
            }
        }

        String prefix = processingEnv.getOptions().get(STATICPROPERTIES_PREFIX);
        if (prefix == null || prefix.isEmpty()) {
            prefix = "D";
        }

        try {
            final TypeElement listType = processingEnv.getElementUtils().getTypeElement("java.util.List");

            for (TypeElement classElement : classes) {

                if (!helperClassesCreated) {
                    createHelperClasses();
                    helperClassesCreated = true;
                }

                PackageElement packageElement = (PackageElement) classElement.getEnclosingElement();

                JavaFileObject javaFile = processingEnv.getFiler().createSourceFile((packageElement.getQualifiedName().length() == 0 ? "" : packageElement.getQualifiedName() + ".") + prefix + classElement.getSimpleName());

                BufferedWriter bw = new BufferedWriter(javaFile.openWriter());
                if (packageElement.getQualifiedName().length() > 0) {
                    bw.append("package ").append(packageElement.getQualifiedName()).append(';');
                    bw.newLine();
                    bw.newLine();
                }

                bw.append("import dyorgio.apt.staticproperties.StaticProperty;");
                bw.newLine();
                bw.append("import dyorgio.apt.staticproperties.StaticListProperty;");
                bw.newLine();
                bw.newLine();

                bw.append("public class ").append(prefix).append(classElement.getSimpleName())
                        .append(" extends StaticProperty {");
                bw.newLine();
                bw.newLine();

                bw.append("\tpublic static final ").append(prefix).append(classElement.getSimpleName()) //
                        .append(' ').append(Character.toLowerCase(classElement.getSimpleName().charAt(0)) + classElement.getSimpleName().toString().substring(1))
                        .append(" = new ").append(prefix).append(classElement.getSimpleName()).append("();");

                bw.newLine();
                bw.newLine();

                bw.append("\tpublic ").append(prefix).append(classElement.getSimpleName()) //
                        .append("(StaticProperty parent, String path) {super(parent,path);}");

                bw.newLine();

                bw.append("\tprivate ").append(prefix).append(classElement.getSimpleName()) //
                        .append("() {super();}");

                bw.newLine();
                bw.newLine();

                for (Element inner : classElement.getEnclosedElements()) {
                    if (inner.getKind() == ElementKind.FIELD) {
                        TypeElement fieldType = getExtracted((VariableElement) inner);
                        if (fieldType != null) {
                            if (classes.contains(fieldType)) {
                                PackageElement fieldPackageElement = (PackageElement) fieldType.getEnclosingElement();
                                String typeName = (fieldPackageElement.getQualifiedName().length() == 0 ? "" : fieldPackageElement.getQualifiedName() + ".") + prefix + fieldType.getSimpleName();
                                bw.append("\tpublic final ").append(typeName).append(' ')//
                                        .append(inner.getSimpleName().toString())//
                                        .append(" = new ").append(typeName).append("(this, \"").append(inner.getSimpleName().toString()).append("\");");
                                bw.newLine();
                            } else if (processingEnv.getTypeUtils().isAssignable(fieldType.asType(), listType.asType())) {

                                TypeMirror listGenericType = ((DeclaredType) inner.asType()).getTypeArguments().get(0);
                                String type = listGenericType.toString();

                                if (classes.contains(processingEnv.getElementUtils().getTypeElement(type))) {
                                    int lastIndex = type.lastIndexOf('.') + 1;
                                    type = type.substring(0, lastIndex) + prefix + type.substring(lastIndex);

                                    bw.append("\tpublic final StaticListProperty<")//
                                            .append(type).append("> ")//
                                            .append(inner.getSimpleName().toString())//
                                            .append(" = new StaticListProperty(this, \"").append(inner.getSimpleName().toString()).append("\"){public ")//
                                            .append(type).append(" get(int index) { return new ").append(type)//
                                            .append("(this, String.valueOf(index));}};");
                                    bw.newLine();
                                } else {
                                    bw.append("\tpublic final StaticListProperty<StaticProperty> ")//
                                            .append(inner.getSimpleName().toString())//
                                            .append(" = new StaticListProperty(this, \"").append(inner.getSimpleName().toString()).append("\"){public ")//
                                            .append("StaticProperty get(int index) { return new StaticProperty(this, String.valueOf(index));}};");
                                    bw.newLine();
                                }
                            } else {
                                bw.append("\tpublic final StaticProperty ")//
                                        .append(inner.getSimpleName().toString())//
                                        .append(" = new StaticProperty(this, \"").append(inner.getSimpleName().toString()).append("\");");
                                bw.newLine();
                            }
                        }
                    }
                }

                bw.append('}');
                bw.flush();
                bw.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return true;
    }

    public TypeElement getExtracted(VariableElement ve) {
        TypeMirror typeMirror = ve.asType();
        Element element = processingEnv.getTypeUtils().asElement(typeMirror);
        return (element instanceof TypeElement)
                ? (TypeElement) element : null;
    }

    private void createHelperClasses() throws IOException {
        String xmlPropertySource
                = "package dyorgio.apt.staticproperties;\n"
                + "public class StaticProperty {\n"
                + "    private final StaticProperty parent;\n"
                + "    private final String path;\n"
                + "    \n"
                + "    protected StaticProperty() {\n"
                + "        this.parent = null;\n"
                + "        this.path = null;\n"
                + "    }\n"
                + "    public StaticProperty(StaticProperty parent, String path) {\n"
                + "        this.parent = parent;\n"
                + "        if (parent.getPath() == null){\n"
                + "            this.path = parent.getPath() + \".\" + path;\n"
                + "        } else {\n"
                + "            this.path = path;\n"
                + "        }\n"
                + "    }\n"
                + "    public StaticProperty getParent() {\n"
                + "        return parent;\n"
                + "    }\n"
                + "    public String getPath() {\n"
                + "        return path;\n"
                + "    }\n"
                + "}";

        JavaFileObject javaFile = processingEnv.getFiler().createSourceFile("dyorgio.apt.staticproperties.StaticProperty");
        BufferedWriter bw = new BufferedWriter(javaFile.openWriter());
        bw.write(xmlPropertySource);
        bw.flush();
        bw.close();

        String xmlListPropertySource
                = "package dyorgio.apt.staticproperties;\n"
                + "public abstract class StaticListProperty<T extends StaticProperty> extends StaticProperty {\n"
                + "    public StaticListProperty(StaticProperty parent, String path) {\n"
                + "        super(parent,path);\n"
                + "    }\n"
                + "    public abstract T get(int index);\n"
                + "}";

        javaFile = processingEnv.getFiler().createSourceFile("dyorgio.apt.staticproperties.StaticListProperty");
        bw = new BufferedWriter(javaFile.openWriter());
        bw.write(xmlListPropertySource);
        bw.flush();
        bw.close();
    }
}
