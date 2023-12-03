
# The Language

This programming language (which I still haven't came up with a name for) is meant to serve as a way to quickly write simple java programs.

## (Possible) Drawbacks

Although Object Oriented Programming features are on the way the language is not designed with them as a driving idea.  

The language is not highly optimized and is executed within java (as opposed to being compiled to byte-code), this makes it less performant.  

Types can't be specified.  

The language has not been thoroughly tested and may be unreliable for important projects.  

# Syntax

This language's syntax isn't exactly the same as any other language but should be pretty intuitive for seasoned programmers.

## Structure

Structurally this language is quite similar to C or Java. Semicolons separate lines and brackets are used for the body of statements and methods.

Example:
```java
x = 0;
x = 100;
if (x < 50) {
    x = 50;
}
```

## Comments

There are two types of comments with support in the language, single-line comments, and multi-line comments. Single-line comments are declared with two forward slashes "//" and all text after the slashes on that line is ignored. Multi-line comments start with "/*" and end with "\*/", all text between the start and end is ignored. 

Example:
```java
// This is a comment.

/*
This is also a comment
*/
```

## Variables

Variables can be declared and accessed at any time.

Example:
```java
z; // Declared
x = 0;
y = 100;
z = y + x;
```

Variables can be accessed from any earlier scopes.
Example:
```java
x = 0;
z;
if (true) {
    x = 100;
    y = 10;
    z = 50;
}
println(x);
println(y);
println(z);
```
Output:
```
100
null
50
```

Variables can be assigned any value, regardless of type.
Example:
```java
x = 0;
println(x);
x = true;
println(x);
x = "hi";
println(x);
```
Output:
```
0
true
hi
```

## Statements

### If, Else, and Else-If Statements

If statements are the text "if " followed by a boolean statement, parentheses are optional.

Example:
```java
if true {
    println("A");
}
if (true) {
    println("B");
}
```
Output:
```
A
B
```

Else statements must follow either an else-if or if statement and will run only if the statement they follow doesn't. 
Else statements are just the text "else".

Example: 
```java
if (false) {
    println("A");
} else {
    println("B");
}
if (true) {
    println("C");
} else {
    println("D");
}
```
Output:
```
B
C
```

Else-if statements must follow either an else-if statement or an if statement and will only run if their boolean is true and if the statement they follow doesn't run. 
Parentheses are optional.

Example:
```java
if (false) {
    println("A");
} else if (true) {
    println("B");
}
if (true) {
    println("C");
} else if (true) {
    println("D");
}
if (true) {
    println("E");
} else if (false) {
    println("F");
}
```
Output:
```
B
C
E
```

### For loops

For loops have three pieces of code they run, each separated by semicolons. The first piece of code runs before the loop runs for the first time. The second piece of code runs before each loop, and the loop only continues if it returns true. The third piece of code runs after every loop.
Parentheses are **not** optional.

Example:
```java
for (i = 0; i < 5; i++) {
    println(i);
}
```
Output:
```
0
1
2
3
4
```

### For each loops

For each loops iterate over an iterable object's values, each time updating a variable's value. A second variable can be specified to track the number of times the loop has ran. An iterable object is either: a java [Iterable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Iterable.html) object, or an array. The variable(s) should be separated from the iterable object by either the text ":" or "in".  

Example:
```java
array = [];  
array[0] = "Banana";  
array[1] = "Apple";  
array[2] = "Chicken";

for (item : array) {
	println(item);
}
```
Output:
```
Banana
Apple
Chicken
```

The easiest and fastest way to iterate over numbers is to use the `range(start, end)` function.  

Example:
```java
for (i : range(0, 10)) {
	print(i);
}
```
Output:
```
0123456789
```

### While loops

While loops run repeatedly until their boolean statement returns false.

Example:
```java
text = "CURRENT";
i = 0;
while (text.equals("CURRENT")) {
    i++;
    println(text + " " + i);
    if (i > 3) {
        text = "";
    }
}
```
Output:
```
CURRENT 1
CURRENT 2
CURRENT 3
CURRENT 4
```

## Functions

### Declaring functions.

Functions are declared by writing the name of the function, followed by parentheses containing the arguments separated by commas.

Example:
```java
squared(x) {
    return x * x;
}
join(x, y) {
    return x + " " + y;
}
```

### Calling functions

To call a function write its name then parentheses containing the arguments separated by commas.
```java
x = squared(3);
y = join("hello", "world");
```

## Accessing java classes, methods, and fields.

> Inner classes cannot be accessed.

### Imports

Packages `java.lang` and `java.util` are imported automatically, all other packages must be imported manually. 
Imports must go at the beginning of the file. 
You can only import packages and classes. 

Imports are written as the text "import " followed by the package, then either a particular public class in the package or all classes in the package (*).

Example:
```java
import java.io.File;
import java.nio.files.*;
```

### Methods

An object's methods can be ran by writing a period after the object, then the name of the method, then, in parentheses, the arguments separated by commas.

Example:
```java
println("hi".toUpperCase());
println("hi ".repeat(3));
```
Output:
```
HI
hi hi hi
```

Static methods of classes can be ran by writing the class' name in substitution for the object in the above syntax.

Example:
```java
println(Math.random());
println("Since this is printing a random number the output won't often be the same.");
```
Output:
```
0.7958408649703334
Since this is printing a random number the output won't often be the same.
```

Notes:  

Varargs are handled automatically.  
The superclass methods of an object are searched recursively until one which can be accessed is found.  
All numbers are doubles in the language. Numbers are casted to the type specified by the method.  

### Constructors

Constructors can be ran by writing the keyword "new" followed by the name of the class then the arguments in parentheses separated by commas.

Example:
```java
x = new ArrayList();
```

### Fields

An object's fields can be accessed by writing a period after the object, followed by the name of the field.

Example:
```java
println(new FakeClass().text);
```

Static fields of classes can be accessed by writing the name of the class in substitution for the object in the above syntax.

Example:
```java
println(Integer.MAX_VALUE);
```
Output:
```
2147483647
```

The length of an array can be accessed by writing a period after the array, followed by the text "length"

Example:
```java
array = "text".split("");
println(array.length);
```
Output:
```
4
```

## Defining classes 

All files in the language are a class, as of now there is no way to specify the accessibility of a class.  

## Defining fields and methods

To define a field write a variable with either an accessibility modifier or static modifier in front of it. The modifier must be written at the root context.

Example:
```java
x = 0; // Not a field.
if (true) {
	private err = "won't work"; // This code won't work.
}
// Valid fields:
static y = 100;
private z = "test";
public static oragne = "#f59e42";
```

Defining methods is the same except instead of variable you're writing functions.  

Example:
```java
// Valid:
static test() { return "test"; }
// Valid:
package test() { return "test"; }
// Valid:
public static test() { return "test"; }
// Invalid:
test() { return "test"; }
// Invalid:
if (true) {
	private test() { return "test"; }
}
```

# Examples

Example chess application:
```java
import java.util.stream.*;

import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

board = List.of("rnbqkbnr", "pppppppp", "", "", "", "", "PPPPPPPP", "RNBQKBNR");

expandName(name) {
    name = name.toLowerCase();
    if (name.equals("r")) {
        return "Rook";
    }
    if (name.equals("n")) {
        return "Knight";
    }
    if (name.equals("b")) {
        return "Bishop";
    }
    if (name.equals("q")) {
        return "Queen";
    }
    if (name.equals("k")) {
        return "King";
    }
    if (name.equals("p")) {
        return "Pawn";
    }
    println("Invalid input: " + name);
    return "";
}

stage = new Stage();
grid = new GridPane();
for (y = 0; y < 8; y++) {
    line = Arrays.asList(board.get(y).split(""));
    for (x = 0; x < 8; x++) {
        pane = new BorderPane();
        color;
        if ((x + y) % 2 == 0) {
            color = Color.BLACK;
        } else {
            color = Color.WHITE;
        }
        if (x < line.size()) {
            label = new Label(expandName(line.get(x)));
            label.setTextFill(color.invert());
            pane = new BorderPane(label);
        }
        pane.setMinSize(100, 100);
        pane.setBackground(new Background(new BackgroundFill(color, new CornerRadii(0), new Insets(0))));
        grid.add(pane, x, y);
    }
}
stage.setScene(new Scene(grid));
stage.show();

```
This application requires the code to be run from within an already running JavaFX environment. 
Result:
![](https://i.imgur.com/DH3AJNJ.png)
