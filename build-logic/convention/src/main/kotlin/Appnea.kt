import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/** Valores que comparten todos los módulos. */
internal object Appnea {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 24
    val JAVA = JavaVersion.VERSION_11
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** SDK, Java 11 y desugaring (java.time en API 24): igual en la app y en las bibliotecas. */
internal fun Project.configurarAndroid(android: CommonExtension) {
    android.apply {
        compileSdk = Appnea.COMPILE_SDK
        defaultConfig.minSdk = Appnea.MIN_SDK
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        compileOptions.isCoreLibraryDesugaringEnabled = true
        compileOptions.sourceCompatibility = Appnea.JAVA
        compileOptions.targetCompatibility = Appnea.JAVA
        // android.util.Log y similares devuelven valores por defecto en los tests de JVM (no fallan)
        testOptions.unitTests.isReturnDefaultValues = true
    }
    dependencies {
        add("coreLibraryDesugaring", libs.findLibrary("desugar-jdk-libs").get())
    }
}
