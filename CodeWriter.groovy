//Nessya Nakache AG743463
//Elana Weiss 341390961

/**
 * The CodeWriter class is responsible for writing the assembly code that is the translation of the VM commands.
 * It takes an asmFile as input and writes the translated code to that file.
 */
class CodeWriter {

    private String vmFileName
    private int jumpCount
    private int callCount
    private final File asmFile
    private final boolean includeComments

    // Open an output file and be ready to write content
    // @param asmFile is the output file
    // @param comment indicates whether to include comments in the output
    CodeWriter(File asmFile, boolean includeComments = true) {
        this.asmFile = asmFile
        this.includeComments = includeComments
        this.jumpCount = 0
        this.callCount = 0
        // empty the file if it already exists
        if (asmFile.exists()) {
            asmFile.delete()
        }
    }

    // Inform the CodeWriter that the translation of a new VM file is started
    // @param vmFileName is the name of the new VM file
    void setFileName(String vmFileName) {
        this.vmFileName = vmFileName
        // remove ".vm" extension from the end of the file name
        this.vmFileName = vmFileName.replace(".vm", "")
    }

    // Write a comment to the output file if includeComments is true
    // @param comment is the comment to write
    void writeComment(String comment) {
        if (includeComments) {
            asmFile.append("${Parser.COMMENT} ${comment}\n")
        }
    }

    // Write the assembly code that is the translation of the given arithmetic command
    // @param command is the arithmetic command
    void writeArithmetic(String command) {
        switch (command) {
            case 'add':
                asmFile.append(arithmeticTemplate() + 'M=M+D\n')
                break
            case 'sub':
                asmFile.append(arithmeticTemplate() + 'M=M-D\n')
                break
            case 'and':
                asmFile.append(arithmeticTemplate() + 'M=M&D\n')
                break
            case 'or':
                asmFile.append(arithmeticTemplate() + 'M=M|D\n')
                break
            case 'gt':
                asmFile.append(comparatorTemplate('JLE')) // not <=
                jumpCount++
                break
            case 'lt':
                asmFile.append(comparatorTemplate('JGE')) // not >=
                jumpCount++
                break
            case 'eq':
                asmFile.append(comparatorTemplate('JNE')) // not <>
                jumpCount++
                break
            case 'not':
                asmFile.append(unaryTemplate() + 'M=!M\n')
                break
            case 'neg':
                asmFile.append(unaryTemplate() + 'M=-M\n')
                break
            default:
                throw new IllegalArgumentException("Invalid arithmetic command: ${command}")
        }
    }

    // Write the assembly code that is the translation of the given memory access command push
    // where the command is either PUSH or POP
    // @param segment is the memory segment (constant, local, argument, this, that, temp, pointer, static)
    // @param index is the index of the memory segment
    void writePush(String segment, int index) {
        switch (segment) {
            case 'constant':
                // push constant index
                asmFile.append(pushConstant(index))
                break
            case 'local':
                asmFile.append(pushSegment('LCL', index))
                break
            case 'argument':
                asmFile.append(pushSegment('ARG', index))
                break
            case 'this':
                asmFile.append(pushSegment('THIS', index))
                break
            case 'that':
                asmFile.append(pushSegment('THAT', index))
                break
            case 'static':
                asmFile.append(pushStatic(index))
                break
            case 'pointer':
                asmFile.append(pushPointerOrTemp(index + 3))
                break
            case 'temp':
                asmFile.append(pushPointerOrTemp(index + 5))
                break
            default:
                throw new IllegalArgumentException("Invalid segment for PUSH command: ${segment}")
        }
    }

    // Write the assembly code that is the translation of the given memory access command pop
    // where the command is either PUSH or POP
    // @param segment is the memory segment (constant, local, argument, this, that, temp, pointer, static)
    // @param index is the index of the memory segment
    void writePop(String segment, int index) {
        switch (segment) {
            case 'local':
                asmFile.append(popSegment('LCL', index))
                break
            case 'argument':
                asmFile.append(popSegment('ARG', index))
                break
            case 'this':
                asmFile.append(popSegment('THIS', index))
                break
            case 'that':
                asmFile.append(popSegment('THAT', index))
                break
            case 'static':
                asmFile.append(popStatic(index))
                break
            case 'pointer':
                asmFile.append(popPointerOrTemp(index + 3))
                break
            case 'temp':
                asmFile.append(popPointerOrTemp(index + 5))
                break
            default:
                throw new IllegalArgumentException("Invalid segment for POP command: ${segment}")
        }
    }

    // Write a label command
    // @param label is the label to write
    void writeLabel(String label) {
        asmFile.append("(${label})\n")
    }

    // Write a goto command
    // @param label is the label to go to
    void writeGoTo(String label) {
        asmFile.append("""@${label}
                         |0;JMP
                         |""".stripMargin())
    }

    // Write an if-goto command
    // @param label is the label to go to if the top of the stack is not zero
    void writeIfGoTo(String label) {
        asmFile.append(popD() +
                       """@${label}
                         |D;JNE
                         |""".stripMargin())
    }

    // Write a function command
    // @param funcName is the name of the function
    // @param numVars is the number of local variables
    void writeFunction(String funcName, int numVars) {
        writeLabel(funcName)
        for (int i = 0; i < numVars; i++) {
            asmFile.append(pushConstant(0))
        }
    }

    // Write a call command
    // @param funcName is the name of the function
    // @param numArgs is the number of arguments
    void writeCall(String funcName, int numArgs) {
        callCount++
        String funcLabel = "${funcName}\$ret.${callCount}"
        // put return address on stack
        asmFile.append("""@${funcLabel}
                         |D=A
                         |""".stripMargin() + pushD())
        // push LCL, ARG, THIS, THAT onto stack
        ['LCL', 'ARG', 'THIS', 'THAT'].each { segment ->
            asmFile.append("""@${segment}
                             |D=M
                             |""".stripMargin() + pushD())
        }
        // reposition ARG to (SP - 5 - numArgs)
        // reposition LCL to SP
        asmFile.append("""@SP
                         |D=M
                         |@5
                         |D=D-A
                         |@${numArgs}
                         |D=D-A
                         |@ARG
                         |M=D
                         |
                         |@SP
                         |D=M
                         |@LCL
                         |M=D
                         |""".stripMargin())
        writeGoTo(funcName)
        writeLabel(funcLabel)
        asmFile.append("\n")
    }

    // Write a return command
    void writeReturn() {
        //sets R13 to have LCL, the location of the end of the backed up segments
        //sets R14 to have LCL-5, the location of the return address
        //replace the first ARG position with the return value, which was poped from the stack
        //set Stack Pointer to be ARG+1, right above where the return address is stored
        //takes the backup addresses and puts them back into THAT, THIS, ARG, LCL
        //jump to the return address
        asmFile.append("""@LCL
                         |D=M
                         |@R13
                         |M=D
                         |@5
                         |D=D-A
                         |A=D
                         |D=M
                         |@R14
                         |M=D
                         |""".stripMargin()
                        + popD()
                        + """@ARG
                             |A=M
                             |M=D
                             |
                             |D=A+1
                             |@SP
                             |M=D
                             |
                             |@R13
                             |A=M-1
                             |D=M
                             |@THAT
                             |M=D
                             |
                             |@2
                             |D=A
                             |@R13
                             |D=M-D
                             |A=D
                             |D=M
                             |@THIS
                             |M=D
                             |
                             |@3
                             |D=A
                             |@R13
                             |D=M-D
                             |A=D
                             |D=M
                             |@ARG
                             |M=D
                             |
                             |@4
                             |D=A
                             |@R13
                             |D=M-D
                             |A=D
                             |D=M
                             |@LCL
                             |M=D
                             |
                             
                             |@R14
                             |A=M
                             |0;JMP
                             |
                             |""".stripMargin())
    }

    // Write the initial bootstrapping code
    void writeInit() {
        String setSP = """@256
                        |D=A
                        |@SP
                        |M=D
                        |""".stripMargin()
        asmFile.append(setSP)
    }

    // Template for add, sub, and, or
    // @return the template for the arithmetic operation
    private String arithmeticTemplate() {
        return """@SP
                 |AM=M-1
                 |D=M
                 |A=A-1
                 |""".stripMargin()
    }

    // Template for gt, lt, eq
    // @param type is the type of comparison done internally (JLE, JGE, JNE)
    // @return the template for the comparison operation
    private String comparatorTemplate(String type) {
        return """@SP
                 |AM=M-1
                 |D=M
                 |A=A-1
                 |D=M-D
                 |@FALSE${jumpCount}
                 |D;${type}
                 |@SP
                 |A=M-1
                 |M=-1
                 |@CONTINUE${jumpCount}
                 |0;JMP
                 |(FALSE${jumpCount})
                 |@SP
                 |A=M-1
                 |M=0
                 |(CONTINUE${jumpCount})
                 |""".stripMargin()
    }

    // Template for unary operations like not, neg
    // @return the template for the unary operation
    private String unaryTemplate() {
        return """@SP
                 |A=M-1
                 |""".stripMargin()
    }

    // Template for push constant
    // @param constant is the constant of the memory segment
    // @return the template for the push operation
    private String pushConstant(int constant) {
        return """@${constant}
                 |D=A
                 |""".stripMargin() + pushD()
    }

    // Template for push local, this, that, argument
    // @param segment is the memory segment
    // @param index is the index of the memory segment
    // @param isDirect is a flag to indicate if the index is direct or indirect
    // @return the template for the push operation
    private String pushSegment(String segment, int index) {
        return """@${index}
                 |D=A
                 |@${segment}
                 |A=M+D
                 |D=M
                 |""".stripMargin() + pushD()
    }

    // Template for push static
    // @param index is the index of the memory segment
    // @return the template for the push operation
    private String pushStatic(int index) {
        return """@${vmFileName}.${index}
                 |D=M
                 |""".stripMargin() + pushD()
    }

    // Template for push pointer, temp
    // @param index is the index of the memory segment
    // @return the template for the push operation
    private String pushPointerOrTemp(int index) {
        return """@${index}
                 |D=M
                 |""".stripMargin() + pushD()
    }

    // Template for pop local, this, that, argument
    // @param segment is the memory segment
    // @param index is the index of the memory segment
    // @param isDirect is a flag to indicate if the index is direct or indirect
    // @return the template for the pop operation
    private String popSegment(String segment, int index) {
        return """@${index}
                 |D=A
                 |@${segment}
                 |D=M+D
                 |@R13
                 |M=D
                 |""".stripMargin() +
               popD() +
               """@R13
                  |A=M
                  |M=D
                  |""".stripMargin()
    }

    // Template for pop static
    // @param index is the index of the memory segment
    // @return the template for the pop operation
    private String popStatic(int index) {
        return popD() +
               """@${vmFileName}.${index}
                  |M=D
                  |""".stripMargin()
    }

    // Template for pop pointer, temp
    // @param index is the index of the memory segment
    // @return the template for the pop operation
    private String popPointerOrTemp(int index) {
        return popD() +
               """@${index}
                  |M=D
                  |""".stripMargin()
    }

    // Push D template
    // @return the template for pushing D to the stack
    private String pushD() {
        return """@SP
                 |M=M+1
                 |A=M-1
                 |M=D
                 |""".stripMargin()
    }

    // Pop D template
    // @return the template for from the stack to D
    private String popD() {
        return """@SP
                 |AM=M-1
                 |D=M
                 |""".stripMargin()
    }

}
