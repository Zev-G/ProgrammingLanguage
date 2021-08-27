import java.io.*;

println(new IOException());

// println("How many time do you want this to run?");
// times = Integer.parseInt(new Scanner(System.in).nextLine());
//
// map = new HashMap();
//
// println(compute(times));

// for (i : range(0, times+1)) {
//     map.put(i, compute(i));
// }
//
// for (entry : map.entrySet()) {
//     println(entry.getKey() + " = " + entry.getValue());
// }

// compute(numTimes) {
//     nums = [1];
//     total = 1;
//     for (i : range(0, numTimes)) {
//         nums.add(total);
//         stringTotal = String.valueOf(Math.floor(total));
//         preTotal = total;
//
//         broke = false;
//         for (char : stringTotal.split("")) {
//             if (!broke) {
//                 if (char.equals(".")) { broke = true; };
//                 else { total += Integer.parseInt(char); }
//             }
//         }
//     }
//
//     text = "";
//     for (num : nums) {
//         text = text + String.valueOf(num.intValue());
//     }
//     return text;
// }

// reverse(text) {
//     array = text.split("");
//     reversed = "";
//     for (i in range(0, array.length)) {
//         reversed = array[i] + reversed;
//     }
//     return reversed;
// }
//
// scanner = new Scanner(System.in);
//
// println("Type some text and it will be reversed. Type \"exit\" to stop.");
//
// input = scanner.nextLine();
// while (!input.equalsIgnoreCase("exit")) {
//     println(reverse(input));
//     input = scanner.nextLine();
// }

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
