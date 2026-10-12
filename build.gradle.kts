import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        // Test against the locally installed PhpStorm's major version when there is one
        val localPhpStormVersion = file("${System.getProperty("user.home")}/Applications/PhpStorm.app/Contents/Info.plist")
            .takeIf { it.exists() }
            ?.readText()
            ?.let { Regex("<key>CFBundleShortVersionString</key>\\s*<string>([^<]+)</string>").find(it) }
            ?.groupValues?.get(1)
            ?.split(".")?.take(2)?.joinToString(".")

        if (localPhpStormVersion != null) {
            println("Using PhpStorm $localPhpStormVersion for testing")
            phpstorm(localPhpStormVersion)
        } else {
            intellijIdeaUltimate("2025.1")
        }

        bundledPlugins("JavaScript", "HtmlTools")
        testFramework(TestFrameworkType.Platform)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        // Defaults to the target platform's build, which follows the local PhpStorm version.
        // No untilBuild: compatibility with newer platforms is checked by the plugin verifier.
        ideaVersion {
            sinceBuild = "251"
        }
    }

    pluginVerification {
        ides {
            // Only verify against the minimum supported version to speed up CI
            create("IU", "2025.1")
        }
    }
}
