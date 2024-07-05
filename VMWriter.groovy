//Nessya Nakache AG743463
//Elana Weiss 341390961

// Groovy program for VM writer

/**
 * The VMWriter class is responsible for writing VM commands to a .vm file.
 */
class VMWriter {

    private File vmFile

    VMWriter(File vmFile) {
        this.vmFile = vmFile
        // delete the file if it already exists
        if (vmFile.exists()) {
            vmFile.delete()
        }
    }

    /**
     * Write an command such as an arithmetic command to the .vm file.
     * @param command the command to write
     */
    void writeCommand(String command) {
        vmFile.append("$command\n")
    }

    /**
     * Write a push command to the .vm file.
     * @param segment the segment to push from
     * @param index the index to push from
     */
    void writePush(String segment, int index) {
        vmFile.append("push $segment $index\n")
    }

    /**
     * Write a pop command to the .vm file.
     * @param segment the segment to pop to
     * @param index the index to pop to
     */
    void writePop(String segment, int index) {
        vmFile.append("pop $segment $index\n")
    }

    /**
     * Write a label command to the .vm file.
     * @param label the label to write
     */
    void writeLabel(String label) {
        vmFile.append("label $label\n")
    }

    /**
     * Write a goto command to the .vm file.
     * @param label the label to go to
     */
    void writeGoto(String label) {
        vmFile.append("goto $label\n")
    }

    /**
     * Write an if-goto command to the .vm file.
     * @param label the label to go to
     */
    void writeIfGoto(String label) {
        vmFile.append("if-goto $label\n")
    }

    /**
     * Write a function command to the .vm file.
     * @param functionName the name of the function
     * @param numLocals the number of local variables
     */
    void writeFunction(String functionName, int numLocals) {
        vmFile.append("function $functionName $numLocals\n")
    }

    /**
     * Write a call command to the .vm file.
     * @param functionName the name of the function
     * @param numArgs the number of arguments
     */
    void writeCall(String functionName, int numArgs) {
        vmFile.append("call $functionName $numArgs\n")
    }

    /**
     * Write a return command to the .vm file.
     */
    void writeReturn() {
        vmFile.append("return\n")
    }
}