package parse;

import java.util.*;
import java.util.function.Function;

public final class Dissectors {

    public static final Function<String, String[]> SEMICOLONS = s -> s.split(";");

    public static class PortionDissector implements Function<Portion, Portion[]> {

        private final List<String> splitters = new ArrayList<>();
        private final List<String> standalones = new ArrayList<>();

        public PortionDissector(Collection<String> splitters, Collection<String> standalones) {
            this.splitters.addAll(splitters);
            this.standalones.addAll(standalones);
        }

        @Override
        public Portion[] apply(Portion portion) {
            String text = portion.getText();
            List<Portion> portions = new ArrayList<>();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                String textAfterI = text.substring(i);
                Optional<String> matchedSplitter = splitters.stream().filter(textAfterI::startsWith).findFirst();
                if (matchedSplitter.isPresent()) {
                    String builderText = builder.toString();
                    if (!builderText.isEmpty()) {
                        portions.add(new Portion(matchedSplitter.get(), builder.toString()));
                    }
                    builder = new StringBuilder();
                    i += matchedSplitter.get().length() - 1;
                } else {
                    Optional<String> matchedStandalone = standalones.stream().filter(textAfterI::startsWith).findFirst();
                    if (matchedStandalone.isPresent()) {
                        String builderText = builder.toString();
                        if (!builderText.isEmpty()) {
                            portions.add(new Portion(null, builder.toString()));
                        }
                        portions.add(new Portion(matchedStandalone.get(), text.substring(i, i + matchedStandalone.get().length())));
                        i += matchedStandalone.get().length() - 1;
                        builder = new StringBuilder();
                    } else {
                        builder.append(text.charAt(i));
                    }
                }
            }
            if (builder.length() > 0) {
                portions.add(new Portion(null, builder.toString()));
            }
            System.out.println(portions);
            return portions.toArray(new Portion[0]);
        }

        public List<String> getSplitters() {
            return splitters;
        }

        public List<String> getStandalones() {
            return standalones;
        }

    }

}
