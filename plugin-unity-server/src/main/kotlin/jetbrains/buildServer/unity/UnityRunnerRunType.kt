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

import jetbrains.buildServer.requirements.Requirement
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.RunType
import jetbrains.buildServer.serverSide.RunTypeExtension
import jetbrains.buildServer.serverSide.RunTypeRegistry
import jetbrains.buildServer.unity.UnityConstants.DOCKER_WRAPPER_ID
import jetbrains.buildServer.util.positioning.PositionAware
import jetbrains.buildServer.web.openapi.PluginDescriptor

/**
 * Cargo runner definition.
 */
class UnityRunnerRunType(private val myPluginDescriptor: PluginDescriptor,
                         private val myRunTypeRegistry: RunTypeRegistry) : RunType() {

    private val myDisplayName: String by lazy {
        val runType = myRunTypeRegistry.findRunType("unityRunner")
        if (runType == null) {
            UnityConstants.RUNNER_DISPLAY_NAME
        } else {
            "${UnityConstants.RUNNER_DISPLAY_NAME} (JetBrains plugin)"
        }
    }

    init {
        myRunTypeRegistry.registerRunType(this)
    }

    override fun getType(): String {
        return UnityConstants.RUNNER_TYPE
    }

    override fun getDisplayName() = myDisplayName

    override fun getDescription(): String {
        return UnityConstants.RUNNER_DESCRIPTION
    }

    override fun getRunnerPropertiesProcessor(): PropertiesProcessor? {
        return UnityRunnerRunTypePropertiesProcessor()
    }

    override fun getEditRunnerParamsJspFilePath(): String? {
        return myPluginDescriptor.getPluginResourcesPath("editUnityParameters.jsp")
    }

    override fun getViewRunnerParamsJspFilePath(): String? {
        return myPluginDescriptor.getPluginResourcesPath("viewUnityParameters.jsp")
    }

    override fun getDefaultRunnerProperties(): MutableMap<String, String> {
        val parameters = mutableMapOf<String,String>()
        parameters[UnityConstants.PARAM_DETECTION_MODE] = UnityConstants.DETECTION_MODE_AUTO

        return parameters
    }

    override fun describeParameters(parameters: Map<String, String>): String {
        val builder = StringBuilder()
        parameters[UnityConstants.PARAM_PROJECT_PATH]?.let {
            if (it.isNotBlank()) {
                builder.addParameter("Project path: $it")
            }
        }
        parameters[UnityConstants.PARAM_EXECUTE_METHOD]?.let {
            if (it.isNotBlank()) {
                builder.addParameter("Execute method: $it")
            }
        }
        parameters[UnityConstants.PARAM_BUILD_TARGET]?.let {
            if (it.isNotBlank()) {
                builder.addParameter("Build target: $it")
            }
        }
        parameters[UnityConstants.PARAM_BUILD_PLAYER]?.let { value ->
            StandalonePlayer.tryParse(value)?.let {
                builder.addParameter("Build player: ${it.description}")
            }
        }
        parameters[UnityConstants.PARAM_RUN_EDITOR_TESTS]?.let {
            if (it.toBoolean()) {
                builder.addParameter("Run editor tests: ON")
            }
        }
        return builder.toString().trim()
    }

    override fun getRunnerSpecificRequirements(parameters: Map<String, String>): List<Requirement> {
        val detectionMode = parameters[UnityConstants.PARAM_DETECTION_MODE]
        val unityVersion = parameters[UnityConstants.PARAM_UNITY_VERSION]
        val isNotVirtual = parameters[UnityConstants.PLUGIN_DOCKER_IMAGE].isNullOrEmpty()
        return if (detectionMode != UnityConstants.DETECTION_MODE_MANUAL && unityVersion != null && isNotVirtual) {
            listOf(Requirements.Unity.create(unityVersion))
        } else {
            emptyList()
        }
    }

    override fun supports(runTypeExtension: RunTypeExtension): Boolean {
        if (runTypeExtension is PositionAware && runTypeExtension.orderId == DOCKER_WRAPPER_ID)
            return true
        return super.supports(runTypeExtension)
    }

    private fun escapeRegex(value: String) =
        if(value.contains('%')) value else value.replace(".", "\\.")

    private fun StringBuilder.addParameter(parameter: String) {
        if (this.isNotEmpty()) {
            append(" $parameter")
        } else {
            append(parameter)
        }
        append("\n")
    }
}
