//Nessya Nakache AG743463
//Elana Weiss 341390961

// Groovy program for VM translator

/**
 * The VMTranslator class is responsible for translating VM commands into Hack assembly code.
 * Run the program with the name of a .vm file or a directory containing .vm files as an argument.
 */
class VMTranslator {

    static CodeWriter codeWriter

    static void main(String[] args) {
        // if the number of arguments is not 1, print usage and exit
        if (args.length != 1) {
            println "Usage: groovy VMTranslator.groovy <vm_file_or_directory>"
            System.exit(1)
        }
        // create a list of .vm files to translate
        def vmFiles = []
        // if the argument is a file, create a list with that file
        if (args[0].endsWith(".vm")) {
            vmFiles = [new File(args[0])]
        }
        // if the argument is a directory, create a list with all .vm files in that directory
        else {
            // open the directory as dir
            def dir = new File(args[0])
            // listFiles() returns null if the directory does not exist
            if (!dir.exists()) {
                println "Directory not found: $args[0]"
                System.exit(1)
            }
            // filter the list of files to keep only the .vm files
            vmFiles = dir.listFiles().findAll { it.name.endsWith(".vm") }
        }
        // create the file name for the .asm file based on the argument
        def asmFileName = ""
        // if a .vm file was passed, change the extension to .asm
        if (args[0].endsWith(".vm")) {
            asmFileName = args[0].replace(".vm", ".asm")
        }
        // otherwise, if a directory was passed, add .asm to the directory name and put it in the directory
        else {
            // get the base name of the directory without the full path
            def basename = new File(args[0]).name
        
            // build the path to the .asm by combinining the directory name and the base name followed by .asm
            asmFileName = new File(args[0], "${basename}.asm").toString()
        }
        // print the output file name
        println "Output: $asmFileName"
        // create a file object for the .asm file we will write to
        def asmFile = new File(asmFileName)
        // create a CodeWriter object
        codeWriter = new CodeWriter(asmFile)
        // write the bootstrap code to the .asm file
        codeWriter.writeInit()
        // if there is a file named Sys.vm in the directory, call Sys.init
        if (vmFiles.any { it.name == "Sys.vm" }) {
            codeWriter.writeCall("Sys.init", 0)
        }
        // translate each .vm file
        for (File vmFile : vmFiles) {
            translate(vmFile)
        }
    }

    static void translate(File vmFile) {
        // create a Parser object for the .vm file
        def parser = new Parser(vmFile)
        // tell the code writer which file we are translating
        codeWriter.setFileName(vmFile.name)
        // while there are more commands in the file, advance the parser
        while (parser.hasMoreCommands()) {
            parser.advance()
            // write the current command as a comment if includeComments is true
            codeWriter.writeComment(parser.currentCommand)
            // write the command to the .asm file based on its command type
            switch (parser.commandType()) {
                case Parser.C_ARITHMETIC:
                    codeWriter.writeArithmetic(parser.arg1())
                    break
                case Parser.C_PUSH:
                    codeWriter.writePush(parser.arg1(), parser.arg2())
                    break
                case Parser.C_POP:
                    codeWriter.writePop(parser.arg1(), parser.arg2())
                    break
                case Parser.C_LABEL:
                    codeWriter.writeLabel(parser.arg1())
                    break
                case Parser.C_GOTO:
                    codeWriter.writeGoTo(parser.arg1())
                    break
                case Parser.C_IF:
                    codeWriter.writeIfGoTo(parser.arg1())
                    break
                case Parser.C_FUNCTION:
                    codeWriter.writeFunction(parser.arg1(), parser.arg2())
                    break
                case Parser.C_RETURN:
                    codeWriter.writeReturn()
                    break
                case Parser.C_CALL:
                    codeWriter.writeCall(parser.arg1(), parser.arg2())
                    break
                case Parser.ERROR:
                    println "Error: Unrecognized command: ${parser.currentCommand}"
                    System.exit(1)
                    break
                default:
                    println "Error: Unhandled command type: ${parser.commandType()}"
                    System.exit(1)
                    break
            }
        }
    }

}
