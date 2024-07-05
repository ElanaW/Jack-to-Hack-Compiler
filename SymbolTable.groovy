//Nessya Nakache AG743463
//Elana Weiss 341390961

/**
 * The SymbolTable class is responsible for managing the symbol table for classes and subroutines.
 */
class SymbolTable {

    private Map<String, Symbol> classTable
    private Map<String, Symbol> subroutineTable

    SymbolTable() {
        // initialize the class and subroutine symbol tables with empty maps
        classTable = [:]
        subroutineTable = [:]
    }

    // start a new subroutine scope
    void startSubroutine() {
        // clear the subroutine symbol table
        subroutineTable.clear()
    }

    // start a new class scope
    void startClass() {
        // clear the class symbol table
        classTable.clear()
    }

    // add a new symbol to the class symbol table
    void define(String name, String type, String kind) {
        // if the kind is "field", change it to "this"
        if (kind == "field") {
            kind = "this"
        }
        // get the index of the new symbol by calling varCount with the given kind
        Integer index = varCount(kind)
        // if the kind is "static" or "this", add the symbol to the class symbol table
        if (kind == "static" || kind == "this") {
            classTable[name] = new Symbol(name, type, kind, index)
        }
        // if the kind is "argument" or "local", add the symbol to the subroutine symbol table
        else {
            subroutineTable[name] = new Symbol(name, type, kind, index)
        }
    }

    // get the number of variables of the given kind in the current scope
    int varCount(String kind) {
        // if the kind is "field" or "this", count from the class symbol table
        if (kind == "static" || kind == "this") {
            // count the number of symbols in the class symbol table with the given kind
            return classTable.findAll { it.value.getKind() == kind }.size()
        }
        // if the kind is "argument" or "local", count from the subroutine symbol table
        else if (kind == "argument" || kind == "local") {
            // count the number of symbols in the subroutine symbol table with the given kind
            return subroutineTable.findAll { it.value.getKind() == kind }.size()
        }
        // otherwise, throw an exception
        else {
            throw new IllegalArgumentException("Invalid kind: $kind")
        }
    }

    // get the kind of the symbol with the given name
    // @param name the name of the symbol
    // @return the kind of the symbol, or null if the symbol is not defined
    String kindOf(String name) {
        try {
            return getSymbol(name).getKind()
        } catch (IllegalArgumentException e) {
            return null
        }
    }

    // get the type of the symbol with the given name
    // @param name the name of the symbol
    // @return the type of the symbol, or null if the symbol is not defined
    String typeOf(String name) {
        try {
            return getSymbol(name).getType()
        } catch (IllegalArgumentException e) {
            return null
        }
    }

    // get the index of the symbol with the given name
    // @param name the name of the symbol
    // @return the index of the symbol
    int indexOf(String name) {
        try {
            return getSymbol(name).getIndex()
        } catch (IllegalArgumentException e) {
            return null
        }
    }

    // get the symbol with the given name from the symbol table
    private Symbol getSymbol(String name) {
        // if the symbol is in the subroutine symbol table, return it
        if (subroutineTable.containsKey(name)) {
            return subroutineTable[name]
        }
        // if the symbol is in the class symbol table, return it
        else if (classTable.containsKey(name)) {
            return classTable[name]
        }
        // otherwise, throw an exception
        else {
            throw new IllegalArgumentException("Undefined symbol: $name")
        }
    }
}