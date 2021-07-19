// import java.nio.file.*;
// import java.util.stream.*;
//
// scanner = new Scanner(System.in);
// registerInstanceMethodsOf(scanner);
//
// print("Enter the directory you would wish to use the contents of: ");
// filePath = nextLine();
// println("Looking for file...");
// loc = Path.of(filePath);
// println("Listing files in folder...");
// files = Files.list(loc).collect(Collectors.toList());
// println("Generating string...");
// playList = new StringBuilder();
// for (file : files) {
//     playList.append(file.toAbsolutePath().toString()).append("\n");
// }
// print("Would you like to print the string [Y/N] ");
// printString = nextLine();
// if (printString.equals("Y")) {
//     println(playList);
//     println(("\n" + ("-" * 20)) * 3);
// }
// print("Would you like to download the string [Y/N] ");
// downloadString = nextLine();
// if (downloadString.equals("Y")) {
//     print("Enter the file output location (i.e. C:/Users/output.txt) ");
//     outputLoc = Path.of(nextLine());
//     println("Checking if file exists...");
//     if (!Files.exists(outputLoc)) {
//         println("File doesn't exist, creating file...");
//         Files.createFile(outputLoc);
//     }
//     println("Writing to file...");
//     Files.writeString(outputLoc, playList.toString());
//     println("Successfully wrote string to file.");
// }
//
// println("Operation Over");
//
