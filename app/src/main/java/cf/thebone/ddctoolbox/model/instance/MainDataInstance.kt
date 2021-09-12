package cf.thebone.ddctoolbox.model.instance

import cf.thebone.ddctoolbox.model.FilterItem
import java.io.Serializable
import java.util.ArrayList

data class MainDataInstance (val projectManagerDataInstance: ProjectManagerDataInstance,
                             val undoStackDataInstance: UndoStackDataInstance,
                             val filterItems: ArrayList<FilterItem>) : Serializable