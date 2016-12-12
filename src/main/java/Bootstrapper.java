import controller.Server;
import controller.ConsoleSpammer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static model.Variables.CONFIG_FILENAME;
import static model.Variables.MCPAL_TAG;

public class Bootstrapper {

    public static String PARAMETER_BACKUP = "b:";
    public static String PARAMETER_RAM = "r:";
    public static String PARAMETER_JAR = "j:";

    private final Path fromPath;
    private String backupPath;
    private String maxHeapSize;
    private String jarName;
    private final Path worldName;
    private List<String> additionalPluginsToRunAfterBackup;

    public static void main(String[] args) throws URISyntaxException, IOException {
        final Bootstrapper main = new Bootstrapper(args);
        main.bootServer();
    }

    public Bootstrapper(String... args) throws IOException, URISyntaxException {
        fromPath = evaluateFromPath();

        if (args.length != 0) {
            final List<String> arguments = Arrays.asList(args);
            extractArgumentsFromCommandLine(arguments);
            writeConfigFile(fromPath, args);
        } else if (args.length == 0 && Files.exists(fromPath.resolve(CONFIG_FILENAME))) {
            final List<String> arguments = Files.readAllLines(fromPath.resolve(CONFIG_FILENAME));
            Files.delete(fromPath.resolve(CONFIG_FILENAME));
            extractArgumentsFromCommandLine(arguments);
        } else {
            throwInvalidStartArgumentsException();
        }

        worldName = searchWorldName(fromPath);

        checkEula(fromPath);
    }

    private Path evaluateFromPath() throws URISyntaxException {
        Path fromPath = Paths.get(Server.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        if (!Files.exists(fromPath)) throw new IllegalArgumentException("Couldn't find the Minecraft server file. " +
                "Please put MCpal into your Minecraft server directory.");
        return fromPath;
    }

    private void extractArgumentsFromCommandLine(List<String> arguments) {
        backupPath = extractSingleArgument(arguments, PARAMETER_BACKUP);
        maxHeapSize = extractSingleArgument(arguments, PARAMETER_RAM);
        jarName = extractSingleArgument(arguments, PARAMETER_JAR);
        if (isOneOfThemNull(backupPath, maxHeapSize, jarName)) throwInvalidStartArgumentsException();
        additionalPluginsToRunAfterBackup = extractAdditionalArguments(arguments);
    }

    private void bootServer() throws IOException {
        final Server MCpal = new Server(fromPath, backupPath, maxHeapSize, jarName, worldName, additionalPluginsToRunAfterBackup);
        MCpal.start();
    }

    private static boolean isOneOfThemNull(Object... objects) {
        for (Object object : objects) {
            if (object == null) return true;
        }
        return false;
    }

    private static List<String> extractAdditionalArguments(List<String> arguments) {
        return arguments.stream()
                .filter(a -> a.startsWith("a:"))
                .collect(Collectors.toList());
    }

    private static String extractSingleArgument(List<String> arguments, String argumentPrefix) {
        return arguments.stream()
                .filter(arg -> arg.startsWith(argumentPrefix))
                .findFirst()
                .map(arg -> arg.substring(2, arg.length()))
                .orElse(null);
    }

    private static Path searchWorldName(Path fromPath) {
        try {
            final DirectoryStream<Path> dirStream = Files.newDirectoryStream(fromPath);
            for (Path currentElement : dirStream) {
                if (Files.isDirectory(currentElement) && couldThisDirectoryPossiblyBeTheWorldFolder(currentElement)) {
                    return currentElement.getFileName();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(new ConsoleSpammer(MCPAL_TAG + "The world didn't exist when MCpal was started. Please " +
                "restart MCpal and it will handle that.")).start();
        return null;
    }

    private static void checkEula(Path fromPath) {
        try {
            final Path eulaPath = fromPath.resolve("eula.txt");
            final File eulaFile = eulaPath.toFile();
            if (Files.exists(eulaPath)) {
                final List<String> readAllLines = Files.readAllLines(eulaPath);
                final StringBuilder sb = new StringBuilder();
                readAllLines.forEach(line -> sb.append(line + System.getProperty("line.separator")));
                String eula = sb.toString();
                if (eula.contains("eula=false")) {
                    eula = eula.replace("eula=false", "eula=true");
                    FileWriter fw = new FileWriter(eulaFile);
                    fw.write(eula);
                    fw.flush();
                    fw.close();
                }
            } else {
                new Thread(new ConsoleSpammer(MCPAL_TAG + "NO EULA FOUND!! Just restart MCpal, the eula will be " +
                        "set to true automatically!")).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean couldThisDirectoryPossiblyBeTheWorldFolder(Path currentElement) {
        Path dim1File = currentElement.resolve("DIM1");
        return Files.exists(dim1File);
    }

    private static void throwInvalidStartArgumentsException() {
        throw new IllegalStateException("Invalid Input Parameters. Please start MCpal like this:\n" +
                "java -jar MCpal.jar b:PATH_TO_BACKUP_FOLDER r:MAX_RAM j:NAME_OF_MINECRAFT_SERVER_JAR\n" +
                "Example: java -jar MCpal.jar b:\"C:\\Users\\Rudolf Ramses\\Minecraft_Server\" r:1024 j:minecraft_server.jar");
    }

    private static void writeConfigFile(Path fromPath, String[] args) throws IOException {
        if (!Files.exists(fromPath.resolve(CONFIG_FILENAME)))
            Files.createFile(Paths.get(fromPath + "/" + CONFIG_FILENAME));
        final FileWriter fw = new FileWriter(fromPath + "/" + CONFIG_FILENAME);
        for (String parameter : args) {
            fw.write(parameter + System.getProperty("line.separator"));
        }
        fw.flush();
        fw.close();
    }

}
