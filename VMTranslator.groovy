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
        // if a file was passed, change the extension to .asm, otherwise use the directory name
        def asmFileName = args[0].endsWith(".vm") ? args[0].replace(".vm", ".asm") : args[0] + ".asm"
        // create a file object for the .asm file we will write to
        def asmFile = new File(asmFileName)
        // create a CodeWriter object
        codeWriter = new CodeWriter(asmFile)
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
                    codeWriter.writePush(parser.arg1(), parser.arg2(), vmFile.name)
                    break
                case Parser.C_POP:
                    codeWriter.writePop(parser.arg1(), parser.arg2(), vmFile.name)
                    break
                case Parser.ERROR:
                    println "Error: Invalid command: ${parser.currentCommand}"
                    System.exit(1)
                    break
            }
        }
    }

}
