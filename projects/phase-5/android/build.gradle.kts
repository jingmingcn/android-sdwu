// Top-level build file. Plugins are declared here and applied in :app.
// AGP 9 has built-in Kotlin support, so the kotlin-android plugin is not needed.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.hilt) apply false
}

/**
 * Scaffolds a new student module from templates/module:
 *
 *     ./gradlew newModule -Pid=flash-cards -Pname="Flash Cards" -Pauthor="Ada Lovelace"
 *
 * It creates modules/flash-cards with a working screen, a test and the discovery file. Nothing
 * else needs editing: settings.gradle.kts and the app pick up every folder in modules/.
 */
tasks.register("newModule") {
    group = "teaching framework"
    description = "Creates a new module in modules/ from templates/module (-Pid, -Pname, -Pauthor)."

    val id = providers.gradleProperty("id")
    val displayName = providers.gradleProperty("name")
    val author = providers.gradleProperty("author").orElse("A Student")
    val templateDir = layout.projectDirectory.dir("templates/module").asFile
    val modulesDir = layout.projectDirectory.dir("modules").asFile

    doLast {
        val usage = "Usage: ./gradlew newModule -Pid=my-tool -Pname=\"My Tool\" -Pauthor=\"Your Name\""
        val moduleId = id.orNull ?: throw GradleException("Missing -Pid. $usage")
        if (!Regex("^[a-z][a-z0-9-]{2,32}$").matches(moduleId)) {
            throw GradleException("-Pid must be 3-33 lowercase letters, digits or dashes, starting with a letter. $usage")
        }
        val target = File(modulesDir, moduleId)
        if (target.exists()) throw GradleException("modules/$moduleId already exists")

        val pkg = moduleId.replace("-", "")
        val className = moduleId.split("-").joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }
        val tokens = mapOf(
            "__ID__" to moduleId,
            "__PKG__" to pkg,
            "__CLASS__" to className,
            "__NAME__" to (displayName.orNull ?: className),
            "__AUTHOR__" to author.get(),
        )
        fun String.fill() = tokens.entries.fold(this) { text, (token, value) -> text.replace(token, value) }

        templateDir.walkTopDown().filter { it.isFile }.forEach { source ->
            val out = File(target, source.relativeTo(templateDir).path.fill())
            out.parentFile.mkdirs()
            out.writeText(source.readText().fill())
        }
        println("Created modules/$moduleId (class ${className}Module, package edu.sdwu.teaching.modules.$pkg).")
        println("Next: sync Gradle in Android Studio (or just build), run the app, open the Modules tab.")
    }
}
