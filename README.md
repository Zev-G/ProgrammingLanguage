Example chess application:
```java
import java.util.stream.*;

import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

board = List.of("rnbqkbnr", "pppppppp", "", "", "", "", "RNBQKBNR", "PPPPPPPP");

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
    // This workaround is being used currently to get around an issue with varargs. This issue should be fixed in not too long and then we will be able to use Arrays.asList(...) instead here.
    line = Arrays.stream(board.get(y).split("")).collect(Collectors.toList());
    for (x = 0; x < 8; x++) {
        pane = new BorderPane();
        color;
        if ((x + y) % 2 == 0) {
            color = Color.web("#000000");
        } else {
            color = Color.web("#ffffff");
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
[![img.png](img.png)]