package com.leo.font.lib.compiler;

import com.leo.font.lib.annotations.AutoScale;
import com.leo.font.lib.compiler.NameStore;
import com.leo.font.lib.compiler.ProcessingUtils;
import com.mindorks.lib.annotations.BindView;
import com.leo.font.lib.annotations.IgnoreScale;
import com.leo.font.lib.annotations.Keep;
import com.mindorks.lib.annotations.OnClick;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

public class Processor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
    }

    /*
     * annotations: list of unique annotations that are getting processed
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {

            // find all the classes that uses the supported annotations
            Set<TypeElement> typeElements = ProcessingUtils.getTypeElementsToProcess(
                    roundEnv.getRootElements(),
                    annotations);

            // for each such class create a wrapper class for binding
            for (TypeElement typeElement : typeElements) {
                String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
                String typeNameContainer = getParentChainType(typeElement);
                String typeName = getParentChain(typeElement);

                ClassName className = ClassName.get(packageName, typeNameContainer);

                ClassName generatedClassName = ClassName
                        .get(packageName, NameStore.getGeneratedClassName(typeName));

                // define the wrapper class
                TypeSpec.Builder classBuilder = TypeSpec.classBuilder(generatedClassName)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Keep.class);

                // add constructor
                classBuilder.addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(className, NameStore.Variable.CONTAINER)
                        .addStatement("$N($N)",
                                NameStore.Method.BIND_FONTS,
                                NameStore.Variable.CONTAINER)
//                        .addStatement("$N($N)",
//                                NameStore.Method.BIND_ON_CLICKS,
//                                NameStore.Variable.CONTAINER)
                        .build());

                // add method that maps the views with id
                MethodSpec.Builder bindViewsMethodBuilder = MethodSpec
                        .methodBuilder(NameStore.Method.BIND_FONTS)
                        .addModifiers(Modifier.PRIVATE)
                        .returns(void.class)
                        .addParameter(className, NameStore.Variable.CONTAINER);

/*                for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
                    BindView bindView = variableElement.getAnnotation(BindView.class);
                    if (bindView != null) {
                        bindViewsMethodBuilder.addStatement("$N.$N = ($T)$N.findViewById($L)",
                                NameStore.Variable.CONTAINER,
                                variableElement.getSimpleName(),
                                variableElement,
                                NameStore.Variable.CONTAINER,
                                bindView.value());
                    }
                }*/
                ClassName fontManagerClassName = ClassName.get(
                        NameStore.Package.FONT_MANAGER,
                        NameStore.Class.FONT_MANAGER);
                for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
                    AutoScale fontAutoScale = variableElement.getAnnotation(AutoScale.class);
                    if (fontAutoScale != null) {
                        if (fontAutoScale.isScale())
                            bindViewsMethodBuilder.addStatement("$T.applyScaleFont($N.$N)",
                                    fontManagerClassName,
                                    NameStore.Variable.CONTAINER,
                                    variableElement.getSimpleName());
                    }
                    IgnoreScale fontIgnoreScale = variableElement.getAnnotation(IgnoreScale.class);
                    if (fontIgnoreScale != null) {
                        if (fontIgnoreScale.isIgnoreScale())
                            bindViewsMethodBuilder.addStatement("$T.applyScaleDownFont($N.$N)",
                                    fontManagerClassName,
                                    NameStore.Variable.CONTAINER,
                                    variableElement.getSimpleName());
                    }
                }
                classBuilder.addMethod(bindViewsMethodBuilder.build());

/*
                // add method that attaches the onClickListeners
                ClassName androidOnClickListenerClassName = ClassName.get(
                        NameStore.Package.ANDROID_VIEW,
                        NameStore.Class.ANDROID_VIEW,
                        NameStore.Class.ANDROID_VIEW_ON_CLICK_LISTENER);

                ClassName androidViewClassName = ClassName.get(
                        NameStore.Package.ANDROID_VIEW,
                        NameStore.Class.ANDROID_VIEW);

                MethodSpec.Builder bindOnClicksMethodBuilder = MethodSpec
                        .methodBuilder(NameStore.Method.BIND_ON_CLICKS)
                        .addModifiers(Modifier.PRIVATE)
                        .returns(void.class)
                        .addParameter(className, NameStore.Variable.CONTAINER, Modifier.FINAL);

                for (ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                    OnClick onClick = executableElement.getAnnotation(OnClick.class);
                    if (onClick != null) {
                        TypeSpec OnClickListenerClass = TypeSpec.anonymousClassBuilder("")
                                .addSuperinterface(androidOnClickListenerClassName)
                                .addMethod(MethodSpec.methodBuilder(NameStore.Method.ANDROID_VIEW_ON_CLICK)
                                        .addModifiers(Modifier.PUBLIC)
                                        .addParameter(androidViewClassName, NameStore.Variable.ANDROID_VIEW)
                                        .addStatement("$N.$N($N)",
                                                NameStore.Variable.CONTAINER,
                                                executableElement.getSimpleName(),
                                                NameStore.Variable.ANDROID_VIEW)
                                        .returns(void.class)
                                        .build())
                                .build();
                        bindOnClicksMethodBuilder.addStatement("$N.findViewById($L).setOnClickListener($L)",
                                NameStore.Variable.CONTAINER,
                                onClick.value(),
                                OnClickListenerClass);
                    }
                }
                classBuilder.addMethod(bindOnClicksMethodBuilder.build());
*/

                // write the defines class to a java file
                try {
                    JavaFile.builder(packageName,
                            classBuilder.build())
                            .build()
                            .writeTo(filer);
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, e.toString(), typeElement);
                }
            }
        }
        return true;
    }
/*
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {

            // find all the classes that uses the supported annotations
            Set<TypeElement> typeElements = ProcessingUtils.getTypeElementsToProcess(
                    roundEnv.getRootElements(),
                    annotations);

            // for each such class create a wrapper class for binding
            for (TypeElement typeElement : typeElements) {
                //String parentClass = getParentChain(typeElement);
                String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
                String typeName = getParentChain(typeElement);
                String typeNameContainer = getParentChainType(typeElement);

                ClassName className = ClassName.get(packageName, typeNameContainer);
                messager.printMessage(Diagnostic.Kind.ERROR, className.toString(), typeElement);
                ClassName generatedClassName = ClassName
                        .get(packageName, NameStore.getGeneratedClassName(typeName));

                // define the wrapper class
                TypeSpec.Builder classBuilder = TypeSpec.classBuilder(generatedClassName)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Keep.class);

                // add constructor
                classBuilder.addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(className, NameStore.Variable.CONTAINER)
                        .addStatement("$N($N)",
                                NameStore.Method.BIND_FONTS,
                                NameStore.Variable.CONTAINER)
                        .build());
                ClassName fontManagerClassName = ClassName.get(
                        NameStore.Package.FONT_MANAGER,
                        NameStore.Class.FONT_MANAGER);
                // add method that maps the views with id
                MethodSpec.Builder bindViewsMethodBuilder = MethodSpec
                        .methodBuilder(NameStore.Method.BIND_FONTS)
                        .addModifiers(Modifier.PRIVATE)
                        .returns(void.class)
                        .addParameter(className, NameStore.Variable.CONTAINER);

                for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
                    AutoScale fontAutoScale = variableElement.getAnnotation(AutoScale.class);
                    if (fontAutoScale != null) {
                        if (fontAutoScale.isScale())
                            bindViewsMethodBuilder.addStatement("$T.applyScaleFont($N.$N)",
                                    fontManagerClassName,
                                    NameStore.Variable.CONTAINER,
                                    variableElement.getSimpleName());
                    }
                    IgnoreScale fontIgnoreScale = variableElement.getAnnotation(IgnoreScale.class);
                    if (fontIgnoreScale != null) {
                        if (fontIgnoreScale.isIgnoreScale())
                            bindViewsMethodBuilder.addStatement("$T.applyScaleDownFont($N.$N)",
                                    fontManagerClassName,
                                    NameStore.Variable.CONTAINER,
                                    variableElement.getSimpleName());
                    }
                }
                classBuilder.addMethod(bindViewsMethodBuilder.build());

                // write the defines class to a java file
                try {
                    JavaFile.builder(packageName,
                            classBuilder.build())
                            .build()
                            .writeTo(filer);
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, e.toString(), typeElement);
                }
            }
        }
        return true;
    }
*/

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new TreeSet<>(Arrays.asList(
                BindView.class.getCanonicalName(),
                OnClick.class.getCanonicalName(),
                AutoScale.class.getCanonicalName(),
                IgnoreScale.class.getCanonicalName(),
                Keep.class.getCanonicalName()));
    }
    private static String getParentChain(final TypeElement targetClass) {
        // if input is top level class return it
        // otherwise return the parent chain plus it

        if (targetClass.getNestingKind() == NestingKind.TOP_LEVEL) {
            return targetClass.getSimpleName().toString();
        } else {
            final Element parent = targetClass.getEnclosingElement();

            if (parent.getKind() != ElementKind.CLASS) {
                return null;
                //throw new RuntimeException("Cannot create parent chain. Non-class parent found.");
            }

            return (getParentChain((TypeElement) parent)) + "$" + targetClass.getSimpleName().toString();
        }
    }
    private static String getParentChainType(final TypeElement targetClass) {
        // if input is top level class return it
        // otherwise return the parent chain plus it

        if (targetClass.getNestingKind() == NestingKind.TOP_LEVEL) {
            return targetClass.getSimpleName().toString();
        } else {
            final Element parent = targetClass.getEnclosingElement();

            if (parent.getKind() != ElementKind.CLASS) {
                return null;
                //throw new RuntimeException("Cannot create parent chain. Non-class parent found.");
            }

            return (getParentChain((TypeElement) parent)) + "." + targetClass.getSimpleName().toString();
        }
    }
}
