package io.codeka.gaia.registries.service

import io.codeka.gaia.hcl.HclParser
import io.codeka.gaia.modules.bo.ModuleMetadata
import io.codeka.gaia.modules.bo.TerraformModule
import io.codeka.gaia.modules.repository.TerraformCLIRepository
import io.codeka.gaia.modules.repository.TerraformModuleRepository
import io.codeka.gaia.registries.RegistryApi
import io.codeka.gaia.registries.RegistryDetails
import io.codeka.gaia.registries.RegistryType
import io.codeka.gaia.registries.SourceRepository
import io.codeka.gaia.teams.User
import org.springframework.stereotype.Service
import java.util.*

interface RegistryService {
    fun importRepository(projectId: String, registryType: RegistryType, user: User): TerraformModule
}

@Service
class RegistryServiceImpl(
        private val cliRepository: TerraformCLIRepository,
        private val hclParser: HclParser,
        private val moduleRepository: TerraformModuleRepository,
        private val registryApis: Map<RegistryType, RegistryApi<out SourceRepository>>
) : RegistryService {

    override fun importRepository(projectId: String, registryType: RegistryType, user: User) : TerraformModule {
        val module = TerraformModule()
        module.id = UUID.randomUUID().toString()

        val codeRepository = registryApis[registryType]?.getRepository(user, projectId)!!

        module.gitRepositoryUrl = codeRepository.htmlUrl
        module.gitBranch = "master"
        module.name = codeRepository.fullName
        module.cliVersion = cliRepository.listCLIVersion().first()

        module.registryDetails = RegistryDetails(registryType, codeRepository.id)
        module.moduleMetadata = ModuleMetadata(createdBy = user)

        // get variables
        val variablesFile = registryApis[registryType]?.getFileContent(user, projectId, "variables.tf")!!
        module.variables = hclParser.parseVariables(variablesFile)

        // saving module !
        return moduleRepository.save(module)
    }

}