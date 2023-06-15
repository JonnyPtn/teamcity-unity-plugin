/*
 * Copyright 2000-2023 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.unity

import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.unity.UnityConstants.PARAM_DETECTION_MODE
import jetbrains.buildServer.unity.UnityConstants.PARAM_UNITY_ROOT
import jetbrains.buildServer.unity.UnityConstants.PARAM_UNITY_VERSION
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.testng.annotations.DataProvider
import kotlin.test.*

class UnityBuildFeatureTests {
    private val pluginDescriptorMock = mockk<PluginDescriptor> {
        every { getPluginResourcesPath(any()) } returns "foo"
    }

    private val buildFeature = UnityBuildFeature(pluginDescriptorMock)

    @Test
    fun getRequirements_happyPath_generateUnityRequirement() {
        // arrange
        val sut = buildFeature

        val unityVersion = "2021.3.16"
        val parameters = mutableMapOf(
            PARAM_UNITY_VERSION to unityVersion
        )

        // act
        val requirements = sut.getRequirements(parameters)

        // assert
        assertNotNull(requirements)
        assertEquals(1, requirements.size)
        val propertyString = requirements.first().propertyName.lowercase()
        assertContains(propertyString, "unity")
        assertContains(propertyString, "2021\\.3\\.16")
    }

    @Test
    fun getRequirements_versionNotSpecified_generateUnityRequirement() {
        // arrange
        val sut = buildFeature

        // act
        val requirements = sut.getRequirements(mutableMapOf())

        // assert
        assertNotNull(requirements)
        assertEquals(1, requirements.size)
        val propertyString = requirements.first().propertyName.lowercase()
        assertContains(propertyString, "unity")
    }

    @DataProvider
    fun activateLicenceTestData(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf("true", true),
            arrayOf("false", false)
        )
    }

    @Test(dataProvider = "activateLicenceTestData")
    fun describeParameters_activateLicenceParamExists_describeWhenTrue(
        activateLicenceParam: String,
        shouldDescribe: Boolean
    ) {
        // arrange
        val sut = buildFeature
        val parameters = mutableMapOf(UnityConstants.PARAM_ACTIVATE_LICENSE to activateLicenceParam)

        // act
        val description = sut.describeParameters(parameters)

        // assert
        assertEquals(description.isNotBlank(), shouldDescribe)
    }

    @Test
    fun describeParameters_activateLicenceParamAbsent_doNotDescribe() {
        // arrange
        val sut = buildFeature
        val parameters: MutableMap<String, String> = mutableMapOf()

        // act
        val description = sut.describeParameters(parameters)

        // assert
        assertTrue { description.isBlank() }
    }

    @Test
    fun describeParameters_cacheServerParamExists_alwaysDescribe() {
        // arrange
        val sut = buildFeature
        val cacheServerParam = "localhost:8080/cs"
        val parameters = mutableMapOf(UnityConstants.PARAM_CACHE_SERVER to cacheServerParam)

        // act
        val description = sut.describeParameters(parameters)

        // assert
        assertTrue { description.isNotBlank() }
        assertTrue { description.contains(cacheServerParam) }
    }

    @Test
    fun describeParameters_cacheServerParamAbsent_doNotDescribe() {
        // arrange
        val sut = buildFeature
        val parameters: MutableMap<String, String> = mutableMapOf()

        // act
        val description = sut.describeParameters(parameters)

        // assert
        assertTrue { description.isBlank() }
    }

    @DataProvider
    fun correctUnityLocationParamsSet(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf(
                mapOf(
                    PARAM_DETECTION_MODE to "auto",
                    PARAM_UNITY_VERSION to "2020.1.1"
                ),
                setOf("2020.1.1"),
                setOf<String>()
            ),
            arrayOf(
                mapOf(
                    PARAM_DETECTION_MODE to "auto",
                    PARAM_UNITY_VERSION to "2020.1.1",
                    PARAM_UNITY_ROOT to "path/to/unity"
                ),
                setOf("2020.1.1"),
                setOf("path/to/unity")
            ),
            arrayOf(
                mapOf(
                    PARAM_DETECTION_MODE to "manual",
                    PARAM_UNITY_ROOT to "path/to/unity"
                ),
                setOf("path/to/unity"),
                setOf<String>()
            ),
            arrayOf(
                mapOf(
                    PARAM_DETECTION_MODE to "manual",
                    PARAM_UNITY_VERSION to "2020.1.1",
                    PARAM_UNITY_ROOT to "path/to/unity"
                ),
                setOf("path/to/unity"),
                setOf("2020.1.1")
            ),
        )
    }

    @Test(dataProvider = "correctUnityLocationParamsSet")
    fun describeParameters_correctUnityLocationParamsSet_describeDistinctively(
        parameters: MutableMap<String, String>,
        shouldContain: Set<String>,
        shouldNotContain: Set<String>
    ) {
        // arrange
        val sut = buildFeature

        // act
        val description = sut.describeParameters(parameters)

        // assert
        assertTrue { description.isNotBlank() }
        shouldContain.forEach { assertTrue { description.contains(it) } }
        shouldNotContain.forEach { assertFalse { description.contains(it) } }
    }

    @DataProvider
    fun incorrectUnityLocationParamsSet(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf(
                mapOf<String, String>()
            ),
            arrayOf(
                mapOf(
                    PARAM_UNITY_VERSION to "2020.1.1",
                    PARAM_UNITY_ROOT to "path/to/unity"
                )
            ),
            arrayOf(
                mapOf(
                    PARAM_DETECTION_MODE to "auto",
                    PARAM_UNITY_ROOT to "path/to/unity"
                )
            ),
            arrayOf(
                mapOf(
                    PARAM_DETECTION_MODE to "manual",
                    PARAM_UNITY_VERSION to "2020.1.1",
                )
            ),
        )
    }

    @Test(dataProvider = "incorrectUnityLocationParamsSet")
    fun describeParameters_incorrectUnityLocationParamsSet_doNotDescribe(parameters: MutableMap<String, String>) {
        // arrange
        val sut = buildFeature

        // act
        val description = sut.describeParameters(parameters)

        // assert
        assertTrue { description.isBlank() }
    }
}