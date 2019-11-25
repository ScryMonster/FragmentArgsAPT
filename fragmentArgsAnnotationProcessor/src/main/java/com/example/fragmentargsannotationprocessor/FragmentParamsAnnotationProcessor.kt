package com.example.fragmentargsannotationprocessor

import com.example.fragmentargsannotation.ArgumentedFragment
import com.example.fragmentargsannotation.FragmentParam
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import com.squareup.kotlinpoet.ClassName
import javax.lang.model.type.TypeMirror


@AutoService(Processor::class)
class FragmentParamsAnnotationProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    private val classBundle = ClassName("android.os", "Bundle")
    private val classParcelable = ClassName("android.os", "Parcelable")
    private val classString = ClassName("kotlin", "String")
    private val parcelableType: TypeMirror?
        get() = elems?.getTypeElement("android.os.Parcelable")?.asType()


    private var filer: Filer? = null
    private var messager: Messager? = null

    private var types: Types? = null
    private var elems: Elements? = null


    override fun init(env: ProcessingEnvironment?) {
        super.init(env)
        filer = env?.filer
        messager = env?.messager
        types = env?.typeUtils
        elems = env?.elementUtils

    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> =
        mutableSetOf(FragmentParam::class.java.name, ArgumentedFragment::class.java.name)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(elements: MutableSet<out TypeElement>?, env: RoundEnvironment?): Boolean {
        env?.getElementsAnnotatedWith(ArgumentedFragment::class.java)
            ?.forEach { argumentedFragment ->
                messager?.printMessage(
                    Diagnostic.Kind.WARNING,
                    "Enclosed type: ${argumentedFragment.kind} "
                )
                messager?.printMessage(
                    Diagnostic.Kind.WARNING,
                    "Enclosed name: ${argumentedFragment.simpleName} "
                )
                if (argumentedFragment.kind == ElementKind.CLASS) {
                    val fType = elems?.getTypeElement("androidx.fragment.app.Fragment")?.asType()
                    val mType =
                        types?.directSupertypes(argumentedFragment.asType())?.find { it == fType }
                            ?: argumentedFragment.asType()





                    messager?.printMessage(Diagnostic.Kind.WARNING, "mType: $mType ")
                    messager?.printMessage(Diagnostic.Kind.WARNING, "fType: $fType ")
                    messager?.printMessage(
                        Diagnostic.Kind.WARNING,
                        "is types the same: ${types?.isSameType(mType, fType) == true} "
                    )
                    if (types?.isSameType(mType, fType) == true) {

                        val arguments = argumentedFragment?.enclosedElements?.filter { it?.getAnnotation(FragmentParam::class.java) != null } ?: arrayListOf()
                        processAnnotation(argumentedFragment, arguments)
                    }
                } else {
                    messager?.printMessage(
                        Diagnostic.Kind.ERROR,
                        "For correct work of the @Builder,please mark only classes with it."
                    )
                    return true
                }
            }

        return false
    }

    private fun processAnnotation(fragmentClass: Element, fileds: List<Element>) {
        val className = fragmentClass.simpleName.toString()
        val packageName = processingEnv.elementUtils.getPackageOf(fragmentClass).toString()
        val fileName = "${className}Args"


        val fileBuilder = FileSpec.builder(packageName, fileName)
        val classBuilder = TypeSpec.objectBuilder(fileName)

        fileds.forEach { argument ->

            val initializer = "${argument.simpleName}_KEY"
            classBuilder.addProperty(
                PropertySpec.varBuilder(
                    "${argument.simpleName}Key",
                    classString
                )
                    .initializer("%S", "$initializer")
                    .build()
            )
        }

        val funF = FunSpec.builder("get$className")
            .returns(fragmentClass.asType().asTypeName())

            .addParameters(fileds.map {
                when {

                    it isSameJavaTypeAs "java.lang.String" -> it.paramName buildParamWithType "java.lang.String"
                    it isSameJavaTypeAs "java.lang.Integer" -> it.paramName buildParamWithType "java.lang.Integer"
                    it isSameJavaTypeAs "java.lang.Double" -> it.paramName buildParamWithType "java.lang.Double"
                    it isSameJavaTypeAs "java.lang.Float" -> it.paramName buildParamWithType "java.lang.Float"
                    it isSameJavaTypeAs "java.lang.Long" -> it.paramName buildParamWithType "java.lang.Long"
                    it isSameJavaTypeAs "java.lang.Character" -> it.paramName buildParamWithType "java.lang.Character"
                    it isSameJavaTypeAs "java.lang.Byte" -> it.paramName buildParamWithType "java.lang.Byte"
                    else -> it.paramName buildParamWithType "android.os.Parcelable"
                }
            })

            .addCode("return ${className}()")

        if (fileds.isNotEmpty()) {
            funF.addCode(
                """
            .also{
                        it.arguments = %T()
                            .apply{
        """.trimIndent(), classBundle
            )
        }
        messager?.printMessage(Diagnostic.Kind.WARNING, "Fields count: ${fileds.count()} ")
        fileds.forEach { argument ->
            if(argument isSameJavaTypeAs "java.lang.String") funF.putElementInBundleAs("putString",argument)
            else if(argument isSameJavaTypeAs "java.lang.Integer") funF.putElementInBundleAs("putInt",argument)
            else if(argument isSameJavaTypeAs "java.lang.Double") funF.putElementInBundleAs("putDouble",argument)
            else if(argument isSameJavaTypeAs "java.lang.Float") funF.putElementInBundleAs("putFloat",argument)
            else if(argument isSameJavaTypeAs "java.lang.Long") funF.putElementInBundleAs("putLong",argument)
            else if(argument isSameJavaTypeAs "java.lang.Short") funF.putElementInBundleAs("putShort",argument)
            else if(argument isSameJavaTypeAs "java.lang.Byte") funF.putElementInBundleAs("putByte",argument)
            else if(argument isSameJavaTypeAs "java.lang.Character") funF.putElementInBundleAs("putChar",argument)
            else funF.putElementInBundleAs("putParcelable",argument)

        }

        if(fileds.isNotEmpty()) {
            funF.addCode(
                """
                    }
                            }
        """.trimIndent()
            )
        }
        classBuilder.addFunction(funF.build())

        val file = fileBuilder.addType(classBuilder.build()).build()
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir))

    }


    private infix fun Element.isSameJavaTypeAs(javaType: String) = types?.isSameType(
        this.asType(),
        elems?.getTypeElement(javaType)?.asType()
    ) == true


    private infix fun String.buildParamWithType(type: String): ParameterSpec {
        messager?.printMessage(Diagnostic.Kind.WARNING, "Field inside type: $javaClass ")
        val type = when (type) {

            "java.lang.String" -> String::class
            "java.lang.Integer" -> Int::class
            "java.lang.Double" -> Double::class
            "java.lang.Float" -> Float::class
            "java.lang.Long" -> Long::class
            "java.lang.Short" -> Short::class
            "java.lang.Character" -> Char::class
            "java.lang.Byte" -> Byte::class
            else -> null
        }

        return type?.let { ParameterSpec.builder(this, it).build() }
            ?: ParameterSpec.builder(this, classParcelable).build()
    }

    private val Element.paramName: String
        get() = "${simpleName}Parameter"

    private fun FunSpec.Builder.putElementInBundleAs(type:String,argument:Element){
        addCode(
            """
                $type(${argument}Key,${argument}Parameter)
                
            """.trimIndent()
        )
    }

}