//Nessya Nakache AG743463
//Elana Weiss 341390961

/**
 * The Parser class is responsible for parsing a .vm file and determining the type of each command and its arguments.
 * It takes a vmFile as input and reads the commands from that file.
 */
class Parser {

    static final String COMMENT = "//"
    static final String C_ARITHMETIC = "C_ARITHMETIC"
    static final String C_PUSH = "C_PUSH"
    static final String C_POP = "C_POP"
    static final String C_LABEL = "C_LABEL"
    static final String C_GOTO = "C_GOTO"
    static final String C_IF = "C_IF"
    static final String C_FUNCTION = "C_FUNCTION"
    static final String C_RETURN = "C_RETURN"
    static final String C_CALL = "C_CALL"
    static final String ERROR = "ERROR"

    private final File vmFile
    private final DataInputStream vmInputStream
    private String currentCommand
    private String nextCommand

    // Constructor for the Parser class
    // @param vmFile the .vm file to parse
    Parser(File vmFile) {
        this.vmFile = vmFile
        this.vmInputStream = vmFile.newDataInputStream()
        this.currentCommand = null
        this.nextCommand = null
        loadNextCommand()
    }

    // Advance the parser to the next command in the file
    void advance() {
        // set the current command to the next command
        this.currentCommand = this.nextCommand
        // load the next command from the file
        loadNextCommand()
    }

    // Load the next line from the file and set it as the next command
    // If the end of the file is reached, set the next command to null
    private void loadNextLine() {
        // read the next line from the file
        def line = this.vmInputStream.readLine()
        // if the line is null (end of the file), clear the next command and return
        if (line == null) {
            this.nextCommand = null
            return
        }
        // otherwise, remove any comments and leading/trailing whitespace and set the next command
        this.nextCommand = line.trim().split(COMMENT)[0].trim()
    }

    // Load the next command from the file - keep loading lines until a non-empty line is found
    void loadNextCommand() {
        // load the next line from the file
        loadNextLine()
        // keep loading lines until a non-empty line is found
        while (this.nextCommand == "") {
            loadNextLine()
        }
    }

    // Return whether there are more commands in the file
    boolean hasMoreCommands() {
        return this.nextCommand != null
    }

    // Return the type of the current command
    // @return the command type
    String commandType() {
        // if the command is an arithmetic command, return C_ARITHMETIC
        if (this.currentCommand.matches("add|sub|neg|eq|gt|lt|and|or|not")) {
            return C_ARITHMETIC
        }
        // if the command is a push command, return C_PUSH
        else if (this.currentCommand.startsWith("push")) {
            return C_PUSH
        }
        // if the command is a pop command, return C_POP
        else if (this.currentCommand.startsWith("pop")) {
            return C_POP
        }
        // if the command is a label command, return C_LABEL
        else if (this.currentCommand.startsWith("label")) {
            return C_LABEL
        }
        // if the command is a goto command, return C_GOTO
        else if (this.currentCommand.startsWith("goto")) {
            return C_GOTO
        }
        // if the command is an if-goto command, return C_IF
        else if (this.currentCommand.startsWith("if-goto")) {
            return C_IF
        }
        // if the command is a function command, return C_FUNCTION
        else if (this.currentCommand.startsWith("function")) {
            return C_FUNCTION
        }
        // if the command is a return command, return C_RETURN
        else if (this.currentCommand.startsWith("return")) {
            return C_RETURN
        }
        // if the command is a call command, return C_CALL
        else if (this.currentCommand.startsWith("call")) {
            return C_CALL
        }
        // otherwise, return an error
        return ERROR
    }

    // Return the first argument of the current command
    // @return the first argument
    String arg1() {
        // if the command is an arithmetic command, return the command itself
        if (this.commandType() == C_ARITHMETIC) {
            return this.currentCommand
        }
        // otherwise, return the first argument of the command
        else if (this.currentCommand.split(" ").size() > 1) {
            return this.currentCommand.split(" ")[1]
        }
        // if the command has no arguments and is not an arithmetic command, throw an exception
        throw new IllegalArgumentException("Command has no arguments: $this.currentCommand")
    }

    // Return the second argument of the current command
    // @return the second argument
    int arg2() {
        // return the second argument of the command
        return this.currentCommand.split(" ")[2].toInteger()
    }

}
