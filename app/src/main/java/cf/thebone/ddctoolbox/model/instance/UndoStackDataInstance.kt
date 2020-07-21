package cf.thebone.ddctoolbox.model.instance

import cf.thebone.ddctoolbox.editor.BaseUndoCommand
import java.io.Serializable

data class UndoStackDataInstance(val stack: ArrayList<BaseUndoCommand> = arrayListOf(),
                                 val seek: Int) : Serializable