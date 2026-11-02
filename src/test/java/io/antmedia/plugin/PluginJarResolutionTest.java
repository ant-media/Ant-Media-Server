package io.antmedia.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class PluginJarResolutionTest extends UnitTestBase<PluginDeployer> {

    @TempDir
    Path extractDir;

    PluginJarResolutionTest() {
        classUnderTest = new PluginDeployer();
    }

    @Test
    void resolvesSingleJarWithoutMandatingFilename() throws Exception {
        File pluginJar = SpringTestPluginJarBuilder.buildPluginJar("Custom Artifact Plugin");
        Path customArtifact = extractDir.resolve("my-custom-artifact.jar");
        Files.copy(pluginJar.toPath(), customArtifact);

        PluginDeployer.PluginJarResolution resolution = classUnderTest.resolvePluginJar(extractDir.toFile());

        assertThat(resolution.failureReason()).isNull();
        assertThat(resolution.pluginJar()).isEqualTo(customArtifact.toFile());
    }

    @Test
    void resolvesPluginJarByManifestWhenDependenciesArePresent() throws Exception {
        File pluginJar = SpringTestPluginJarBuilder.buildPluginJar("Manifest Plugin");
        File dependencyJar = SpringTestPluginJarBuilder.buildComponentJar("dependency");
        Path customArtifact = extractDir.resolve("manifest-plugin-1.0.0.jar");
        Files.copy(pluginJar.toPath(), customArtifact);
        Files.copy(dependencyJar.toPath(), extractDir.resolve("dependency.jar"));

        PluginDeployer.PluginJarResolution resolution = classUnderTest.resolvePluginJar(extractDir.toFile());

        assertThat(resolution.failureReason()).isNull();
        assertThat(resolution.pluginJar()).isEqualTo(customArtifact.toFile());
    }

    @Test
    void returnsReasonWhenPluginJarIsAmbiguous() throws Exception {
        File first = SpringTestPluginJarBuilder.buildPluginJar("First Plugin");
        File second = SpringTestPluginJarBuilder.buildPluginJar("Second Plugin");
        Files.copy(first.toPath(), extractDir.resolve("first.jar"));
        Files.copy(second.toPath(), extractDir.resolve("second.jar"));

        PluginDeployer.PluginJarResolution resolution = classUnderTest.resolvePluginJar(extractDir.toFile());

        assertThat(resolution.pluginJar()).isNull();
        assertThat(resolution.failureReason())
                .isEqualTo("Could not identify a unique plugin JAR in ZIP; found 2 JAR files and 2 plugin manifests");
    }
}
