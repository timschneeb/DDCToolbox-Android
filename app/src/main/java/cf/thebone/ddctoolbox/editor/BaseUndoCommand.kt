package cf.thebone.ddctoolbox.editor

import java.io.Serializable
import cf.thebone.ddctoolbox.model.FilterItem

class BaseUndoCommand(var previousState: ArrayList<FilterItem>? = null,
                      var currentState: ArrayList<FilterItem>? = null,
                      var description: String) : Serializable