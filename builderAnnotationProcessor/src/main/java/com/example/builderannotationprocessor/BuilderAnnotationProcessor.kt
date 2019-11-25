package com.example.builderannotationprocessor

import com.example.annotation.Builder
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


@AutoService(Processor::class)
class BuilderAnnotationProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    private var filer: Filer? = null
    private var messager: Messager? = null


    override fun init(env: ProcessingEnvironment?) {
        super.init(env)
        filer = env?.filer
        messager = env?.messager

    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf(Builder::class.java.name)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(p0: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        roundEnv?.getElementsAnnotatedWith(Builder::class.java)?.forEach { element ->
            if (element.kind != ElementKind.CLASS){
                messager?.printMessage(Diagnostic.Kind.ERROR,"For correct work of the @Builder,please mark only classes with it.")
                return true
            }
            else processAnnotation(element)
        }

        return false
    }

    private fun processAnnotation(element: Element){
        val className = element.simpleName.toString()
        val packageName = processingEnv.elementUtils.getPackageOf(element).toString()
        val fileName = "${className}Builder"

        val fileBuilder = FileSpec.builder(packageName, fileName)
        val classBuilder = TypeSpec.classBuilder(fileName)

        val funBuilder = FunSpec.builder("build")
            .returns(element.asType().asTypeName())
//            .addStatement("val local$className = $className()")

        classBuilder.addProperty(
            PropertySpec.builder("local$className",element.asType().asTypeName(),KModifier.PRIVATE)
                .initializer("${element.asType().asTypeName()}()")
                .build()

        )

        for (enclosed in element.enclosedElements){
            messager?.printMessage(Diagnostic.Kind.WARNING,"Enclosed type: ${enclosed.kind} ")
            if (enclosed.kind == ElementKind.FIELD){
                classBuilder.addProperty(
                    PropertySpec.varBuilder(enclosed.simpleName.toString(),enclosed.asType().asTypeName().asNullable(),KModifier.PRIVATE)
                        .initializer("null")
                        .build()
                )

                classBuilder.addFunction(
                    FunSpec.builder("set${enclosed.simpleName}")
//                        .addStatement("this.${enclosed.simpleName} = ${enclosed.simpleName}")
//                        .addStatement("return this")
                        .addStatement("return apply{ this.${enclosed.simpleName} = ${enclosed.simpleName} }")
                        .build()
                )

                classBuilder.addFunction(
                    FunSpec.builder("get${enclosed.simpleName}")
                        .returns(enclosed.asType().asTypeName().asNullable())
                        .addStatement("return this.${enclosed.simpleName}")
                        .build()
                )

                funBuilder.addStatement("${enclosed.simpleName}?.let{ local$className.${enclosed.simpleName} = it }")
            }
        }


        funBuilder.addStatement("return local$className")
        val build = funBuilder.build()
        classBuilder.addFunction(build)




        val file = fileBuilder.addType(classBuilder.build()).build()
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir))
    }


}