package edu.kit.kastel.sdq.lissa.ratlr.utils.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.kit.kastel.sdq.lissa.ratlr.artifactprovider.CodeGraphProvider;
import edu.kit.kastel.sdq.lissa.ratlr.context.CodeGraph;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringJoiner;

public class CodeTools {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ContextStore contextStore;

    public CodeTools(ContextStore contextStore) {
        this.contextStore = contextStore;
    }
    
    @Tool("Returns all names of files contained in the specified package and subpackages.")
    public String getFileNamesInPackageWithSubpackages(
            @P("The name of the package to retrieve all file names from.") String packageName) {
        return null;
    }

    @Tool("Returns all names of files in the root directory of the project.")
    public String getAllFileNamesInRootDirectory() throws IOException {
        return getAllFileNames(getRoot());
    }
    
    private String getAllFileNames(Path path) throws IOException {
        StringJoiner joiner = new StringJoiner("\n");
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            for (Path directoryEntry : directoryStream) {
                if (Files.isRegularFile(directoryEntry)) {
                    joiner.add(directoryEntry.relativize(path).toString().replace("\\", "/"));
                }
            }
        }
        return joiner.toString();
    }
    
    @Tool("Returns the content of a file.")
    public String getContentOfFile(
            @P("The path to the file to retrieve the content from.") String pathToFile) throws IOException {
        return Files.readString(getRoot().resolve(pathToFile));
    }
    
    private Path getRoot() {
        return contextStore.getContext(CodeGraphProvider.CONTEXT_IDENTIFIER, CodeGraph.class).getCodeRoot();
    }
}
