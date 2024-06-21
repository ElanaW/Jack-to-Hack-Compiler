//Nessya Nakache AG743463
//Elana Weiss 341390961

import static Parser

/**
 * The CodeWriter class is responsible for writing the assembly code that is the translation of the VM commands.
 * It takes an asmFile as input and writes the translated code to that file.
 */
class CodeWriter {

    private String currentFile
    private int jumpFlag
    private final File asmFile
    private final boolean includeComments

    // Open an output file and be ready to write content
    // @param asmFile is the output file
    // @param comment indicates whether to include comments in the output
    CodeWriter(File asmFile, boolean includeComments = true) {
        this.asmFile = asmFile
        this.includeComments = includeComments
        this.jumpFlag = 0
        // empty the file if it already exists
        if (asmFile.exists()) {
            asmFile.delete()
        }
    }

    // Inform the CodeWriter that the translation of a new VM file is started
    // @param asmFileName is the name of the new VM file
    void setFileName(String asmFileName) {
        currentFile = asmFileName
    }

    // Write a comment to the output file
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
                jumpFlag++
                break
            case 'lt':
                asmFile.append(comparatorTemplate('JGE')) // not >=
                jumpFlag++
                break
            case 'eq':
                asmFile.append(comparatorTemplate('JNE')) // not <>
                jumpFlag++
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
    // @param fileName is the name of the current file
    void writePush(String segment, int index, String fileName) {
        switch (segment) {
            case 'constant':
                asmFile.append("@${index}\n" + "D=A\n" + pushD())
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
                asmFile.append(pushStatic(index, fileName))
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
    // @param fileName is the name of the current file
    void writePop(String segment, int index, String fileName) {
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
                asmFile.append(popStatic(index, fileName))
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
    // @param type is the type of comparison
    // @return the template for the comparison operation
    private String comparatorTemplate(String type) {
        return """@SP
                 |AM=M-1
                 |D=M
                 |A=A-1
                 |D=M-D
                 |@FALSE${jumpFlag}
                 |D;${type}
                 |@SP
                 |A=M-1
                 |M=-1
                 |@CONTINUE${jumpFlag}
                 |0;JMP
                 |(FALSE${jumpFlag})
                 |@SP
                 |A=M-1
                 |M=0
                 |(CONTINUE${jumpFlag})
                 |""".stripMargin()
    }

    // Template for unary operations like not, neg
    // @return the template for the unary operation
    private String unaryTemplate() {
        return """@SP
                 |A=M-1
                 |""".stripMargin()
    }

    // Template for push local, this, that, argument, temp, pointer, static
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
    // @param fileName is the name of the current file
    // @return the template for the push operation
    private String pushStatic(int index, String fileName) {
        return """@${fileName}.${index}
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

    // Template for pop local, this, that, argument, temp, pointer, static
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
    // @param fileName is the name of the current file
    // @return the template for the pop operation
    private String popStatic(int index, String fileName) {
        return popD() +
               """@${fileName}.${index}
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
