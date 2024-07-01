//Nessya Nakache AG743463
//Elana Weiss 341390961

/**
 * The JackCompiler class tests the Jack to VM translation.
 */
class JackCompiler {

    static void main(String[] args) {
        // if the number of arguments is not 1, print usage and exit
        if (args.length != 1) {
            println "Usage: groovy JackCompiler.groovy <jack_file_or_directory>"
            System.exit(1)
        }
        // create a list of .jack files to compile
        def jackFiles = []
        // if the argument is a file, create a list with that file
        if (args[0].endsWith(".jack")) {
            jackFiles = [new File(args[0])]
        }
        // if the argument is a directory, create a list with all .jack files in that directory
        else {
            // open the directory as dir
            def dir = new File(args[0])
            // listFiles() returns null if the directory does not exist
            if (!dir.exists()) {
                println "Directory not found: $args[0]"
                System.exit(1)
            }
            // filter the list of files to keep only the .jack files
            jackFiles = dir.listFiles().findAll { it.name.endsWith(".jack") }
        }
        // create a CompilationEngine object
        def compilationEngine = new CompilationEngine(System.out)
        // for each .jack file, compile it to .vm
        for (jackFile in jackFiles) {
            println "Compiling: $jackFile"
            // compile the .jack file to .vm
            compilationEngine.compile(jackFile)
        }
    }
}