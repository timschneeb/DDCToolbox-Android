package cf.thebone.ddctoolbox.model.instance

import java.io.Serializable

data class ProjectManagerDataInstance(val currentProjectName: String,
                                      val currentDirectoryName: String,
                                      val isFileLoaded: Boolean,
                                      val isModified: Boolean) : Serializable