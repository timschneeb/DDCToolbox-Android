package cf.thebone.ddctoolbox.editor

import cf.thebone.ddctoolbox.model.FilterItem
import cf.thebone.ddctoolbox.model.instance.UndoStackDataInstance

class UndoStack {
    private var stack: ArrayList<BaseUndoCommand> = arrayListOf()
    private var seek: Int = 0

    fun restoreInstance(instance: UndoStackDataInstance){
        stack = instance.stack
        seek = instance.seek
    }

    fun saveInstance(): UndoStackDataInstance{
        return UndoStackDataInstance(stack, seek)
    }

    fun canUndo(): Boolean{
        return stack.isNotEmpty() && seek >= 0
    }
    fun canRedo(): Boolean{
        return seek < (stack.size - 1)
    }

    fun pullPrevCommand(): ArrayList<FilterItem>?{
        if(!canUndo() || seek >= stack.size) return null

        seek--
        if(seek < 0){
            val cmd = stack[0]
            return cmd.previousState
        } else {
            val cmd = stack[seek]
            return cmd.currentState
        }
    }
    fun pullNextCommand(): ArrayList<FilterItem>?{
        if(!canRedo()) return null
        seek++
        val cmd = stack[seek]
        return cmd.currentState
    }

    fun pushCommand(prevItem: ArrayList<FilterItem>,nextItem: ArrayList<FilterItem>,description: String){
        if(canRedo() && stack.size > 0)
        //Remove all (already) undid entries behind our seek head
            for(i in stack.size - 1 downTo seek + 1)
                stack.removeAt(i)

        stack.add(BaseUndoCommand(prevItem,nextItem,description))
        //Reset seek head
        seek = stack.size - 1
    }

    fun clearStack(){
        stack.clear()
        seek = 0
    }
}