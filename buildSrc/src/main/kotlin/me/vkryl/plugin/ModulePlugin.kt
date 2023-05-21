package me.vkryl.plugin

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

open class ModulePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val plugins = arrayOf(
      "kotlin-android"
    )
    for (plugin in plugins) {
      project.plugins.apply(plugin)
    }

    val androidExt = project.extensions.getByName("android")
    if (androidExt is BaseExtension) {
      androidExt.apply {
        compileSdkVersion(33)
      }
    }
  }
}