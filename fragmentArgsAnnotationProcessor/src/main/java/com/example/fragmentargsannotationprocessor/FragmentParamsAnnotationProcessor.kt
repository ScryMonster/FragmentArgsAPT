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
import kotlin.reflect.KClass


@AutoService(Processor::class)
class FragmentParamsAnnotationProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    private val classBundle = ClassName("android.os", "Bundle")
    private val classParcelable = ClassName("android.os", "Parcelable")
    private val classString = ClassName("kotlin", "String")


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
                        types?.directSupertypes(argumentedFragment.asType())?.firstOrNull { it == fType }
                            ?: argumentedFragment.asType()





                    messager?.printMessage(Diagnostic.Kind.WARNING, "mType: $mType ")
                    messager?.printMessage(Diagnostic.Kind.WARNING, "fType: $fType ")
                    messager?.printMessage(
                        Diagnostic.Kind.WARNING,
                        "is types the same: ${types?.isSameType(mType, fType) == true} "
                    )
                    if (types?.isSameType(mType, fType) == true) {

                        val arguments = argumentedFragment?.enclosedElements?.filter {
                            it?.getAnnotation(FragmentParam::class.java) != null
                        } ?: arrayListOf()
                        if (arguments.isEmpty()) {
                            messager?.printMessage(
                                Diagnostic.Kind.WARNING,
                                "It is useless to use '@ArgumentedFragment' without marking it's fields with '@FragmentParam'"
                            )
                        }
                        processAnnotation(argumentedFragment, arguments)
                    }
                } else {
                    messager?.printMessage(
                        Diagnostic.Kind.ERROR,
                        "For correct work of the @ArgumentedFragment,please mark only classes with it."
                    )
                    return true
                }
            }

        return false
    }

    private fun processAnnotation(fragmentClass: Element, fields: List<Element>) {
        val className = fragmentClass.simpleName.toString()
        val packageName = elems?.getPackageOf(fragmentClass).toString()
        val fileName = "${className}Args"


        val fileBuilder = FileSpec.builder(packageName, fileName)
        val classBuilder = TypeSpec.objectBuilder(fileName)

//        fields.forEach { argument ->
//
//            val initializer = "${argument.simpleName}_KEY"
//            fileBuilder.addProperty(
//                PropertySpec.builder(
//                    "${argument.simpleName}Key",
//                    classString
//                )
//                    .initializer("%S", "$initializer")
//                    .build()
//            )
//        }

        fields.forEach { argument ->

            val initializer = "${argument.simpleName}_KEY"
            fileBuilder.addProperty(
                PropertySpec.builder(
                    "${argument.simpleName}Key",
                    classString,
                    KModifier.PRIVATE
                )
                    .initializer("%S", "$initializer")
                    .build()
            )

            val (getFromBundle,asType,type) = when {
                argument isSameJavaTypeAs "java.lang.String" -> Triple("getString","as? kotlin.String",ClassName("kotlin","String").asNullable())
                argument isSameJavaTypeAs "java.lang.Integer" -> Triple("getInt","as? kotlin.Int",ClassName("kotlin","Int").asNullable())
                argument isSameJavaTypeAs "java.lang.Double" -> Triple("getDouble","as? kotlin.Double",ClassName("kotlin","Double").asNullable())
                argument isSameJavaTypeAs "java.lang.Float" ->  Triple("getFloat","as? kotlin.Float",ClassName("kotlin","Float").asNullable())
                argument isSameJavaTypeAs "java.lang.Long" ->  Triple("getLong","as? kotlin.Long",ClassName("kotlin","Long").asNullable())
                argument isSameJavaTypeAs "java.lang.Short" ->  Triple("getShort","as? kotlin.Short",ClassName("kotlin","Short").asNullable())
                argument isSameJavaTypeAs "java.lang.Byte" ->  Triple("getByte","as? kotlin.Byte",ClassName("kotlin","Byte").asNullable())
                argument isSameJavaTypeAs "java.lang.Character" ->  Triple("getChar","as? kotlin.Char",ClassName("kotlin","Char").asNullable())
                else -> Triple("getParcelable","as? ${argument.asType().asTypeName()}",classParcelable)
            }


            fileBuilder.addProperty(
                PropertySpec.builder(
                    "generated${argument.let { it.toString().replaceFirst(it.toString().first(),it.toString().first().toUpperCase()) }}",
                    type.asNullable()
                )
                    .receiver(fragmentClass.asType().asTypeName())
                    .getter(
                        FunSpec.getterBuilder()
                            .addCode("return arguments?.$getFromBundle(${argument.simpleName}Key) $asType").build()
                    )
                    .build()
            )
        }



        val funF = FunSpec.builder("get$className")
            .returns(fragmentClass.asType().asTypeName())
            .addParameters(fields.map {
                when {

                    it isSameJavaTypeAs "java.lang.String" -> buildParamWithType(it.paramName ,String::class)
                    it isSameJavaTypeAs "java.lang.Integer" -> buildParamWithType(it.paramName ,Int::class)
                    it isSameJavaTypeAs "java.lang.Double" -> buildParamWithType(it.paramName ,Double::class)
                    it isSameJavaTypeAs "java.lang.Float" -> buildParamWithType(it.paramName ,Float::class)
                    it isSameJavaTypeAs "java.lang.Long" -> buildParamWithType(it.paramName ,Long::class)
                    it isSameJavaTypeAs "java.lang.Character" -> buildParamWithType(it.paramName ,Char::class)
                    it isSameJavaTypeAs "java.lang.Byte" -> buildParamWithType(it.paramName ,Byte::class)
                    else -> buildParcelableParam(it.paramName, it.asType().asTypeName().toString())
                }
            })

            .addCode("return ${className}()")

        if (fields.isNotEmpty()) {
            funF.addCode(
                """
            .also{
                        it.arguments = %T()
                            .apply{
        """.trimIndent(), classBundle
            )
        }
        messager?.printMessage(Diagnostic.Kind.WARNING, "Fields count: ${fields.count()} ")
        fields.forEach { argument ->
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

        if (fields.isNotEmpty()) {
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


    private fun buildParamWithType(name:String,type: KClass<*>): ParameterSpec {
        messager?.printMessage(Diagnostic.Kind.WARNING, "Field inside type: $javaClass ")
        return ParameterSpec.builder(name, type).build()
    }

    private fun buildParcelableParam(name: String, type: String): ParameterSpec {
        messager?.printMessage(Diagnostic.Kind.WARNING, "Field inside type ss: $javaClass ")
        val className = getSpecificClassName(type)
        return ParameterSpec.builder(name, className).build()
    }

    private fun getSpecificClassName(type: String): ClassName {
        val indexOfLastDot = type.lastIndexOf('.')
        val packageName = type.substring(0, indexOfLastDot)
        messager?.printMessage(Diagnostic.Kind.WARNING, "Package name: $packageName ")

        val typeName = type.substring(indexOfLastDot + 1, type.length)
        messager?.printMessage(Diagnostic.Kind.WARNING, "Type name: $typeName ")

        return ClassName(packageName, typeName)
    }

    private val Element.paramName: String
        get() = "${simpleName}Parameter"

    private fun FunSpec.Builder.putElementInBundleAs(putType: String, argument: Element) {
        addCode(
            """
                $putType(${argument}Key,${argument}Parameter) 
                
            """.trimIndent()
        )  //e.g. putInt(cardNumberKey,cardNumberParameter)
    }

}