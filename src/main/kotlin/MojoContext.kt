package io.toolisticon.maven

import io.toolisticon.maven.model.ArtifactId
import io.toolisticon.maven.model.GroupId
import io.toolisticon.maven.mojo.MavenExt.hasRuntimeDependency
import mu.KLogger
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.BuildPluginManager
import org.apache.maven.project.MavenProject
import org.twdata.maven.mojoexecutor.MojoExecutor
import kotlin.streams.asSequence

/**
 * An executor able to execute [MojoCommand]s. Has to provide the [MojoExecutor.ExecutionEnvironment] to work.
 */
interface MojoCommandExecutor {
  fun execute(command: MojoCommand)
}

/**
 * Context holding the required values to create a [MojoExecutor.ExecutionEnvironment]. Implements [MojoCommandExecutor]
 * to execute commands.
 */
class MojoContext(
  val logger: KLogger = KotlinMojoHelper.logger(),
  val mavenProject: MavenProject? = null,
  val mavenSession: MavenSession,
  val pluginManager: BuildPluginManager
) : MojoCommandExecutor {

  val executionEnvironment by lazy {
    if (mavenProject != null) {
      MojoExecutor.executionEnvironment(mavenProject, mavenSession, pluginManager)
    } else {
      MojoExecutor.executionEnvironment(mavenSession, pluginManager)
    }
  }

  override fun execute(command: MojoCommand): Unit = with(command) {
    MojoExecutor.executeMojo(plugin, goal, configuration, executionEnvironment)
  }

  val classpathElements: Set<String> by lazy {
    mavenProject?.compileClasspathElements?.stream()?.asSequence()
      ?.filterNot { mavenProject.build.outputDirectory == it } // TODO copied from camunda-swagger, why is that?
      ?.sortedBy { it.substringAfterLast("/") }?.toSet()
      ?: emptySet()
  }

  fun hasRuntimeDependency(groupId: GroupId, artifactId: ArtifactId) = mavenProject?.hasRuntimeDependency(groupId, artifactId) ?: false

  override fun toString(): String {
    val projectName = mavenProject?.let { "${it.groupId}.${it.artifacts}" } ?: "N/A"

    return "MojoContext(logger=${logger.name}, " +
      "project=$projectName, " +
      "session=$mavenSession, " +
      "buildPluginManager=$pluginManager, " +
      "classpathElements=$classpathElements"
  }

}
