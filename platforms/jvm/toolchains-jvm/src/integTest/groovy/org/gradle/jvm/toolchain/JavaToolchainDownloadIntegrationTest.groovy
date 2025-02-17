/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.jvm.toolchain

import net.rubygrapefruit.platform.internal.DefaultSystemInfo
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.executer.DocumentationUtils
import org.gradle.internal.os.OperatingSystem
import org.gradle.platform.internal.DefaultBuildPlatform
import org.gradle.test.fixtures.file.TestFile

import static org.gradle.integtests.fixtures.SuggestionsMessages.GET_HELP
import static org.gradle.integtests.fixtures.SuggestionsMessages.INFO_DEBUG
import static org.gradle.integtests.fixtures.SuggestionsMessages.SCAN
import static org.gradle.integtests.fixtures.SuggestionsMessages.STACKTRACE_MESSAGE

class JavaToolchainDownloadIntegrationTest extends AbstractIntegrationSpec {

    def "fails for missing combination"() {
        setFoojayDiscoToolchainProvider()

        buildFile << """
            apply plugin: "java"

            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(14)
                    implementation = JvmImplementation.J9
                    vendor = JvmVendorSpec.ADOPTIUM
                }
            }
        """

        file("src/main/java/Foo.java") << "public class Foo {}"

        when:
        failure = executer
            .withTasks("compileJava")
            .requireOwnGradleUserHomeDir()
            .withToolchainDetectionEnabled()
            .withToolchainDownloadEnabled()
            .runWithFailure()

        then:
        failure.assertHasDescription("Could not determine the dependencies of task ':compileJava'.")
               .assertHasCause("Failed to calculate the value of task ':compileJava' property 'javaCompiler'")
               .assertHasCause("No matching toolchains found for requested specification: {languageVersion=14, vendor=ADOPTIUM, implementation=J9} ${getFailureMessageBuildPlatform()}.")
               .assertHasCause("No locally installed toolchains match and the configured toolchain download repositories aren't able to provide a match either.")
               .assertHasResolutions(
                   DocumentationUtils.normalizeDocumentationLink("Learn more about toolchain auto-detection at https://docs.gradle.org/current/userguide/toolchains.html#sec:auto_detection."),
                   DocumentationUtils.normalizeDocumentationLink("Learn more about toolchain repositories at https://docs.gradle.org/current/userguide/toolchains.html#sub:download_repositories."),
                   STACKTRACE_MESSAGE,
                   INFO_DEBUG,
                   SCAN,
                   GET_HELP)
    }

    def 'toolchain selection that requires downloading fails when it is disabled'() {
        setFoojayDiscoToolchainProvider()

        buildFile << """
            apply plugin: "java"

            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(14)
                }
            }
        """

        propertiesFile << """
            org.gradle.java.installations.auto-download=false
        """

        file("src/main/java/Foo.java") << "public class Foo {}"

        when:
        failure = executer
            .withTasks("compileJava")
            .requireOwnGradleUserHomeDir()
            .runWithFailure()

        then:
        failure.assertHasDescription("Could not determine the dependencies of task ':compileJava'.")
               .assertHasCause("Failed to calculate the value of task ':compileJava' property 'javaCompiler'")
               .assertHasCause("No matching toolchains found for requested specification: {languageVersion=14, vendor=any, implementation=vendor-specific} ${getFailureMessageBuildPlatform()}.")
               .assertHasCause("No locally installed toolchains match and toolchain auto-provisioning is not enabled.")
               .assertHasResolutions(
                   DocumentationUtils.normalizeDocumentationLink("Learn more about toolchain auto-detection at https://docs.gradle.org/current/userguide/toolchains.html#sec:auto_detection."),
                   STACKTRACE_MESSAGE,
                   INFO_DEBUG,
                   SCAN,
                   GET_HELP)
    }

    def 'toolchain download on http fails'() {
        setUnsecuredToolchainProvider()

        buildFile << """
            apply plugin: "java"

            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(99)
                }
            }
        """

        file("src/main/java/Foo.java") << "public class Foo {}"

        when:
        failure = executer
            .withTasks("compileJava")
            .requireOwnGradleUserHomeDir()
            .withToolchainDetectionEnabled()
            .withToolchainDownloadEnabled()
            .runWithFailure()

        then:
        failure.assertHasDescription("Could not determine the dependencies of task ':compileJava'.")
               .assertHasCause("Failed to calculate the value of task ':compileJava' property 'javaCompiler'")
               .assertHasCause("Unable to download toolchain matching the requirements ({languageVersion=99, vendor=any, implementation=vendor-specific}) from 'http://exoticJavaToolchain.com/java-99'.")
               .assertHasCause("Attempting to download a file from an insecure URI http://exoticJavaToolchain.com/java-99. This is not supported, use a secure URI instead.")
    }

    private TestFile setFoojayDiscoToolchainProvider() {
        settingsFile << """
            plugins {
                id 'org.gradle.toolchains.foojay-resolver-convention' version '0.4.0'
            }
        """
    }

    private TestFile setUnsecuredToolchainProvider() {
        settingsFile << """
            public abstract class CustomToolchainResolverPlugin implements Plugin<Settings> {
                @Inject
                protected abstract JavaToolchainResolverRegistry getToolchainResolverRegistry();

                void apply(Settings settings) {
                    settings.getPlugins().apply("jvm-toolchain-management");

                    JavaToolchainResolverRegistry registry = getToolchainResolverRegistry();
                    registry.register(CustomToolchainResolver.class);
                }
            }


            import java.util.Optional;
            import org.gradle.platform.BuildPlatform;

            public abstract class CustomToolchainResolver implements JavaToolchainResolver {
                @Override
                public Optional<JavaToolchainDownload> resolve(JavaToolchainRequest request) {
                    URI uri = URI.create("http://exoticJavaToolchain.com/java-" + request.getJavaToolchainSpec().getLanguageVersion().get());
                    return Optional.of(JavaToolchainDownload.fromUri(uri));
                }
            }


            apply plugin: CustomToolchainResolverPlugin

            toolchainManagement {
                jvm {
                    javaRepositories {
                        repository('custom') {
                            resolverClass = CustomToolchainResolver
                        }
                    }
                }
            }
        """
    }

    private def getFailureMessageBuildPlatform() {
        def buildPlatform = new DefaultBuildPlatform(new DefaultSystemInfo(), OperatingSystem.current())
        return "for ${buildPlatform.operatingSystem} on ${buildPlatform.architecture.toString().toLowerCase()}"
    }

}
